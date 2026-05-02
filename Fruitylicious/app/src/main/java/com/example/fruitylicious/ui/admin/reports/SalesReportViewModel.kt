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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class SalesReportUiState(
    val period: String = "daily",
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

    fun setPeriod(period: String, branchId: Int?) {
        _uiState.update { it.copy(period = period) }
        loadReport(branchId, period)
    }

    fun loadReport(branchId: Int?, period: String = _uiState.value.period) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val localBranchId = sessionManager.getBranchId()
            val isOnline = networkMonitor.isOnline()
            val range = getRange(period)

            if (branchId == localBranchId || !isOnline) {
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
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Failed to load local sales report."
                        )
                    }
                }
            } else {
                try {
                    val result = if (branchId == null) {
                        reportRepository.getCombinedSalesReport(range.currentStart, range.currentEnd)
                    } else {
                        reportRepository.getSalesReport(branchId, range.currentStart, range.currentEnd)
                    }

                    result.onSuccess { reportDto ->
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
                                topItems = reportDto.items.sortedByDescending { it.quantitySold }.take(5).map { item ->
                                    TopSellingItemRow(
                                        productName = item.productName,
                                        totalQty = item.quantitySold
                                    )
                                },
                                isLoading = false,
                                error = null
                            )
                        }
                    }.onFailure { e ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = e.message ?: "Failed to load remote sales report."
                            )
                        }
                    }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Failed to load remote sales report."
                        )
                    }
                }
            }
        }
    }

    private data class Range(
        val currentStart: Long,
        val currentEnd: Long,
        val previousStart: Long,
        val previousEnd: Long
    )

    private fun getRange(period: String): Range {
        val now = Calendar.getInstance()
        val currentEnd = now.timeInMillis

        return when (period) {
            "weekly" -> {
                val start = Calendar.getInstance()
                while (start.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
                    start.add(Calendar.DAY_OF_YEAR, -1)
                }
                start.setStartOfDay()

                val previousEnd = start.timeInMillis - 1
                val previousStart = start.timeInMillis - 7L * 24L * 60L * 60L * 1000L

                Range(
                    currentStart = start.timeInMillis,
                    currentEnd = currentEnd,
                    previousStart = previousStart,
                    previousEnd = previousEnd
                )
            }

            "monthly" -> {
                val start = Calendar.getInstance()
                start.set(Calendar.DAY_OF_MONTH, 1)
                start.setStartOfDay()

                val previousEnd = start.timeInMillis - 1

                val previousStartCalendar = start.clone() as Calendar
                previousStartCalendar.add(Calendar.MONTH, -1)

                Range(
                    currentStart = start.timeInMillis,
                    currentEnd = currentEnd,
                    previousStart = previousStartCalendar.timeInMillis,
                    previousEnd = previousEnd
                )
            }

            else -> {
                val start = Calendar.getInstance()
                start.setStartOfDay()

                Range(
                    currentStart = start.timeInMillis,
                    currentEnd = currentEnd,
                    previousStart = start.timeInMillis - 24L * 60L * 60L * 1000L,
                    previousEnd = start.timeInMillis - 1
                )
            }
        }
    }

    private fun Calendar.setStartOfDay() {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
}