package com.example.fruitylicious.ui.shared.waste

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitylicious.data.local.dao.BranchDao
import com.example.fruitylicious.data.local.dao.IngredientDao
import com.example.fruitylicious.data.local.dao.InventoryDao
import com.example.fruitylicious.data.local.dao.WasteLogDao
import com.example.fruitylicious.data.local.entity.BranchEntity
import com.example.fruitylicious.data.local.entity.IngredientEntity
import com.example.fruitylicious.data.local.entity.WasteLogEntity
import com.example.fruitylicious.data.repository.ReportRepository
import com.example.fruitylicious.data.repository.StaffLogRepository
import com.example.fruitylicious.data.repository.WasteRepository
import com.example.fruitylicious.util.BranchConfig
import com.example.fruitylicious.util.ImageStorage
import com.example.fruitylicious.util.NetworkMonitor
import com.example.fruitylicious.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    val branchName: String,
    val dateTime: Long,
    val imagePath: String? = null
)

data class WasteManagementUiState(
    val ingredients: List<WasteIngredientRow> = emptyList(),
    val history: List<WasteHistoryRow> = emptyList(),
    val branches: List<BranchEntity> = emptyList(),
    val selectedBranchId: Int? = null,
    val isAdmin: Boolean = false,
    val isOnline: Boolean = false,
    val isRemoteAccessLocked: Boolean = false,
    val localBranchId: Int = 1,
    val userBranchId: String = "B1",
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val currentPage: Int = 0,
    val isClockedIn: Boolean = true,
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class WasteManagementViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val wasteRepository: WasteRepository,
    private val inventoryDao: InventoryDao,
    private val ingredientDao: IngredientDao,
    private val wasteLogDao: WasteLogDao,
    private val branchDao: BranchDao,
    private val reportRepository: ReportRepository,
    private val staffLogRepository: StaffLogRepository,
    private val sessionManager: SessionManager,
    private val branchConfig: BranchConfig,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val localBranchId = sessionManager.getBranchId().takeIf { it > 0 } ?: branchConfig.branchId

    private val _uiState = MutableStateFlow(
        WasteManagementUiState(
            isAdmin = isAdminUser(),
            selectedBranchId = localBranchId,
            localBranchId = localBranchId,
            userBranchId = "B$localBranchId"
        )
    )

    val uiState: StateFlow<WasteManagementUiState> = _uiState.asStateFlow()

    private val PAGE_SIZE = 20
    private var lockoutJob: kotlinx.coroutines.Job? = null
    private val refreshTrigger = MutableStateFlow(0)
    private var branches: List<BranchEntity> = emptyList()

    init {
        observeBranches()
        observeNetwork()
        observeLocalData()
        observeClockInStatus()
        loadHistory()
    }

    private fun observeLocalData() {
        viewModelScope.launch {
            combine(
                wasteLogDao.observeAllWasteLogs(),
                ingredientDao.observeIngredients(),
                inventoryDao.observeInventoryByBranch(localBranchId),
                refreshTrigger
            ) { logs, ingredients, inventory, _ ->
                Triple(logs, ingredients, inventory)
            }.collect { (logs, ingredients, inventory) ->
                val state = _uiState.value
                
                val ingredientRows = inventory.mapNotNull { item ->
                    val ingredient = ingredients.find { it.ingredientId == item.ingredientId } ?: return@mapNotNull null
                    WasteIngredientRow(
                        ingredientId = item.ingredientId,
                        branchId = item.branchId,
                        ingredientName = ingredient.ingredientName,
                        currentStock = item.currentStock,
                        unitType = ingredient.unitType
                    )
                }.sortedBy { it.ingredientName.lowercase() }

                val historyRows = buildHistoryRows(logs, ingredients)

                if (state.selectedBranchId == localBranchId || !state.isOnline || state.error != null) {
                    _uiState.update {
                        it.copy(
                            ingredients = ingredientRows,
                            history = historyRows,
                            isLoading = false,
                            hasMore = historyRows.size >= (state.currentPage + 1) * PAGE_SIZE,
                            error = state.error
                        )
                    }
                } else {
                    _uiState.update { it.copy(ingredients = ingredientRows) }
                }
            }
        }
    }

    private fun buildHistoryRows(entities: List<WasteLogEntity>, ingredients: List<IngredientEntity>): List<WasteHistoryRow> {
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

                WasteHistoryRow(
                    wasteId = log.wasteId,
                    ingredientName = ingredient?.ingredientName ?: "Unknown ingredient",
                    quantity = log.quantity,
                    unitType = ingredient?.unitType ?: "",
                    reason = log.reason,
                    branchId = log.branchId,
                    branchName = branchName,
                    dateTime = log.dateTime,
                    imagePath = log.image
                )
            }
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
                error = null,
                history = emptyList()
            )
        }

        refreshTrigger.value += 1
        loadHistory()
    }

    fun refresh() {
        _uiState.update {
            it.copy(
                isLoading = true,
                error = null,
                currentPage = 0,
                history = emptyList()
            )
        }

        refreshTrigger.value += 1
        loadHistory()
    }

    private fun observeBranches() {
        viewModelScope.launch {
            branchDao.observeAllBranches().collectLatest { branchList ->
                branches = branchList
                _uiState.update { it.copy(branches = branchList) }
                refreshTrigger.value += 1
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
                _uiState.update { it.copy(isClockedIn = true) }
                return@launch
            }

            val userId = sessionManager.getUserId()
            staffLogRepository.observeStaffLogsByUser(userId).collectLatest { logs ->
                val hasActiveLog = logs.any { it.clockOut == null }
                _uiState.update {
                    it.copy(
                        isClockedIn = hasActiveLog,
                        error = if (!hasActiveLog) "You must clock in before recording waste." else null
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
                _uiState.update { it.copy(isLoading = true, error = null, history = emptyList()) }
            } else {
                _uiState.update { it.copy(isLoadingMore = true) }
            }

            val result = reportRepository.getWastePage(
                branchId = state.selectedBranchId,
                from = 0L,
                to = System.currentTimeMillis(),
                page = page,
                size = PAGE_SIZE
            )

            result.fold(
                onSuccess = { pageResponse ->
                    val newRows = pageResponse.items.map { item ->
                         WasteHistoryRow(
                            wasteId = item.wasteId,
                            ingredientName = item.ingredientName,
                            quantity = item.quantity,
                            unitType = item.unitType,
                            reason = item.reason,
                            branchId = item.branchId ?: state.selectedBranchId ?: 0,
                            branchName = branches.firstOrNull { it.branchId == item.branchId }?.branchName ?: "Remote Branch",
                            dateTime = item.dateTime,
                            imagePath = ImageStorage.saveBase64Image(
                                context = context,
                                base64Value = item.image,
                                folder = "waste"
                            )
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
                            isLoadingMore = false,
                            error = error.message ?: "Failed to load remote waste history.",
                            isRemoteAccessLocked = true
                        )
                    }
                    startLockoutTimer()
                    refreshTrigger.value += 1
                }
            )
        }
    }

    private fun startLockoutTimer() {
        lockoutJob?.cancel()
        lockoutJob = viewModelScope.launch {
            kotlinx.coroutines.delay(5 * 60 * 1000L)
            _uiState.update { it.copy(isRemoteAccessLocked = false) }
        }
    }

    fun submitWaste(
        ingredient: WasteIngredientRow?,
        quantityText: String,
        reason: String,
        imagePath: String
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
            setError("Waste can only be recorded for the local branch.")
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
            val userId = sessionManager.getUserId()
            val result = wasteRepository.logWaste(
                ingredientId = ingredient.ingredientId,
                branchId = localBranchId,
                userId = userId,
                quantity = quantity,
                image = imagePath,
                reason = reason.ifBlank { "Waste entry" }
            )

            if (result.isSuccess) {
                _uiState.update {
                    it.copy(successMessage = "Waste entry saved.", error = null)
                }
            } else {
                _uiState.update {
                    it.copy(error = result.exceptionOrNull()?.message ?: "Failed to save waste entry.", successMessage = null)
                }
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
        return sessionManager.isAdmin()
    }
}
