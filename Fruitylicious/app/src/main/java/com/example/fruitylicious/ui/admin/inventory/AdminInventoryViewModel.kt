package com.example.fruitylicious.ui.admin.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitylicious.data.local.entity.InventoryEntity
import com.example.fruitylicious.data.repository.IngredientRepository
import com.example.fruitylicious.domain.usecase.inventory.CheckLowStockUseCase
import com.example.fruitylicious.domain.usecase.inventory.GetInventoryUseCase
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

data class AdminInventoryUiState(
    val branchId: Int = 0,
    val branchName: String = "",
    val inventory: List<InventoryEntity> = emptyList(),
    val ingredientNames: Map<Int, String> = emptyMap(),
    val ingredientUnits: Map<Int, String> = emptyMap(),
    val lowStockIngredientIds: Set<Int> = emptySet(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class AdminInventoryViewModel @Inject constructor(
    private val getInventoryUseCase: GetInventoryUseCase,
    private val checkLowStockUseCase: CheckLowStockUseCase,
    private val ingredientRepository: IngredientRepository,
    sessionManager: SessionManager,
    branchConfig: BranchConfig
) : ViewModel() {

    private val branchId = sessionManager.getBranchId().takeIf { it > 0 } ?: branchConfig.branchId

    private val _uiState = MutableStateFlow(
        AdminInventoryUiState(
            branchId = branchId,
            branchName = branchConfig.branchName
        )
    )
    val uiState: StateFlow<AdminInventoryUiState> = _uiState.asStateFlow()

    init {
        observeInventory()
        observeIngredients()
        observeLowStock()
    }

    private fun observeInventory() {
        viewModelScope.launch {
            getInventoryUseCase(branchId).collectLatest { inventory ->
                _uiState.update {
                    it.copy(
                        inventory = inventory,
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun observeIngredients() {
        viewModelScope.launch {
            ingredientRepository.observeIngredients().collectLatest { ingredients ->
                _uiState.update {
                    it.copy(
                        ingredientNames = ingredients.associate { ingredient ->
                            ingredient.ingredientId to ingredient.ingredientName
                        },
                        ingredientUnits = ingredients.associate { ingredient ->
                            ingredient.ingredientId to ingredient.unitType
                        }
                    )
                }
            }
        }
    }

    private fun observeLowStock() {
        viewModelScope.launch {
            checkLowStockUseCase(branchId).collectLatest { lowStock ->
                _uiState.update {
                    it.copy(
                        lowStockIngredientIds = lowStock.map { item -> item.ingredientId }.toSet()
                    )
                }
            }
        }
    }
}