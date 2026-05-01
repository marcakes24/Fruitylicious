package com.example.fruitylicious.ui.shared.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitylicious.data.local.dao.IngredientDao
import com.example.fruitylicious.data.local.dao.InventoryDao
import com.example.fruitylicious.data.local.entity.IngredientEntity
import com.example.fruitylicious.data.local.entity.InventoryEntity
import com.example.fruitylicious.util.SessionManager
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
    val isAdmin: Boolean = false,
    val userBranchId: String = "B1",
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class InventoryMonitoringViewModel @Inject constructor(
    private val inventoryDao: InventoryDao,
    private val ingredientDao: IngredientDao,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        InventoryMonitoringUiState(
            isAdmin = sessionManager.getRole()?.equals("admin", ignoreCase = true) == true,
            userBranchId = "B${sessionManager.getBranchId()}"
        )
    )
    val uiState: StateFlow<InventoryMonitoringUiState> = _uiState.asStateFlow()

    private var inventoryItems = emptyList<InventoryEntity>()
    private var ingredients = emptyList<IngredientEntity>()

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
