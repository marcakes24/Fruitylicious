package com.example.fruitylicious.ui.staff.sales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitylicious.data.local.dao.SalesBreakdownRow
import com.example.fruitylicious.data.local.dao.TransactionDao
import com.example.fruitylicious.data.repository.StaffLogRepository
import com.example.fruitylicious.util.BranchConfig
import com.example.fruitylicious.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HourlySales(
    val hour: Int,
    val totalSales: Float,
    val transactionCount: Int
)

data class StaffSalesSummaryUiState(
    val todaySales: Double = 0.0,
    val transactionCount: Int = 0,
    val cashTotal: Double = 0.0,
    val gcashTotal: Double = 0.0,
    val pendingCount: Int = 0,
    val branchId: Int = 1,
    val branchName: String = "",
    val isLoading: Boolean = true,
    val isClockedIn: Boolean = false,
    val error: String? = null,
    val dailySalesData: List<HourlySales> = emptyList(),
    val selectedHourlySales: HourlySales? = null,
    val salesBreakdown: List<SalesBreakdownRow> = emptyList()
)

@HiltViewModel
class StaffSalesSummaryViewModel @Inject constructor(
    private val transactionDao: TransactionDao,
    private val staffLogRepository: StaffLogRepository,
    private val sessionManager: SessionManager,
    branchConfig: BranchConfig
) : ViewModel() {

    private val branchId = branchConfig.branchId
    private val branchName = branchConfig.branchName

    private val _uiState = MutableStateFlow(
        StaffSalesSummaryUiState(
            branchId = branchId,
            branchName = branchName
        )
    )

    val uiState: StateFlow<StaffSalesSummaryUiState> = _uiState.asStateFlow()

    init {
        loadTodaySales()
        observeClockInStatus()
    }

    fun onHourSelected(hourlySales: HourlySales?) {
        _uiState.update { it.copy(selectedHourlySales = hourlySales) }
    }

    fun loadTodaySales() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, error = null)
            }

            val now = System.currentTimeMillis()
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val todayStart = calendar.timeInMillis

            calendar.add(Calendar.DAY_OF_YEAR, 1)
            val todayEnd = calendar.timeInMillis - 1

            try {
                val transactions = transactionDao.getTransactionsByDateRange(
                    branchId = branchId,
                    from = todayStart,
                    to = todayEnd
                )
                
                val completed = transactions.filter { it.status.equals("completed", ignoreCase = true) }

                // 10 AM to 8 PM
                val hourlySales = (10..19).map { hour ->
                    val hourStart = getHourTimestamp(hour)
                    val hourEnd = getHourTimestamp(hour + 1)
                    
                    val hourTransactions = completed.filter { 
                        it.dateTime in hourStart until hourEnd
                    }
                    
                    HourlySales(
                        hour = hour,
                        totalSales = hourTransactions.sumOf { it.totalAmount }.toFloat(),
                        transactionCount = hourTransactions.size
                    )
                }

                val breakdown = transactionDao.getSalesBreakdown(
                    branchId = branchId,
                    from = todayStart,
                    to = todayEnd
                )

                val todaySales = completed.sumOf { it.totalAmount }
                val transactionCount = completed.size

                val cashTotal = completed.filter { it.paymentType.equals("Cash", ignoreCase = true) }
                    .sumOf { it.totalAmount }

                val gcashTotal = completed.filter { it.paymentType.equals("Gcash", ignoreCase = true) }
                    .sumOf { it.totalAmount }

                val pendingCount = transactionDao.getActiveQueueCountForBranch(
                    branchId = branchId
                )

                _uiState.update {
                    it.copy(
                        todaySales = todaySales,
                        transactionCount = transactionCount,
                        cashTotal = cashTotal,
                        gcashTotal = gcashTotal,
                        pendingCount = pendingCount,
                        dailySalesData = hourlySales,
                        salesBreakdown = breakdown,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load today's sales."
                    )
                }
            }
        }
    }

    private fun getHourTimestamp(hour: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun observeClockInStatus() {
        viewModelScope.launch {
            if (sessionManager.isAdmin()) {
                _uiState.update { it.copy(isClockedIn = true) }
                return@launch
            }

            val userId = sessionManager.getUserId()
            staffLogRepository.observeStaffLogsByUser(userId).collectLatest { logs ->
                val hasActiveLog = logs.any { it.clockOut == null }
                _uiState.update { it.copy(isClockedIn = hasActiveLog) }
            }
        }
    }
}