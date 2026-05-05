package com.example.fruitylicious.ui.admin.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitylicious.data.local.dao.TransactionDao
import com.example.fruitylicious.data.local.entity.BranchEntity
import com.example.fruitylicious.data.repository.ReportRepository
import com.example.fruitylicious.util.BranchConfig
import com.example.fruitylicious.util.NetworkMonitor
import com.example.fruitylicious.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.fruitylicious.data.local.dao.BranchDao

data class SalesSummaryUiState(
    val selectedBranchId: Int? = 1,
    val period: String = "daily",
    val selectedDate: Long = System.currentTimeMillis(),
    val rangeText: String = "",
    val isAdmin: Boolean = false,
    val localBranchId: Int = 1,
    val branches: List<BranchEntity> = emptyList(),
    val isOnline: Boolean = false,
    val canAccessCrossBranch: Boolean = false,
    val todaySales: Double = 0.0,
    val yesterdaySales: Double = 0.0,
    val weekData: List<Double> = List(7) { 0.0 },
    val monthTransactions: Int = 0,
    val monthTotal: Double = 0.0,
    val todayCash: Double = 0.0,
    val todayGcash: Double = 0.0,
    val monthLineData: List<Double> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SalesSummaryViewModel @Inject constructor(
    private val transactionDao: TransactionDao,
    private val sessionManager: SessionManager,
    private val branchConfig: BranchConfig,
    private val branchDao: BranchDao,
    private val reportRepository: ReportRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val localBranchId = branchConfig.branchId

    private val _uiState = MutableStateFlow(
        SalesSummaryUiState(
            isAdmin = isAdminUser(),
            localBranchId = localBranchId,
            selectedBranchId = localBranchId
        )
    )

    val uiState: StateFlow<SalesSummaryUiState> = _uiState.asStateFlow()

    init {
        observeBranches()
        observeNetworkStatus()
        loadSummary()
    }

    private fun observeBranches() {
        viewModelScope.launch {
            branchDao.observeAllBranches().collectLatest { branchList ->
                _uiState.update {
                    it.copy(branches = branchList)
                }
            }
        }
    }

    private fun observeNetworkStatus() {
        viewModelScope.launch {
            networkMonitor.observeNetworkStatus().collectLatest { online ->
                _uiState.update { state ->
                    val canAccess = state.isAdmin

                    val selectedBranchId = if (canAccess) {
                        state.selectedBranchId
                    } else {
                        state.localBranchId
                    }

                    state.copy(
                        isOnline = online,
                        canAccessCrossBranch = canAccess,
                        selectedBranchId = selectedBranchId
                    )
                }

                loadSummary()
            }
        }
    }

    fun onBranchSelected(branchId: Int?) {
        val state = _uiState.value

        val finalBranchId = if (state.canAccessCrossBranch) {
            branchId
        } else {
            state.localBranchId
        }

        _uiState.update {
            it.copy(selectedBranchId = finalBranchId)
        }

        loadSummary(finalBranchId)
    }

    fun setPeriod(period: String) {
        _uiState.update { it.copy(period = period) }
        loadSummary()
    }

    fun setSelectedDate(date: Long) {
        _uiState.update { it.copy(selectedDate = date) }
        loadSummary()
    }

    fun navigatePeriod(delta: Int) {
        val current = Calendar.getInstance().apply {
            timeInMillis = _uiState.value.selectedDate
        }
        when (_uiState.value.period) {
            "daily" -> current.add(Calendar.DAY_OF_YEAR, delta)
            "weekly" -> current.add(Calendar.WEEK_OF_YEAR, delta)
            "monthly" -> current.add(Calendar.MONTH, delta)
        }
        setSelectedDate(current.timeInMillis)
    }

    fun loadSummary(
        branchId: Int? = _uiState.value.selectedBranchId
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null
                )
            }

            val isOnline = networkMonitor.isOnline()
            val isAdmin = isAdminUser()
            val range = getRange(_uiState.value.period, _uiState.value.selectedDate)

            _uiState.update {
                it.copy(rangeText = formatRangeText(_uiState.value.period, range))
            }

            when {
                branchId == localBranchId -> {
                    loadLocalSummary(localBranchId, range)
                }

                !isAdmin -> {
                    loadLocalSummary(localBranchId, range)
                }

                !isOnline -> {
                    loadLocalSummary(localBranchId, range)
                }

                branchId == null -> {
                    loadRemoteSummary(null, range)
                }

                else -> {
                    loadRemoteSummary(branchId, range)
                }
            }
        }
    }

    private suspend fun loadLocalSummary(
        branchId: Int,
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

            // For charts, we still want to show the context of the week/month
            val weekStart = Calendar.getInstance().apply {
                timeInMillis = range.currentStart
                while (get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
                    add(Calendar.DAY_OF_YEAR, -1)
                }
                setStartOfDay()
            }

            val weekData = MutableList(7) { 0.0 }
            repeat(7) { index ->
                val ds = weekStart.clone() as Calendar
                ds.add(Calendar.DAY_OF_YEAR, index)
                val de = ds.clone() as Calendar
                de.add(Calendar.DAY_OF_YEAR, 1)
                weekData[index] = transactionDao.getSalesTotal(branchId, ds.timeInMillis, de.timeInMillis - 1)
            }

            val monthStart = Calendar.getInstance().apply {
                timeInMillis = range.currentStart
                set(Calendar.DAY_OF_MONTH, 1)
                setStartOfDay()
            }
            
            val monthTotal = transactionDao.getSalesTotal(branchId, monthStart.timeInMillis, range.currentEnd)
            val monthTransactions = transactionDao.getTransactionCount(branchId, monthStart.timeInMillis, range.currentEnd)

            val cal = Calendar.getInstance().apply { timeInMillis = range.currentEnd }
            val daysCount = if (isSameMonth(monthStart, Calendar.getInstance())) {
                Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
            } else {
                monthStart.getActualMaximum(Calendar.DAY_OF_MONTH)
            }

            val monthLineData = MutableList(daysCount) { 0.0 }
            repeat(daysCount) { index ->
                val ds = monthStart.clone() as Calendar
                ds.add(Calendar.DAY_OF_MONTH, index)
                val de = ds.clone() as Calendar
                de.add(Calendar.DAY_OF_MONTH, 1)
                monthLineData[index] = transactionDao.getSalesTotal(branchId, ds.timeInMillis, de.timeInMillis - 1)
            }

            _uiState.update {
                it.copy(
                    todaySales = totalSales,
                    yesterdaySales = previousSales,
                    weekData = weekData,
                    monthTotal = monthTotal,
                    monthTransactions = monthTransactions,
                    todayCash = cashTotal,
                    todayGcash = gcashTotal,
                    monthLineData = monthLineData,
                    isLoading = false,
                    error = null
                )
            }
        } catch (exception: Exception) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = exception.message ?: "Failed to load local sales summary."
                )
            }
        }
    }

    private suspend fun loadRemoteSummary(
        branchId: Int?,
        range: Range
    ) {
        try {
            val todayReport = getRemoteSalesReport(branchId, range.currentStart, range.currentEnd)
            val previousReport = getRemoteSalesReport(branchId, range.previousStart, range.previousEnd)

            // Context for charts
            val weekStart = Calendar.getInstance().apply {
                timeInMillis = range.currentStart
                while (get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) { add(Calendar.DAY_OF_YEAR, -1) }
                setStartOfDay()
            }
            val weekData = MutableList(7) { 0.0 }
            repeat(7) { index ->
                val ds = weekStart.clone() as Calendar
                ds.add(Calendar.DAY_OF_YEAR, index)
                val de = ds.clone() as Calendar
                de.add(Calendar.DAY_OF_YEAR, 1)
                weekData[index] = getRemoteSalesReport(branchId, ds.timeInMillis, de.timeInMillis - 1).totalSales
            }

            val monthStart = Calendar.getInstance().apply {
                timeInMillis = range.currentStart
                set(Calendar.DAY_OF_MONTH, 1)
                setStartOfDay()
            }
            val monthReport = getRemoteSalesReport(branchId, monthStart.timeInMillis, range.currentEnd)

            val daysCount = if (isSameMonth(monthStart, Calendar.getInstance())) {
                Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
            } else {
                monthStart.getActualMaximum(Calendar.DAY_OF_MONTH)
            }
            val monthLineData = MutableList(daysCount) { 0.0 }
            repeat(daysCount) { index ->
                val ds = monthStart.clone() as Calendar
                ds.add(Calendar.DAY_OF_MONTH, index)
                val de = ds.clone() as Calendar
                de.add(Calendar.DAY_OF_MONTH, 1)
                monthLineData[index] = getRemoteSalesReport(branchId, ds.timeInMillis, de.timeInMillis - 1).totalSales
            }

            _uiState.update {
                it.copy(
                    todaySales = todayReport.totalSales,
                    yesterdaySales = previousReport.totalSales,
                    weekData = weekData,
                    monthTotal = monthReport.totalSales,
                    monthTransactions = monthReport.totalTransactions,
                    todayCash = todayReport.cashTotal,
                    todayGcash = todayReport.gcashTotal,
                    monthLineData = monthLineData,
                    isLoading = false,
                    error = null
                )
            }
        } catch (exception: Exception) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = exception.message ?: "Failed to load remote sales summary."
                )
            }
        }
    }

    private data class Range(
        val currentStart: Long,
        val currentEnd: Long,
        val previousStart: Long,
        val previousEnd: Long
    )

    private fun getRange(period: String, baseDate: Long): Range {
        val base = Calendar.getInstance().apply { timeInMillis = baseDate }
        val isCurrentPeriod = isSamePeriod(period, base, Calendar.getInstance())

        return when (period) {
            "weekly" -> {
                val start = base.clone() as Calendar
                while (start.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) { start.add(Calendar.DAY_OF_YEAR, -1) }
                start.setStartOfDay()
                val end = if (isCurrentPeriod) Calendar.getInstance() else {
                    val e = start.clone() as Calendar
                    e.add(Calendar.DAY_OF_YEAR, 7)
                    e.add(Calendar.MILLISECOND, -1)
                    e
                }
                val ps = start.clone() as Calendar; ps.add(Calendar.DAY_OF_YEAR, -7)
                val pe = start.clone() as Calendar; pe.add(Calendar.MILLISECOND, -1)
                Range(start.timeInMillis, end.timeInMillis, ps.timeInMillis, pe.timeInMillis)
            }
            "monthly" -> {
                val start = base.clone() as Calendar
                start.set(Calendar.DAY_OF_MONTH, 1)
                start.setStartOfDay()
                val end = if (isCurrentPeriod) Calendar.getInstance() else {
                    val e = start.clone() as Calendar
                    e.add(Calendar.MONTH, 1)
                    e.add(Calendar.MILLISECOND, -1)
                    e
                }
                val ps = start.clone() as Calendar; ps.add(Calendar.MONTH, -1)
                val pe = start.clone() as Calendar; pe.add(Calendar.MILLISECOND, -1)
                Range(start.timeInMillis, end.timeInMillis, ps.timeInMillis, pe.timeInMillis)
            }
            else -> {
                val start = base.clone() as Calendar
                start.setStartOfDay()
                val end = if (isCurrentPeriod) Calendar.getInstance() else {
                    val e = start.clone() as Calendar
                    e.add(Calendar.DAY_OF_YEAR, 1)
                    e.add(Calendar.MILLISECOND, -1)
                    e
                }
                val ps = start.clone() as Calendar; ps.add(Calendar.DAY_OF_YEAR, -1)
                val pe = start.clone() as Calendar; pe.add(Calendar.MILLISECOND, -1)
                Range(start.timeInMillis, end.timeInMillis, ps.timeInMillis, pe.timeInMillis)
            }
        }
    }

    private fun isSamePeriod(period: String, c1: Calendar, c2: Calendar): Boolean {
        return when (period) {
            "daily" -> c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
            "weekly" -> c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) && c1.get(Calendar.WEEK_OF_YEAR) == c2.get(Calendar.WEEK_OF_YEAR)
            "monthly" -> c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) && c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH)
            else -> false
        }
    }

    private fun isSameMonth(c1: Calendar, c2: Calendar) =
        c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) && c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH)

    private fun formatRangeText(period: String, range: Range): String {
        val sdf = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.US)
        return when (period) {
            "daily" -> sdf.format(java.util.Date(range.currentStart))
            else -> "${sdf.format(java.util.Date(range.currentStart))} - ${sdf.format(java.util.Date(range.currentEnd))}"
        }
    }


    private suspend fun getRemoteSalesReport(
        branchId: Int?,
        from: Long,
        to: Long
    ): com.example.fruitylicious.data.remote.dto.SalesReportDto {
        val result = if (branchId == null) {
            reportRepository.getCombinedSalesReport(
                from = from,
                to = to
            )
        } else {
            reportRepository.getSalesReport(
                branchId = branchId,
                from = from,
                to = to
            )
        }

        return result.getOrElse { error ->
            throw error
        }
    }

    private fun Calendar.setStartOfDay() {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    private fun isAdminUser(): Boolean {
        val role = sessionManager.getRole()

        return role.equals("admin", ignoreCase = true) ||
                role.equals("owner", ignoreCase = true)
    }
}