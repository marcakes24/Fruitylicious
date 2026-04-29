package com.example.fruitylicious.ui.staff.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitylicious.data.repository.CartRepository
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

data class ReceiptUiState(
    val transactionId: String = "",
    val branchName: String = "",
    val cashierName: String = "",
    val dateTimeText: String = "",
    val paymentType: String = "Cash",
    val status: String = "completed",
    val totalAmount: Double = 0.0
)

@HiltViewModel
class ReceiptViewModel @Inject constructor(
    sessionManager: SessionManager,
    branchConfig: BranchConfig,
    private val cartRepository: CartRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ReceiptUiState(
            branchName = branchConfig.branchName,
            cashierName = sessionManager.getUserName().ifBlank { "Staff" },
            dateTimeText = DateTimeUtil.formatDateTime(System.currentTimeMillis())
        )
    )
    val uiState: StateFlow<ReceiptUiState> = _uiState.asStateFlow()

    init {
        observeReceiptSummary()
    }

    private fun observeReceiptSummary() {
        viewModelScope.launch {
            cartRepository.lastReceiptSummary.collectLatest { receipt ->
                if (receipt != null) {
                    _uiState.update {
                        it.copy(
                            transactionId = receipt.transactionId,
                            paymentType = receipt.paymentType,
                            totalAmount = receipt.totalAmount,
                            dateTimeText = DateTimeUtil.formatDateTime(receipt.completedAt),
                            status = "completed"
                        )
                    }
                }
            }
        }
    }
}