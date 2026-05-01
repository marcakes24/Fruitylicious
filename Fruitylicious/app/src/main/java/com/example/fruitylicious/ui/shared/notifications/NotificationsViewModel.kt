package com.example.fruitylicious.ui.shared.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitylicious.data.local.dao.IngredientDao
import com.example.fruitylicious.data.local.dao.InventoryDao
import com.example.fruitylicious.data.local.entity.IngredientEntity
import com.example.fruitylicious.data.local.entity.InventoryEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationRow(
    val ingredientId: Int,
    val branchId: Int,
    val name: String,
    val currentStock: Double,
    val unitType: String,
    val lowStockThreshold: Double,
    val status: String,
    val progress: Float
)

data class NotificationsUiState(
    val notifications: List<NotificationRow> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val inventoryDao: InventoryDao,
    private val ingredientDao: IngredientDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

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
                rebuildNotifications()
            }
        }
    }

    private fun observeIngredients() {
        viewModelScope.launch {
            ingredientDao.observeIngredients().collectLatest { items ->
                ingredients = items
                rebuildNotifications()
            }
        }
    }

    private fun rebuildNotifications() {
        val ingredientMap = ingredients.associateBy { it.ingredientId }

        val rows = inventoryItems.mapNotNull { inventory ->
            val ingredient = ingredientMap[inventory.ingredientId] ?: return@mapNotNull null
            val threshold = ingredient.lowStockThreshold

            if (threshold <= 0.0) {
                return@mapNotNull null
            }

            val status = when {
                inventory.currentStock <= threshold -> "Critical"
                inventory.currentStock <= threshold * 2 -> "Warning"
                else -> null
            } ?: return@mapNotNull null

            val progress = (inventory.currentStock / threshold)
                .toFloat()
                .coerceIn(0f, 1f)

            NotificationRow(
                ingredientId = ingredient.ingredientId,
                branchId = inventory.branchId,
                name = ingredient.ingredientName,
                currentStock = inventory.currentStock,
                unitType = ingredient.unitType,
                lowStockThreshold = threshold,
                status = status,
                progress = progress
            )
        }.sortedWith(
            compareBy<NotificationRow> {
                when (it.status) {
                    "Critical" -> 0
                    "Warning" -> 1
                    else -> 2
                }
            }.thenBy { it.name.lowercase() }
        )

        _uiState.update {
            it.copy(
                notifications = rows,
                isLoading = false,
                error = null
            )
        }
    }
}