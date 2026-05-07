package com.example.fruitylicious.ui.admin.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitylicious.data.local.dao.BranchDao
import com.example.fruitylicious.data.local.dao.RestockFrequencyRow
import com.example.fruitylicious.data.local.dao.RestockLogDao
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

data class RestockReportUiState(
    val totalToday: Double = 0.0,
    val frequencyItems: List<RestockFrequencyRow> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val currentPage: Int = 0,
    val hasMore: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class RestockReportViewModel @Inject constructor(
    private val restockLogDao: RestockLogDao,
    private val branchDao: BranchDao,
    private val reportRepository: ReportRepository,
    private val networkMonitor: NetworkMonitor,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(RestockReportUiState())
    val uiState: StateFlow<RestockReportUiState> = _uiState.asStateFlow()

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
                    frequencyItems = emptyList()
                )
            }

            val localBranchId = sessionManager.getBranchId()
            val isOnline = networkMonitor.isOnline()
            val isAdmin = isAdminUser()

            val range = getMonthlyRange()

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

            val range = getMonthlyRange()
            val nextPage = state.currentPage + 1

            val result = reportRepository.getRestockPage(
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
                        .map { (ingredientName, groupedItems) ->
                            val totalQuantity = groupedItems.sumOf { it.quantityAdded }
                            val count = groupedItems.size

                            RestockFrequencyRow(
                                ingredientName = ingredientName,
                                restockCount = count,
                                avgUnits = if (count > 0) totalQuantity / count.toDouble() else 0.0
                            )
                        }

                    _uiState.update {
                        it.copy(
                            frequencyItems = it.frequencyItems + newRows,
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
            val totalToday = restockLogDao.getTotalRestockedUnits(
                branchId = branchId,
                from = from,
                to = to
            )

            val frequencyItems = restockLogDao.getRestockFrequencyReport(
                branchId = branchId
            )

            _uiState.update {
                it.copy(
                    totalToday = totalToday,
                    frequencyItems = frequencyItems,
                    isLoading = false,
                    hasMore = false,
                    error = null
                )
            }
        } catch (exception: Exception) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = exception.message ?: "Failed to load local restock report."
                )
            }
        }
    }

    private suspend fun loadRemoteReport(
        branchId: Int?,
        from: Long,
        to: Long
    ) {
        val summaryResult = reportRepository.getRestockSummary(
            branchId = branchId,
            from = from,
            to = to
        )

        val pageResult = reportRepository.getRestockPage(
            branchId = branchId,
            from = from,
            to = to,
            page = 0,
            size = PAGE_SIZE
        )

        summaryResult.fold(
            onSuccess = { summary ->
                _uiState.update {
                    it.copy(totalToday = summary.totalRestockQuantity)
                }
            },
            onFailure = { error ->
                _uiState.update { it.copy(error = error.message) }
            }
        )

        pageResult.fold(
            onSuccess = { pageResponse ->
                val frequencyRows = pageResponse.items
                    .groupBy { it.ingredientName }
                    .map { (ingredientName, groupedItems) ->
                        val totalQuantity = groupedItems.sumOf { it.quantityAdded }
                        val count = groupedItems.size

                        RestockFrequencyRow(
                            ingredientName = ingredientName,
                            restockCount = count,
                            avgUnits = if (count > 0) totalQuantity / count.toDouble() else 0.0
                        )
                    }
                    .sortedByDescending { it.restockCount }

                _uiState.update {
                    it.copy(
                        frequencyItems = frequencyRows,
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

    private fun getMonthlyRange(): Pair<Long, Long> {
        val start = Calendar.getInstance()
        start.set(Calendar.DAY_OF_MONTH, 1)
        start.set(Calendar.HOUR_OF_DAY, 0)
        start.set(Calendar.MINUTE, 0)
        start.set(Calendar.SECOND, 0)
        start.set(Calendar.MILLISECOND, 0)
        return start.timeInMillis to System.currentTimeMillis()
    }

    private fun isAdminUser(): Boolean {
        return sessionManager.isAdmin()
    }
}
