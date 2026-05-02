package com.example.fruitylicious.ui.admin.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitylicious.data.local.dao.RestockFrequencyRow
import com.example.fruitylicious.data.local.dao.RestockLogDao
import com.example.fruitylicious.data.repository.ReportRepository
import com.example.fruitylicious.util.NetworkMonitor
import com.example.fruitylicious.util.SessionManager
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
    private val restockLogDao: RestockLogDao,
    private val reportRepository: ReportRepository,
    private val networkMonitor: NetworkMonitor,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(RestockReportUiState())
    val uiState: StateFlow<RestockReportUiState> = _uiState.asStateFlow()

    fun loadReport(branchId: Int?) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, error = null)
            }

            val localBranchId = sessionManager.getBranchId()
            val isOnline = networkMonitor.isOnline()

            val todayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val now = System.currentTimeMillis()

            if (branchId == localBranchId || !isOnline) {
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
                            error = e.message ?: "Failed to load local restock report."
                        )
                    }
                }
            } else {
                // Load Remote
                try {
                    val result = reportRepository.getRestockReport(branchId ?: 0, todayStart, now)

                    result.onSuccess { reportDto ->
                        // Map RestockReportDto to UI State
                        val freqRows = reportDto.items.groupBy { it.ingredientName }
                            .map { (name, items) ->
                                RestockFrequencyRow(
                                    ingredientName = name,
                                    restockCount = items.size,
                                    avgUnits = items.sumOf { it.quantityAdded } / items.size.toDouble()
                                )
                            }.sortedByDescending { it.restockCount }

                        _uiState.update {
                            it.copy(
                                totalToday = reportDto.totalRestockQuantity,
                                frequencyItems = freqRows,
                                isLoading = false,
                                error = null
                            )
                        }
                    }.onFailure { e ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = e.message ?: "Failed to load remote restock report."
                            )
                        }
                    }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Failed to load remote restock report."
                        )
                    }
                }
            }
        }
    }
}