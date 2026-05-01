package com.example.fruitylicious.ui.staff.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitylicious.data.repository.CartItem
import com.example.fruitylicious.data.repository.CartRepository
import com.example.fruitylicious.data.repository.TransactionRepository
import com.example.fruitylicious.util.BranchConfig
import com.example.fruitylicious.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CheckoutReceiptItem(
    val productName: String,
    val sizeName: String,
    val quantity: Int,
    val subtotal: Double,
    val addonsText: String
)

data class CheckoutUiState(
    val cartItems: List<CartItem> = emptyList(),
    val totalAmount: Double = 0.0,
    val receiptItems: List<CheckoutReceiptItem> = emptyList(),
    val transactionId: String = "",
    val paymentType: String = "Cash",
    val receivedAmount: Double = 0.0,
    val change: Double = 0.0,
    val completedAt: Long = 0L,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class CheckoutViewModel @Inject constructor(
    private val cartRepository: CartRepository,
    private val transactionRepository: TransactionRepository,
    private val sessionManager: SessionManager,
    private val branchConfig: BranchConfig
) : ViewModel() {

    private val _uiState = MutableStateFlow(CheckoutUiState())
    val uiState: StateFlow<CheckoutUiState> = _uiState.asStateFlow()

    init {
        observeCart()
    }

    private fun observeCart() {
        viewModelScope.launch {
            cartRepository.cartItems.collectLatest { items ->
                _uiState.update {
                    it.copy(
                        cartItems = items,
                        totalAmount = items.sumOf { item -> item.subtotal }
                    )
                }
            }
        }
    }

    fun confirmPayment(
        paymentType: String,
        receivedAmountText: String
    ) {
        val state = _uiState.value
        val cartItems = state.cartItems
        val totalAmount = state.totalAmount

        if (cartItems.isEmpty()) {
            setError("Cart is empty.")
            return
        }

        val cleanPaymentType = paymentType.trim()

        val receivedAmount = if (cleanPaymentType.equals("Cash", ignoreCase = true)) {
            receivedAmountText.toDoubleOrNull() ?: 0.0
        } else {
            totalAmount
        }

        if (cleanPaymentType.equals("Cash", ignoreCase = true) && receivedAmount < totalAmount) {
            setError("Amount received must be greater than or equal to total.")
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null
                )
            }

            val receiptItems = cartItems.map { item ->
                CheckoutReceiptItem(
                    productName = item.productName,
                    sizeName = item.sizeName,
                    quantity = item.quantity,
                    subtotal = item.subtotal,
                    addonsText = item.addons.joinToString(", ") { addon -> addon.addonName }
                )
            }

            val result = transactionRepository.createTransaction(
                userId = sessionManager.getUserId(),
                branchId = branchConfig.branchId,
                cartItems = cartItems,
                paymentType = cleanPaymentType
            )

            result.fold(
                onSuccess = { transactionId ->
                    val now = System.currentTimeMillis()
                    val change = if (cleanPaymentType.equals("Cash", ignoreCase = true)) {
                        receivedAmount - totalAmount
                    } else {
                        0.0
                    }

                    cartRepository.saveReceiptSummary(
                        transactionId = transactionId,
                        paymentType = cleanPaymentType,
                        totalAmount = totalAmount,
                        completedAt = now
                    )

                    cartRepository.clearCart()

                    _uiState.update {
                        it.copy(
                            receiptItems = receiptItems,
                            transactionId = transactionId,
                            paymentType = cleanPaymentType,
                            receivedAmount = receivedAmount,
                            change = change,
                            completedAt = now,
                            isLoading = false,
                            error = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to create transaction."
                        )
                    }
                }
            )
        }
    }

    fun clearError() {
        _uiState.update {
            it.copy(error = null)
        }
    }

    private fun setError(message: String) {
        _uiState.update {
            it.copy(
                error = message,
                isLoading = false
            )
        }
    }
}