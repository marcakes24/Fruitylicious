package com.example.fruitylicious.ui.admin.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitylicious.data.local.dao.SalesBreakdownRow
import com.example.fruitylicious.data.local.dao.TopSellingItemRow
import com.example.fruitylicious.data.local.dao.TransactionDao
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

data class SalesReportUiState(
    val period: String = "daily",
    val selectedDate: Long = System.currentTimeMillis(),
    val rangeText: String = "",
    val totalSales: Double = 0.0,
    val previousSales: Double = 0.0,
    val cashTotal: Double = 0.0,
    val gcashTotal: Double = 0.0,
    val topItems: List<TopSellingItemRow> = emptyList(),
    val salesBreakdown: List<SalesBreakdownRow> = emptyList(),
    val isLoading: Boolean = false,
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

    fun loadReport(
        branchId: Int?,
        period: String = _uiState.value.period
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null
                )
            }

            val localBranchId = sessionManager.getBranchId()
            val isOnline = networkMonitor.isOnline()
            val isAdmin = isAdminUser()
            val range = getRange(period, _uiState.value.selectedDate)

            _uiState.update {
                it.copy(rangeText = formatRangeText(period, range))
            }

            when {
                branchId == localBranchId -> {
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
                        branchId = localBranchId,
                        period = period,
                        range = range
                    )
                }

                branchId == null -> {
                    loadRemoteCombinedReport(
                        period = period,
                        range = range
                    )
                }

                else -> {
                    loadRemoteBranchReport(
                        branchId = branchId,
                        period = period,
                        range = range
                    )
                }
            }
        }
    }

    private suspend fun loadLocalReport(
        branchId: Int,
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

            _uiState.update {
                it.copy(
                    period = period,
                    totalSales = totalSales,
                    previousSales = previousSales,
                    cashTotal = cashTotal,
                    gcashTotal = gcashTotal,
                    salesBreakdown = breakdown,
                    topItems = topItems,
                    isLoading = false,
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

    private suspend fun loadRemoteBranchReport(
        branchId: Int,
        period: String,
        range: Range
    ) {
        val result = reportRepository.getSalesReport(
            branchId = branchId,
            from = range.currentStart,
            to = range.currentEnd
        )

        result.fold(
            onSuccess = { reportDto ->
                _uiState.update {
                    it.copy(
                        period = period,
                        totalSales = reportDto.totalSales,
                        previousSales = reportDto.previousSales,
                        cashTotal = reportDto.cashTotal,
                        gcashTotal = reportDto.gcashTotal,
                        salesBreakdown = reportDto.items.map { item ->
                            SalesBreakdownRow(
                                productName = item.productName,
                                qty = item.quantitySold,
                                totalAmount = item.grossSales
                            )
                        },
                        topItems = reportDto.items
                            .sortedByDescending { item -> item.quantitySold }
                            .take(5)
                            .map { item ->
                                TopSellingItemRow(
                                    productName = item.productName,
                                    totalQty = item.quantitySold
                                )
                            },
                        isLoading = false,
                        error = null
                    )
                }
            },
            onFailure = { exception ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load remote sales report."
                    )
                }
            }
        )
    }

    private suspend fun loadRemoteCombinedReport(
        period: String,
        range: Range
    ) {
        val result = reportRepository.getCombinedSalesReport(
            from = range.currentStart,
            to = range.currentEnd
        )

        result.fold(
            onSuccess = { reportDto ->
                _uiState.update {
                    it.copy(
                        period = period,
                        totalSales = reportDto.totalSales,
                        previousSales = reportDto.previousSales,
                        cashTotal = reportDto.cashTotal,
                        gcashTotal = reportDto.gcashTotal,
                        salesBreakdown = reportDto.items.map { item ->
                            SalesBreakdownRow(
                                productName = item.productName,
                                qty = item.quantitySold,
                                totalAmount = item.grossSales
                            )
                        },
                        topItems = reportDto.items
                            .sortedByDescending { item -> item.quantitySold }
                            .take(5)
                            .map { item ->
                                TopSellingItemRow(
                                    productName = item.productName,
                                    totalQty = item.quantitySold
                                )
                            },
                        isLoading = false,
                        error = null
                    )
                }
            },
            onFailure = { exception ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load combined sales report."
                    )
                }
            }
        )
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
        val role = sessionManager.getRole()

        return role.equals("admin", ignoreCase = true) ||
                role.equals("owner", ignoreCase = true)
    }
}