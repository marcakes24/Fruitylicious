package com.example.fruitylicious.ui.admin.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitylicious.data.repository.InventoryRepository
import com.example.fruitylicious.data.repository.TransactionRepository
import com.example.fruitylicious.domain.usecase.auth.LogoutUseCase
import com.example.fruitylicious.util.BranchConfig
import com.example.fruitylicious.util.NetworkMonitor
import com.example.fruitylicious.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class AdminDashboardUiState(
    val userName: String = "",
    val selectedBranch: String = "B1",
    val isAdmin: Boolean = false,
    val userBranchId: String = "B1",
    val dateText: String = "",
    val isOnline: Boolean = false,
    val hasNotifications: Boolean = false,
    val weeklySalesData: List<Float> = List(7) { 0f },
    val weeklyTotalSales: Double = 0.0,
    val weeklyTransactionCount: Int = 0,
    val error: String? = null
)

@HiltViewModel
class AdminDashboardViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val branchConfig: BranchConfig,
    private val networkMonitor: NetworkMonitor,
    private val transactionRepository: TransactionRepository,
    private val inventoryRepository: InventoryRepository,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    private var salesJob: Job? = null

    private val _uiState = MutableStateFlow(
        AdminDashboardUiState(
            userName = sessionManager.getUserName().ifBlank { "Admin User" },
            selectedBranch = "B${branchConfig.branchId}",
            isAdmin = sessionManager.getRole()?.equals("admin", ignoreCase = true) == true,
            userBranchId = "B${sessionManager.getBranchId()}",
            dateText = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.US).format(Date())
        )
    )

    val uiState: StateFlow<AdminDashboardUiState> = _uiState.asStateFlow()

    init {
        observeNetwork()
        observeWeeklySales(_uiState.value.selectedBranch)
        observeNotifications()
    }

    fun onBranchSelected(branch: String) {
        _uiState.update {
            it.copy(selectedBranch = branch)
        }
        observeWeeklySales(branch)
    }

    fun logout() {
        logoutUseCase()
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
            combine(
                inventoryRepository.observeLowStockItems(1),
                inventoryRepository.observeLowStockItems(2)
            ) { b1, b2 ->
                b1.isNotEmpty() || b2.isNotEmpty()
            }.collectLatest { hasNotifs ->
                _uiState.update { it.copy(hasNotifications = hasNotifs) }
            }
        }
    }

    private fun observeWeeklySales(branch: String) {
        salesJob?.cancel()

        val weekRange = getCurrentWeekRange()

        salesJob = viewModelScope.launch {
            val flow = when (branch) {
                "B1" -> transactionRepository.observeTransactionsByDateRange(
                    branchId = 1,
                    from = weekRange.first,
                    to = weekRange.second
                )

                "B2" -> transactionRepository.observeTransactionsByDateRange(
                    branchId = 2,
                    from = weekRange.first,
                    to = weekRange.second
                )

                else -> transactionRepository.observeAllTransactionsByDateRange(
                    from = weekRange.first,
                    to = weekRange.second
                )
            }

            flow.collectLatest { transactions ->
                val completed = transactions.filter {
                    it.status.equals("completed", ignoreCase = true)
                }

                val salesPerDay = MutableList(7) { 0f }

                completed.forEach { transaction ->
                    val index = getMondayBasedDayIndex(transaction.dateTime)
                    salesPerDay[index] += transaction.totalAmount.toFloat()
                }

                _uiState.update {
                    it.copy(
                        weeklySalesData = salesPerDay,
                        weeklyTotalSales = completed.sumOf { transaction -> transaction.totalAmount },
                        weeklyTransactionCount = completed.size,
                        error = null
                    )
                }
            }
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