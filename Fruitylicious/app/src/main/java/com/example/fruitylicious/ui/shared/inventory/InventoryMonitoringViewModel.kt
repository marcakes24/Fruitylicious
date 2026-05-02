package com.example.fruitylicious.ui.shared.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitylicious.data.local.dao.BranchDao
import com.example.fruitylicious.data.local.dao.IngredientDao
import com.example.fruitylicious.data.local.dao.InventoryDao
import com.example.fruitylicious.data.local.entity.BranchEntity
import com.example.fruitylicious.data.local.entity.IngredientEntity
import com.example.fruitylicious.data.local.entity.InventoryEntity
import com.example.fruitylicious.data.repository.ReportRepository
import com.example.fruitylicious.util.BranchConfig
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

data class InventoryMonitoringRow(
    val ingredientId: Int,
    val branchId: Int,
    val branchName: String,
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
    val branches: List<BranchEntity> = emptyList(),
    val selectedBranchId: Int? = null,
    val isAdmin: Boolean = false,
    val isOnline: Boolean = false,
    val localBranchId: Int = 1,
    val userBranchId: String = "B1",
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class InventoryMonitoringViewModel @Inject constructor(
    private val inventoryDao: InventoryDao,
    private val ingredientDao: IngredientDao,
    private val branchDao: BranchDao,
    private val reportRepository: ReportRepository,
    private val sessionManager: SessionManager,
    private val branchConfig: BranchConfig,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val localBranchId = branchConfig.branchId

    private val _uiState = MutableStateFlow(
        InventoryMonitoringUiState(
            isAdmin = isAdminUser(),
            selectedBranchId = localBranchId,
            localBranchId = localBranchId,
            userBranchId = "B$localBranchId"
        )
    )

    val uiState: StateFlow<InventoryMonitoringUiState> = _uiState.asStateFlow()

    private var localInventoryItems = emptyList<InventoryEntity>()
    private var localIngredients = emptyList<IngredientEntity>()
    private var branches = emptyList<BranchEntity>()

    init {
        observeBranches()
        observeNetwork()
        observeInventory()
        observeIngredients()
    }

    fun selectBranch(branchId: Int?) {
        val state = _uiState.value

        val finalBranchId = if (state.isAdmin && state.isOnline) {
            branchId
        } else {
            localBranchId
        }

        _uiState.update {
            it.copy(
                selectedBranchId = finalBranchId,
                isLoading = true,
                error = null
            )
        }

        loadRows()
    }

    fun refresh() {
        _uiState.update {
            it.copy(
                isLoading = true,
                error = null
            )
        }

        loadRows()
    }

    private fun observeBranches() {
        viewModelScope.launch {
            branchDao.observeAllBranches().collectLatest { branchList ->
                branches = branchList

                _uiState.update {
                    it.copy(branches = branchList)
                }

                loadRows()
            }
        }
    }

    private fun observeNetwork() {
        viewModelScope.launch {
            networkMonitor.observeNetworkStatus().collectLatest { online ->
                _uiState.update { current ->
                    val forcedBranchId = if (!online || !current.isAdmin) {
                        localBranchId
                    } else {
                        current.selectedBranchId
                    }

                    current.copy(
                        isOnline = online,
                        selectedBranchId = forcedBranchId
                    )
                }

                loadRows()
            }
        }
    }

    private fun observeInventory() {
        viewModelScope.launch {
            inventoryDao.observeInventoryByBranch(localBranchId).collectLatest { items ->
                localInventoryItems = items
                loadRows()
            }
        }
    }

    private fun observeIngredients() {
        viewModelScope.launch {
            ingredientDao.observeIngredients().collectLatest { items ->
                localIngredients = items
                loadRows()
            }
        }
    }

    private fun loadRows() {
        val state = _uiState.value

        when {
            !state.isAdmin -> {
                loadLocalRows(localBranchId)
            }

            !state.isOnline -> {
                loadLocalRows(localBranchId)
            }

            state.selectedBranchId == null -> {
                loadRemoteAllBranches()
            }

            else -> {
                loadRemoteBranch(state.selectedBranchId)
            }
        }
    }

    private fun loadLocalRows(branchId: Int) {
        val ingredientMap = localIngredients.associateBy { it.ingredientId }
        val branchName = branches.firstOrNull { it.branchId == branchId }?.branchName
            ?: "Branch $branchId"

        val rows = localInventoryItems
            .filter { it.branchId == branchId }
            .mapNotNull { inventory ->
                val ingredient = ingredientMap[inventory.ingredientId] ?: return@mapNotNull null

                InventoryMonitoringRow(
                    ingredientId = inventory.ingredientId,
                    branchId = inventory.branchId,
                    branchName = branchName,
                    ingredientName = ingredient.ingredientName,
                    currentStock = inventory.currentStock,
                    unitType = ingredient.unitType,
                    lowStockThreshold = ingredient.lowStockThreshold,
                    lastModified = inventory.lastModified
                )
            }
            .sortedBy { it.ingredientName.lowercase() }

        _uiState.update {
            it.copy(
                rows = rows,
                isLoading = false,
                error = null
            )
        }
    }

    private fun loadRemoteBranch(branchId: Int) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, error = null)
            }

            val result = reportRepository.getInventoryReport(branchId)

            result.fold(
                onSuccess = { report ->
                    val rows = report.items.map { item ->
                        InventoryMonitoringRow(
                            ingredientId = item.ingredientId,
                            branchId = report.branchId ?: branchId,
                            branchName = report.branchName ?: "Branch $branchId",
                            ingredientName = item.ingredientName,
                            currentStock = item.currentStock,
                            unitType = item.unitType,
                            lowStockThreshold = item.lowStockThreshold,
                            lastModified = report.generatedAt
                        )
                    }.sortedBy { it.ingredientName.lowercase() }

                    _uiState.update {
                        it.copy(
                            rows = rows,
                            isLoading = false,
                            error = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            rows = emptyList(),
                            isLoading = false,
                            error = error.message ?: "Failed to load remote inventory."
                        )
                    }
                }
            )
        }
    }

    private fun loadRemoteAllBranches() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, error = null)
            }

            val branchList = branches.ifEmpty {
                listOf(
                    BranchEntity(
                        branchId = localBranchId,
                        branchName = "Branch $localBranchId",
                        address = "",
                        contactNumber = "",
                        lastModified = 0L,
                        isSynced = true,
                        syncedAt = null
                    )
                )
            }

            val allRows = mutableListOf<InventoryMonitoringRow>()
            var firstError: String? = null

            for (branch in branchList) {
                val result = reportRepository.getInventoryReport(branch.branchId)

                result.fold(
                    onSuccess = { report ->
                        val rows = report.items.map { item ->
                            InventoryMonitoringRow(
                                ingredientId = item.ingredientId,
                                branchId = report.branchId ?: branch.branchId,
                                branchName = report.branchName ?: branch.branchName,
                                ingredientName = item.ingredientName,
                                currentStock = item.currentStock,
                                unitType = item.unitType,
                                lowStockThreshold = item.lowStockThreshold,
                                lastModified = report.generatedAt
                            )
                        }

                        allRows.addAll(rows)
                    },
                    onFailure = { error ->
                        if (firstError == null) {
                            firstError = error.message
                        }
                    }
                )
            }

            val aggregatedRows = aggregateInventoryRows(allRows)

            _uiState.update {
                it.copy(
                    rows = aggregatedRows,
                    isLoading = false,
                    error = firstError
                )
            }
        }
    }

    private fun aggregateInventoryRows(
        rows: List<InventoryMonitoringRow>
    ): List<InventoryMonitoringRow> {
        return rows
            .groupBy { it.ingredientId }
            .map { (_, groupedRows) ->
                val first = groupedRows.first()
                val totalStock = groupedRows.sumOf { it.currentStock }
                val latestModified = groupedRows.maxOfOrNull { it.lastModified } ?: first.lastModified

                InventoryMonitoringRow(
                    ingredientId = first.ingredientId,
                    branchId = 0,
                    branchName = "All Branches",
                    ingredientName = first.ingredientName,
                    currentStock = totalStock,
                    unitType = first.unitType,
                    lowStockThreshold = first.lowStockThreshold,
                    lastModified = latestModified
                )
            }
            .sortedBy { it.ingredientName.lowercase() }
    }

    private fun isAdminUser(): Boolean {
        val role = sessionManager.getRole()

        return role.equals("admin", ignoreCase = true) ||
                role.equals("owner", ignoreCase = true)
    }
}