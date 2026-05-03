package com.example.fruitylicious.ui.shared.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitylicious.data.local.dao.IngredientDao
import com.example.fruitylicious.data.local.dao.InventoryAdjustmentDao
import com.example.fruitylicious.data.local.dao.InventoryDao
import com.example.fruitylicious.data.local.entity.IngredientEntity
import com.example.fruitylicious.data.local.entity.InventoryAdjustmentEntity
import com.example.fruitylicious.data.local.entity.InventoryEntity
import com.example.fruitylicious.data.repository.AdjustmentRepository
import com.example.fruitylicious.util.BranchConfig
import com.example.fruitylicious.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.abs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdjustmentIngredientRow(
    val ingredientId: Int,
    val branchId: Int,
    val ingredientName: String,
    val currentStock: Double,
    val unitType: String
)

data class AdjustmentHistoryRow(
    val adjustmentId: String,
    val ingredientName: String,
    val adjustmentType: String,
    val quantity: Double,
    val reason: String,
    val dateTime: Long,
    val branchId: Int
)

data class InventoryAdjustmentUiState(
    val ingredients: List<AdjustmentIngredientRow> = emptyList(),
    val history: List<AdjustmentHistoryRow> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class InventoryAdjustmentViewModel @Inject constructor(
    private val adjustmentRepository: AdjustmentRepository,
    private val inventoryDao: InventoryDao,
    private val ingredientDao: IngredientDao,
    private val adjustmentDao: InventoryAdjustmentDao,
    private val sessionManager: SessionManager,
    private val branchConfig: BranchConfig
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryAdjustmentUiState())
    val uiState: StateFlow<InventoryAdjustmentUiState> = _uiState.asStateFlow()

    private var inventoryItems: List<InventoryEntity> = emptyList()
    private var ingredients: List<IngredientEntity> = emptyList()
    private var adjustments: List<InventoryAdjustmentEntity> = emptyList()

    init {
        observeInventory()
        observeIngredients()
        observeAdjustments()
    }

    private fun observeInventory() {
        viewModelScope.launch {
            inventoryDao.observeAllInventory().collectLatest { items ->
                inventoryItems = items
                rebuildState()
            }
        }
    }

    private fun observeIngredients() {
        viewModelScope.launch {
            ingredientDao.observeIngredients().collectLatest { items ->
                ingredients = items
                rebuildState()
            }
        }
    }

    private fun observeAdjustments() {
        viewModelScope.launch {
            adjustmentDao.observeAllAdjustments().collectLatest { items ->
                adjustments = items
                rebuildState()
            }
        }
    }

    private fun rebuildState() {
        val ingredientMap = ingredients.associateBy { it.ingredientId }

        val ingredientRows = inventoryItems.mapNotNull { inventory ->
            val ingredient = ingredientMap[inventory.ingredientId] ?: return@mapNotNull null

            AdjustmentIngredientRow(
                ingredientId = inventory.ingredientId,
                branchId = inventory.branchId,
                ingredientName = ingredient.ingredientName,
                currentStock = inventory.currentStock,
                unitType = ingredient.unitType
            )
        }.sortedBy { it.ingredientName.lowercase() }

        val historyRows = adjustments.map { adjustment ->
            val isAdd = adjustment.adjustmentAmount >= 0.0

            AdjustmentHistoryRow(
                adjustmentId = adjustment.adjustmentId,
                ingredientName = ingredientMap[adjustment.ingredientId]?.ingredientName ?: "Unknown ingredient",
                adjustmentType = if (isAdd) "Add" else "Reduce",
                quantity = abs(adjustment.adjustmentAmount),
                reason = adjustment.reason,
                dateTime = adjustment.dateTime,
                branchId = adjustment.branchId
            )
        }

        _uiState.update {
            it.copy(
                ingredients = ingredientRows,
                history = historyRows,
                isLoading = false,
                error = null
            )
        }
    }

    fun submitAdjustment(
        ingredient: AdjustmentIngredientRow?,
        type: String,
        quantityText: String,
        reason: String
    ) {
        if (ingredient == null) {
            setError("Select an ingredient.")
            return
        }

        val quantity = quantityText.toDoubleOrNull()

        if (quantity == null || quantity <= 0.0) {
            setError("Enter a valid quantity.")
            return
        }

        if (type == "Reduce" && ingredient.currentStock < quantity) {
            setError("Insufficient stock.")
            return
        }

        viewModelScope.launch {
            val userId = sessionManager.getUserId()
            val branchId = ingredient.branchId.takeIf { it > 0 } ?: branchConfig.branchId

            val signedAdjustmentAmount = if (type == "Add") {
                quantity
            } else {
                -quantity
            }

            val result = adjustmentRepository.adjustInventory(
                ingredientId = ingredient.ingredientId,
                branchId = branchId,
                userId = userId,
                adjustmentAmount = signedAdjustmentAmount,
                reason = reason.ifBlank { "Inventory adjustment" }
            )

            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        successMessage = "Adjustment saved.",
                        error = null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        error = result.exceptionOrNull()?.message ?: "Failed to save adjustment.",
                        successMessage = null
                    )
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update {
            it.copy(
                error = null,
                successMessage = null
            )
        }
    }

    private fun setError(message: String) {
        _uiState.update {
            it.copy(
                error = message,
                successMessage = null
            )
        }
    }
}