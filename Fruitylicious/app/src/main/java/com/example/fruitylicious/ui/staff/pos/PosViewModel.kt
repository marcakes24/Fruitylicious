package com.example.fruitylicious.ui.staff.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitylicious.data.local.entity.IngredientEntity
import com.example.fruitylicious.data.local.entity.InventoryEntity
import com.example.fruitylicious.data.local.entity.ProductEntity
import com.example.fruitylicious.data.local.entity.ProductRecipeEntity
import com.example.fruitylicious.data.local.entity.ProductVariantEntity
import com.example.fruitylicious.data.repository.CartAddon
import com.example.fruitylicious.data.repository.CartItem
import com.example.fruitylicious.data.repository.CartRepository
import com.example.fruitylicious.data.repository.IngredientRepository
import com.example.fruitylicious.data.repository.InventoryRepository
import com.example.fruitylicious.data.repository.ProductRepository
import com.example.fruitylicious.data.repository.ProductVariantRepository
import com.example.fruitylicious.data.repository.RecipeRepository
import com.example.fruitylicious.data.repository.StaffLogRepository
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

data class PosUiState(
    val products: List<ProductEntity> = emptyList(),
    val addons: List<ProductEntity> = emptyList(),
    val recipes: List<ProductRecipeEntity> = emptyList(),
    val inventory: List<InventoryEntity> = emptyList(),
    val ingredients: List<IngredientEntity> = emptyList(),
    val cartItems: List<CartItem> = emptyList(),
    val variantsByProductId: Map<Int, List<ProductVariantEntity>> = emptyMap(),
    val totalAmount: Double = 0.0,
    val isLoading: Boolean = true,
    val isClockedIn: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class PosViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val cartRepository: CartRepository,
    private val productVariantRepository: ProductVariantRepository,
    private val recipeRepository: RecipeRepository,
    private val inventoryRepository: InventoryRepository,
    private val ingredientRepository: IngredientRepository,
    private val staffLogRepository: StaffLogRepository,
    private val sessionManager: SessionManager,
    private val branchConfig: BranchConfig
) : ViewModel() {

    private val _uiState = MutableStateFlow(PosUiState())
    val uiState: StateFlow<PosUiState> = _uiState.asStateFlow()

    init {
        observeProducts()
        observeAddons()
        observeCart()
        observeVariants()
        observeRecipes()
        observeInventory()
        observeIngredients()
        checkClockInStatus()
    }

    private fun checkClockInStatus() {
        viewModelScope.launch {
            val role = sessionManager.getRole()
            if (role?.equals("admin", ignoreCase = true) == true) {
                _uiState.update { it.copy(isClockedIn = true) }
                return@launch
            }

            val userId = sessionManager.getUserId()
            val activeLog = staffLogRepository.getActiveLogForUser(userId)
            
            _uiState.update { 
                it.copy(
                    isClockedIn = activeLog != null,
                    error = if (activeLog == null) "You must clock in before using the POS." else null
                )
            }
        }
    }

    private fun observeProducts() {
        viewModelScope.launch {
            productRepository.observeProducts().collectLatest { products ->
                val mainProducts = products.filter { !it.isAddon }
                _uiState.update {
                    it.copy(
                        products = mainProducts,
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

    private fun observeRecipes() {
        viewModelScope.launch {
            recipeRepository.observeRecipes().collectLatest { recipes ->
                _uiState.update {
                    it.copy(recipes = recipes)
                }
            }
        }
    }

    private fun observeInventory() {
        viewModelScope.launch {
            inventoryRepository.observeInventory(branchConfig.branchId).collectLatest { inventory ->
                _uiState.update {
                    it.copy(inventory = inventory)
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