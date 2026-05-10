package com.example.fruitylicious.ui.shared.restock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitylicious.data.local.dao.BranchDao
import com.example.fruitylicious.data.local.dao.IngredientDao
import com.example.fruitylicious.data.local.dao.InventoryDao
import com.example.fruitylicious.data.local.dao.RestockLogDao
import com.example.fruitylicious.data.local.entity.BranchEntity
import com.example.fruitylicious.data.local.entity.IngredientEntity
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

data class RestockIngredientRow(
    val ingredientId: Int,
    val branchId: Int,
    val ingredientName: String,
    val currentStock: Double,
    val unitType: String,
    val imagePath: String? = null
)

data class RestockHistoryRow(
    val restockId: String,
    val ingredientName: String,
    val supplier: String,
    val quantityAdded: Double,
    val unitType: String,
    val branchId: Int,
    val branchName: String,
    val dateTime: Long,
    val imagePath: String? = null
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
            selectedBranchId = localBranchId,
            localBranchId = localBranchId,
            userBranchId = "B$localBranchId"
        )
    )

    val uiState: StateFlow<RestockUiState> = _uiState.asStateFlow()

    private val PAGE_SIZE = 20
    private val refreshTrigger = MutableStateFlow(0)
    private var branches: List<BranchEntity> = emptyList()

    init {
        observeBranches()
        observeNetwork()
        observeLocalData()
        observeClockInStatus()

        // Reactive history loading: only one central point for loading
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
                    delay(300) // Debounce branch selection and status changes
                    loadHistory()
                } else {
                    // For local branch, observeLocalData handles everything
                }
            }
        }
    }

    private fun observeLocalData() {
        viewModelScope.launch {
            combine(
                restockLogDao.observeAllRestockLogs(),
                ingredientDao.observeIngredients(),
                inventoryDao.observeInventoryByBranch(localBranchId),
                refreshTrigger
            ) { logs, ingredients, inventory, _ ->
                val ingredientRows = ingredients.map { ingredient ->
                    val inv = inventory.find { it.ingredientId == ingredient.ingredientId }
                    RestockIngredientRow(
                        ingredientId = ingredient.ingredientId,
                        branchId = localBranchId,
                        ingredientName = ingredient.ingredientName,
                        currentStock = inv?.currentStock ?: 0.0,
                        unitType = ingredient.unitType,
                        imagePath = ingredient.image
                    )
                }.sortedBy { it.ingredientName.lowercase() }

                val historyRows = buildHistoryRows(logs, ingredients)
                Triple(ingredientRows, historyRows, logs.size)
            }
            .flowOn(kotlinx.coroutines.Dispatchers.Default)
            .flowOn(kotlinx.coroutines.Dispatchers.Default)
            .collect { (ingredientRows, historyRows, _) ->
                _uiState.update { state ->
                    // Avoid flickering: don't show local data as placeholder if remote fetch is in progress for non-local branch
                    val isRemoteFetchInProgress = state.isOnline && state.selectedBranchId != localBranchId && state.isLoading

                    if (state.selectedBranchId == localBranchId || !state.isOnline || state.error != null || (state.history.isEmpty() && !isRemoteFetchInProgress)) {
                        state.copy(
                            ingredients = ingredientRows,
                            history = historyRows,
                            isLoading = if (state.selectedBranchId != localBranchId && state.isOnline && state.error == null) state.isLoading else false,
                            hasMore = historyRows.size >= (state.currentPage + 1) * PAGE_SIZE
                        )
                    } else {
                        state.copy(ingredients = ingredientRows)
                    }
                }
            }
        }
    }

    private fun buildHistoryRows(entities: List<RestockLogEntity>, ingredients: List<IngredientEntity>): List<RestockHistoryRow> {
        val state = _uiState.value
        val ingredientMap = ingredients.associateBy { it.ingredientId }

        return entities
            .filter { log ->
                state.selectedBranchId == null || log.branchId == state.selectedBranchId
            }
            .take(PAGE_SIZE + (state.currentPage * PAGE_SIZE))
            .map { log ->
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
                    dateTime = log.dateTime,
                    imagePath = ingredient?.image
                )
            }
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
                currentPage = 0,
                hasMore = true,
                error = null
                // Removed: history = emptyList() to prevent flickering
            )
        }

        refreshTrigger.value += 1
    }

    fun refresh() {
        _uiState.update {
            it.copy(
                isLoading = true,
                error = null,
                currentPage = 0
                // Removed: history = emptyList() to prevent flickering
            )
        }

        refreshTrigger.value += 1
    }

    private fun observeBranches() {
        viewModelScope.launch {
            branchDao.observeAllBranches().collectLatest { branchList ->
                branches = branchList
                _uiState.update { it.copy(branches = branchList) }
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
                _uiState.update {
                    it.copy(
                        isClockedIn = hasActiveLog,
                        error = if (!hasActiveLog) "You must clock in before restocking." else null
                    )
                }
            }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMore) return
        
        if (state.isOnline && state.selectedBranchId != localBranchId) {
            loadRemoteHistoryPage(state.currentPage + 1)
        } else {
            _uiState.update { it.copy(currentPage = it.currentPage + 1) }
            refreshTrigger.value += 1
        }
    }

    private fun loadHistory() {
        val state = _uiState.value

        if (state.isOnline && state.selectedBranchId != localBranchId) {
            loadRemoteHistoryPage(0)
        } else {
            // Local load handled by observeLocalData
        }
    }

    private fun loadRemoteHistoryPage(page: Int) {
        val state = _uiState.value
        viewModelScope.launch {
            if (page == 0) {
                _uiState.update { it.copy(isLoading = true, error = null) }
            } else {
                _uiState.update { it.copy(isLoadingMore = true) }
            }

            val result = reportRepository.getRestockPage(
                branchId = state.selectedBranchId,
                from = 0L,
                to = System.currentTimeMillis(),
                page = page,
                size = PAGE_SIZE
            )

            result.fold(
                onSuccess = { pageResponse ->
                    val newRows = pageResponse.items.map { item ->
                        RestockHistoryRow(
                            restockId = item.restockId,
                            ingredientName = item.ingredientName,
                            supplier = item.supplier,
                            quantityAdded = item.quantityAdded,
                            unitType = item.unitType,
                            branchId = item.branchId ?: state.selectedBranchId ?: 0,
                            branchName = branches.firstOrNull { it.branchId == item.branchId }?.branchName ?: "Remote Branch",
                            dateTime = item.dateTime,
                            imagePath = item.image
                        )
                    }

                    _uiState.update {
                        it.copy(
                            history = if (page == 0) newRows else it.history + newRows,
                            isLoading = false,
                            isLoadingMore = false,
                            currentPage = page,
                            hasMore = pageResponse.hasNext,
                            error = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isLoadingMore = false
                        )
                    }
                    refreshTrigger.value += 1
                }
            )
        }
    }

    fun submitRestock(
        ingredient: RestockIngredientRow?,
        quantityText: String,
        supplier: String
    ) {
        if (_uiState.value.isSubmitting) return

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
            _uiState.update { it.copy(isSubmitting = true, error = null, successMessage = null) }

            try {
                val userId = sessionManager.getUserId()
                val result = restockRepository.restock(
                    ingredientId = ingredient.ingredientId,
                    branchId = localBranchId,
                    userId = userId,
                    quantityAdded = quantity,
                    supplier = supplier.ifBlank { "N/A" }
                )

                if (result.isSuccess) {
                    _uiState.update { it.copy(successMessage = "Restock saved.", error = null) }
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Failed to save restock."
                    _uiState.update { it.copy(error = error, successMessage = null) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to save restock.", successMessage = null) }
            } finally {
                _uiState.update { it.copy(isSubmitting = false) }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(error = null, successMessage = null) }
    }

    private fun setError(message: String) {
        _uiState.update {
            it.copy(error = message, successMessage = null, isSubmitting = false)
        }
    }

    private fun isAdminUser(): Boolean {
        return sessionManager.isAdmin()
    }
}
