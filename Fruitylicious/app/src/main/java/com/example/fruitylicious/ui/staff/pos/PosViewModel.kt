package com.example.fruitylicious.ui.staff.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitylicious.data.local.entity.ProductEntity
import com.example.fruitylicious.data.local.entity.ProductVariantEntity
import com.example.fruitylicious.data.repository.CartAddon
import com.example.fruitylicious.data.repository.CartItem
import com.example.fruitylicious.data.repository.CartRepository
import com.example.fruitylicious.data.repository.ProductRepository
import com.example.fruitylicious.data.repository.ProductVariantRepository
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
    val addons: List<ProductEntity> = emptyList(),
    val cartItems: List<CartItem> = emptyList(),
    val variantsByProductId: Map<Int, List<ProductVariantEntity>> = emptyMap(),
    val totalAmount: Double = 0.0,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class PosViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val cartRepository: CartRepository,
    private val productVariantRepository: ProductVariantRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PosUiState())
    val uiState: StateFlow<PosUiState> = _uiState.asStateFlow()

    init {
        observeProducts()
        observeAddons()
        observeCart()
        observeVariants()
    }

    private fun observeProducts() {
        viewModelScope.launch {
            productRepository.observeMainProducts().collectLatest { products ->
                _uiState.update {
                    it.copy(
                        products = products,
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun observeVariants() {
        viewModelScope.launch {
            productVariantRepository.observeAllVariants().collectLatest { variants ->
                _uiState.update {
                    it.copy(
                        variantsByProductId = variants.groupBy { it.productId }
                    )
                }
            }
        }
    }

    private fun observeAddons() {
        viewModelScope.launch {
            productRepository.observeAddons().collectLatest { addons ->
                _uiState.update {
                    it.copy(addons = addons)
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

    fun addCustomizedItem(
        product: ProductEntity,
        variant: ProductVariantEntity,
        mixAddon: ProductEntity?,
        selectedAddons: List<ProductEntity>,
        quantity: Int
    ) {
        val addons = buildList {
            mixAddon?.let {
                add(
                    CartAddon(
                        addonProductId = it.productId,
                        addonName = it.productName,
                        quantity = 1,
                        unitPrice = it.price,
                        subtotal = it.price
                    )
                )
            }

            selectedAddons.forEach { addon ->
                add(
                    CartAddon(
                        addonProductId = addon.productId,
                        addonName = addon.productName,
                        quantity = 1,
                        unitPrice = addon.price,
                        subtotal = addon.price
                    )
                )
            }
        }

        val subtotal = (variant.price + addons.sumOf { it.subtotal }) * quantity

        cartRepository.addCustomItem(
            CartItem(
                productId = product.productId,
                variantId = variant.variantId,
                productName = product.productName,
                sizeName = variant.sizeName,
                quantity = quantity,
                unitPrice = variant.price,
                subtotal = subtotal,
                addons = addons
            )
        )
    }

    fun updateQuantity(cartLineId: String, delta: Int) {
        cartRepository.updateQuantity(cartLineId, delta)
    }

    fun removeItem(cartLineId: String) {
        cartRepository.removeLine(cartLineId)
    }

    fun clearCart() {
        cartRepository.clearCart()
    }
}