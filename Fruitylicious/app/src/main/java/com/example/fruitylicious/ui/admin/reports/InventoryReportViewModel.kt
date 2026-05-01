package com.example.fruitylicious.ui.admin.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitylicious.data.local.dao.IngredientDao
import com.example.fruitylicious.data.local.dao.InventoryDao
import com.example.fruitylicious.data.local.entity.IngredientEntity
import com.example.fruitylicious.data.local.entity.InventoryEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InventoryReportRow(
    val ingredientId: Int,
    val branchId: Int,
    val ingredientName: String,
    val category: String,
    val currentStock: Double,
    val unitType: String,
    val lowStockThreshold: Double
)

data class InventoryReportUiState(
    val rows: List<InventoryReportRow> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class InventoryReportViewModel @Inject constructor(
    private val inventoryDao: InventoryDao,
    private val ingredientDao: IngredientDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryReportUiState())
    val uiState: StateFlow<InventoryReportUiState> = _uiState.asStateFlow()

    private var inventoryItems: List<InventoryEntity> = emptyList()
    private var ingredients: List<IngredientEntity> = emptyList()

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

            InventoryReportRow(
                ingredientId = inventory.ingredientId,
                branchId = inventory.branchId,
                ingredientName = ingredient.ingredientName,
                category = if (ingredient.isPackaging) {
                    "Packaging"
                } else {
                    "Ingredients"
                },
                currentStock = inventory.currentStock,
                unitType = ingredient.unitType,
                lowStockThreshold = ingredient.lowStockThreshold
            )
        }.sortedWith(
            compareBy<InventoryReportRow> { it.branchId }
                .thenBy { it.ingredientName.lowercase() }
        )

        _uiState.update {
            it.copy(
                rows = rows,
                isLoading = false,
                error = null
            )
        }
    }
}