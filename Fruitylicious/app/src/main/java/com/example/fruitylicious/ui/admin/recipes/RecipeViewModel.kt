package com.example.fruitylicious.ui.admin.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitylicious.data.local.entity.IngredientEntity
import com.example.fruitylicious.data.local.entity.ProductEntity
import com.example.fruitylicious.data.local.entity.ProductRecipeEntity
import com.example.fruitylicious.data.repository.IngredientRepository
import com.example.fruitylicious.data.repository.ProductRepository
import com.example.fruitylicious.domain.usecase.admin.ManageRecipeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import kotlin.math.absoluteValue

data class RecipeUiState(
    val recipes: List<ProductRecipeEntity> = emptyList(),
    val products: List<ProductEntity> = emptyList(),
    val ingredients: List<IngredientEntity> = emptyList(),

    val productNames: Map<Int, String> = emptyMap(),
    val ingredientNames: Map<Int, String> = emptyMap(),
    val ingredientUnits: Map<Int, String> = emptyMap(),

    val selectedProductId: Int = 0,
    val selectedProductName: String = "",
    val selectedIngredientId: Int = 0,
    val selectedIngredientName: String = "",

    val quantityRequired: String = "",

    val productError: String? = null,
    val ingredientError: String? = null,
    val quantityRequiredError: String? = null,

    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val successMessage: String? = null,
    val error: String? = null
)

