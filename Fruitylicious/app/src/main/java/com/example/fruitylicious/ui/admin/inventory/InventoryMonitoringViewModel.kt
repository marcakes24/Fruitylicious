package com.example.fruitylicious.ui.admin.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitylicious.data.local.dao.IngredientDao
import com.example.fruitylicious.data.local.dao.InventoryDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InventoryMonitoringRow(
    val ingredientId: Int,
    val branchId: Int,
    val ingredientName: String,
    val currentStock: Double,
    val unitType: String,
    val lowStockThreshold: Double,
    val lastModified: Long
) {
    val status: String
        get() {
            return if (lowStockThreshold > 0.0) {
                when {
                    currentStock <= lowStockThreshold -> "Low"
                    currentStock <= lowStockThreshold * 2 -> "Normal"
                    else -> "Good"
                }
            } else {
                when {
                    currentStock >= 50.0 -> "Good"
                    currentStock >= 20.0 -> "Normal"
                    else -> "Low"
                }
            }
        }
}

data class InventoryMonitoringUiState(
    val rows: List<InventoryMonitoringRow> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class InventoryMonitoringViewModel @Inject constructor(
    private val inventoryDao: InventoryDao,
    private val ingredientDao: IngredientDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryMonitoringUiState())
    val uiState: StateFlow<InventoryMonitoringUiState> = _uiState.asStateFlow()

    private var inventoryItems = emptyList<com.example.fruitylicious.data.local.entity.InventoryEntity>()
    private var ingredients = emptyList<com.example.fruitylicious.data.local.entity.IngredientEntity>()

    init {
        observeInventory()
        observeIngredients()
    }

    private fun observeInventory() {
        viewModelScope.launch {
            inventoryDao.observeAllInventory().collectLatest { items ->
                inventoryItems = items
                rebuildRows()
            }
        }
    }

    private fun observeIngredients() {
        viewModelScope.launch {
            ingredientDao.observeIngredients().collectLatest { items ->
                ingredients = items
                rebuildRows()
            }
        }
    }

    private fun rebuildRows() {
        val ingredientMap = ingredients.associateBy { it.ingredientId }

        val rows = inventoryItems.mapNotNull { inventory ->
            val ingredient = ingredientMap[inventory.ingredientId] ?: return@mapNotNull null

            InventoryMonitoringRow(
                ingredientId = inventory.ingredientId,
                branchId = inventory.branchId,
                ingredientName = ingredient.ingredientName,
                currentStock = inventory.currentStock,
                unitType = ingredient.unitType,
                lowStockThreshold = ingredient.lowStockThreshold,
                lastModified = inventory.lastModified
            )
        }.sortedBy { it.ingredientName.lowercase() }

        _uiState.update {
            it.copy(
                rows = rows,
                isLoading = false,
                error = null
            )
        }
    }
}
