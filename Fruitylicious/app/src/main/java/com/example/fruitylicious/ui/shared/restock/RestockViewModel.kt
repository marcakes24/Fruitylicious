package com.example.fruitylicious.ui.shared.restock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.example.fruitylicious.data.local.dao.AuditLogDao
import com.example.fruitylicious.data.local.dao.IngredientDao
import com.example.fruitylicious.data.local.dao.InventoryDao
import com.example.fruitylicious.data.local.dao.RestockLogDao
import com.example.fruitylicious.data.local.db.PosDatabase
import com.example.fruitylicious.data.local.entity.AuditLogEntity
import com.example.fruitylicious.data.local.entity.IngredientEntity
import com.example.fruitylicious.data.local.entity.InventoryEntity
import com.example.fruitylicious.data.local.entity.RestockLogEntity
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

data class RestockIngredientRow(
    val ingredientId: Int,
    val branchId: Int,
    val ingredientName: String,
    val currentStock: Double,
    val unitType: String
)

data class RestockHistoryRow(
    val restockId: String,
    val ingredientName: String,
    val supplier: String,
    val quantityAdded: Double,
    val unitType: String,
    val branchId: Int,
    val dateTime: Long
)

data class RestockUiState(
    val ingredients: List<RestockIngredientRow> = emptyList(),
    val history: List<RestockHistoryRow> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class RestockViewModel @Inject constructor(
    private val database: PosDatabase,
    private val ingredientDao: IngredientDao,
    private val inventoryDao: InventoryDao,
    private val restockLogDao: RestockLogDao,
    private val auditLogDao: AuditLogDao,
    private val sessionManager: SessionManager,
    private val branchConfig: BranchConfig
) : ViewModel() {

    private val _uiState = MutableStateFlow(RestockUiState())
    val uiState: StateFlow<RestockUiState> = _uiState.asStateFlow()

    private var ingredients: List<IngredientEntity> = emptyList()
    private var inventory: List<InventoryEntity> = emptyList()
    private var restockLogs: List<RestockLogEntity> = emptyList()

    init {
        observeIngredients()
        observeInventory()
        observeRestockLogs()
    }

    private fun observeIngredients() {
        viewModelScope.launch {
            ingredientDao.observeIngredients().collectLatest { items ->
                ingredients = items
                rebuildState()
            }
        }
    }

    private fun observeInventory() {
        viewModelScope.launch {
            inventoryDao.observeAllInventory().collectLatest { items ->
                inventory = items
                rebuildState()
            }
        }
    }

    private fun observeRestockLogs() {
        viewModelScope.launch {
            restockLogDao.observeAllRestockLogs().collectLatest { items ->
                restockLogs = items
                rebuildState()
            }
        }
    }

    private fun rebuildState() {
        val inventoryMap = inventory.associateBy { "${it.ingredientId}:${it.branchId}" }
        val ingredientMap = ingredients.associateBy { it.ingredientId }

        val branchIds = listOf(1, 2)

        val ingredientRows = ingredients.flatMap { ingredient ->
            branchIds.map { branchId ->
                val inv = inventoryMap["${ingredient.ingredientId}:$branchId"]

                RestockIngredientRow(
                    ingredientId = ingredient.ingredientId,
                    branchId = branchId,
                    ingredientName = ingredient.ingredientName,
                    currentStock = inv?.currentStock ?: 0.0,
                    unitType = ingredient.unitType
                )
            }
        }.sortedWith(
            compareBy<RestockIngredientRow> { it.branchId }
                .thenBy { it.ingredientName.lowercase() }
        )

        val historyRows = restockLogs.map { log ->
            val ingredient = ingredientMap[log.ingredientId]

            RestockHistoryRow(
                restockId = log.restockId,
                ingredientName = ingredient?.ingredientName ?: "Unknown ingredient",
                supplier = log.supplier,
                quantityAdded = log.quantityAdded,
                unitType = ingredient?.unitType ?: "",
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

    fun submitRestock(
        ingredient: RestockIngredientRow?,
        quantityText: String,
        supplier: String
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

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val userId = sessionManager.getUserId()
            val branchId = ingredient.branchId.takeIf { it > 0 } ?: branchConfig.branchId

            database.withTransaction {
                val existingInventory = inventoryDao.getInventoryItem(
                    ingredientId = ingredient.ingredientId,
                    branchId = branchId
                )

                if (existingInventory == null) {
                    inventoryDao.upsertInventoryItem(
                        InventoryEntity(
                            ingredientId = ingredient.ingredientId,
                            branchId = branchId,
                            currentStock = quantity,
                            lastModified = now,
                            isSynced = false,
                            syncedAt = null
                        )
                    )
                } else {
                    inventoryDao.addStock(
                        ingredientId = ingredient.ingredientId,
                        branchId = branchId,
                        amount = quantity,
                        lastModified = now
                    )
                }

                restockLogDao.upsertRestockLog(
                    RestockLogEntity(
                        restockId = UUID.randomUUID().toString(),
                        ingredientId = ingredient.ingredientId,
                        branchId = branchId,
                        userId = userId,
                        quantityAdded = quantity,
                        supplier = supplier.ifBlank { "N/A" },
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
                        action = "Restocked ${ingredient.ingredientName}: $quantity ${ingredient.unitType}.",
                        tableAffected = "restock_logs",
                        timestamp = now,
                        lastModified = now,
                        isSynced = false,
                        syncedAt = null
                    )
                )
            }

            _uiState.update {
                it.copy(
                    successMessage = "Restock saved.",
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