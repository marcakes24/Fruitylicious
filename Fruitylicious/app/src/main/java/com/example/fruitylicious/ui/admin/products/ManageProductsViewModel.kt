package com.example.fruitylicious.ui.admin.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitylicious.data.local.entity.ProductEntity
import com.example.fruitylicious.data.local.entity.ProductVariantEntity
import com.example.fruitylicious.data.repository.ProductVariantRepository
import com.example.fruitylicious.domain.usecase.admin.ManageProductUseCase
import com.example.fruitylicious.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.absoluteValue

data class ProductVariantRow(
    val product: ProductEntity,
    val variants: List<ProductVariantEntity>
)

data class ManageProductsUiState(
    val products: List<ProductEntity> = emptyList(),
    val addons: List<ProductEntity> = emptyList(),
    val variants: List<ProductVariantEntity> = emptyList(),
    val productRows: List<ProductVariantRow> = emptyList(),
    val isAdmin: Boolean = false,
    val userBranchId: String = "B1",
    val isLoading: Boolean = true,
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class ManageProductsViewModel @Inject constructor(
    private val manageProductUseCase: ManageProductUseCase,
    private val productVariantRepository: ProductVariantRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ManageProductsUiState(
            isAdmin = sessionManager.getRole()?.equals("admin", ignoreCase = true) == true,
            userBranchId = "B${sessionManager.getBranchId()}"
        )
    )
    val uiState: StateFlow<ManageProductsUiState> = _uiState.asStateFlow()

    init {
        observeProducts()
        observeVariants()
    }

    private fun observeProducts() {
        viewModelScope.launch {
            manageProductUseCase.observeProducts().collectLatest { products ->
                _uiState.update { state ->
                    val mainProducts = products.filter { !it.isAddon }
                    val addons = products.filter { it.isAddon }

                    state.copy(
                        products = mainProducts,
                        addons = addons,
                        productRows = buildRows(mainProducts, state.variants),
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun observeVariants() {
        viewModelScope.launch {
            productVariantRepository.observeAllVariants().collectLatest { variants ->
                _uiState.update { state ->
                    state.copy(
                        variants = variants,
                        productRows = buildRows(state.products, variants),
                        isLoading = false
                    )
                }
            }
        }
    }

    fun saveFullProduct(
        existingProductId: Int?,
        name: String,
        image: String?,
        mediumPrice: Double,
        largePrice: Double
    ) {
        val cleanName = name.trim()

        if (cleanName.isBlank()) {
            setError("Product name is required.")
            return
        }

        if (mediumPrice < 0.0 || largePrice < 0.0) {
            setError("Price cannot be negative.")
            return
        }

        viewModelScope.launch {
            val productId = existingProductId ?: generateId()

            val productResult = manageProductUseCase.saveProduct(
                productId = productId,
                image = image?.takeIf { it.isNotBlank() },
                productName = cleanName,
                isAddon = false,
                price = 0.0
            )

            if (productResult.isFailure) {
                setError(productResult.exceptionOrNull()?.message ?: "Failed to save product.")
                return@launch
            }

            // For variants, we need to check if they exist or create new ones
            val existingVariants = productVariantRepository.getVariantsForProduct(productId)
            val mediumVariant = existingVariants.find { it.sizeName.equals("Medium", ignoreCase = true) }
            val largeVariant = existingVariants.find { it.sizeName.equals("Large", ignoreCase = true) }

            val mediumResult = productVariantRepository.saveVariant(
                variantId = mediumVariant?.variantId ?: generateId(),
                productId = productId,
                sizeName = "Medium",
                price = mediumPrice
            )

            val largeResult = productVariantRepository.saveVariant(
                variantId = largeVariant?.variantId ?: generateId(),
                productId = productId,
                sizeName = "Large",
                price = largePrice
            )

            _uiState.update {
                if (mediumResult.isSuccess && largeResult.isSuccess) {
                    it.copy(
                        successMessage = "Product saved with Medium and Large sizes.",
                        error = null
                    )
                } else {
                    val error = mediumResult.exceptionOrNull()?.message 
                        ?: largeResult.exceptionOrNull()?.message 
                        ?: "Failed to save variants."
                    it.copy(error = error)
                }
            }
        }
    }

    fun saveAddon(
        existingAddonId: Int?,
        name: String,
        image: String?,
        price: Double
    ) {
        val cleanName = name.trim()

        if (cleanName.isBlank()) {
            setError("Add-on name is required.")
            return
        }

        if (price < 0.0) {
            setError("Price cannot be negative.")
            return
        }

        viewModelScope.launch {
            val result = manageProductUseCase.saveProduct(
                productId = existingAddonId ?: generateId(),
                image = image?.takeIf { it.isNotBlank() },
                productName = cleanName,
                isAddon = true,
                price = price
            )

            _uiState.update {
                if (result.isSuccess) {
                    it.copy(successMessage = "Add-on saved.", error = null)
                } else {
                    it.copy(error = result.exceptionOrNull()?.message ?: "Failed to save add-on.")
                }
            }
        }
    }

    fun deleteFullProduct(productId: Int) {
        viewModelScope.launch {
            val result = manageProductUseCase.deleteProduct(productId)

            _uiState.update {
                if (result.isSuccess) {
                    it.copy(successMessage = "Whole product deleted.", error = null)
                } else {
                    it.copy(error = result.exceptionOrNull()?.message ?: "Failed to delete product.")
                }
            }
        }
    }

    fun deleteAddon(productId: Int) {
        viewModelScope.launch {
            val result = manageProductUseCase.deleteProduct(productId)

            _uiState.update {
                if (result.isSuccess) {
                    it.copy(successMessage = "Add-on deleted.", error = null)
                } else {
                    it.copy(error = result.exceptionOrNull()?.message ?: "Failed to delete add-on.")
                }
            }
        }
    }

    fun updateProductImage(
        productId: Int,
        imagePath: String?
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            manageProductUseCase.updateProductImage(
                productId = productId,
                imagePath = imagePath,
                lastModified = now
            )
        }
    }

    fun clearMessages() {
        _uiState.update {
            it.copy(error = null, successMessage = null)
        }
    }

    private fun buildRows(
        products: List<ProductEntity>,
        variants: List<ProductVariantEntity>
    ): List<ProductVariantRow> {
        val variantsByProduct = variants.groupBy { it.productId }

        return products.map { product ->
            ProductVariantRow(
                product = product,
                variants = variantsByProduct[product.productId] ?: emptyList()
            )
        }.sortedBy { it.product.productName.lowercase() }
    }

    private fun setError(message: String) {
        _uiState.update {
            it.copy(error = message, successMessage = null)
        }
    }

    private fun generateId(): Int {
        return System.currentTimeMillis().hashCode().absoluteValue
    }
}