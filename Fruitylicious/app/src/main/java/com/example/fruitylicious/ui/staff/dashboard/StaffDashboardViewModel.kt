package com.example.fruitylicious.ui.staff.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

data class StaffDashboardUiState(
    val branchName: String = "",
    val userName: String = "",
    val branchId: Int = 0,
    val isOnline: Boolean = false,
    val hasNotifications: Boolean = false,
    val dateText: String = "",
    val weeklySalesData: List<Float> = List(7) { 0f },
    val weeklyTotalSales: Double = 0.0,
    val weeklyTransactionCount: Int = 0,
    val error: String? = null,
    val isSyncing: Boolean = false,
    val syncMessage: String? = null,
    val syncError: String? = null
)

@HiltViewModel
class StaffDashboardViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val branchConfig: BranchConfig,
    private val networkMonitor: NetworkMonitor,
    private val transactionRepository: TransactionRepository,
    private val inventoryRepository: InventoryRepository,
    private val logoutUseCase: LogoutUseCase,
    private val syncRepository: SyncRepository
) : ViewModel() {

    private val branchId: Int =
        sessionManager.getBranchId().takeIf { it > 0 } ?: branchConfig.branchId

    private val _uiState = MutableStateFlow(
        StaffDashboardUiState(
            branchName = branchConfig.branchName,
            userName = sessionManager.getUserName(),
            branchId = branchId,
            dateText = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.US).format(Date())
        )
    )

    val uiState: StateFlow<StaffDashboardUiState> = _uiState.asStateFlow()

    init {
        observeNetwork()
        observeWeeklySales()
        observeNotifications()
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

    private fun observeWeeklySales() {
        val weekRange = getCurrentWeekRange()

        viewModelScope.launch {
            transactionRepository
                .observeTransactionsByDateRange(
                    branchId = branchId,
                    from = weekRange.first,
                    to = weekRange.second
                )
                .collectLatest { transactions ->
                    val completedTransactions = transactions.filter {
                        it.status.equals("completed", ignoreCase = true)
                    }

                    val salesPerDay = MutableList(7) { 0f }

                    completedTransactions.forEach { transaction ->
                        val index = getMondayBasedDayIndex(transaction.dateTime)
                        salesPerDay[index] += transaction.totalAmount.toFloat()
                    }

                    _uiState.update {
                        it.copy(
                            weeklySalesData = salesPerDay,
                            weeklyTotalSales = completedTransactions.sumOf { transaction ->
                                transaction.totalAmount
                            },
                            weeklyTransactionCount = completedTransactions.size,
                            error = null
                        )
                    }
                }
        }
    }

    fun logout() {
        logoutUseCase()
    }

    fun syncNow() {
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