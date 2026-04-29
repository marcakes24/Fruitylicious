package com.example.fruitylicious.ui.staff.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitylicious.data.repository.CartItem
import com.example.fruitylicious.data.repository.CartRepository
import com.example.fruitylicious.domain.usecase.pos.CreateTransactionUseCase
import com.example.fruitylicious.util.BranchConfig
import com.example.fruitylicious.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CheckoutUiState(
    val cartItems: List<CartItem> = emptyList(),
    val totalAmount: Double = 0.0,
    val paymentType: String = "Cash",
    val isLoading: Boolean = false,
    val error: String? = null,
    val completedTransactionId: String? = null
)

@HiltViewModel
class CheckoutViewModel @Inject constructor(
    private val createTransactionUseCase: CreateTransactionUseCase,
    private val sessionManager: SessionManager,
    private val branchConfig: BranchConfig,
    private val cartRepository: CartRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CheckoutUiState())
    val uiState: StateFlow<CheckoutUiState> = _uiState.asStateFlow()

    init {
        observeCart()
    }

    private fun observeCart() {
        viewModelScope.launch {
            cartRepository.cartItems.collectLatest { cartItems ->
                _uiState.update {
                    it.copy(
                        cartItems = cartItems,
                        totalAmount = cartItems.sumOf { item -> item.subtotal }
                    )
                }
            }
        }
    }

    fun onPaymentTypeChanged(paymentType: String) {
        _uiState.update {
            it.copy(
                paymentType = paymentType,
                error = null
            )
        }
    }

    fun completePayment() {
        val state = _uiState.value

        if (state.cartItems.isEmpty()) {
            _uiState.update {
                it.copy(error = "Cart is empty.")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, error = null)
            }

            val completedAt = System.currentTimeMillis()

            val result = createTransactionUseCase(
                userId = sessionManager.getUserId(),
                branchId = sessionManager.getBranchId().takeIf { it > 0 } ?: branchConfig.branchId,
                cartItems = state.cartItems,
                paymentType = state.paymentType
            )

            result.onSuccess { transactionId ->
                cartRepository.saveReceiptSummary(
                    transactionId = transactionId,
                    paymentType = state.paymentType,
                    totalAmount = state.totalAmount,
                    completedAt = completedAt
                )

                cartRepository.clearCart()
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    completedTransactionId = result.getOrNull(),
                    error = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun consumeCompletedTransaction() {
        _uiState.update {
            it.copy(completedTransactionId = null)
        }
    }
}