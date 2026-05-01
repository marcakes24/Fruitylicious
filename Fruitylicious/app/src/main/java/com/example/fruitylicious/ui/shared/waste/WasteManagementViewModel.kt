package com.example.fruitylicious.ui.shared.waste

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.example.fruitylicious.data.local.dao.AuditLogDao
import com.example.fruitylicious.data.local.dao.IngredientDao
import com.example.fruitylicious.data.local.dao.InventoryDao
import com.example.fruitylicious.data.local.dao.WasteLogDao
import com.example.fruitylicious.data.local.db.PosDatabase
import com.example.fruitylicious.data.local.entity.AuditLogEntity
import com.example.fruitylicious.data.local.entity.IngredientEntity
import com.example.fruitylicious.data.local.entity.InventoryEntity
import com.example.fruitylicious.data.local.entity.WasteLogEntity
import com.example.fruitylicious.util.BranchConfig
import com.example.fruitylicious.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class WasteIngredientRow(
    val ingredientId: Int,
    val branchId: Int,
    val ingredientName: String,
    val currentStock: Double,
    val unitType: String
)

data class WasteHistoryRow(
    val wasteId: String,
    val ingredientName: String,
    val quantity: Double,
    val unitType: String,
    val reason: String,
    val branchId: Int,
    val dateTime: Long
)

data class WasteManagementUiState(
    val ingredients: List<WasteIngredientRow> = emptyList(),
    val history: List<WasteHistoryRow> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class WasteManagementViewModel @Inject constructor(
    private val database: PosDatabase,
    private val inventoryDao: InventoryDao,
    private val ingredientDao: IngredientDao,
    private val wasteLogDao: WasteLogDao,
    private val auditLogDao: AuditLogDao,
    private val sessionManager: SessionManager,
    private val branchConfig: BranchConfig
) : ViewModel() {

    private val _uiState = MutableStateFlow(WasteManagementUiState())
    val uiState: StateFlow<WasteManagementUiState> = _uiState.asStateFlow()

    private var inventory: List<InventoryEntity> = emptyList()
    private var ingredients: List<IngredientEntity> = emptyList()
    private var wasteLogs: List<WasteLogEntity> = emptyList()

    init {
        observeInventory()
        observeIngredients()
        observeWasteLogs()
    }

    private fun observeInventory() {
        viewModelScope.launch {
            inventoryDao.observeAllInventory().collectLatest { items ->
                inventory = items
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

    private fun observeWasteLogs() {
        viewModelScope.launch {
            wasteLogDao.observeAllWasteLogs().collectLatest { items ->
                wasteLogs = items
                rebuildState()
            }
        }
    }

    private fun rebuildState() {
        val ingredientMap = ingredients.associateBy { it.ingredientId }

        val ingredientRows = inventory.mapNotNull { item ->
            val ingredient = ingredientMap[item.ingredientId] ?: return@mapNotNull null

            WasteIngredientRow(
                ingredientId = item.ingredientId,
                branchId = item.branchId,
                ingredientName = ingredient.ingredientName,
                currentStock = item.currentStock,
                unitType = ingredient.unitType
            )
        }.sortedWith(
            compareBy<WasteIngredientRow> { it.branchId }
                .thenBy { it.ingredientName.lowercase() }
        )

        val historyRows = wasteLogs.map { log ->
            val ingredient = ingredientMap[log.ingredientId]

            WasteHistoryRow(
                wasteId = log.wasteId,
                ingredientName = ingredient?.ingredientName ?: "Unknown ingredient",
                quantity = log.quantity,
                unitType = ingredient?.unitType ?: "",
                reason = log.reason,
                branchId = log.branchId,
                dateTime = log.dateTime
            )
        }

        _uiState.update {
            it.copy(
                ingredients = ingredientRows,
                history = historyRows,
                isLoading = false
            )
        }
    }

    fun submitWaste(
        ingredient: WasteIngredientRow?,
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

        if (ingredient.currentStock < quantity) {
            setError("Insufficient stock.")
            return
        }

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val userId = sessionManager.getUserId()
            val branchId = ingredient.branchId.takeIf { it > 0 } ?: branchConfig.branchId

            database.withTransaction {
                inventoryDao.deductStock(
                    ingredientId = ingredient.ingredientId,
                    branchId = branchId,
                    amount = quantity,
                    lastModified = now
                )

                wasteLogDao.upsertWasteLog(
                    WasteLogEntity(
                        wasteId = UUID.randomUUID().toString(),
                        ingredientId = ingredient.ingredientId,
                        branchId = branchId,
                        userId = userId,
                        quantity = quantity,
                        reason = reason.ifBlank { "Waste entry" },
                        image = null,
                        dateTime = now,
                        lastModified = now,
                        isSynced = false,
                        syncedAt = null
                    )
                )

                auditLogDao.upsertAuditLog(
                    AuditLogEntity(
                        logId = UUID.randomUUID().toString(),
                        userId = userId,
                        branchId = branchId,
                        action = "Recorded waste for ${ingredient.ingredientName}: $quantity ${ingredient.unitType}.",
                        tableAffected = "waste_logs",
                        timestamp = now,
                        lastModified = now,
                        isSynced = false,
                        syncedAt = null
                    )
                )
            }

            _uiState.update {
                it.copy(
                    successMessage = "Waste entry saved.",
                    error = null
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.update {
            it.copy(error = null, successMessage = null)
        }
    }

    private fun setError(message: String) {
        _uiState.update {
            it.copy(error = message, successMessage = null)
        }
    }
}