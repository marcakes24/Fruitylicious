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
import com.example.fruitylicious.data.repository.RestockRepository
import com.example.fruitylicious.data.repository.StaffLogRepository
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
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val currentPage: Int = 0,
    val isSubmitting: Boolean = false,
    val isClockedIn: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class RestockViewModel @Inject constructor(
    private val restockRepository: RestockRepository,
    private val ingredientDao: IngredientDao,
    private val inventoryDao: InventoryDao,
    private val restockLogDao: RestockLogDao,
    private val branchDao: BranchDao,
    private val reportRepository: ReportRepository,
    private val staffLogRepository: StaffLogRepository,
    private val sessionManager: SessionManager,
    private val branchConfig: BranchConfig,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val localBranchId = sessionManager.getBranchId().takeIf { it > 0 } ?: branchConfig.branchId

    private val _uiState = MutableStateFlow(
        RestockUiState(
            isAdmin = isAdminUser(),
            selectedBranchId = sessionManager.getBranchId().takeIf { it > 0 } ?: branchConfig.branchId,
            localBranchId = sessionManager.getBranchId().takeIf { it > 0 } ?: branchConfig.branchId,
            userBranchId = "B${sessionManager.getBranchId().takeIf { it > 0 } ?: branchConfig.branchId}"
        )
    )

    val uiState: StateFlow<RestockUiState> = _uiState.asStateFlow()

    private val PAGE_SIZE = 20

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
                currentPage = 0,
                hasMore = true,
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

                loadHistory()
            }
        }
    }

    private fun observeClockInStatus() {
        viewModelScope.launch {
            if (sessionManager.isAdmin()) {
                _uiState.update {
                    it.copy(
                        isClockedIn = true,
                        error = null
                    )
                }
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

        val ingredientRows = localIngredients
            .map { ingredient ->
                val inventory = inventoryMap[ingredient.ingredientId]

                RestockIngredientRow(
                    ingredientId = ingredient.ingredientId,
                    branchId = localBranchId,
                    ingredientName = ingredient.ingredientName,
                    currentStock = inventory?.currentStock ?: 0.0,
                    unitType = ingredient.unitType
                )
            }
            .sortedBy { it.ingredientName.lowercase() }

        _uiState.update {
            it.copy(
                ingredients = ingredientRows,
                isLoading = false
            )
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMore) return
        
        // Pagination only supported for local history for now in this simple implementation
        if (state.isAdmin && state.isOnline && state.selectedBranchId != localBranchId) return

        _uiState.update { it.copy(isLoadingMore = true) }

        viewModelScope.launch {
            val nextPage = state.currentPage + 1
            val offset = nextPage * PAGE_SIZE
            
            val newEntities = if (state.selectedBranchId == null) {
                restockLogDao.getAllRestockLogsPaged(PAGE_SIZE, offset)
            } else {
                restockLogDao.getRestockLogsByBranchPaged(state.selectedBranchId, PAGE_SIZE, offset)
            }
            
            if (newEntities.isEmpty()) {
                _uiState.update { it.copy(isLoadingMore = false, hasMore = false) }
                return@launch
            }
            
            val ingredientMap = localIngredients.associateBy { it.ingredientId }
            val newRows = newEntities.map { log ->
                val ingredient = ingredientMap[log.ingredientId]
                val branchName = branches.firstOrNull { it.branchId == log.branchId }?.branchName
                    ?: "Branch ${log.branchId}"

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

            _uiState.update { 
                it.copy(
                    history = it.history + newRows,
                    currentPage = nextPage,
                    isLoadingMore = false,
                    hasMore = newRows.size == PAGE_SIZE
                )
            }
        }
    }

    private fun loadHistory() {
        val state = _uiState.value
        val selectedBranchId = state.selectedBranchId

        when {
            !state.isAdmin -> {
                loadLocalHistory(localBranchId)
            }

            !state.isOnline -> {
                loadLocalHistory(localBranchId)
            }

            selectedBranchId == localBranchId -> {
                loadLocalHistory(localBranchId)
            }

            selectedBranchId == null -> {
                loadRemoteAllBranchesHistory()
            }

            else -> {
                loadRemoteBranchHistory(selectedBranchId)
            }
        }
    }

    private fun loadLocalHistory(branchId: Int) {
        val ingredientMap = localIngredients.associateBy { it.ingredientId }
        val branchName = branches.firstOrNull { it.branchId == branchId }?.branchName
            ?: "Branch $branchId"

        val historyRows = localRestockLogs
            .filter { it.branchId == branchId }
            .take(PAGE_SIZE)
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
                hasMore = historyRows.size >= PAGE_SIZE,
                currentPage = 0,
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
                    val rows = report.items
                        .map { item ->
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
                        }
                        .sortedByDescending { it.dateTime }

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
        if (_uiState.value.isSubmitting) {
            return
        }

        if (ingredient == null) {
            setError("Select an ingredient.")
            return
        }

        val quantity = quantityText.toDoubleOrNull()

        if (quantity == null || quantity <= 0.0) {
            setError("Enter a valid quantity.")
            return
        }

        if (!_uiState.value.isClockedIn) {
            setError("You must be clocked in to perform this action.")
            return
        }

        if (ingredient.branchId != localBranchId) {
            setError("Restock can only be recorded for the local branch.")
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSubmitting = true,
                    error = null,
                    successMessage = null
                )
            }

            try {
                val userId = sessionManager.getUserId()
                val branchId = localBranchId

                val result = restockRepository.restock(
                    ingredientId = ingredient.ingredientId,
                    branchId = branchId,
                    userId = userId,
                    quantityAdded = quantity,
                    supplier = supplier.ifBlank { "N/A" }
                )

                if (result.isSuccess) {
                    _uiState.update {
                        it.copy(
                            successMessage = "Restock saved.",
                            error = null
                        )
                    }
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Failed to save restock."
                    _uiState.update {
                        it.copy(
                            error = error,
                            successMessage = null
                        )
                    }
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = e.message ?: "Failed to save restock.",
                        successMessage = null
                    )
                }
            } finally {
                // ✅ Always resets isSubmitting — even if a CancellationException
                // is thrown, ensuring the button never stays permanently grayed out.
                _uiState.update {
                    it.copy(isSubmitting = false)
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
                successMessage = null,
                isSubmitting = false
            )
        }
    }

    private fun isAdminUser(): Boolean {
        return sessionManager.isAdmin()
    }
}