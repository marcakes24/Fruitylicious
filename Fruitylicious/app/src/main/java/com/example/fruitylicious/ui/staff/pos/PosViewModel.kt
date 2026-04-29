package com.example.fruitylicious.ui.staff.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitylicious.data.local.entity.ProductEntity
import com.example.fruitylicious.data.repository.CartItem
import com.example.fruitylicious.data.repository.CartRepository
import com.example.fruitylicious.data.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PosUiState(
    val products: List<ProductEntity> = emptyList(),
    val cartItems: List<CartItem> = emptyList(),
    val totalAmount: Double = 0.0,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class PosViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val cartRepository: CartRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PosUiState(isLoading = true))
    val uiState: StateFlow<PosUiState> = _uiState.asStateFlow()

    init {
        observeProducts()
        observeCart()
    }

    private fun observeProducts() {
        viewModelScope.launch {
            productRepository.observeProducts().collectLatest { products ->
                _uiState.update {
                    it.copy(
                        products = products,
                        isLoading = false
                    )
                }
            }
        }
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

    fun addToCart(product: ProductEntity) {
        cartRepository.addProduct(
            productId = product.productId,
            productName = product.productName,
            unitPrice = product.price
        )
    }

    fun removeFromCart(productId: Int) {
        cartRepository.removeProduct(productId)
    }

    fun clearCart() {
        cartRepository.clearCart()
    }
}