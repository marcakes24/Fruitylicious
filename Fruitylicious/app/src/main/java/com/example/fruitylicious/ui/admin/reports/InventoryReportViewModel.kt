package com.example.fruitylicious.ui.admin.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitylicious.data.local.dao.BranchDao
import com.example.fruitylicious.data.local.dao.IngredientDao
import com.example.fruitylicious.data.local.dao.InventoryDao
import com.example.fruitylicious.data.local.entity.IngredientEntity
import com.example.fruitylicious.data.local.entity.InventoryEntity
import com.example.fruitylicious.data.remote.dto.InventoryReportItemDto
import com.example.fruitylicious.data.repository.ReportRepository
import com.example.fruitylicious.util.NetworkMonitor
import com.example.fruitylicious.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InventoryReportRow(
    val ingredientId: Int,
    val branchId: Int,
    val ingredientName: String,
    val category: String,
    val currentStock: Double,
    val unitType: String,
    val lowStockThreshold: Double,
    val image: String? = null
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
    private val branchDao: BranchDao,
    private val reportRepository: ReportRepository,
    private val networkMonitor: NetworkMonitor,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryReportUiState())
    val uiState: StateFlow<InventoryReportUiState> = _uiState.asStateFlow()

    private var localInventoryItems: List<InventoryEntity> = emptyList()
    private var localIngredients: List<IngredientEntity> = emptyList()
    private var selectedBranchId: Int? = sessionManager.getBranchId()

    init {
        observeInventory()
        observeIngredients()
    }

    fun loadReport(
        branchId: Int?
    ) {
        selectedBranchId = branchId

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null
                )
            }

            val localBranchId = sessionManager.getBranchId()
            val isOnline = networkMonitor.isOnline()
            val isAdmin = isAdminUser()
            val isRemoteNeeded = isAdmin && isOnline && (branchId == null || branchId != localBranchId)

            // 1. Load Local first as placeholder ONLY if remote is NOT needed
            if (!isRemoteNeeded) {
                loadLocalReport(localBranchId)
            }

            // 2. Then Load Remote if needed and possible
            if (isRemoteNeeded) {
                _uiState.update { it.copy(isLoading = true) }
                if (branchId == null) {
                    loadRemoteAllBranchesReport()
                } else {
                    loadRemoteBranchReport(branchId)
                }
            }
        }
    }

    private fun observeInventory() {
        viewModelScope.launch {
            inventoryDao.observeAllInventory().collectLatest { items ->
                localInventoryItems = items

                val localBranchId = sessionManager.getBranchId()
                val isOnline = networkMonitor.isOnline()
                val isAdmin = isAdminUser()

                if (!isAdmin || !isOnline) {
                    loadLocalReport(localBranchId)
                }
            }
        }
    }

    private fun observeIngredients() {
        viewModelScope.launch {
            ingredientDao.observeIngredients().collectLatest { items ->
                localIngredients = items

                val localBranchId = sessionManager.getBranchId()
                val isOnline = networkMonitor.isOnline()
                val isAdmin = isAdminUser()

                if (!isAdmin || !isOnline) {
                    loadLocalReport(localBranchId)
                }
            }
        }
    }

    private fun loadLocalReport(
        branchId: Int
    ) {
        val ingredientMap = localIngredients.associateBy {
            it.ingredientId
        }

        val rows = localInventoryItems
            .filter {
                it.branchId == branchId
            }
            .mapNotNull { inventory ->
                val ingredient = ingredientMap[inventory.ingredientId]
                    ?: return@mapNotNull null

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
                    lowStockThreshold = ingredient.lowStockThreshold,
                    image = ingredient.image
                )
            }
            .sortedBy {
                it.ingredientName.lowercase()
            }

        _uiState.update {
            it.copy(
                rows = rows,
                isLoading = false,
                error = null
            )
        }
    }

    private suspend fun loadRemoteBranchReport(
        branchId: Int
    ) {
        val result = reportRepository.getInventoryReport(
            branchId = branchId
        )

        result.fold(
            onSuccess = { reportDto ->
                val rows = reportDto.items
                    .map { item ->
                        item.toInventoryReportRow(
                            branchId = reportDto.branchId ?: branchId
                        )
                    }
                    .sortedBy {
                        it.ingredientName.lowercase()
                    }

                _uiState.update {
                    it.copy(
                        rows = rows,
                        isLoading = false,
                        error = null
                    )
                }
            },
            onFailure = { exception ->
                _uiState.update {
                    it.copy(
                        rows = emptyList(),
                        isLoading = false
                    )
                }
            }
        )
    }

    private suspend fun loadRemoteAllBranchesReport() {
        try {
            val branches = branchDao.getAllBranches()

            if (branches.isEmpty()) {
                _uiState.update {
                    it.copy(
                        rows = emptyList(),
                        isLoading = false,
                        error = "No branches found."
                    )
                }
                return
            }

            val allRows = mutableListOf<InventoryReportRow>()
            var firstError: String? = null

            for (branch in branches) {
                val result = reportRepository.getInventoryReport(
                    branchId = branch.branchId
                )

                result.fold(
                    onSuccess = { reportDto ->
                        val rows = reportDto.items.map { item ->
                            item.toInventoryReportRow(
                                branchId = reportDto.branchId ?: branch.branchId
                            )
                        }

                        allRows.addAll(rows)
                    },
                    onFailure = { exception ->
                        if (firstError == null) {
                            firstError = exception.message
                        }
                    }
                )
            }

            val aggregatedRows = aggregateRows(allRows)

            _uiState.update {
                it.copy(
                    rows = aggregatedRows,
                    isLoading = false
                )
            }
        } catch (exception: Exception) {
            _uiState.update {
                it.copy(
                    rows = emptyList(),
                    isLoading = false
                )
            }
        }
    }

    private fun InventoryReportItemDto.toInventoryReportRow(
        branchId: Int
    ): InventoryReportRow {
        val localIngredient = localIngredients.firstOrNull {
            it.ingredientId == ingredientId
        }

        return InventoryReportRow(
            ingredientId = ingredientId,
            branchId = branchId,
            ingredientName = ingredientName,
            category = if (localIngredient?.isPackaging == true) {
                "Packaging"
            } else {
                "Ingredients"
            },
            currentStock = currentStock,
            unitType = unitType,
            lowStockThreshold = lowStockThreshold,
            image = localIngredient?.image
        )
    }

    private fun aggregateRows(
        rows: List<InventoryReportRow>
    ): List<InventoryReportRow> {
        return rows
            .groupBy {
                it.ingredientId
            }
            .map { (_, groupedRows) ->
                val first = groupedRows.first()

                InventoryReportRow(
                    ingredientId = first.ingredientId,
                    branchId = 0,
                    ingredientName = first.ingredientName,
                    category = first.category,
                    currentStock = groupedRows.sumOf {
                        it.currentStock
                    },
                    unitType = first.unitType,
                    lowStockThreshold = first.lowStockThreshold,
                    image = first.image
                )
            }
            .sortedBy {
                it.ingredientName.lowercase()
            }
    }

    private fun isAdminUser(): Boolean {
        return sessionManager.isAdmin()
    }
}