@HiltViewModel
class RecipeViewModel @Inject constructor(
    private val manageRecipeUseCase: ManageRecipeUseCase,
    private val productRepository: ProductRepository,
    private val ingredientRepository: IngredientRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecipeUiState())
    val uiState: StateFlow<RecipeUiState> = _uiState.asStateFlow()

    init {
        observeRecipes()
        observeProducts()
        observeIngredients()
    }

    private fun observeRecipes() {
        viewModelScope.launch {
            manageRecipeUseCase.observeRecipes().collectLatest { recipes ->
                _uiState.update {
                    it.copy(recipes = recipes)
                }
            }
        }
    }

    private fun observeProducts() {
        viewModelScope.launch {
            productRepository.observeProducts().collectLatest { products ->
                _uiState.update { state ->
                    val selectedProduct = products.firstOrNull {
                        it.productId == state.selectedProductId
                    }

                    state.copy(
                        products = products,
                        productNames = products.associate {
                            it.productId to it.productName
                        },
                        selectedProductName = selectedProduct?.productName.orEmpty()
                            .ifBlank { state.selectedProductName }
                    )
                }
            }
        }
    }

    private fun observeIngredients() {
        viewModelScope.launch {
            ingredientRepository.observeIngredients().collectLatest { ingredients ->
                _uiState.update { state ->
                    val selectedIngredient = ingredients.firstOrNull {
                        it.ingredientId == state.selectedIngredientId
                    }

                    state.copy(
                        ingredients = ingredients,
                        ingredientNames = ingredients.associate {
                            it.ingredientId to it.ingredientName
                        },
                        ingredientUnits = ingredients.associate {
                            it.ingredientId to it.unitType
                        },
                        selectedIngredientName = selectedIngredient?.ingredientName.orEmpty()
                            .ifBlank { state.selectedIngredientName }
                    )
                }
            }
        }
    }

    fun onProductSelected(productId: Int) {
        val product = _uiState.value.products.firstOrNull {
            it.productId == productId
        }

        _uiState.update {
            it.copy(
                selectedProductId = productId,
                selectedProductName = product?.productName.orEmpty(),
                productError = null,
                error = null,
                successMessage = null
            )
        }
    }

    fun onIngredientSelected(ingredientId: Int) {
        val ingredient = _uiState.value.ingredients.firstOrNull {
            it.ingredientId == ingredientId
        }

        _uiState.update {
            it.copy(
                selectedIngredientId = ingredientId,
                selectedIngredientName = ingredient?.ingredientName.orEmpty(),
                ingredientError = null,
                error = null,
                successMessage = null
            )
        }
    }

    fun onQuantityRequiredChanged(value: String) {
        _uiState.update {
            it.copy(
                quantityRequired = value,
                quantityRequiredError = null,
                error = null,
                successMessage = null
            )
        }
    }

    fun saveRecipe() {
        val state = _uiState.value
        val quantityValue = state.quantityRequired.toDoubleOrNull()

        val productError = if (state.selectedProductId <= 0) {
            "Product is required."
        } else {
            null
        }

        val ingredientError = if (state.selectedIngredientId <= 0) {
            "Ingredient is required."
        } else {
            null
        }

        val quantityError = when {
            state.quantityRequired.isBlank() -> "Quantity required is required."
            quantityValue == null -> "Quantity required must be numeric."
            quantityValue <= 0.0 -> "Quantity required must be greater than zero."
            else -> null
        }

        if (productError != null || ingredientError != null || quantityError != null) {
            _uiState.update {
                it.copy(
                    productError = productError,
                    ingredientError = ingredientError,
                    quantityRequiredError = quantityError,
                    successMessage = null
                )
            }
            return
        }

        val duplicateRecipe = state.recipes.firstOrNull {
            it.productId == state.selectedProductId &&
                    it.ingredientId == state.selectedIngredientId
        }

        if (duplicateRecipe != null) {
            _uiState.update {
                it.copy(
                    error = "This ingredient is already part of the selected product recipe.",
                    successMessage = null
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSaving = true,
                    error = null,
                    successMessage = null
                )
            }

            val result = manageRecipeUseCase.saveRecipe(
                recipeId = generateRecipeId(),
                productId = state.selectedProductId,
                ingredientId = state.selectedIngredientId,
                quantityRequired = quantityValue ?: 0.0
            )

            _uiState.update {
                if (result.isSuccess) {
                    it.copy(
                        selectedIngredientId = 0,
                        selectedIngredientName = "",
                        quantityRequired = "",
                        productError = null,
                        ingredientError = null,
                        quantityRequiredError = null,
                        isSaving = false,
                        successMessage = "Recipe line saved.",
                        error = null
                    )
                } else {
                    it.copy(
                        isSaving = false,
                        successMessage = null,
                        error = result.exceptionOrNull()?.message ?: "Failed to save recipe line."
                    )
                }
            }
        }
    }

    fun deleteRecipe(recipeId: Int) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isDeleting = true,
                    error = null,
                    successMessage = null
                )
            }

            val result = manageRecipeUseCase.deleteRecipe(recipeId)

            _uiState.update {
                if (result.isSuccess) {
                    it.copy(
                        isDeleting = false,
                        successMessage = "Recipe line deleted.",
                        error = null
                    )
                } else {
                    it.copy(
                        isDeleting = false,
                        error = result.exceptionOrNull()?.message ?: "Failed to delete recipe line."
                    )
                }
            }
        }
    }

    fun deleteRecipesForSelectedProduct() {
        val productId = _uiState.value.selectedProductId

        if (productId <= 0) {
            _uiState.update {
                it.copy(error = "Select a product first.")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isDeleting = true,
                    error = null,
                    successMessage = null
                )
            }

            val result = manageRecipeUseCase.deleteRecipesForProduct(productId)

            _uiState.update {
                if (result.isSuccess) {
                    it.copy(
                        isDeleting = false,
                        successMessage = "All recipe lines for selected product deleted.",
                        error = null
                    )
                } else {
                    it.copy(
                        isDeleting = false,
                        error = result.exceptionOrNull()?.message
                            ?: "Failed to delete product recipe lines."
                    )
                }
            }
        }
    }

    fun clearForm() {
        _uiState.update {
            it.copy(
                selectedProductId = 0,
                selectedProductName = "",
                selectedIngredientId = 0,
                selectedIngredientName = "",
                quantityRequired = "",
                productError = null,
                ingredientError = null,
                quantityRequiredError = null,
                successMessage = null,
                error = null
            )
        }
    }

    fun clearMessages() {
        _uiState.update {
            it.copy(
                successMessage = null,
                error = null
            )
        }
    }

    private fun generateRecipeId(): Int {
        return UUID.randomUUID()
            .mostSignificantBits
            .hashCode()
            .absoluteValue
    }
}