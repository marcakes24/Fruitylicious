package com.example.fruitylicious.ui.shared.restock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.example.fruitylicious.data.local.dao.AuditLogDao
import com.example.fruitylicious.data.local.dao.BranchDao
import com.example.fruitylicious.data.local.dao.IngredientDao
import com.example.fruitylicious.data.local.dao.InventoryDao
import com.example.fruitylicious.data.local.dao.RestockLogDao
import com.example.fruitylicious.data.local.db.PosDatabase
import com.example.fruitylicious.data.local.entity.AuditLogEntity
import com.example.fruitylicious.data.local.entity.BranchEntity
import com.example.fruitylicious.data.local.entity.IngredientEntity
import com.example.fruitylicious.data.local.entity.InventoryEntity
import com.example.fruitylicious.data.local.entity.RestockLogEntity
import com.example.fruitylicious.data.repository.ReportRepository
import com.example.fruitylicious.data.repository.StaffLogRepository
import com.example.fruitylicious.util.BranchConfig
import com.example.fruitylicious.util.NetworkMonitor
import com.example.fruitylicious.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    val branchName: String,
    val dateTime: Long
)

data class RestockUiState(
    val ingredients: List<RestockIngredientRow> = emptyList(),
    val history: List<RestockHistoryRow> = emptyList(),
    val branches: List<BranchEntity> = emptyList(),
    val selectedBranchId: Int? = null,
    val isAdmin: Boolean = false,
    val isOnline: Boolean = false,
    val localBranchId: Int = 1,
    val userBranchId: String = "B1",
    val isLoading: Boolean = true,
    val isClockedIn: Boolean = true,
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
    private val branchDao: BranchDao,
    private val reportRepository: ReportRepository,
    private val staffLogRepository: StaffLogRepository,
    private val sessionManager: SessionManager,
    private val branchConfig: BranchConfig,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val localBranchId = branchConfig.branchId

    private val _uiState = MutableStateFlow(
        RestockUiState(
            isAdmin = isAdminUser(),
            selectedBranchId = localBranchId,
            localBranchId = localBranchId,
            userBranchId = "B$localBranchId"
        )
    )

    val uiState: StateFlow<RestockUiState> = _uiState.asStateFlow()

    private var localIngredients: List<IngredientEntity> = emptyList()
    private var localInventory: List<InventoryEntity> = emptyList()
    private var localRestockLogs: List<RestockLogEntity> = emptyList()
    private var branches: List<BranchEntity> = emptyList()

    init {
        observeBranches()
        observeNetwork()
        observeIngredients()
        observeInventory()
        observeRestockLogs()
        observeClockInStatus()
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

        loadHistory()
    }

    fun refresh() {
        _uiState.update {
            it.copy(
                isLoading = true,
                error = null
            )
        }

        rebuildIngredientRows()
        loadHistory()
    }

    private fun observeBranches() {
        viewModelScope.launch {
            branchDao.observeAllBranches().collectLatest { branchList ->
                branches = branchList

                _uiState.update {
                    it.copy(branches = branchList)
                }

                rebuildIngredientRows()
                loadHistory()
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

                loadHistory()
            }
        }
    }

    private fun observeClockInStatus() {
        viewModelScope.launch {
            val role = sessionManager.getRole()

            if (
                role?.equals("admin", ignoreCase = true) == true ||
                role?.equals("owner", ignoreCase = true) == true
            ) {
                _uiState.update { it.copy(isClockedIn = true) }
                return@launch
            }

            val userId = sessionManager.getUserId()

            staffLogRepository.observeStaffLogsByUser(userId).collectLatest { logs ->
                val hasActiveLog = logs.any { it.clockOut == null }

                _uiState.update {
                    it.copy(
                        isClockedIn = hasActiveLog,
                        error = if (!hasActiveLog) {
                            "You must clock in before restocking."
                        } else {
                            null
                        }
                    )
                }
            }
        }
    }

    private fun observeIngredients() {
        viewModelScope.launch {
            ingredientDao.observeIngredients().collectLatest { items ->
                localIngredients = items
                rebuildIngredientRows()
                loadHistory()
            }
        }
    }

    private fun observeInventory() {
        viewModelScope.launch {
            inventoryDao.observeInventoryByBranch(localBranchId).collectLatest { items ->
                localInventory = items
                rebuildIngredientRows()
            }
        }
    }

    private fun observeRestockLogs() {
        viewModelScope.launch {
            restockLogDao.observeRestockLogsByBranch(localBranchId).collectLatest { items ->
                localRestockLogs = items
                loadHistory()
            }
        }
    }

    private fun rebuildIngredientRows() {
        val inventoryMap = localInventory.associateBy { it.ingredientId }

        val ingredientRows = localIngredients.map { ingredient ->
            val inventory = inventoryMap[ingredient.ingredientId]

            RestockIngredientRow(
                ingredientId = ingredient.ingredientId,
                branchId = localBranchId,
                ingredientName = ingredient.ingredientName,
                currentStock = inventory?.currentStock ?: 0.0,
                unitType = ingredient.unitType
            )
        }.sortedBy { it.ingredientName.lowercase() }

        _uiState.update {
            it.copy(
                ingredients = ingredientRows,
                isLoading = false
            )
        }
    }

    private fun loadHistory() {
        val state = _uiState.value

        when {
            !state.isAdmin -> {
                loadLocalHistory(localBranchId)
            }

            !state.isOnline -> {
                loadLocalHistory(localBranchId)
            }

            state.selectedBranchId == null -> {
                loadRemoteAllBranchesHistory()
            }

            else -> {
                loadRemoteBranchHistory(state.selectedBranchId)
            }
        }
    }

    private fun loadLocalHistory(branchId: Int) {
        val ingredientMap = localIngredients.associateBy { it.ingredientId }
        val branchName = branches.firstOrNull { it.branchId == branchId }?.branchName
            ?: "Branch $branchId"

        val historyRows = localRestockLogs
            .filter { it.branchId == branchId }
            .map { log ->
                val ingredient = ingredientMap[log.ingredientId]

                RestockHistoryRow(
                    restockId = log.restockId,
                    ingredientName = ingredient?.ingredientName ?: "Unknown ingredient",
                    supplier = log.supplier,
                    quantityAdded = log.quantityAdded,
                    unitType = ingredient?.unitType ?: "",
                    branchId = log.branchId,
                    branchName = branchName,
                    dateTime = log.dateTime
                )
            }
            .sortedByDescending { it.dateTime }

        _uiState.update {
            it.copy(
                history = historyRows,
                isLoading = false,
                error = null
            )
        }
    }

    private fun loadRemoteBranchHistory(branchId: Int) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, error = null)
            }

            val from = 0L
            val to = System.currentTimeMillis()

            val result = reportRepository.getRestockReport(
                branchId = branchId,
                from = from,
                to = to
            )

            result.fold(
                onSuccess = { report ->
                    val rows = report.items.map { item ->
                        RestockHistoryRow(
                            restockId = item.restockId,
                            ingredientName = item.ingredientName,
                            supplier = item.supplier,
                            quantityAdded = item.quantityAdded,
                            unitType = item.unitType,
                            branchId = report.branchId ?: branchId,
                            branchName = report.branchName ?: "Branch $branchId",
                            dateTime = item.dateTime
                        )
                    }.sortedByDescending { it.dateTime }

                    _uiState.update {
                        it.copy(
                            history = rows,
                            isLoading = false,
                            error = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            history = emptyList(),
                            isLoading = false,
                            error = error.message ?: "Failed to load remote restock history."
                        )
                    }
                }
            )
        }
    }

    private fun loadRemoteAllBranchesHistory() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, error = null)
            }

            val from = 0L
            val to = System.currentTimeMillis()
            val allRows = mutableListOf<RestockHistoryRow>()
            var firstError: String? = null

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

            for (branch in branchList) {
                val result = reportRepository.getRestockReport(
                    branchId = branch.branchId,
                    from = from,
                    to = to
                )

                result.fold(
                    onSuccess = { report ->
                        val rows = report.items.map { item ->
                            RestockHistoryRow(
                                restockId = item.restockId,
                                ingredientName = item.ingredientName,
                                supplier = item.supplier,
                                quantityAdded = item.quantityAdded,
                                unitType = item.unitType,
                                branchId = report.branchId ?: branch.branchId,
                                branchName = report.branchName ?: branch.branchName,
                                dateTime = item.dateTime
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

            _uiState.update {
                it.copy(
                    history = allRows.sortedByDescending { row -> row.dateTime },
                    isLoading = false,
                    error = firstError
                )
            }
        }
    }

    fun submitRestock(
        ingredient: RestockIngredientRow?,
        quantityText: String,
        supplier: String
    ) {
        if (!_uiState.value.isClockedIn) {
            setError("You must be clocked in to perform this action.")
            return
        }

        if (ingredient == null) {
            setError("Select an ingredient.")
            return
        }

        if (ingredient.branchId != localBranchId) {
            setError("Restock can only be recorded for the local branch.")
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
            val branchId = localBranchId

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

    private fun isAdminUser(): Boolean {
        val role = sessionManager.getRole()

        return role.equals("admin", ignoreCase = true) ||
                role.equals("owner", ignoreCase = true)
    }
}