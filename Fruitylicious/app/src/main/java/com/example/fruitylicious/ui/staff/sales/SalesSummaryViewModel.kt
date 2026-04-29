package com.example.fruitylicious.ui.staff.sales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitylicious.data.repository.TransactionRepository
import com.example.fruitylicious.util.BranchConfig
import com.example.fruitylicious.util.DateTimeUtil
import com.example.fruitylicious.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SalesSummaryUiState(
    val branchName: String = "",
    val dateText: String = "",
    val completedSalesTotal: Double = 0.0,
    val completedTransactionCount: Int = 0,
    val voidedTransactionCount: Int = 0,
    val averageTransactionValue: Double = 0.0,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class SalesSummaryViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    sessionManager: SessionManager,
    branchConfig: BranchConfig
) : ViewModel() {

    private val branchId = sessionManager.getBranchId().takeIf { it > 0 } ?: branchConfig.branchId
    private val from = DateTimeUtil.startOfToday()
    private val to = DateTimeUtil.endOfToday()

    private val _uiState = MutableStateFlow(
        SalesSummaryUiState(
            branchName = branchConfig.branchName,
            dateText = DateTimeUtil.formatDate(System.currentTimeMillis())
        )
    )
    val uiState: StateFlow<SalesSummaryUiState> = _uiState.asStateFlow()

    init {
        observeTodayTransactions()
    }

    private fun observeTodayTransactions() {
        viewModelScope.launch {
            transactionRepository.observeTransactionsByDateRange(branchId, from, to).collectLatest { transactions ->
                val completedTransactions = transactions.filter { it.status == "completed" }
                val completedTotal = completedTransactions.sumOf { it.totalAmount }
                val completedCount = completedTransactions.size

                _uiState.update {
                    it.copy(
                        completedSalesTotal = completedTotal,
                        completedTransactionCount = completedCount,
                        voidedTransactionCount = transactions.count { transaction -> transaction.status == "void" },
                        averageTransactionValue = if (completedCount > 0) completedTotal / completedCount else 0.0,
                        isLoading = false
                    )
                }
            }
        }
    }
}