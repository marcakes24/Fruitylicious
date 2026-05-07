package com.example.fruitylicious.ui.admin.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitylicious.data.local.dao.BranchDao
import com.example.fruitylicious.data.local.dao.WasteItemRow
import com.example.fruitylicious.data.local.dao.WasteLogDao
import com.example.fruitylicious.data.local.dao.WasteReasonRow
import com.example.fruitylicious.data.repository.ReportRepository
import com.example.fruitylicious.util.NetworkMonitor
import com.example.fruitylicious.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WasteReportUiState(
    val totalWaste: Double = 0.0,
    val mostWasted: String = "—",
    val mostWastedQty: Double = 0.0,
    val reasonData: List<WasteReasonRow> = emptyList(),
    val wasteByItem: List<WasteItemRow> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val currentPage: Int = 0,
    val hasMore: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class WasteReportViewModel @Inject constructor(
    private val wasteLogDao: WasteLogDao,
    private val branchDao: BranchDao,
    private val reportRepository: ReportRepository,
    private val networkMonitor: NetworkMonitor,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(WasteReportUiState())
    val uiState: StateFlow<WasteReportUiState> = _uiState.asStateFlow()

    private val PAGE_SIZE = 50

    fun loadReport(
        branchId: Int?
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    currentPage = 0,
                    hasMore = false,
                    wasteByItem = emptyList()
                )
            }

            val localBranchId = sessionManager.getBranchId()
            val isOnline = networkMonitor.isOnline()
            val isAdmin = isAdminUser()
            val range = getCurrentWeekRange()

            when {
                branchId == localBranchId -> {
                    loadLocalReport(
                        branchId = localBranchId,
                        from = range.first,
                        to = range.second
                    )
                }

                !isAdmin -> {
                    loadLocalReport(
                        branchId = localBranchId,
                        from = range.first,
                        to = range.second
                    )
                }

                !isOnline -> {
                    loadLocalReport(
                        branchId = localBranchId,
                        from = range.first,
                        to = range.second
                    )
                }

                else -> {
                    loadRemoteReport(
                        branchId = branchId,
                        from = range.first,
                        to = range.second
                    )
                }
            }
        }
    }

    fun loadMore(branchId: Int?) {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMore) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }

            val range = getCurrentWeekRange()
            val nextPage = state.currentPage + 1

            val result = reportRepository.getWastePage(
                branchId = branchId,
                from = range.first,
                to = range.second,
                page = nextPage,
                size = PAGE_SIZE
            )

            result.fold(
                onSuccess = { pageResponse ->
                    val newRows = pageResponse.items
                        .groupBy { it.ingredientName }
                        .map { (ingredientName, items) ->
                            WasteItemRow(
                                ingredientName = ingredientName,
                                totalQuantity = items.sumOf { it.quantity }
                            )
                        }
                    
                    // Note: Simple grouping on client side for now as the items are paginated.
                    // Ideal would be if the backend provided paginated grouped data.
                    
                    _uiState.update {
                        it.copy(
                            wasteByItem = it.wasteByItem + newRows,
                            currentPage = nextPage,
                            hasMore = pageResponse.hasNext,
                            isLoadingMore = false
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingMore = false,
                            error = error.message
                        )
                    }
                }
            )
        }
    }

    private suspend fun loadLocalReport(
        branchId: Int,
        from: Long,
        to: Long
    ) {
        try {
            val totalWaste = wasteLogDao.getTotalWasteQuantity(
                branchId = branchId,
                from = from,
                to = to
            )

            val reasonData = wasteLogDao.getWasteReasonReport(
                branchId = branchId,
                from = from,
                to = to
            )

            val wasteByItem = wasteLogDao.getWasteByItemReport(
                branchId = branchId,
                from = from,
                to = to
            )

            val topItem = wasteByItem.maxByOrNull {
                it.totalQuantity
            }

            _uiState.update {
                it.copy(
                    totalWaste = totalWaste,
                    mostWasted = topItem?.ingredientName ?: "—",
                    mostWastedQty = topItem?.totalQuantity ?: 0.0,
                    reasonData = reasonData,
                    wasteByItem = wasteByItem,
                    isLoading = false,
                    hasMore = false,
                    error = null
                )
            }
        } catch (exception: Exception) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = exception.message ?: "Failed to load local waste report."
                )
            }
        }
    }

    private suspend fun loadRemoteReport(
        branchId: Int?,
        from: Long,
        to: Long
    ) {
        val summaryResult = reportRepository.getWasteSummary(
            branchId = branchId,
            from = from,
            to = to
        )

        val pageResult = reportRepository.getWastePage(
            branchId = branchId,
            from = from,
            to = to,
            page = 0,
            size = PAGE_SIZE
        )

        summaryResult.fold(
            onSuccess = { summary ->
                _uiState.update {
                    it.copy(
                        totalWaste = summary.totalWasteQuantity,
                        mostWasted = summary.topWastedIngredient ?: "—"
                    )
                }
            },
            onFailure = { error ->
                _uiState.update { it.copy(error = error.message) }
            }
        )

        pageResult.fold(
            onSuccess = { pageResponse ->
                val itemRows = pageResponse.items
                    .groupBy { it.ingredientName }
                    .map { (ingredientName, groupedItems) ->
                        WasteItemRow(
                            ingredientName = ingredientName,
                            totalQuantity = groupedItems.sumOf { it.quantity }
                        )
                    }
                    .sortedByDescending { it.totalQuantity }

                val reasonRows = pageResponse.items
                    .groupBy { it.reason.ifBlank { "Unspecified" } }
                    .map { (reason, groupedItems) ->
                        WasteReasonRow(
                            reason = reason,
                            count = groupedItems.size
                        )
                    }
                    .sortedByDescending { it.count }

                _uiState.update {
                    it.copy(
                        wasteByItem = itemRows,
                        reasonData = reasonRows,
                        isLoading = false,
                        currentPage = 0,
                        hasMore = pageResponse.hasNext
                    )
                }
            },
            onFailure = { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = it.error ?: error.message
                    )
                }
            }
        )
    }

    private fun getCurrentWeekRange(): Pair<Long, Long> {
        val start = Calendar.getInstance()

        while (start.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            start.add(Calendar.DAY_OF_YEAR, -1)
        }

        start.set(Calendar.HOUR_OF_DAY, 0)
        start.set(Calendar.MINUTE, 0)
        start.set(Calendar.SECOND, 0)
        start.set(Calendar.MILLISECOND, 0)

        val end = System.currentTimeMillis()

        return start.timeInMillis to end
    }

    private fun isAdminUser(): Boolean {
        return sessionManager.isAdmin()
    }
}