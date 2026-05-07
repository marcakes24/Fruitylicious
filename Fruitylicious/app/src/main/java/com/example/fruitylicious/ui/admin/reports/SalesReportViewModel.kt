package com.example.fruitylicious.ui.admin.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitylicious.data.local.dao.SalesBreakdownRow
import com.example.fruitylicious.data.local.dao.StaffSalesRow
import com.example.fruitylicious.data.local.dao.TopAddonRow
import com.example.fruitylicious.data.local.dao.TopComboRow
import com.example.fruitylicious.data.local.dao.TopSellingItemRow
import com.example.fruitylicious.data.local.dao.TransactionDao
import com.example.fruitylicious.data.repository.ReportRepository
import com.example.fruitylicious.util.NetworkMonitor
import com.example.fruitylicious.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import javax.inject.Inject
import com.example.fruitylicious.data.remote.dto.TransactionReportItemDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.Job

data class SalesDataPoint(
    val label: String,
    val timestamp: Long,
    val sales: Double,
    val transactionCount: Int
)

data class SalesSeries(
    val name: String,
    val points: List<SalesDataPoint>
)

data class SalesReportUiState(
    val period: String = "daily",
    val selectedDate: Long = System.currentTimeMillis(),
    val rangeText: String = "",
    val totalSales: Double = 0.0,
    val previousSales: Double = 0.0,
    val cashTotal: Double = 0.0,
    val gcashTotal: Double = 0.0,
    val transactionCount: Int = 0,
    val timeSeriesData: List<SalesSeries> = emptyList(),
    val topItems: List<TopSellingItemRow> = emptyList(),
    val topAddons: List<TopAddonRow> = emptyList(),
    val topCombos: List<TopComboRow> = emptyList(),
    val salesBreakdown: List<SalesBreakdownRow> = emptyList(),
    val staffActivity: List<StaffSalesRow> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val currentPage: Int = 0,
    val hasMore: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SalesReportViewModel @Inject constructor(
    private val transactionDao: TransactionDao,
    private val reportRepository: ReportRepository,
    private val networkMonitor: NetworkMonitor,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SalesReportUiState())
    val uiState: StateFlow<SalesReportUiState> = _uiState.asStateFlow()

    private var reportJob: Job? = null
    private var chartJob: Job? = null

    private val PAGE_SIZE = 50

    private data class ChartTransaction(
        val branchId: Int,
        val totalAmount: Double,
        val dateTime: Long,
        val status: String
    )

    fun setPeriod(
        period: String,
        branchId: Int?
    ) {
        _uiState.update {
            it.copy(
                period = period,
                selectedDate = System.currentTimeMillis()
            )
        }

        loadReport(
            branchId = branchId,
            period = period
        )
    }

    fun setSelectedDate(
        date: Long,
        branchId: Int?
    ) {
        _uiState.update {
            it.copy(selectedDate = date)
        }

        loadReport(
            branchId = branchId
        )
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

    fun refresh(branchId: Int?) {
        loadReport(branchId)
    }

    fun loadMoreItems(branchId: Int?) {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMore) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }

            val isOnline = networkMonitor.isOnline()
            val isAdmin = isAdminUser()
            val localBranchId = sessionManager.getBranchId()
            val range = getRange(state.period, state.selectedDate)
            val nextPage = state.currentPage + 1

            if (isOnline && (branchId == null || branchId != localBranchId)) {
                val result = reportRepository.getSalesItemsPage(
                    branchId = branchId,
                    from = range.currentStart,
                    to = range.currentEnd,
                    page = nextPage,
                    size = PAGE_SIZE
                )

                result.fold(
                    onSuccess = { pageResponse ->
                        val newRows = pageResponse.items.map { item ->
                            SalesBreakdownRow(
                                productName = item.productName,
                                qty = item.quantitySold,
                                totalAmount = item.grossSales,
                                b1Qty = item.b1Qty,
                                b2Qty = item.b2Qty,
                                b1Amount = item.b1Amount,
                                b2Amount = item.b2Amount
                            )
                        }
                        _uiState.update {
                            it.copy(
                                salesBreakdown = it.salesBreakdown + newRows,
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
            } else {
                // Local pagination fallback if needed, or just stop
                _uiState.update { it.copy(isLoadingMore = false, hasMore = false) }
            }
        }
    }

    fun loadReport(
        branchId: Int?,
        period: String = _uiState.value.period
    ) {
        reportJob?.cancel()
        chartJob?.cancel()

        reportJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    currentPage = 0,
                    hasMore = false,
                    salesBreakdown = emptyList()
                )
            }

            val localBranchId = sessionManager.getBranchId()
            val isOnline = networkMonitor.isOnline()
            val isAdmin = isAdminUser()
            val range = getRange(period, _uiState.value.selectedDate)

            _uiState.update {
                it.copy(rangeText = formatRangeText(period, range))
            }

            // Load Chart Data separately via Flow for real-time updates if local, or one-shot if remote
            observeChartData(branchId, period, range)

            when {
                branchId != null && branchId == localBranchId -> {
                    loadLocalReport(
                        branchId = localBranchId,
                        period = period,
                        range = range
                    )
                }

                !isAdmin -> {
                    loadLocalReport(
                        branchId = localBranchId,
                        period = period,
                        range = range
                    )
                }

                !isOnline -> {
                    loadLocalReport(
                        branchId = branchId, // Can be null now
                        period = period,
                        range = range
                    )
                }

                else -> {
                    loadRemoteReport(
                        branchId = branchId,
                        period = period,
                        range = range
                    )
                }
            }
        }
    }

    private fun observeChartData(branchId: Int?, period: String, range: Range) {
        chartJob = viewModelScope.launch {
            val localBranchId = sessionManager.getBranchId()
            val isOnline = networkMonitor.isOnline()
            val isAdmin = isAdminUser()

            if (branchId != null && branchId == localBranchId) {
                // Local Observe
                transactionDao.observeTransactionsByDateRange(branchId, range.currentStart, range.currentEnd)
                    .collectLatest { transactions ->
                        val chartTransactions = transactions.map {
                            ChartTransaction(it.branchId, it.totalAmount, it.dateTime, it.status)
                        }
                        updateChart(chartTransactions, branchId, period, range)
                    }
            } else if (isAdmin && isOnline) {
                // Remote Fetch
                val result = if (branchId == null) {
                    reportRepository.getCombinedTransactionReport(range.currentStart, range.currentEnd)
                } else {
                    reportRepository.getTransactionReport(branchId, range.currentStart, range.currentEnd)
                }

                result.onSuccess { report ->
                    val chartTransactions = report.transactions.map {
                        ChartTransaction(it.branchId, it.totalAmount, it.dateTime, it.status)
                    }
                    updateChart(chartTransactions, branchId, period, range)

                    val aggregatedBreakdown = aggregateBreakdownFromReport(report.transactions)
                    val topAddons = aggregateAddonsFromReport(report.transactions)
                    val topCombos = aggregateCombosFromReport(report.transactions)
                    val staffActivity = aggregateStaffActivityFromReport(report.transactions)
                    _uiState.update { 
                        it.copy(
                            salesBreakdown = aggregatedBreakdown,
                            topAddons = topAddons,
                            topCombos = topCombos,
                            staffActivity = staffActivity
                        )
                    }
                }
            } else {
                // Fallback to local combined if possible
                val flow = if (branchId == null) {
                    transactionDao.observeAllTransactionsByDateRange(range.currentStart, range.currentEnd)
                } else {
                    transactionDao.observeTransactionsByDateRange(branchId, range.currentStart, range.currentEnd)
                }

                flow.collectLatest { transactions ->
                    val chartTransactions = transactions.map {
                        ChartTransaction(it.branchId, it.totalAmount, it.dateTime, it.status)
                    }
                    updateChart(chartTransactions, branchId, period, range)
                }
            }
        }
    }

    private fun updateChart(
        transactions: List<ChartTransaction>,
        selectedBranchId: Int?,
        period: String,
        range: Range
    ) {
        val completed = transactions.filter { it.status.equals("completed", ignoreCase = true) }
        val series = aggregateTimeSeries(completed, selectedBranchId, period, range)

        _uiState.update {
            it.copy(
                timeSeriesData = series,
                transactionCount = completed.size
            )
        }
    }

    private fun aggregateTimeSeries(
        transactions: List<ChartTransaction>,
        selectedBranchId: Int?,
        period: String,
        range: Range
    ): List<SalesSeries> {
        return when (selectedBranchId) {
            null -> {
                val b1 = transactions.filter { it.branchId == 1 }
                val b2 = transactions.filter { it.branchId == 2 }

                listOf(
                    SalesSeries("Branch 1", aggregatePoints(b1, period, range)),
                    SalesSeries("Branch 2", aggregatePoints(b2, period, range)),
                    SalesSeries("Combined", aggregatePoints(transactions, period, range))
                )
            }

            else -> {
                listOf(
                    SalesSeries(
                        "Branch $selectedBranchId",
                        aggregatePoints(transactions, period, range)
                    )
                )
            }
        }
    }

    private fun aggregatePoints(
        transactions: List<ChartTransaction>,
        period: String,
        range: Range
    ): List<SalesDataPoint> {
        val points = mutableListOf<SalesDataPoint>()
        
        when (period) {
            "daily" -> {
                // Hourly aggregation: 10 AM to 8 PM
                for (hour in 10..20) {
                    val slotStart = Calendar.getInstance().apply {
                        timeInMillis = range.currentStart
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    
                    val slotEnd = slotStart + 3600000 - 1
                    
                    val inSlot = transactions.filter { it.dateTime in slotStart..slotEnd }
                    val label = when {
                        hour == 12 -> "12 PM"
                        hour > 12 -> "${hour - 12} PM"
                        else -> "$hour AM"
                    }
                    
                    points.add(SalesDataPoint(
                        label = label,
                        timestamp = slotStart,
                        sales = inSlot.sumOf { it.totalAmount },
                        transactionCount = inSlot.size
                    ))
                }
            }
            "weekly" -> {
                // Per day (Mon-Sun)
                for (day in 0..6) {
                    val slotStart = Calendar.getInstance().apply {
                        timeInMillis = range.currentStart
                        add(Calendar.DAY_OF_YEAR, day)
                        setStartOfDay()
                    }.timeInMillis
                    
                    val slotEnd = slotStart + 86400000 - 1
                    
                    val inSlot = transactions.filter { it.dateTime in slotStart..slotEnd }
                    val sdf = java.text.SimpleDateFormat("EEE", java.util.Locale.US)
                    val label = sdf.format(java.util.Date(slotStart))
                    
                    points.add(SalesDataPoint(
                        label = label,
                        timestamp = slotStart,
                        sales = inSlot.sumOf { it.totalAmount },
                        transactionCount = inSlot.size
                    ))
                }
            }
            "monthly" -> {
                // Per day (1 to end of month)
                val startCal = Calendar.getInstance().apply { timeInMillis = range.currentStart }
                val daysInMonth = startCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                
                for (day in 1..daysInMonth) {
                    val slotStart = Calendar.getInstance().apply {
                        timeInMillis = range.currentStart
                        set(Calendar.DAY_OF_MONTH, day)
                        setStartOfDay()
                    }.timeInMillis
                    
                    val slotEnd = slotStart + 86400000 - 1
                    
                    val inSlot = transactions.filter { it.dateTime in slotStart..slotEnd }
                    points.add(SalesDataPoint(
                        label = day.toString(),
                        timestamp = slotStart,
                        sales = inSlot.sumOf { it.totalAmount },
                        transactionCount = inSlot.size
                    ))
                }
            }
        }
        return points
    }

    private suspend fun loadLocalReport(
        branchId: Int?,
        period: String,
        range: Range
    ) {
        try {
            val totalSales = transactionDao.getSalesTotal(
                branchId = branchId,
                from = range.currentStart,
                to = range.currentEnd
            )

            val previousSales = transactionDao.getSalesTotal(
                branchId = branchId,
                from = range.previousStart,
                to = range.previousEnd
            )

            val cashTotal = transactionDao.getPaymentTotal(
                branchId = branchId,
                paymentType = "Cash",
                from = range.currentStart,
                to = range.currentEnd
            )

            val gcashTotal = transactionDao.getPaymentTotal(
                branchId = branchId,
                paymentType = "Gcash",
                from = range.currentStart,
                to = range.currentEnd
            )

            val breakdown = transactionDao.getSalesBreakdown(
                branchId = branchId,
                from = range.currentStart,
                to = range.currentEnd
            )

            val topItems = transactionDao.getTopSellingItems(
                branchId = branchId,
                from = range.currentStart,
                to = range.currentEnd
            )

            val topAddons = transactionDao.getTopAddons(
                branchId = branchId,
                from = range.currentStart,
                to = range.currentEnd
            )

            val topCombos = transactionDao.getTopFruitCombos(
                branchId = branchId,
                from = range.currentStart,
                to = range.currentEnd
            )

            val staffActivity = transactionDao.getStaffSalesActivity(
                branchId = branchId,
                from = range.currentStart,
                to = range.currentEnd
            )

            _uiState.update {
                it.copy(
                    period = period,
                    totalSales = totalSales,
                    previousSales = previousSales,
                    cashTotal = cashTotal,
                    gcashTotal = gcashTotal,
                    salesBreakdown = breakdown,
                    topItems = topItems,
                    topAddons = topAddons,
                    topCombos = topCombos,
                    staffActivity = staffActivity,
                    isLoading = false,
                    hasMore = false, // Local load usually loads all for now
                    error = null
                )
            }
        } catch (exception: Exception) {
            if (exception is kotlinx.coroutines.CancellationException) throw exception
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = exception.message ?: "Failed to load local sales report."
                )
            }
        }
    }

    private suspend fun loadRemoteReport(
        branchId: Int?,
        period: String,
        range: Range
    ) {
        // 1. Load Summary
        val summaryResult = reportRepository.getSalesSummary(
            branchId = branchId,
            from = range.currentStart,
            to = range.currentEnd
        )

        // 2. Load Top Items
        val topItemsResult = reportRepository.getTopSellingItems(
            branchId = branchId,
            from = range.currentStart,
            to = range.currentEnd,
            limit = 5
        )

        // 3. Load First Page of Items
        val itemsResult = reportRepository.getSalesItemsPage(
            branchId = branchId,
            from = range.currentStart,
            to = range.currentEnd,
            page = 0,
            size = PAGE_SIZE
        )

        // 4. Load Transactions for Addons/Combos
        val transactionsResult = if (branchId == null) {
            reportRepository.getCombinedTransactionReport(range.currentStart, range.currentEnd)
        } else {
            reportRepository.getTransactionReport(branchId, range.currentStart, range.currentEnd)
        }

        summaryResult.fold(
            onSuccess = { summary ->
                _uiState.update {
                    it.copy(
                        totalSales = summary.totalSales,
                        previousSales = summary.previousSales,
                        cashTotal = summary.cashTotal,
                        gcashTotal = summary.gcashTotal
                    )
                }
            },
            onFailure = { error ->
                _uiState.update { it.copy(error = error.message) }
            }
        )

        topItemsResult.fold(
            onSuccess = { topItems ->
                _uiState.update {
                    it.copy(
                        topItems = topItems.map { item ->
                            TopSellingItemRow(
                                productName = item.productName,
                                totalQty = item.quantitySold
                            )
                        }
                    )
                }
            },
            onFailure = { /* silent error for top items */ }
        )

        transactionsResult.onSuccess { report ->
            val topAddons = aggregateAddonsFromReport(report.transactions)
            val topCombos = aggregateCombosFromReport(report.transactions)
            val staffActivity = aggregateStaffActivityFromReport(report.transactions)
            _uiState.update {
                it.copy(
                    topAddons = topAddons,
                    topCombos = topCombos,
                    staffActivity = staffActivity
                )
            }
        }

        itemsResult.fold(
            onSuccess = { pageResponse ->
                _uiState.update {
                    it.copy(
                        salesBreakdown = if (branchId == null && it.salesBreakdown.isNotEmpty()) {
                            it.salesBreakdown
                        } else {
                            pageResponse.items.map { item ->
                                SalesBreakdownRow(
                                    productName = item.productName,
                                    qty = item.quantitySold,
                                    totalAmount = item.grossSales,
                                    b1Qty = item.b1Qty,
                                    b2Qty = item.b2Qty,
                                    b1Amount = item.b1Amount,
                                    b2Amount = item.b2Amount
                                )
                            }
                        },
                        currentPage = 0,
                        hasMore = pageResponse.hasNext,
                        isLoading = false
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

    private fun aggregateBreakdownFromReport(transactions: List<TransactionReportItemDto>): List<SalesBreakdownRow> {
        val completed = transactions.filter { it.status.equals("completed", ignoreCase = true) }
        val productMap = mutableMapOf<String, SalesBreakdownRow>()

        completed.forEach { trans ->
            trans.items.forEach { item ->
                val current = productMap.getOrDefault(item.productName, SalesBreakdownRow(item.productName, 0, 0.0))
                
                val b1Qty = if (trans.branchId == 1) item.quantity else 0
                val b2Qty = if (trans.branchId == 2) item.quantity else 0
                val b1Amount = if (trans.branchId == 1) item.subtotal else 0.0
                val b2Amount = if (trans.branchId == 2) item.subtotal else 0.0

                productMap[item.productName] = current.copy(
                    qty = current.qty + item.quantity,
                    totalAmount = current.totalAmount + item.subtotal,
                    b1Qty = current.b1Qty + b1Qty,
                    b2Qty = current.b2Qty + b2Qty,
                    b1Amount = current.b1Amount + b1Amount,
                    b2Amount = current.b2Amount + b2Amount
                )
            }
        }
        return productMap.values.sortedByDescending { it.totalAmount }
    }

    private fun aggregateAddonsFromReport(transactions: List<TransactionReportItemDto>): List<TopAddonRow> {
        val completed = transactions.filter { it.status.equals("completed", ignoreCase = true) }
        val addonMap = mutableMapOf<String, Int>()
        completed.forEach { trans ->
            trans.items.forEach { item ->
                item.addons.forEach { addonName ->
                    addonMap[addonName] = addonMap.getOrDefault(addonName, 0) + item.quantity
                }
            }
        }
        return addonMap.map { TopAddonRow(it.key, it.value) }.sortedByDescending { it.totalQty }.take(5)
    }

    private fun aggregateCombosFromReport(transactions: List<TransactionReportItemDto>): List<TopComboRow> {
        val completed = transactions.filter { it.status.equals("completed", ignoreCase = true) }
        val comboMap = mutableMapOf<String, Int>()
        completed.forEach { trans ->
            if (trans.items.size >= 2) {
                val sortedNames = trans.items.map { it.productName }.sorted()
                for (i in 0 until sortedNames.size) {
                    for (j in i + 1 until sortedNames.size) {
                        val combo = "${sortedNames[i]} + ${sortedNames[j]}"
                        comboMap[combo] = comboMap.getOrDefault(combo, 0) + 1
                    }
                }
            }
        }
        return comboMap.map { TopComboRow(it.key, it.value) }.sortedByDescending { it.count }.take(5)
    }

    private fun aggregateStaffActivityFromReport(transactions: List<TransactionReportItemDto>): List<StaffSalesRow> {
        val completed = transactions.filter { it.status.equals("completed", ignoreCase = true) }
        val staffMap = mutableMapOf<String, StaffSalesRow>()
        completed.forEach { trans ->
            val current = staffMap.getOrDefault(trans.userName, StaffSalesRow(trans.userName, 0, 0.0))
            staffMap[trans.userName] = current.copy(
                transactionCount = current.transactionCount + 1,
                totalSales = current.totalSales + trans.totalAmount
            )
        }
        return staffMap.values.sortedByDescending { it.totalSales }
    }

    private data class Range(
        val currentStart: Long,
        val currentEnd: Long,
        val previousStart: Long,
        val previousEnd: Long
    )

    private fun getRange(period: String, baseDate: Long): Range {
        val base = Calendar.getInstance().apply {
            timeInMillis = baseDate
        }
        
        val isCurrentPeriod = isSamePeriod(period, base, Calendar.getInstance())

        return when (period) {
            "weekly" -> {
                val start = base.clone() as Calendar
                while (start.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
                    start.add(Calendar.DAY_OF_YEAR, -1)
                }
                start.setStartOfDay()

                val end = if (isCurrentPeriod) {
                    Calendar.getInstance()
                } else {
                    val e = start.clone() as Calendar
                    e.add(Calendar.DAY_OF_YEAR, 7)
                    e.add(Calendar.MILLISECOND, -1)
                    e
                }

                val previousStart = start.clone() as Calendar
                previousStart.add(Calendar.DAY_OF_YEAR, -7)

                val previousEnd = start.clone() as Calendar
                previousEnd.add(Calendar.MILLISECOND, -1)

                Range(
                    currentStart = start.timeInMillis,
                    currentEnd = end.timeInMillis,
                    previousStart = previousStart.timeInMillis,
                    previousEnd = previousEnd.timeInMillis
                )
            }

            "monthly" -> {
                val start = base.clone() as Calendar
                start.set(Calendar.DAY_OF_MONTH, 1)
                start.setStartOfDay()

                val end = if (isCurrentPeriod) {
                    Calendar.getInstance()
                } else {
                    val e = start.clone() as Calendar
                    e.add(Calendar.MONTH, 1)
                    e.add(Calendar.MILLISECOND, -1)
                    e
                }

                val previousStart = start.clone() as Calendar
                previousStart.add(Calendar.MONTH, -1)

                val previousEnd = start.clone() as Calendar
                previousEnd.add(Calendar.MILLISECOND, -1)

                Range(
                    currentStart = start.timeInMillis,
                    currentEnd = end.timeInMillis,
                    previousStart = previousStart.timeInMillis,
                    previousEnd = previousEnd.timeInMillis
                )
            }

            else -> {
                val start = base.clone() as Calendar
                start.setStartOfDay()

                val end = if (isCurrentPeriod) {
                    Calendar.getInstance()
                } else {
                    val e = start.clone() as Calendar
                    e.add(Calendar.DAY_OF_YEAR, 1)
                    e.add(Calendar.MILLISECOND, -1)
                    e
                }

                val previousStart = start.clone() as Calendar
                previousStart.add(Calendar.DAY_OF_YEAR, -1)

                val previousEnd = start.clone() as Calendar
                previousEnd.add(Calendar.MILLISECOND, -1)

                Range(
                    currentStart = start.timeInMillis,
                    currentEnd = end.timeInMillis,
                    previousStart = previousStart.timeInMillis,
                    previousEnd = previousEnd.timeInMillis
                )
            }
        }
    }

    private fun isSamePeriod(period: String, c1: Calendar, c2: Calendar): Boolean {
        return when (period) {
            "daily" -> c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                    c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
            "weekly" -> c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                    c1.get(Calendar.WEEK_OF_YEAR) == c2.get(Calendar.WEEK_OF_YEAR)
            "monthly" -> c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                    c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH)
            else -> false
        }
    }

    private fun formatRangeText(period: String, range: Range): String {
        val sdf = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.US)
        return when (period) {
            "daily" -> sdf.format(java.util.Date(range.currentStart))
            else -> "${sdf.format(java.util.Date(range.currentStart))} - ${sdf.format(java.util.Date(range.currentEnd))}"
        }
    }

    private fun Calendar.setStartOfDay() {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    private fun isAdminUser(): Boolean {
        return sessionManager.isAdmin()
    }
}