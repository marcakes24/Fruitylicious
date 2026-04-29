package com.example.fruitylicious.ui.admin.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitylicious.data.local.entity.ProductEntity
import com.example.fruitylicious.domain.usecase.admin.ManageProductUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.absoluteValue

data class ProductUiState(
    val products: List<ProductEntity> = emptyList(),
    val productName: String = "",
    val price: String = "",
    val image: String = "",
    val isAddon: Boolean = false,
    val productNameError: String? = null,
    val priceError: String? = null,
    val isSaving: Boolean = false,
    val successMessage: String? = null,
    val error: String? = null
)

@HiltViewModel
class ProductViewModel @Inject constructor(
    private val manageProductUseCase: ManageProductUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductUiState())
    val uiState: StateFlow<ProductUiState> = _uiState.asStateFlow()

    init {
        observeProducts()
    }

    private fun observeProducts() {
        viewModelScope.launch {
            manageProductUseCase.observeProducts().collectLatest { products ->
                _uiState.update {
                    it.copy(products = products)
                }
            }
        }
    }

    fun onProductNameChanged(value: String) {
        _uiState.update {
            it.copy(productName = value, productNameError = null, error = null, successMessage = null)
        }
    }

    fun onPriceChanged(value: String) {
        _uiState.update {
            it.copy(price = value, priceError = null, error = null, successMessage = null)
        }
    }

    fun onImageChanged(value: String) {
        _uiState.update {
            it.copy(image = value, error = null, successMessage = null)
        }
    }

    fun onIsAddonChanged(value: Boolean) {
        _uiState.update {
            it.copy(isAddon = value)
        }
    }

    fun saveProduct() {
        val state = _uiState.value
        val priceValue = state.price.toDoubleOrNull()

        val nameError = if (state.productName.trim().isBlank()) "Product name is required." else null
        val priceError = when {
            state.price.isBlank() -> "Price is required."
            priceValue == null -> "Price must be numeric."
            priceValue < 0.0 -> "Price cannot be negative."
            else -> null
        }

        if (nameError != null || priceError != null) {
            _uiState.update {
                it.copy(productNameError = nameError, priceError = priceError)
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(isSaving = true, error = null, successMessage = null)
            }

            val generatedId = System.currentTimeMillis().hashCode().absoluteValue

            val result = manageProductUseCase.saveProduct(
                productId = generatedId,
                image = state.image.ifBlank { null },
                productName = state.productName,
                isAddon = state.isAddon,
                price = priceValue ?: 0.0
            )

            _uiState.update {
                if (result.isSuccess) {
                    it.copy(
                        productName = "",
                        price = "",
                        image = "",
                        isAddon = false,
                        isSaving = false,
                        successMessage = "Product saved.",
                        error = null
                    )
                } else {
                    it.copy(
                        isSaving = false,
                        error = result.exceptionOrNull()?.message
                    )
                }
            }
        }
    }

    fun deleteProduct(productId: Int) {
        viewModelScope.launch {
            val result = manageProductUseCase.deleteProduct(productId)
            _uiState.update {
                it.copy(error = result.exceptionOrNull()?.message)
            }
        }
    }
}