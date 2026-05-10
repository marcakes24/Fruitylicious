package com.example.fruitylicious.ui.admin.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitylicious.data.local.dao.BranchDao
import com.example.fruitylicious.data.local.dao.WasteItemRow
import com.example.fruitylicious.data.local.dao.WasteLogDao
import com.example.fruitylicious.data.local.dao.WasteReasonRow
import com.example.fruitylicious.data.local.dao.StaffWasteRow
import com.example.fruitylicious.data.local.dao.WasteUnitTotal
import com.example.fruitylicious.data.repository.ReportRepository
import com.example.fruitylicious.util.NetworkMonitor
import com.example.fruitylicious.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WasteReportUiState(
    val period: String = "daily",
    val selectedDate: Long = System.currentTimeMillis(),
    val rangeText: String = "",
    val totalWaste: Double = 0.0,
    val totalsByUnit: List<WasteUnitTotal> = emptyList(),
    val totalEntries: Int = 0,
    val mostWasted: String = "—",
    val mostWastedQty: Double = 0.0,
    val mostWastedUnit: String = "",
    val reasonData: List<WasteReasonRow> = emptyList(),
    val wasteByItem: List<WasteItemRow> = emptyList(),
    val staffActivity: List<StaffWasteRow> = emptyList(),
    val selectedUnit: String? = null,
    val availableUnits: List<String> = emptyList(),
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

    private val localBranchId = sessionManager.getBranchId()
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
                    wasteByItem = emptyList(),
                    rangeText = formatRangeText(state.period, range)
                )
            }

            val localBranchId = sessionManager.getBranchId()
            val isOnline = networkMonitor.isOnline()
            val isAdmin = isAdminUser()

            // 1. Load Local first as placeholder
            loadLocalReport(
                branchId = branchId ?: localBranchId,
                from = range.first,
                to = range.second
            )

            // 2. Then Load Remote/Combined if needed
            if (isAdmin && isOnline && (branchId == null || branchId != localBranchId)) {
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
                        .groupBy { it.ingredientId }
                        .map { (_, groupedItems) ->
                            val first = groupedItems.first()
                            WasteItemRow(
                                ingredientName = first.ingredientName,
                                totalQuantity = groupedItems.sumOf { it.quantity },
                                unitType = first.unitType,
                                b1Qty = groupedItems.filter { (it.branchId ?: localBranchId) == 1 }.sumOf { it.quantity },
                                b2Qty = groupedItems.filter { (it.branchId ?: localBranchId) == 2 }.sumOf { it.quantity }
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

    private suspend fun loadCombinedReport(
        from: Long,
        to: Long
    ) {
        try {
            val localId = localBranchId
            val allBranches = branchDao.getAllBranches()
            val otherBranch = allBranches.firstOrNull { it.branchId != localId }
            
            // 1. Load Local Data
            val localTotal = wasteLogDao.getTotalWasteQuantity(null, from, to)
            val localCount = wasteLogDao.getTotalWasteCount(null, from, to)
            val localReasons = wasteLogDao.getWasteReasonReport(null, from, to)
            val localItems = wasteLogDao.getWasteByItemReport(null, from, to)
            val localStaff = wasteLogDao.getStaffWasteActivity(null, from, to)

            var finalTotal = localTotal
            var finalCount = localCount
            var finalReasons = localReasons.toMutableList()
            var finalItems = localItems.toMutableList()
            var finalStaff = localStaff.toMutableList()

            // 2. Load Remote Data for the other branch if online
            if (otherBranch != null && networkMonitor.isOnline()) {
                val otherId = otherBranch.branchId
                val remoteSummary = reportRepository.getWasteSummary(otherId, from, to)
                val remotePage = reportRepository.getWastePage(otherId, from, to, 0, PAGE_SIZE)

                remoteSummary.onSuccess { summary ->
                    finalTotal += summary.totalWasteQuantity
                }

                remotePage.onSuccess { page ->
                    finalCount += page.totalItems.toInt()
                    // Map remote items to WasteItemRow
                    val remoteRows = page.items
                        .groupBy { it.ingredientId }
                        .map { (_, grouped) ->
                            val first = grouped.first()
                            WasteItemRow(
                                ingredientName = first.ingredientName,
                                totalQuantity = grouped.sumOf { it.quantity },
                                unitType = first.unitType,
                                b1Qty = if (otherId == 1) grouped.sumOf { it.quantity } else 0.0,
                                b2Qty = if (otherId == 2) grouped.sumOf { it.quantity } else 0.0
                            )
                        }
                    
                    // Merge with localItems
                    val mergedItems = (finalItems + remoteRows)
                        .groupBy { it.ingredientName }
                        .map { (name, rows) ->
                            val first = rows.first()
                            WasteItemRow(
                                ingredientName = name,
                                totalQuantity = rows.sumOf { it.totalQuantity },
                                unitType = first.unitType,
                                b1Qty = rows.sumOf { it.b1Qty },
                                b2Qty = rows.sumOf { it.b2Qty }
                            )
                        }
                    finalItems = mergedItems.toMutableList()

                    // Merge Reasons
                    val remoteReasons = page.items
                        .groupBy { it.reason.trim().lowercase() }
                        .map { (reason, grouped) ->
                            WasteReasonRow(
                                reason = reason.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() },
                                count = grouped.size,
                                b1Count = if (otherId == 1) grouped.size else 0,
                                b2Count = if (otherId == 2) grouped.size else 0
                            )
                        }
                    
                    val mergedReasons = (finalReasons + remoteReasons)
                        .groupBy { it.reason.lowercase() }
                        .map { (_, rows) ->
                            val first = rows.first()
                            WasteReasonRow(
                                reason = first.reason,
                                count = rows.sumOf { it.count },
                                b1Count = rows.sumOf { it.b1Count },
                                b2Count = rows.sumOf { it.b2Count }
                            )
                        }
                    finalReasons = mergedReasons.toMutableList()

                    // Merge Staff
                    val remoteStaff = page.items
                        .groupBy { it.userName }
                        .map { (name, grouped) -> StaffWasteRow(name, grouped.size) }
                    
                    finalStaff = (finalStaff + remoteStaff)
                        .groupBy { it.staffName }
                        .map { (name, rows) -> StaffWasteRow(name, rows.sumOf { it.count }) }
                        .toMutableList()
                }
            }

            val topItem = finalItems.maxByOrNull { it.totalQuantity }
            val unitTotals = finalItems.groupBy { it.unitType }
                .map { (unit, rows) -> WasteUnitTotal(unit, rows.sumOf { it.totalQuantity }) }
                .sortedByDescending { it.totalQuantity }

            _uiState.update {
                it.copy(
                    totalWaste = finalTotal,
                    totalEntries = finalCount,
                    totalsByUnit = unitTotals,
                    wasteByItem = finalItems.sortedByDescending { it.totalQuantity },
                    reasonData = finalReasons.sortedByDescending { it.count },
                    staffActivity = finalStaff.sortedByDescending { it.count },
                    mostWasted = topItem?.ingredientName ?: "—",
                    mostWastedQty = topItem?.totalQuantity ?: 0.0,
                    mostWastedUnit = topItem?.unitType ?: "",
                    availableUnits = finalItems.map { it.unitType }.distinct().sorted(),
                    isLoading = false,
                    error = null
                )
            }

        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load combined waste report."
                )
            }
        }
    }

    private suspend fun loadLocalReport(
        branchId: Int?,
        from: Long,
        to: Long
    ) {
        try {
            val totalWaste = wasteLogDao.getTotalWasteQuantity(
                branchId = branchId,
                from = from,
                to = to
            )
            val totalCount = wasteLogDao.getTotalWasteCount(branchId, from, to)
            val staffActivity = wasteLogDao.getStaffWasteActivity(branchId, from, to)

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

            // If we are viewing "All" (branchId == null) and online, we should probably fetch from remote 
            // because local only has this branch's data. 
            // But the current UI logic calls loadLocalReport if branchId matches local or if offline.

            val topItem = wasteByItem.maxByOrNull {
                it.totalQuantity
            }
            val unitTotals = wasteByItem.groupBy { it.unitType }
                .map { (unit, rows) -> WasteUnitTotal(unit, rows.sumOf { it.totalQuantity }) }
                .sortedByDescending { it.totalQuantity }

            _uiState.update {
                it.copy(
                    totalWaste = totalWaste,
                    totalEntries = totalCount,
                    totalsByUnit = unitTotals,
                    mostWasted = topItem?.ingredientName ?: "—",
                    mostWastedQty = topItem?.totalQuantity ?: 0.0,
                    mostWastedUnit = topItem?.unitType ?: "",
                    reasonData = reasonData,
                    wasteByItem = wasteByItem,
                    staffActivity = staffActivity,
                    availableUnits = wasteByItem.map { row -> row.unitType }.distinct().sorted(),
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
    ) = coroutineScope {
        val summaryDeferred = async {
            reportRepository.getWasteSummary(
                branchId = branchId,
                from = from,
                to = to
            )
        }

        val pageDeferred = async {
            reportRepository.getWastePage(
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
                val items = pageResponse.items
                val itemRows = items
                    .groupBy { it.ingredientId }
                    .map { (_, groupedItems) ->
                        val first = groupedItems.first()
                        WasteItemRow(
                            ingredientName = first.ingredientName,
                            totalQuantity = groupedItems.sumOf { it.quantity },
                            unitType = first.unitType,
                            b1Qty = groupedItems.filter { (it.branchId ?: localBranchId) == 1 }.sumOf { it.quantity },
                            b2Qty = groupedItems.filter { (it.branchId ?: localBranchId) == 2 }.sumOf { it.quantity }
                        )
                    }
                    .sortedByDescending { it.totalQuantity }

                val topItem = itemRows.firstOrNull()
                val unitTotals = itemRows.groupBy { it.unitType }
                    .map { (unit, rows) -> WasteUnitTotal(unit, rows.sumOf { it.totalQuantity }) }
                    .sortedByDescending { it.totalQuantity }

                val staffRows = items.groupBy { it.userName }
                    .map { (name, grouped) -> StaffWasteRow(name, grouped.size) }
                    .sortedByDescending { it.count }

                val reasonRows = items
                    .groupBy { 
                        it.reason.trim().lowercase()
                            .replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase(Locale.US) else char.toString() }
                            .ifBlank { "Unspecified" } 
                    }
                    .map { (reason, groupedItems) ->
                        WasteReasonRow(
                            reason = reason,
                            count = groupedItems.size,
                            b1Count = groupedItems.count { it.branchId == 1 },
                            b2Count = groupedItems.count { it.branchId == 2 }
                        )
                    }
                    .sortedByDescending { it.count }

                _uiState.update {
                    it.copy(
                        wasteByItem = itemRows,
                        reasonData = reasonRows,
                        staffActivity = staffRows,
                        totalEntries = pageResponse.totalItems.toInt(),
                        totalsByUnit = unitTotals,
                        availableUnits = itemRows.map { row -> row.unitType }.distinct().sorted(),
                        mostWasted = it.mostWasted.takeIf { m -> m != "—" } 
                            ?: topItem?.ingredientName 
                            ?: "—",
                        mostWastedQty = if (it.mostWasted == "—") topItem?.totalQuantity ?: 0.0 else it.mostWastedQty,
                        mostWastedUnit = if (it.mostWasted == "—") topItem?.unitType ?: "" else it.mostWastedUnit,
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
        val sdf = java.text.SimpleDateFormat("MMM dd, yyyy", Locale.US)
        return when (period) {
            "daily" -> sdf.format(java.util.Date(range.first))
            else -> "${sdf.format(java.util.Date(range.first))} - ${sdf.format(java.util.Date(range.second))}"
        }
    }

    private fun isAdminUser(): Boolean {
        return sessionManager.isAdmin()
    }
}