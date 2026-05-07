package com.example.fruitylicious.ui.admin.dashboard

import com.example.fruitylicious.data.repository.InventoryRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitylicious.data.local.dao.BranchDao
import com.example.fruitylicious.data.local.entity.BranchEntity
import com.example.fruitylicious.data.repository.ReportRepository
import com.example.fruitylicious.data.repository.SyncRepository
import com.example.fruitylicious.data.repository.TransactionRepository
import com.example.fruitylicious.domain.usecase.auth.LogoutUseCase
import com.example.fruitylicious.util.BranchConfig
import com.example.fruitylicious.util.NetworkMonitor
import com.example.fruitylicious.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminDashboardUiState(
    val userName: String = "",
    val selectedBranchId: Int? = null,
    val selectedBranchName: String = "All Branches",
    val isAdmin: Boolean = false,
    val localBranchId: Int = 1,
    val userBranchId: String = "B1",
    val branches: List<BranchEntity> = emptyList(),
    val dateText: String = "",
    val isOnline: Boolean = false,
    val canAccessCrossBranch: Boolean = false,
    val hasNotifications: Boolean = false,
    val weeklySalesData: List<Float> = List(7) { 0f },
    val weeklyTotalSales: Double = 0.0,
    val weeklyTransactionCount: Int = 0,
    val error: String? = null,
    val isSyncing: Boolean = false,
    val syncMessage: String? = null,
    val syncError: String? = null
)

