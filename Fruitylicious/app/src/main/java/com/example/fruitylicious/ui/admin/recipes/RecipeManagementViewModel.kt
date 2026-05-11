package com.example.fruitylicious.ui.admin.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitylicious.data.local.dao.ProductRecipeDao
import com.example.fruitylicious.data.local.entity.IngredientEntity
import com.example.fruitylicious.data.local.entity.ProductEntity
import com.example.fruitylicious.data.local.entity.ProductRecipeEntity
import com.example.fruitylicious.data.local.entity.ProductVariantEntity
import com.example.fruitylicious.data.repository.IngredientRepository
import com.example.fruitylicious.data.repository.ProductRepository
import com.example.fruitylicious.data.repository.ProductVariantRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject
import kotlin.math.absoluteValue

data class RecipeLineUi(
    val ingredientId: Int = 0,
    val quantity: String = "",
    val unit: String = ""
)

data class RecipeLineDraft(
    val packagingLines: List<RecipeLineUi>,
    val recipeLines: List<RecipeLineUi>
)

data class RecipeManagementUiState(
    val products: List<ProductEntity> = emptyList(),
    val addons: List<ProductEntity> = emptyList(),
    val productIdsWithRecipes: Set<Int> = emptySet(),
    val variantsByProductId: Map<Int, List<ProductVariantEntity>> = emptyMap(),
    val ingredients: List<IngredientEntity> = emptyList(),
    val selectedProduct: ProductEntity? = null,
    val selectedVariant: ProductVariantEntity? = null,
    val packagingLines: List<RecipeLineUi> = emptyList(),
    val recipeLines: List<RecipeLineUi> = emptyList(),
    val variantDrafts: Map<Int?, RecipeLineDraft> = emptyMap(),
    val isLoading: Boolean = true,
    val isRecipesLoaded: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class RecipeManagementViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val productVariantRepository: ProductVariantRepository,
    private val ingredientRepository: IngredientRepository,
    private val productRecipeDao: ProductRecipeDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecipeManagementUiState())
    val uiState: StateFlow<RecipeManagementUiState> = _uiState.asStateFlow()

    private var loadRecipeJob: Job? = null

    init {
        observeProducts()
        observeVariants()
        observeIngredients()
        observeRecipes()
    }

    private fun observeRecipes() {
        viewModelScope.launch {
            productRecipeDao.observeRecipes().collectLatest { recipes ->
                val ids = recipes.map { it.productId }.toSet()
                _uiState.update { it.copy(
                    productIdsWithRecipes = ids,
                    isRecipesLoaded = true
                ) }
            }
        }
    }

    private fun observeProducts() {
        viewModelScope.launch {
            productRepository.observeProducts().collectLatest { products ->
                val main = products.filter { !it.isAddon }
                val addons = products.filter { it.isAddon }
                _uiState.update {
                    it.copy(
                        products = main,
                        addons = addons,
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
                        variantsByProductId = variants.groupBy { variant -> variant.productId }
                    )
                }
            }
        }
    }

    private fun observeIngredients() {
        viewModelScope.launch {
            ingredientRepository.observeIngredients().collectLatest { ingredients ->
                _uiState.update {
                    it.copy(ingredients = ingredients)
                }
            }
        }
    }

    fun selectProduct(product: ProductEntity) {
        val variants = _uiState.value.variantsByProductId[product.productId].orEmpty()
        val firstVariant = variants.firstOrNull()

        _uiState.update {
            it.copy(
                selectedProduct = product,
                selectedVariant = firstVariant,
                recipeLines = emptyList(),
                packagingLines = emptyList(),
                variantDrafts = emptyMap(),
                error = null,
                successMessage = null
            )
        }

        if (firstVariant != null) {
            loadRecipe(firstVariant.variantId)
        } else {
            loadProductRecipe(product.productId)
        }
    }

    fun selectVariant(variant: ProductVariantEntity) {
        val currentState = _uiState.value
        val oldVariantId = currentState.selectedVariant?.variantId

        // Checkpoint: save current work into drafts
        val updatedDrafts = currentState.variantDrafts.toMutableMap()
        updatedDrafts[oldVariantId] = RecipeLineDraft(
            packagingLines = currentState.packagingLines,
            recipeLines = currentState.recipeLines
        )

        val existingDraft = updatedDrafts[variant.variantId]

        if (existingDraft != null) {
            _uiState.update {
                it.copy(
                    selectedVariant = variant,
                    packagingLines = existingDraft.packagingLines,
                    recipeLines = existingDraft.recipeLines,
                    variantDrafts = updatedDrafts,
                    error = null,
                    successMessage = null
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    selectedVariant = variant,
                    recipeLines = emptyList(),
                    packagingLines = emptyList(),
                    variantDrafts = updatedDrafts,
                    error = null,
                    successMessage = null
                )
            }
            loadRecipe(variant.variantId)
        }
    }

    private fun loadRecipe(variantId: Int) {
        loadRecipeJob?.cancel()
        loadRecipeJob = viewModelScope.launch {
            val recipes = productRecipeDao.getRecipesForVariant(variantId)
            mapRecipesToUi(recipes)
        }
    }

    private fun loadProductRecipe(productId: Int) {
        loadRecipeJob?.cancel()
        loadRecipeJob = viewModelScope.launch {
            val recipes = productRecipeDao.getRecipesForProduct(productId)
                .filter { it.variantId == null }
            mapRecipesToUi(recipes)
        }
    }

    private fun mapRecipesToUi(recipes: List<ProductRecipeEntity>) {
        val currentState = _uiState.value
        val ingredientsMap = currentState.ingredients.associateBy { it.ingredientId }

        val packagingEntities = recipes.filter { 
            ingredientsMap[it.ingredientId]?.isPackaging == true 
        }
        val ingredientEntities = recipes.filter { 
            ingredientsMap[it.ingredientId]?.isPackaging != true 
        }

        val pLines = if (packagingEntities.isEmpty()) {
            if (currentState.selectedProduct?.isAddon == true) emptyList() else listOf(RecipeLineUi())
        } else {
            packagingEntities.map { recipe ->
                val ingredient = ingredientsMap[recipe.ingredientId]
                RecipeLineUi(
                    ingredientId = recipe.ingredientId,
                    quantity = formatQuantity(recipe.quantityRequired),
                    unit = ingredient?.let { item -> recipeInputUnitFor(item) } ?: ""
                )
            }
        }

        val rLines = if (ingredientEntities.isEmpty()) {
            listOf(RecipeLineUi())
        } else {
            ingredientEntities.map { recipe ->
                val ingredient = ingredientsMap[recipe.ingredientId]

                RecipeLineUi(
                    ingredientId = recipe.ingredientId,
                    quantity = formatQuantity(recipe.quantityRequired),
                    unit = ingredient?.let { item ->
                        recipeInputUnitFor(item)
                    } ?: ""
                )
            }
        }

        _uiState.update { state ->
            val updatedDrafts = state.variantDrafts.toMutableMap()
            updatedDrafts[state.selectedVariant?.variantId] = RecipeLineDraft(pLines, rLines)
            
            state.copy(
                packagingLines = pLines,
                recipeLines = rLines,
                variantDrafts = updatedDrafts
            )
        }
    }

    private fun formatQuantity(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toInt().toString()
        } else {
            String.format(Locale.US, "%.2f", value)
        }
    }

    fun addLine() {
        _uiState.update {
            it.copy(recipeLines = it.recipeLines + RecipeLineUi())
        }
    }

    fun removeLine(index: Int) {
        _uiState.update {
            it.copy(
                recipeLines = it.recipeLines.filterIndexed { i, _ -> i != index }
                    .ifEmpty { listOf(RecipeLineUi()) }
            )
        }
    }

    fun addPackagingLine() {
        _uiState.update {
            it.copy(packagingLines = it.packagingLines + RecipeLineUi())
        }
    }

    fun removePackagingLine(index: Int) {
        _uiState.update { state ->
            state.copy(
                packagingLines = state.packagingLines.filterIndexed { i, _ -> i != index }
                    .ifEmpty { if (state.selectedProduct?.isAddon == true) emptyList() else listOf(RecipeLineUi()) }
            )
        }
    }

    fun updateLineIngredient(index: Int, ingredient: IngredientEntity?) {
        _uiState.update { state ->
            state.copy(
                recipeLines = state.recipeLines.mapIndexed { i, line ->
                    if (i == index) {
                        if (ingredient == null) {
                            line.copy(ingredientId = 0, unit = "")
                        } else {
                            line.copy(
                                ingredientId = ingredient.ingredientId,
                                unit = recipeInputUnitFor(ingredient)
                            )
                        }
                    } else {
                        line
                    }
                }
            )
        }
    }

    fun updatePackagingIngredient(index: Int, ingredient: IngredientEntity?) {
        _uiState.update { state ->
            state.copy(
                packagingLines = state.packagingLines.mapIndexed { i, line ->
                    if (i == index) {
                        if (ingredient == null) {
                            line.copy(ingredientId = 0, unit = "")
                        } else {
                            line.copy(
                                ingredientId = ingredient.ingredientId,
                                unit = recipeInputUnitFor(ingredient)
                            )
                        }
                    } else {
                        line
                    }
                }
            )
        }
    }

    fun updateLineQuantity(index: Int, quantity: String) {
        _uiState.update { state ->
            state.copy(
                recipeLines = state.recipeLines.mapIndexed { i, line ->
                    if (i == index) line.copy(quantity = quantity) else line
                }
            )
        }
    }

    fun updatePackagingQuantity(index: Int, quantity: String) {
        _uiState.update { state ->
            state.copy(
                packagingLines = state.packagingLines.mapIndexed { i, line ->
                    if (i == index) line.copy(quantity = quantity) else line
                }
            )
        }
    }

    fun saveRecipe() {
        val state = _uiState.value
        val product = state.selectedProduct
        val variant = state.selectedVariant

        if (product == null) {
            setError("Select a product first.")
            return
        }

        // If product has variants, one must be selected. If no variants, variant can be null.
        val variants = state.variantsByProductId[product.productId].orEmpty()
        if (variants.isNotEmpty() && variant == null) {
            setError("Select a size first.")
            return
        }

        val validPackagingLines = state.packagingLines.mapNotNull { line ->
            val quantity = line.quantity.toDoubleOrNull()
            if (line.ingredientId > 0 && quantity != null && quantity > 0.0) {
                line to quantity
            } else {
                null
            }
        }

        if (!product.isAddon && validPackagingLines.isEmpty()) {
            setError("At least one packaging item is required for main products.")
            return
        }

        val validRegularLines = state.recipeLines.mapNotNull { line ->
            val quantity = line.quantity.toDoubleOrNull()
            if (line.ingredientId > 0 && quantity != null && quantity > 0.0) {
                line to quantity
            } else {
                null
            }
        }

        if (validRegularLines.isEmpty()) {
            setError("Add at least one valid ingredient.")
            return
        }

        viewModelScope.launch {
            val now = System.currentTimeMillis()

            if (variant != null) {
                productRecipeDao.softDeleteRecipesByVariant(variant.variantId, now)
            } else {
                productRecipeDao.softDeleteRecipesByProduct(product.productId, now)
            }

            val allLinesToSave = mutableListOf<Pair<RecipeLineUi, Double>>()
            allLinesToSave.addAll(validPackagingLines)
            allLinesToSave.addAll(validRegularLines)

            val recipes = allLinesToSave.map { (line, quantity) ->
                ProductRecipeEntity(
                    recipeId = generateId(),
                    productId = product.productId,
                    variantId = variant?.variantId,
                    ingredientId = line.ingredientId,
                    quantityRequired = quantity,
                    lastModified = now,
                    isSynced = false,
                    syncedAt = null
                )
            }

            productRecipeDao.upsertRecipes(recipes)

            _uiState.update {
                it.copy(
                    successMessage = "Recipe saved.",
                    error = null
                )
            }
        }
    }

    fun dismissDialog() {
        _uiState.update {
            it.copy(
                selectedProduct = null,
                selectedVariant = null,
                recipeLines = emptyList(),
                packagingLines = emptyList(),
                variantDrafts = emptyMap(),
                error = null,
                successMessage = null
            )
        }
    }

    private fun setError(message: String) {
        _uiState.update {
            it.copy(error = message, successMessage = null)
        }
    }

    private fun generateId(): Int {
        return System.nanoTime().hashCode().absoluteValue
    }

    private fun recipeInputUnitFor(ingredient: IngredientEntity): String {
        return ingredient.unitType
    }
}
