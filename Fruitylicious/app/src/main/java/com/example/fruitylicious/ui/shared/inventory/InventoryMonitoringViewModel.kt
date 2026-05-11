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
import com.example.fruitylicious.data.repository.StaffLogRepository
import com.example.fruitylicious.util.BranchConfigManager
import com.example.fruitylicious.util.NetworkMonitor
import com.example.fruitylicious.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext

data class InventoryMonitoringRow(
    val ingredientId: Int,
    val branchId: Int,
    val branchName: String,
    val ingredientName: String,
    val currentStock: Double,
    val unitType: String,
    val lowStockThreshold: Double,
    val lastModified: Long,
    val image: String? = null
) {
    val status: String
        get() {
            return if (lowStockThreshold > 0.0) {
                when {
                    currentStock <= lowStockThreshold -> "Critical"
                    currentStock <= lowStockThreshold * 2 -> "Warning"
                    else -> "Normal"
                }
            } else {
                when {
                    currentStock >= 50.0 -> "Normal"
                    currentStock >= 20.0 -> "Warning"
                    else -> "Critical"
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
    val isClockedIn: Boolean = false,
    val localBranchId: Int = 0,
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
    private val staffLogRepository: StaffLogRepository,
    private val sessionManager: SessionManager,
    private val branchConfigManager: BranchConfigManager,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val localBranchId = sessionManager.getBranchId().takeIf { it > 0 } ?: branchConfigManager.branchId

    private val _uiState = MutableStateFlow(
        InventoryMonitoringUiState(
            isAdmin = isAdminUser(),
            selectedBranchId = localBranchId,
            localBranchId = localBranchId,
            userBranchId = "B$localBranchId"
        )
    )

    val uiState: StateFlow<InventoryMonitoringUiState> = _uiState.asStateFlow()

    private var branches: List<BranchEntity> = emptyList()
    private var loadJob: Job? = null
    private val refreshTrigger = MutableStateFlow(0)

    init {
        observeBranches()
        observeNetwork()
        observeLocalData()
        observeClockInStatus()

        // Reactive loading: only one central point for loading
        viewModelScope.launch {
            combine(
                _uiState.map { it.isOnline }.distinctUntilChanged(),
                _uiState.map { it.selectedBranchId }.distinctUntilChanged(),
                refreshTrigger
            ) { online, branchId, trigger ->
                Triple(online, branchId, trigger)
            }.collectLatest { (online, branchId, _) ->
                // ONLY load if online AND selected branch is NOT the local branch
                if (online && branchId != localBranchId) {
                    delay(300) // Debounce branch selection
                    loadRows()
                } else {
                    // For local branch, observeLocalData handles everything
                    loadJob?.cancel()
                }
            }
        }
    }

    private fun observeLocalData() {
        viewModelScope.launch {
            combine(
                inventoryDao.observeAllInventory(),
                ingredientDao.observeIngredients(),
                refreshTrigger
            ) { inventory, ingredients, _ ->
                inventory to ingredients
            }
                .flowOn(Dispatchers.Default)
                .map { (inventory, ingredients) ->
                    buildInventoryRows(inventory, ingredients)
                }
                .collectLatest { rows ->
                    _uiState.update { state ->
                        // Avoid showing local data as a "flickering" placeholder if we are currently loading remote data
                        val isActivelyLoadingRemote = state.isOnline && state.selectedBranchId != localBranchId && state.isLoading

                        if (state.selectedBranchId == localBranchId || !state.isOnline || state.error != null || (state.rows.isEmpty() && !isActivelyLoadingRemote)) {
                            state.copy(
                                rows = rows,
                                isLoading = if (state.selectedBranchId != localBranchId && state.isOnline && state.error == null) state.isLoading else false
                            )
                        } else {
                            state
                        }
                    }
                }
        }
    }

    private fun buildInventoryRows(
        inventory: List<InventoryEntity>,
        ingredients: List<IngredientEntity>
    ): List<InventoryMonitoringRow> {
        val state = _uiState.value
        val ingredientMap = ingredients.associateBy { it.ingredientId }
        
        val filteredInventory = if (state.selectedBranchId == null) {
            // Aggregate inventory items by ingredientId if "All Branches" selected
            inventory.groupBy { it.ingredientId }.map { (ingredientId, items) ->
                val totalStock = items.sumOf { it.currentStock }
                val latestModified = items.maxOfOrNull { it.lastModified } ?: 0L
                // Return a virtual inventory entity for aggregation
                InventoryEntity(
                    ingredientId = ingredientId,
                    branchId = 0, // Virtual ID for all
                    currentStock = totalStock,
                    lastModified = latestModified,
                    isSynced = true,
                    syncedAt = null
                )
            }
        } else {
            inventory.filter { it.branchId == state.selectedBranchId }
        }

        return filteredInventory.mapNotNull { inv ->
            val ingredient = ingredientMap[inv.ingredientId] ?: return@mapNotNull null
            val branchName = if (inv.branchId == 0) "All Branches" 
                else branches.firstOrNull { it.branchId == inv.branchId }?.branchName ?: "Branch ${inv.branchId}"

            InventoryMonitoringRow(
                ingredientId = inv.ingredientId,
                branchId = inv.branchId,
                branchName = branchName,
                ingredientName = ingredient.ingredientName,
                currentStock = inv.currentStock,
                unitType = ingredient.unitType,
                lowStockThreshold = ingredient.lowStockThreshold,
                lastModified = inv.lastModified,
                image = ingredient.image
            )
        }.sortedBy { it.ingredientName.lowercase() }
    }

    fun onBranchSelected(branchId: Int?) {
        val state = _uiState.value
        val finalBranchId = if (state.isAdmin && state.isOnline) {
            branchId
        } else {
            localBranchId
        }

        if (state.selectedBranchId == finalBranchId) return

        _uiState.update {
            it.copy(
                selectedBranchId = finalBranchId,
                isLoading = true,
                error = null
                // Removed: rows = emptyList() to prevent flickering
            )
        }

        refreshTrigger.value += 1
    }

    private fun loadRows() {
        val state = _uiState.value
        if (state.isOnline && state.selectedBranchId != localBranchId) {
            loadRemoteRows(state.selectedBranchId)
        } else {
            loadJob?.cancel()
        }
    }

    fun refresh() {
        _uiState.update {
            it.copy(
                isLoading = true,
                error = null,
                rows = emptyList()
            )
        }

        refreshTrigger.value += 1
    }

    private fun observeBranches() {
        viewModelScope.launch {
            branchDao.observeAllBranches().collectLatest { branchList ->
                branches = ensureLocalBranchExists(branchList)
                _uiState.update { it.copy(branches = branches) }
            }
        }
    }

    private fun observeNetwork() {
        viewModelScope.launch {
            networkMonitor.observeNetworkStatus().collectLatest { online ->
                _uiState.update { current ->
                    val forcedBranchId = if (!current.isAdmin) {
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

    private fun observeClockInStatus() {
        viewModelScope.launch {
            if (sessionManager.isAdmin()) {
                _uiState.update { it.copy(isClockedIn = true) }
                return@launch
            }

            val userId = sessionManager.getUserId()
            staffLogRepository.observeStaffLogsByUser(userId).collectLatest { logs ->
                val hasActiveLog = logs.any { it.clockOut == null }
                _uiState.update { it.copy(isClockedIn = hasActiveLog) }
            }
        }
    }

    private fun loadRemoteRows(branchId: Int?) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }

                if (branchId == null) {
                    loadRemoteAllBranches()
                } else {
                    loadRemoteBranch(branchId)
                }
            } catch (t: Throwable) {
                if (t is kotlinx.coroutines.CancellationException) {
                    return@launch
                }

                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        error = t.message ?: "Failed to load remote inventory."
                    )
                }
            }
        }
    }

    private suspend fun loadRemoteBranch(branchId: Int) {
        val result = reportRepository.getInventoryReport(branchId = branchId)

        result.fold(
            onSuccess = { report ->
                val rows = withContext(Dispatchers.Default) {
                    report.items.map { item ->
                        InventoryMonitoringRow(
                            ingredientId = item.ingredientId,
                            branchId = report.branchId ?: branchId,
                            branchName = report.branchName ?: "Branch $branchId",
                            ingredientName = item.ingredientName,
                            currentStock = item.currentStock,
                            unitType = item.unitType,
                            lowStockThreshold = item.lowStockThreshold,
                            lastModified = report.generatedAt,
                            image = null // Images are local only or handled by UI
                        )
                    }.sortedBy { it.ingredientName.lowercase() }
                }

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
                        isLoading = false
                    )
                }
                refreshTrigger.value += 1
            }
        )
    }

    private suspend fun loadRemoteAllBranches() {
        val allRows = mutableListOf<InventoryMonitoringRow>()
        var firstError: String? = null

        try {
            // Use supervisorScope to isolate sibling failures and handle cancellation better
            supervisorScope {
                val deferredResults = branches.map { branch ->
                    async {
                        branch to reportRepository.getInventoryReport(branchId = branch.branchId)
                    }
                }

                val results = deferredResults.awaitAll()

                results.forEach { (branch, result) ->
                    result.fold(
                        onSuccess = { report ->
                            allRows.addAll(report.items.map { item ->
                                InventoryMonitoringRow(
                                    ingredientId = item.ingredientId,
                                    branchId = branch.branchId,
                                    branchName = branch.branchName,
                                    ingredientName = item.ingredientName,
                                    currentStock = item.currentStock,
                                    unitType = item.unitType,
                                    lowStockThreshold = item.lowStockThreshold,
                                    lastModified = report.generatedAt,
                                    image = null
                                )
                            })
                        },
                        onFailure = { error ->
                            if (firstError == null) firstError = error.message
                        }
                    )
                }
            }
        } catch (t: Throwable) {
            // Re-throw CancellationException so the top-level launch knows to stop silently
            if (t is kotlinx.coroutines.CancellationException) throw t
            if (firstError == null) firstError = t.message
        }

        if (allRows.isNotEmpty()) {
            val aggregated = withContext(Dispatchers.Default) {
                aggregateRows(allRows)
            }
            _uiState.update {
                it.copy(
                    rows = aggregated,
                    isLoading = false
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    isLoading = false
                )
            }
            refreshTrigger.value += 1
        }
    }

    private fun aggregateRows(rows: List<InventoryMonitoringRow>): List<InventoryMonitoringRow> {
        return rows.groupBy { it.ingredientId }.map { (ingredientId, items) ->
            val first = items.first()
            val totalStock = items.sumOf { it.currentStock }
            val latestModified = items.maxOfOrNull { it.lastModified } ?: 0L

            InventoryMonitoringRow(
                ingredientId = ingredientId,
                branchId = 0,
                branchName = "All Branches",
                ingredientName = first.ingredientName,
                currentStock = totalStock,
                unitType = first.unitType,
                lowStockThreshold = first.lowStockThreshold,
                lastModified = latestModified,
                image = null
            )
        }.sortedBy { it.ingredientName.lowercase() }
    }

    private fun ensureLocalBranchExists(branchList: List<BranchEntity>): List<BranchEntity> {
        val hasLocal = branchList.any { it.branchId == localBranchId }
        if (hasLocal) return branchList.sortedBy { it.branchId }

        val local = BranchEntity(
            branchId = localBranchId,
            branchName = branchConfigManager.branchName.ifBlank { "Branch $localBranchId" },
            address = "",
            contactNumber = "",
            lastModified = 0L,
            isSynced = true,
            syncedAt = null
        )
        return (branchList + local).sortedBy { it.branchId }
    }

    private fun isAdminUser(): Boolean {
        return sessionManager.isAdmin()
    }
}
