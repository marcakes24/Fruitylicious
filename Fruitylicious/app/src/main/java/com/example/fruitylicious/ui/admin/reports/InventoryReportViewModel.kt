package com.example.fruitylicious.ui.admin.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitylicious.data.local.dao.IngredientDao
import com.example.fruitylicious.data.local.dao.InventoryDao
import com.example.fruitylicious.data.local.entity.IngredientEntity
import com.example.fruitylicious.data.local.entity.InventoryEntity
import com.example.fruitylicious.data.repository.ReportRepository
import com.example.fruitylicious.util.NetworkMonitor
import com.example.fruitylicious.util.SessionManager
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
    private val ingredientDao: IngredientDao,
    private val reportRepository: ReportRepository,
    private val networkMonitor: NetworkMonitor,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryReportUiState())
    val uiState: StateFlow<InventoryReportUiState> = _uiState.asStateFlow()

    private var inventoryItems: List<InventoryEntity> = emptyList()
    private var ingredients: List<IngredientEntity> = emptyList()
    private var selectedBranchId: Int? = null

    init {
        observeInventory()
        observeIngredients()
    }

    fun loadReport(branchId: Int?) {
        selectedBranchId = branchId
        
        val localBranchId = sessionManager.getBranchId()
        val isOnline = networkMonitor.isOnline()

        if (branchId != null && branchId != localBranchId && isOnline) {
            fetchRemoteReport(branchId)
        } else {
            rebuildRows()
        }
    }

    private fun fetchRemoteReport(branchId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            reportRepository.getInventoryReport(branchId).onSuccess { reportDto ->
                val ingredientMap = ingredients.associateBy { it.ingredientId }
                val rows = reportDto.items.map { item ->
                    val ingredient = ingredientMap[item.ingredientId]
                    InventoryReportRow(
                        ingredientId = item.ingredientId,
                        branchId = branchId,
                        ingredientName = item.ingredientName,
                        category = if (ingredient?.isPackaging == true) "Packaging" else "Ingredients",
                        currentStock = item.currentStock,
                        unitType = item.unitType,
                        lowStockThreshold = item.lowStockThreshold
                    )
                }
                _uiState.update { it.copy(rows = rows, isLoading = false, error = null) }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load remote inventory.") }
            }
        }
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
        val branchId = selectedBranchId
        val localBranchId = sessionManager.getBranchId()
        
        val filteredInventory = when {
            branchId == null -> inventoryItems // All local inventory
            branchId == localBranchId -> inventoryItems.filter { it.branchId == localBranchId }
            else -> emptyList() // Remote handled separately
        }

        val rows = if (branchId == null) {
            // Aggregate by ingredient across all branches
            filteredInventory.groupBy { it.ingredientId }.mapNotNull { (ingredientId, items) ->
                val ingredient = ingredientMap[ingredientId] ?: return@mapNotNull null
                InventoryReportRow(
                    ingredientId = ingredientId,
                    branchId = 0, // 0 as placeholder for "All"
                    ingredientName = ingredient.ingredientName,
                    category = if (ingredient.isPackaging) "Packaging" else "Ingredients",
                    currentStock = items.sumOf { it.currentStock },
                    unitType = ingredient.unitType,
                    lowStockThreshold = ingredient.lowStockThreshold
                )
            }
        } else {
            filteredInventory.mapNotNull { inventory ->
                val ingredient = ingredientMap[inventory.ingredientId] ?: return@mapNotNull null
                InventoryReportRow(
                    ingredientId = inventory.ingredientId,
                    branchId = inventory.branchId,
                    ingredientName = ingredient.ingredientName,
                    category = if (ingredient.isPackaging) "Packaging" else "Ingredients",
                    currentStock = inventory.currentStock,
                    unitType = ingredient.unitType,
                    lowStockThreshold = ingredient.lowStockThreshold
                )
            }
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