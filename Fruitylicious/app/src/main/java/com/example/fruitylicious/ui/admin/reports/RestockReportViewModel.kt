package com.example.fruitylicious.ui.admin.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitylicious.data.local.dao.BranchDao
import com.example.fruitylicious.data.local.dao.RestockFrequencyRow
import com.example.fruitylicious.data.local.dao.RestockIngredientRow
import com.example.fruitylicious.data.local.dao.RestockLogDao
import com.example.fruitylicious.data.local.dao.RestockUnitTotal
import com.example.fruitylicious.data.local.dao.StaffRestockRow
import com.example.fruitylicious.data.repository.ReportRepository
import com.example.fruitylicious.util.BranchConfigManager
import com.example.fruitylicious.util.NetworkMonitor
import com.example.fruitylicious.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RestockReportUiState(
    val period: String = "daily",
    val selectedDate: Long = System.currentTimeMillis(),
    val rangeText: String = "",
    val totalToday: Double = 0.0,
    val totalsByUnit: List<RestockUnitTotal> = emptyList(),
    val totalEntries: Int = 0,
    val mostRestockedIngredient: String = "—",
    val mostRestockedQty: Double = 0.0,
    val mostRestockedUnit: String = "",
    val staffWithMostRestocks: String = "—",
    val staffRestockCount: Int = 0,
    val staffActivity: List<StaffRestockRow> = emptyList(),
    val topIngredients: List<RestockIngredientRow> = emptyList(),
    val frequencyItems: List<RestockFrequencyRow> = emptyList(),
    val selectedUnit: String? = null,
    val availableUnits: List<String> = emptyList(),
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
    private val sessionManager: SessionManager,
    private val branchConfigManager: BranchConfigManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(RestockReportUiState())
    val uiState: StateFlow<RestockReportUiState> = _uiState.asStateFlow()

    private val localBranchId = sessionManager.getBranchId().takeIf { it > 0 } ?: branchConfigManager.branchId
    private val PAGE_SIZE = 50
    private var reportJob: kotlinx.coroutines.Job? = null

    fun setPeriod(
        period: String,
        branchId: Int?
    ) {
        _uiState.update {
            it.copy(
                period = period,
                selectedDate = System.currentTimeMillis(),
                selectedUnit = null
            )
        }
        loadReport(branchId)
    }

    fun setSelectedDate(
        date: Long,
        branchId: Int?
    ) {
        _uiState.update {
            it.copy(selectedDate = date, selectedUnit = null)
        }
        loadReport(branchId)
    }

    fun setUnitFilter(unit: String?) {
        _uiState.update { it.copy(selectedUnit = unit) }
    }

    fun navigatePeriod(
        delta: Int,
        branchId: Int?
    ) {
        val current = Calendar.getInstance().apply {
            timeInMillis = _uiState.value.selectedDate
        }

        when (_uiState.value.period) {
            "daily" -> current.add(Calendar.DAY_OF_YEAR, delta)
            "weekly" -> current.add(Calendar.WEEK_OF_YEAR, delta)
            "monthly" -> current.add(Calendar.MONTH, delta)
        }

        setSelectedDate(current.timeInMillis, branchId)
    }

    fun loadReport(
        branchId: Int?
    ) {
        reportJob?.cancel()
        reportJob = viewModelScope.launch {
            delay(300) // Debounce branch selection

            val state = _uiState.value
            val range = getRange(state.period, state.selectedDate)

            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    currentPage = 0,
                    hasMore = false,
                    rangeText = formatRangeText(state.period, range)
                )
            }

            val localBranchId = sessionManager.getBranchId()
            val isOnline = networkMonitor.isOnline()
            val isAdmin = isAdminUser()
            val isRemoteNeeded = isAdmin && isOnline && (branchId == null || branchId != localBranchId)

            // 1. Load Local only if remote is NOT needed (offline or local branch)
            // This prevents flickering/jumping between local-only and combined data.
            if (!isRemoteNeeded) {
                loadLocalReport(
                    branchId = branchId ?: localBranchId,
                    from = range.first,
                    to = range.second,
                    shouldSetLoading = true
                )
            }

            // 2. Then Load Remote/Combined if needed. Combined loader handles local data internally.
            if (isRemoteNeeded) {
                _uiState.update { it.copy(isLoading = true) }
                if (branchId == null) {
                    loadCombinedReport(range.first, range.second)
                } else {
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

            val range = getRange(state.period, state.selectedDate)
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
                            isLoadingMore = false
                        )
                    }
                }
            )
        }
    }

    private suspend fun loadCombinedReport(
        from: Long,
        to: Long
    ) {
        try {
            val localId = sessionManager.getBranchId()
            val allBranches = branchDao.getAllBranches()
            val otherBranch = allBranches.firstOrNull { it.branchId != localId }

            // Local data
            val localTotalQty = restockLogDao.getTotalRestockedUnits(null, from, to)
            val localTotalEntries = restockLogDao.getTotalRestockCount(null, from, to)
            val localTopIngredients = restockLogDao.getMostRestockedIngredients(null, from, to)
            val localStaffActivity = restockLogDao.getStaffRestockActivity(null, from, to)
            val localFreq = restockLogDao.getRestockFrequencyReport(null, from, to)

            var finalTotalQty = localTotalQty
            var finalTotalEntries = localTotalEntries
            var finalTopIngredients = localTopIngredients.toMutableList()
            var finalStaffActivity = localStaffActivity.toMutableList()
            var finalFreq = localFreq.toMutableList()

            if (otherBranch != null && networkMonitor.isOnline()) {
                val otherId = otherBranch.branchId
                val remoteSummary = reportRepository.getRestockSummary(otherId, from, to)
                val remotePage = reportRepository.getRestockPage(otherId, from, to, 0, PAGE_SIZE)

                remoteSummary.onSuccess { summary ->
                    finalTotalQty += summary.totalRestockQuantity
                }

                remotePage.onSuccess { page ->
                    finalTotalEntries += page.totalItems.toInt()
                    
                    val remoteItems = page.items
                    
                    // Merge Ingredients
                    val remoteIngredients = remoteItems.groupBy { it.ingredientName }
                        .map { (name, items) ->
                            val first = items.first()
                            RestockIngredientRow(name, items.sumOf { it.quantityAdded }, items.size, first.unitType)
                        }
                    finalTopIngredients = (finalTopIngredients + remoteIngredients)
                        .groupBy { it.ingredientName }
                        .map { (name, rows) ->
                            val first = rows.first()
                            RestockIngredientRow(name, rows.sumOf { it.totalQuantity }, rows.sumOf { it.count }, first.unitType)
                        }.toMutableList()

                    // Merge Staff
                    val remoteStaff = remoteItems.groupBy { it.userName }
                        .map { (name, items) -> StaffRestockRow(name, items.size) }
                    finalStaffActivity = (finalStaffActivity + remoteStaff)
                        .groupBy { it.staffName }
                        .map { (name, rows) -> StaffRestockRow(name, rows.sumOf { it.count }) }
                        .toMutableList()
                    
                    // Freq - Simple merge
                    val remoteFreq = remoteItems.groupBy { it.ingredientName }
                        .map { (name, items) -> 
                            RestockFrequencyRow(name, items.size, items.sumOf { it.quantityAdded } / items.size)
                        }
                    finalFreq = (finalFreq + remoteFreq)
                        .groupBy { it.ingredientName }
                        .map { (name, rows) ->
                            val totalCount = rows.sumOf { it.restockCount }
                            RestockFrequencyRow(name, totalCount, (rows.sumOf { it.avgUnits * it.restockCount }) / totalCount)
                        }.toMutableList()
                }
            }

            val bestIngredient = finalTopIngredients.maxByOrNull { it.totalQuantity }
            val bestStaff = finalStaffActivity.maxByOrNull { it.count }
            val unitTotals = finalTopIngredients.groupBy { it.unitType }
                .map { (unit, rows) -> RestockUnitTotal(unit, rows.sumOf { it.totalQuantity }) }
                .sortedByDescending { it.totalQuantity }

            _uiState.update {
                it.copy(
                    totalToday = finalTotalQty,
                    totalsByUnit = unitTotals,
                    totalEntries = finalTotalEntries,
                    mostRestockedIngredient = bestIngredient?.ingredientName ?: "—",
                    mostRestockedQty = bestIngredient?.totalQuantity ?: 0.0,
                    mostRestockedUnit = bestIngredient?.unitType ?: "",
                    staffWithMostRestocks = bestStaff?.staffName ?: "—",
                    staffRestockCount = bestStaff?.count ?: 0,
                    staffActivity = finalStaffActivity.sortedByDescending { it.count },
                    topIngredients = finalTopIngredients.sortedByDescending { it.totalQuantity },
                    frequencyItems = finalFreq.sortedByDescending { it.restockCount },
                    availableUnits = finalTopIngredients.map { it.unitType }.distinct().sorted(),
                    isLoading = false
                )
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun loadLocalReport(
        branchId: Int,
        from: Long,
        to: Long,
        shouldSetLoading: Boolean = true
    ) {
        try {
            val totalToday = restockLogDao.getTotalRestockedUnits(
                branchId = branchId,
                from = from,
                to = to
            )
            
            val totalEntries = restockLogDao.getTotalRestockCount(branchId, from, to)
            val topIngredients = restockLogDao.getMostRestockedIngredients(branchId, from, to)
            val staffActivity = restockLogDao.getStaffRestockActivity(branchId, from, to)

            val frequencyItems = restockLogDao.getRestockFrequencyReport(
                branchId = branchId,
                from = from,
                to = to
            )

            val bestIngredient = topIngredients.maxByOrNull { it.totalQuantity }
            val bestStaff = staffActivity.maxByOrNull { it.count }
            val unitTotals = topIngredients.groupBy { it.unitType }
                .map { (unit, rows) -> RestockUnitTotal(unit, rows.sumOf { it.totalQuantity }) }
                .sortedByDescending { it.totalQuantity }

            _uiState.update {
                it.copy(
                    totalToday = totalToday,
                    totalsByUnit = unitTotals,
                    totalEntries = totalEntries,
                    mostRestockedIngredient = bestIngredient?.ingredientName ?: "—",
                    mostRestockedQty = bestIngredient?.totalQuantity ?: 0.0,
                    mostRestockedUnit = bestIngredient?.unitType ?: "",
                    staffWithMostRestocks = bestStaff?.staffName ?: "—",
                    staffRestockCount = bestStaff?.count ?: 0,
                    staffActivity = staffActivity,
                    topIngredients = topIngredients,
                    frequencyItems = frequencyItems,
                    availableUnits = topIngredients.map { it.unitType }.distinct().sorted(),
                    isLoading = if (shouldSetLoading) false else it.isLoading,
                    hasMore = false,
                    error = null
                )
            }
        } catch (exception: Exception) {
            _uiState.update {
                it.copy(
                    isLoading = if (shouldSetLoading) false else it.isLoading,
                    error = exception.message ?: "Failed to load local restock report."
                )
            }
        }
    }

    private suspend fun loadRemoteReport(
        branchId: Int?,
        from: Long,
        to: Long
    ) = coroutineScope {
        val summaryDeferred = async {
            reportRepository.getRestockSummary(
                branchId = branchId,
                from = from,
                to = to
            )
        }

        val pageDeferred = async {
            reportRepository.getRestockPage(
                branchId = branchId,
                from = from,
                to = to,
                page = 0,
                size = PAGE_SIZE
            )
        }

        val summaryResult = summaryDeferred.await()
        val pageResult = pageDeferred.await()

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
                val items = pageResponse.items
                
                val ingredientRows = items.groupBy { it.ingredientName }
                    .map { (name, list) -> 
                        val first = list.first()
                        RestockIngredientRow(name, list.sumOf { it.quantityAdded }, list.size, first.unitType)
                    }.sortedByDescending { it.totalQuantity }
                
                val staffRows = items.groupBy { it.userName }
                    .map { (name, list) -> StaffRestockRow(name, list.size) }
                    .sortedByDescending { it.count }

                val frequencyRows = items
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
                
                val bestIngredient = ingredientRows.firstOrNull()
                val bestStaff = staffRows.firstOrNull()
                val unitTotals = ingredientRows.groupBy { it.unitType }
                    .map { (unit, rows) -> RestockUnitTotal(unit, rows.sumOf { it.totalQuantity }) }
                    .sortedByDescending { it.totalQuantity }

                _uiState.update {
                    it.copy(
                        totalEntries = pageResponse.totalItems.toInt(),
                        totalToday = pageResponse.items.sumOf { item -> item.quantityAdded },
                        totalsByUnit = unitTotals,
                        mostRestockedIngredient = bestIngredient?.ingredientName ?: "—",
                        mostRestockedQty = bestIngredient?.totalQuantity ?: 0.0,
                        mostRestockedUnit = bestIngredient?.unitType ?: "",
                        staffWithMostRestocks = bestStaff?.staffName ?: "—",
                        staffRestockCount = bestStaff?.count ?: 0,
                        staffActivity = staffRows,
                        topIngredients = ingredientRows,
                        frequencyItems = frequencyRows,
                        availableUnits = ingredientRows.map { it.unitType }.distinct().sorted(),
                        isLoading = false,
                        currentPage = 0,
                        hasMore = pageResponse.hasNext
                    )
                }
            },
            onFailure = { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false
                    )
                }
            }
        )
    }

    private fun getRange(period: String, baseDate: Long): Pair<Long, Long> {
        val base = Calendar.getInstance().apply {
            timeInMillis = baseDate
        }

        return when (period) {
            "weekly" -> {
                val start = base.clone() as Calendar
                while (start.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
                    start.add(Calendar.DAY_OF_YEAR, -1)
                }
                start.set(Calendar.HOUR_OF_DAY, 0)
                start.set(Calendar.MINUTE, 0)
                start.set(Calendar.SECOND, 0)
                start.set(Calendar.MILLISECOND, 0)

                val end = start.clone() as Calendar
                end.add(Calendar.DAY_OF_YEAR, 7)
                end.add(Calendar.MILLISECOND, -1)

                start.timeInMillis to end.timeInMillis
            }

            "monthly" -> {
                val start = base.clone() as Calendar
                start.set(Calendar.DAY_OF_MONTH, 1)
                start.set(Calendar.HOUR_OF_DAY, 0)
                start.set(Calendar.MINUTE, 0)
                start.set(Calendar.SECOND, 0)
                start.set(Calendar.MILLISECOND, 0)

                val end = start.clone() as Calendar
                end.add(Calendar.MONTH, 1)
                end.add(Calendar.MILLISECOND, -1)

                start.timeInMillis to end.timeInMillis
            }

            else -> {
                val start = base.clone() as Calendar
                start.set(Calendar.HOUR_OF_DAY, 0)
                start.set(Calendar.MINUTE, 0)
                start.set(Calendar.SECOND, 0)
                start.set(Calendar.MILLISECOND, 0)

                val end = start.clone() as Calendar
                end.add(Calendar.DAY_OF_YEAR, 1)
                end.add(Calendar.MILLISECOND, -1)

                start.timeInMillis to end.timeInMillis
            }
        }
    }

    private fun formatRangeText(period: String, range: Pair<Long, Long>): String {
        val sdf = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.US)
        return when (period) {
            "daily" -> sdf.format(java.util.Date(range.first))
            else -> "${sdf.format(java.util.Date(range.first))} - ${sdf.format(java.util.Date(range.second))}"
        }
    }

    private fun isAdminUser(): Boolean {
        return sessionManager.isAdmin()
    }
}
