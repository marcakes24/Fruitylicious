package com.example.fruitylicious.ui.admin.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitylicious.data.local.dao.WasteItemRow
import com.example.fruitylicious.data.local.dao.WasteLogDao
import com.example.fruitylicious.data.local.dao.WasteReasonRow
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

data class WasteReportUiState(
    val totalWaste: Double = 0.0,
    val mostWasted: String = "—",
    val mostWastedQty: Double = 0.0,
    val reasonData: List<WasteReasonRow> = emptyList(),
    val wasteByItem: List<WasteItemRow> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class WasteReportViewModel @Inject constructor(
    private val wasteLogDao: WasteLogDao,
    private val reportRepository: ReportRepository,
    private val networkMonitor: NetworkMonitor,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(WasteReportUiState())
    val uiState: StateFlow<WasteReportUiState> = _uiState.asStateFlow()

    fun loadReport(branchId: Int?) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, error = null)
            }

            val localBranchId = sessionManager.getBranchId()
            val isOnline = networkMonitor.isOnline()

            val weekStart = Calendar.getInstance().apply {
                while (get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
                    add(Calendar.DAY_OF_YEAR, -1)
                }

                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val now = System.currentTimeMillis()

            if (branchId == localBranchId || !isOnline) {
                try {
                    val totalWaste = wasteLogDao.getTotalWasteQuantity(
                        branchId = branchId,
                        from = weekStart,
                        to = now
                    )

                    val reasonData = wasteLogDao.getWasteReasonReport(
                        branchId = branchId,
                        from = weekStart,
                        to = now
                    )

                    val wasteByItem = wasteLogDao.getWasteByItemReport(
                        branchId = branchId,
                        from = weekStart,
                        to = now
                    )

                    val topItem = wasteByItem.maxByOrNull { it.totalQuantity }

                    _uiState.update {
                        it.copy(
                            totalWaste = totalWaste,
                            mostWasted = topItem?.ingredientName ?: "—",
                            mostWastedQty = topItem?.totalQuantity ?: 0.0,
                            reasonData = reasonData,
                            wasteByItem = wasteByItem,
                            isLoading = false,
                            error = null
                        )
                    }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Failed to load local waste report."
                        )
                    }
                }
            } else {
                // Load Remote
                try {
                    // Assuming branchId could be null for "All"
                    // If backend doesn't have a specific combined waste endpoint,
                    // we might need one or handle it in getWasteReport(null, ...)
                    val result = reportRepository.getWasteReport(branchId ?: 0, weekStart, now)

                    result.onSuccess { reportDto ->
                        // Map WasteReportDto to UI State
                        val reasonRows = reportDto.items.groupBy { it.reason }
                            .map { (reason, items) ->
                                WasteReasonRow(
                                    reason = reason,
                                    count = items.size
                                )
                            }.sortedByDescending { it.count }

                        val itemRows = reportDto.items.groupBy { it.ingredientName }
                            .map { (name, items) ->
                                WasteItemRow(
                                    ingredientName = name,
                                    totalQuantity = items.sumOf { it.quantity }
                                )
                            }.sortedByDescending { it.totalQuantity }

                        val topItem = itemRows.maxByOrNull { it.totalQuantity }

                        _uiState.update {
                            it.copy(
                                totalWaste = reportDto.totalWasteQuantity,
                                mostWasted = topItem?.ingredientName ?: "—",
                                mostWastedQty = topItem?.totalQuantity ?: 0.0,
                                reasonData = reasonRows,
                                wasteByItem = itemRows,
                                isLoading = false,
                                error = null
                            )
                        }
                    }.onFailure { e ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = e.message ?: "Failed to load remote waste report."
                            )
                        }
                    }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Failed to load remote waste report."
                        )
                    }
                }
            }
        }
    }
}