@HiltViewModel
class AdminDashboardViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val branchConfig: BranchConfig,
    private val networkMonitor: NetworkMonitor,
    private val transactionRepository: TransactionRepository,
    private val inventoryRepository: InventoryRepository,
    private val reportRepository: ReportRepository,
    private val branchDao: BranchDao,
    private val logoutUseCase: LogoutUseCase,
    private val syncRepository: SyncRepository
) : ViewModel() {

    private var salesJob: Job? = null
    private var notificationJob: Job? = null

    private val localBranchId = sessionManager.getBranchId().takeIf { it > 0 } ?: branchConfig.branchId

    private val _uiState = MutableStateFlow(
        AdminDashboardUiState(
            userName = sessionManager.getUserName().ifBlank { "Owner User" },
            selectedBranchId = if (isAdminUser()) null else localBranchId,
            selectedBranchName = if (isAdminUser()) "All Branches" else "Branch $localBranchId",
            isAdmin = isAdminUser(),
            localBranchId = localBranchId,
            userBranchId = "B$localBranchId",
            dateText = SimpleDateFormat(
                "EEEE, MMMM dd, yyyy",
                Locale.US
            ).format(Date()),
            canAccessCrossBranch = isAdminUser()
        )
    )

    val uiState: StateFlow<AdminDashboardUiState> = _uiState.asStateFlow()

    init {
        observeBranches()
        observeNetwork()
        loadDashboard()
    }

    fun onBranchSelected(branchId: Int?) {
        val state = _uiState.value
        val finalBranchId = if (state.canAccessCrossBranch) branchId else localBranchId
        
        val branchName = when (finalBranchId) {
            null -> "All Branches"
            else -> state.branches.find { it.branchId == finalBranchId }?.branchName ?: "Branch $finalBranchId"
        }

        _uiState.update {
            it.copy(
                selectedBranchId = finalBranchId,
                selectedBranchName = branchName
            )
        }
        loadDashboard()
    }

    fun logout() {
        logoutUseCase()
    }

    private fun observeBranches() {
        viewModelScope.launch {
            branchDao.observeAllBranches().collectLatest { branchList ->
                _uiState.update { state ->
                    val updatedBranchName = when (val bid = state.selectedBranchId) {
                        null -> "All Branches"
                        else -> branchList.find { it.branchId == bid }?.branchName ?: "Branch $bid"
                    }
                    state.copy(
                        branches = branchList,
                        selectedBranchName = updatedBranchName
                    )
                }

                loadDashboard()
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

    private fun loadDashboard() {
        observeWeeklySales()
        observeNotifications()
    }

    private fun observeWeeklySales() {
        salesJob?.cancel()

        salesJob = viewModelScope.launch {
            val state = _uiState.value
            val isOnline = networkMonitor.isOnline()
            val selectedBranchId = state.selectedBranchId

            if (selectedBranchId == localBranchId) {
                observeLocalWeeklySales()
            } else if (state.isAdmin && isOnline) {
                loadRemoteWeeklySales()
            } else {
                observeLocalWeeklySales()
            }
        }
    }

    private suspend fun observeLocalWeeklySales() {
        val weekRange = getCurrentWeekRange()
        val branchId = _uiState.value.selectedBranchId

        val flow = if (branchId == null) {
            transactionRepository.observeAllTransactionsByDateRange(weekRange.first, weekRange.second)
        } else {
            transactionRepository.observeTransactionsByDateRange(branchId, weekRange.first, weekRange.second)
        }

        flow.collectLatest { transactions ->
            val salesPerDay = MutableList(7) { 0f }
            val completed = transactions.filter { it.status.equals("completed", ignoreCase = true) }

            completed.forEach { transaction ->
                val index = getMondayBasedDayIndex(transaction.dateTime)
                if (index in 0..6) {
                    salesPerDay[index] += transaction.totalAmount.toFloat()
                }
            }

            _uiState.update {
                it.copy(
                    weeklySalesData = salesPerDay,
                    weeklyTotalSales = completed.sumOf { it.totalAmount },
                    weeklyTransactionCount = completed.size,
                    error = null
                )
            }
        }
    }

    private suspend fun loadRemoteWeeklySales() {
        val branchId = _uiState.value.selectedBranchId
        val weekStart = getCurrentWeekStartCalendar()

        val salesPerDay = MutableList(7) { 0f }
        var weeklyTotal = 0.0
        var weeklyCount = 0
        var firstError: String? = null

        repeat(7) { index ->
            val dayStart = weekStart.clone() as Calendar
            dayStart.add(Calendar.DAY_OF_YEAR, index)

            val dayEnd = dayStart.clone() as Calendar
            dayEnd.add(Calendar.DAY_OF_YEAR, 1)

            val result = if (branchId == null) {
                reportRepository.getCombinedSalesReport(
                    from = dayStart.timeInMillis,
                    to = dayEnd.timeInMillis - 1
                )
            } else {
                reportRepository.getSalesReport(
                    branchId = branchId,
                    from = dayStart.timeInMillis,
                    to = dayEnd.timeInMillis - 1
                )
            }

            result.fold(
                onSuccess = { report ->
                    salesPerDay[index] = report.totalSales.toFloat()
                    weeklyTotal += report.totalSales
                    weeklyCount += report.totalTransactions
                },
                onFailure = { error ->
                    if (firstError == null) {
                        firstError = error.message
                    }
                }
            )
        }

        _uiState.update {
            it.copy(
                weeklySalesData = salesPerDay,
                weeklyTotalSales = weeklyTotal,
                weeklyTransactionCount = weeklyCount,
                error = firstError
            )
        }
    }

    private fun observeNotifications() {
        notificationJob?.cancel()

        notificationJob = viewModelScope.launch {
            val state = _uiState.value
            val isOnline = networkMonitor.isOnline()

            if (state.isAdmin && isOnline) {
                loadRemoteNotifications()
            } else {
                observeLocalNotifications()
            }
        }
    }

    private suspend fun observeLocalNotifications() {
        val branchId = _uiState.value.selectedBranchId ?: localBranchId
        inventoryRepository.observeLowStockItems(branchId)
            .collectLatest { lowStockItems ->
                _uiState.update {
                    it.copy(
                        hasNotifications = lowStockItems.isNotEmpty()
                    )
                }
            }
    }

    private suspend fun loadRemoteNotifications() {
        val branchId = _uiState.value.selectedBranchId

        if (branchId == null) {
            val branches = branchDao.getAllBranches()
            var hasLowStock = false

            for (branch in branches) {
                val result = reportRepository.getInventoryReport(branch.branchId)
                result.onSuccess { report ->
                    if (report.items.any { it.isLowStock }) {
                        hasLowStock = true
                    }
                }
            }

            _uiState.update {
                it.copy(hasNotifications = hasLowStock)
            }
        } else {
            val result = reportRepository.getInventoryReport(branchId)

            result.fold(
                onSuccess = { report ->
                    _uiState.update {
                        it.copy(
                            hasNotifications = report.items.any { item ->
                                item.isLowStock
                            }
                        )
                    }
                },
                onFailure = {
                    _uiState.update {
                        it.copy(hasNotifications = false)
                    }
                }
            )
        }
    }

    private fun getCurrentWeekRange(): Pair<Long, Long> {
        val calendar = getCurrentWeekStartCalendar()
        val start = calendar.timeInMillis

        calendar.add(Calendar.DAY_OF_YEAR, 7)
        val end = calendar.timeInMillis - 1

        return start to end
    }

    private fun getCurrentWeekStartCalendar(): Calendar {
        val calendar = Calendar.getInstance()

        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }

        return calendar
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

            _uiState.update {
                it.copy(
                    isSyncing = false,
                    syncMessage = if (result.success) result.message else null,
                    syncError = if (result.success) null else result.message
                )
            }

            // Removed loadDashboard() here because observers will trigger it
            // if data actually changed during sync, avoiding redundant requests.
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

    private fun isAdminUser(): Boolean {
        return sessionManager.isAdmin()
    }
}
