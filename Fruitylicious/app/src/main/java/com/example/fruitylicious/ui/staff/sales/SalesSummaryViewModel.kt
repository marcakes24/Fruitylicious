package com.example.fruitylicious.ui.staff.sales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitylicious.data.local.dao.TransactionDao
import com.example.fruitylicious.util.BranchConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StaffSalesSummaryUiState(
    val todaySales: Double = 0.0,
    val transactionCount: Int = 0,
    val cashTotal: Double = 0.0,
    val gcashTotal: Double = 0.0,
    val pendingCount: Int = 0,
    val branchId: Int = 1,
    val branchName: String = "",
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class StaffSalesSummaryViewModel @Inject constructor(
    private val transactionDao: TransactionDao,
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
    }

    fun loadTodaySales() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, error = null)
            }

            val now = System.currentTimeMillis()
            val todayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            try {
                val todaySales = transactionDao.getTodaySalesForBranch(
                    branchId = branchId,
                    from = todayStart,
                    to = now
                )

                val transactionCount = transactionDao.getTodayTransactionCountForBranch(
                    branchId = branchId,
                    from = todayStart,
                    to = now
                )

                val cashTotal = transactionDao.getTodayPaymentTotalForBranch(
                    branchId = branchId,
                    paymentType = "Cash",
                    from = todayStart,
                    to = now
                )

                val gcashTotal = transactionDao.getTodayPaymentTotalForBranch(
                    branchId = branchId,
                    paymentType = "Gcash",
                    from = todayStart,
                    to = now
                )

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
}