package com.example.fruitylicious.ui.admin.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitylicious.data.local.dao.RestockFrequencyRow
import com.example.fruitylicious.data.local.dao.RestockLogDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class RestockReportUiState(
    val totalToday: Double = 0.0,
    val frequencyItems: List<RestockFrequencyRow> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class RestockReportViewModel @Inject constructor(
    private val restockLogDao: RestockLogDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(RestockReportUiState())
    val uiState: StateFlow<RestockReportUiState> = _uiState.asStateFlow()

    fun loadReport(branch: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, error = null)
            }

            val branchId = when (branch) {
                "B1" -> 1
                "B2" -> 2
                else -> null
            }

            val todayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val now = System.currentTimeMillis()

            try {
                val totalToday = restockLogDao.getTotalRestockedUnits(
                    branchId = branchId,
                    from = todayStart,
                    to = now
                )

                val frequencyItems = restockLogDao.getRestockFrequencyReport(
                    branchId = branchId
                )

                _uiState.update {
                    it.copy(
                        totalToday = totalToday,
                        frequencyItems = frequencyItems,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load restock report."
                    )
                }
            }
        }
    }
}