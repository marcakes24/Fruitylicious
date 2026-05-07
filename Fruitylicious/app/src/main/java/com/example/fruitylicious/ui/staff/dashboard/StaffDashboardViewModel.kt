package com.example.fruitylicious.ui.staff.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitylicious.data.local.dao.StaffLogDao
import com.example.fruitylicious.data.repository.InventoryRepository
import com.example.fruitylicious.data.repository.SyncRepository
import com.example.fruitylicious.data.repository.TransactionRepository
import com.example.fruitylicious.domain.usecase.auth.LogoutUseCase
import com.example.fruitylicious.util.BranchConfig
import com.example.fruitylicious.util.DateTimeUtil
import com.example.fruitylicious.util.NetworkMonitor
import com.example.fruitylicious.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class HourlySales(
    val hour: Int,
    val totalSales: Float,
    val transactionCount: Int
)

data class StaffDashboardUiState(
    val branchName: String = "",
    val userName: String = "",
    val branchId: Int = 0,
    val isAdmin: Boolean = false,
    val isOnline: Boolean = false,
    val hasNotifications: Boolean = false,
    val dateText: String = "",
    val dailySalesData: List<HourlySales> = emptyList(),
    val dailyTotalSales: Double = 0.0,
    val dailyTransactionCount: Int = 0,
    val selectedHourlySales: HourlySales? = null,
    val error: String? = null,
    val isSyncing: Boolean = false,
    val syncMessage: String? = null,
    val syncError: String? = null,
    val isClockedIn: Boolean = false
)

@HiltViewModel
class StaffDashboardViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val branchConfig: BranchConfig,
    private val networkMonitor: NetworkMonitor,
    private val transactionRepository: TransactionRepository,
    private val inventoryRepository: InventoryRepository,
    private val logoutUseCase: LogoutUseCase,
    private val syncRepository: SyncRepository,
    private val staffLogDao: StaffLogDao
) : ViewModel() {

    private val userId: Int = sessionManager.getUserId()

    private val branchId: Int =
        sessionManager.getBranchId().takeIf { it > 0 } ?: branchConfig.branchId

    private val _uiState = MutableStateFlow(
        StaffDashboardUiState(
            branchName = branchConfig.branchName,
            userName = sessionManager.getUserName(),
            branchId = branchId,
            isAdmin = sessionManager.isAdmin(),
            dateText = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.US).format(Date())
        )
    )

    val uiState: StateFlow<StaffDashboardUiState> = _uiState.asStateFlow()

    init {
        observeNetwork()
        observeDailySales()
        observeNotifications()
        observeClockStatus()
    }

    private fun observeClockStatus() {
        viewModelScope.launch {
            staffLogDao.observeStaffLogsByUser(userId).collectLatest { logs ->
                val isClockedIn = logs.any { it.clockOut == null }
                _uiState.update { it.copy(isClockedIn = isClockedIn) }
            }
        }
    }

    private fun observeNetwork() {
        viewModelScope.launch {
            networkMonitor.observeNetworkStatus().collectLatest { isOnline ->
                _uiState.update {
                    it.copy(isOnline = isOnline)
                }
            }
        }
    }

    private fun observeNotifications() {
        viewModelScope.launch {
            inventoryRepository.observeLowStockItems(branchId).collectLatest { items ->
                _uiState.update { it.copy(hasNotifications = items.isNotEmpty()) }
            }
        }
    }

    private fun observeDailySales() {
        val todayRange = getTodayRange()

        viewModelScope.launch {
            transactionRepository
                .observeTransactionsByDateRange(
                    branchId = branchId,
                    from = todayRange.first,
                    to = todayRange.second
                )
                .collectLatest { transactions ->
                    val completedTransactions = transactions.filter {
                        it.status.equals("completed", ignoreCase = true)
                    }

                    // 10 AM to 8 PM (10 slots: 10-11, 11-12, ..., 19-20)
                    val hourlySales = (10..19).map { hour ->
                        val hourStart = getHourTimestamp(hour)
                        val hourEnd = getHourTimestamp(hour + 1)
                        
                        val hourTransactions = completedTransactions.filter { 
                            it.dateTime in hourStart until hourEnd
                        }
                        
                        HourlySales(
                            hour = hour,
                            totalSales = hourTransactions.sumOf { it.totalAmount }.toFloat(),
                            transactionCount = hourTransactions.size
                        )
                    }

                    _uiState.update {
                        it.copy(
                            dailySalesData = hourlySales,
                            dailyTotalSales = completedTransactions.sumOf { transaction ->
                                transaction.totalAmount
                            },
                            dailyTransactionCount = completedTransactions.size,
                            error = null
                        )
                    }
                }
        }
    }

    fun onHourSelected(hourlySales: HourlySales?) {
        _uiState.update { it.copy(selectedHourlySales = hourlySales) }
    }

    private fun getTodayRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis
        
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val end = calendar.timeInMillis - 1
        
        return start to end
    }

    private fun getHourTimestamp(hour: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    fun logout() {
        if (_uiState.value.isClockedIn) {
            _uiState.update { 
                it.copy(error = "You must clock out before logging out.") 
            }
            return
        }
        logoutUseCase()
    }

    fun syncNow() {
        if (_uiState.value.isSyncing) return

        viewModelScope.launch {
            if (!networkMonitor.isOnline()) {
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        syncMessage = null,
                        syncError = "Cannot sync. Device is offline."
                    )
                }
                return@launch
            }

            _uiState.update {
                it.copy(
                    isSyncing = true,
                    syncMessage = null,
                    syncError = null
                )
            }

            val lastPulledAt = sessionManager.getLastPulledAt()
            val result = syncRepository.sync(lastPulledAt)
            val completedAt = System.currentTimeMillis()

            sessionManager.saveSyncStatus(
                syncedAt = completedAt,
                success = result.success,
                message = result.message
            )

            if (result.success) {
                sessionManager.saveLastPulledAt(completedAt)
            }

            _uiState.update {
                it.copy(
                    isSyncing = false,
                    syncMessage = if (result.success) result.message else null,
                    syncError = if (result.success) null else result.message
                )
            }
        }
    }

    fun clearSyncMessage() {
        _uiState.update {
            it.copy(
                syncMessage = null,
                syncError = null
            )
        }
    }

    private fun getCurrentWeekRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()

        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }

        val start = calendar.timeInMillis

        calendar.add(Calendar.DAY_OF_YEAR, 7)
        val end = calendar.timeInMillis - 1

        return start to end
    }

    private fun getMondayBasedDayIndex(timestamp: Long): Int {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp

        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }
    }
}