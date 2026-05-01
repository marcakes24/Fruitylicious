package com.example.fruitylicious.ui.admin.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitylicious.data.local.dao.TransactionDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class SalesSummaryUiState(
    val selectedBranch: String = "All",
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
    private val transactionDao: TransactionDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(SalesSummaryUiState())
    val uiState: StateFlow<SalesSummaryUiState> = _uiState.asStateFlow()

    init {
        loadSummary("All")
    }

    fun onBranchSelected(branch: String) {
        _uiState.update { it.copy(selectedBranch = branch) }
        loadSummary(branch)
    }

    fun loadSummary(branch: String = _uiState.value.selectedBranch) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val branchId = when (branch) {
                "B1" -> 1
                "B2" -> 2
                else -> null
            }

            try {
                val now = Calendar.getInstance()

                val todayStart = Calendar.getInstance().apply {
                    setStartOfDay()
                }

                val yesterdayStart = Calendar.getInstance().apply {
                    setStartOfDay()
                    add(Calendar.DAY_OF_YEAR, -1)
                }

                val yesterdayEnd = todayStart.timeInMillis - 1

                val todaySales = transactionDao.getSalesTotal(
                    branchId = branchId,
                    from = todayStart.timeInMillis,
                    to = now.timeInMillis
                )

                val yesterdaySales = transactionDao.getSalesTotal(
                    branchId = branchId,
                    from = yesterdayStart.timeInMillis,
                    to = yesterdayEnd
                )

                val todayCash = transactionDao.getPaymentTotal(
                    branchId = branchId,
                    paymentType = "Cash",
                    from = todayStart.timeInMillis,
                    to = now.timeInMillis
                )

                val todayGcash = transactionDao.getPaymentTotal(
                    branchId = branchId,
                    paymentType = "Gcash",
                    from = todayStart.timeInMillis,
                    to = now.timeInMillis
                )

                val weekStart = Calendar.getInstance().apply {
                    setStartOfDay()
                    while (get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
                        add(Calendar.DAY_OF_YEAR, -1)
                    }
                }

                val weekData = MutableList(7) { 0.0 }

                repeat(7) { index ->
                    val dayStart = weekStart.clone() as Calendar
                    dayStart.add(Calendar.DAY_OF_YEAR, index)

                    val dayEnd = dayStart.clone() as Calendar
                    dayEnd.add(Calendar.DAY_OF_YEAR, 1)

                    weekData[index] = transactionDao.getSalesTotal(
                        branchId = branchId,
                        from = dayStart.timeInMillis,
                        to = dayEnd.timeInMillis - 1
                    )
                }

                val monthStart = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    setStartOfDay()
                }

                val monthTotal = transactionDao.getSalesTotal(
                    branchId = branchId,
                    from = monthStart.timeInMillis,
                    to = now.timeInMillis
                )

                val monthTransactions = transactionDao.getTransactionCount(
                    branchId = branchId,
                    from = monthStart.timeInMillis,
                    to = now.timeInMillis
                )

                val daysInMonthSoFar = now.get(Calendar.DAY_OF_MONTH)
                val monthLineData = MutableList(daysInMonthSoFar) { 0.0 }

                repeat(daysInMonthSoFar) { index ->
                    val dayStart = monthStart.clone() as Calendar
                    dayStart.add(Calendar.DAY_OF_MONTH, index)

                    val dayEnd = dayStart.clone() as Calendar
                    dayEnd.add(Calendar.DAY_OF_MONTH, 1)

                    monthLineData[index] = transactionDao.getSalesTotal(
                        branchId = branchId,
                        from = dayStart.timeInMillis,
                        to = dayEnd.timeInMillis - 1
                    )
                }

                _uiState.update {
                    it.copy(
                        todaySales = todaySales,
                        yesterdaySales = yesterdaySales,
                        weekData = weekData,
                        monthTotal = monthTotal,
                        monthTransactions = monthTransactions,
                        todayCash = todayCash,
                        todayGcash = todayGcash,
                        monthLineData = monthLineData,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load sales summary."
                    )
                }
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