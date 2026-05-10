package com.example.fruitylicious.ui.shared.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitylicious.data.local.dao.BranchDao
import com.example.fruitylicious.data.local.dao.IngredientDao
import com.example.fruitylicious.data.local.dao.InventoryAdjustmentDao
import com.example.fruitylicious.data.local.dao.InventoryDao
import com.example.fruitylicious.data.local.dao.UserDao
import com.example.fruitylicious.data.local.entity.BranchEntity
import com.example.fruitylicious.data.local.entity.IngredientEntity
import com.example.fruitylicious.data.local.entity.InventoryAdjustmentEntity
import com.example.fruitylicious.data.local.entity.InventoryEntity
import com.example.fruitylicious.data.local.entity.UserEntity
import com.example.fruitylicious.data.repository.AdjustmentRepository
import com.example.fruitylicious.data.repository.ReportRepository
import com.example.fruitylicious.util.BranchConfig
import com.example.fruitylicious.util.NetworkMonitor
import com.example.fruitylicious.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.abs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.util.Calendar

data class AdjustmentIngredientRow(
    val ingredientId: Int,
    val branchId: Int,
    val ingredientName: String,
    val currentStock: Double,
    val unitType: String
)

data class AdjustmentHistoryRow(
    val adjustmentId: String,
    val ingredientName: String,
    val adjustmentType: String,
    val quantity: Double,
    val reason: String,
    val dateTime: Long,
    val branchId: Int,
    val branchName: String,
    val userName: String
)

data class InventoryAdjustmentUiState(
    val ingredients: List<AdjustmentIngredientRow> = emptyList(),
    val history: List<AdjustmentHistoryRow> = emptyList(),
    val branches: List<BranchEntity> = emptyList(),
    val selectedBranchId: Int? = null,
    val searchQuery: String = "",
    val startDate: Long? = null,
    val endDate: Long? = null,
    val totalAdjustments: Int = 0,
    val netAdjustmentQuantity: Double = 0.0,
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val currentPage: Int = 0,
    val hasMore: Boolean = false,
    val isAdmin: Boolean = false,
    val isOnline: Boolean = false,
    val localBranchId: Int = 1,
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class InventoryAdjustmentViewModel @Inject constructor(
    private val adjustmentRepository: AdjustmentRepository,
    private val reportRepository: ReportRepository,
    private val inventoryDao: InventoryDao,
    private val ingredientDao: IngredientDao,
    private val adjustmentDao: InventoryAdjustmentDao,
    private val branchDao: BranchDao,
    private val userDao: UserDao,
    private val sessionManager: SessionManager,
    private val branchConfig: BranchConfig,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val localBranchId = sessionManager.getBranchId().takeIf { it > 0 } ?: branchConfig.branchId
    private val _uiState = MutableStateFlow(
        InventoryAdjustmentUiState(
            isAdmin = isAdminUser(),
            selectedBranchId = localBranchId,
            localBranchId = localBranchId,
            startDate = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis,
            endDate = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis
        )
    )
    val uiState: StateFlow<InventoryAdjustmentUiState> = _uiState.asStateFlow()

    private val PAGE_SIZE = 50
    private var loadJob: kotlinx.coroutines.Job? = null

    private var inventoryItems: List<InventoryEntity> = emptyList()
    private var ingredients: List<IngredientEntity> = emptyList()
    private var branches: List<BranchEntity> = emptyList()
    private var users: List<UserEntity> = emptyList()

    private val refreshTrigger = MutableStateFlow(0)

    init {
        observeBranches()
        observeNetwork()
        observeInventory()
        observeIngredients()
        observeUsers()
        observeLocalAdjustments()

        // Reactive history loading: only one central point for loading
        viewModelScope.launch {
            combine(
                _uiState.map { it.isOnline }.distinctUntilChanged(),
                _uiState.map { it.selectedBranchId }.distinctUntilChanged(),
                _uiState.map { it.startDate }.distinctUntilChanged(),
                _uiState.map { it.endDate }.distinctUntilChanged(),
                refreshTrigger
            ) { online, branchId, start, end, trigger ->
                online to branchId
            }.collectLatest { (online, branchId) ->
                // ONLY load if online AND selected branch is NOT the local branch
                if (online && branchId != localBranchId) {
                    kotlinx.coroutines.delay(300) // Debounce branch selection and status changes
                    loadHistory()
                } else {
                    // For local branch, observeLocalAdjustments/loadLocalHistory handles it
                    loadLocalHistory()
                }
            }
        }
    }

    private fun observeLocalAdjustments() {
        viewModelScope.launch {
            adjustmentDao.observeAllAdjustments().collectLatest {
                val state = _uiState.value
                // If we are looking at the local branch or offline, refresh history
                if (!state.isOnline || state.selectedBranchId == localBranchId) {
                    loadLocalHistory()
                }
            }
        }
    }

    private fun observeBranches() {
        viewModelScope.launch {
            branchDao.observeAllBranches().collectLatest { items ->
                branches = items
                _uiState.update { it.copy(branches = items) }
                rebuildIngredients()
            }
        }
    }

    private fun observeNetwork() {
        viewModelScope.launch {
            networkMonitor.observeNetworkStatus().collectLatest { online ->
                _uiState.update { current ->
                    val forcedBranchId = if (!current.isAdmin) localBranchId else current.selectedBranchId
                    current.copy(isOnline = online, selectedBranchId = forcedBranchId)
                }
            }
        }
    }

    private fun observeInventory() {
        viewModelScope.launch {
            inventoryDao.observeAllInventory().collectLatest { items ->
                inventoryItems = items
                rebuildIngredients()
            }
        }
    }

    private fun observeIngredients() {
        viewModelScope.launch {
            ingredientDao.observeIngredients().collectLatest { items ->
                ingredients = items
                rebuildIngredients()
            }
        }
    }

    private fun observeUsers() {
        viewModelScope.launch {
            userDao.observeAllUsers().collectLatest { items ->
                users = items
                rebuildIngredients()
            }
        }
    }

    fun selectBranch(branchId: Int?) {
        val state = _uiState.value
        val finalBranchId = if (state.isAdmin && state.isOnline) branchId else localBranchId

        if (state.selectedBranchId == finalBranchId) return

        _uiState.update { it.copy(selectedBranchId = finalBranchId, isLoading = true) }
        refreshTrigger.value += 1
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setDateRange(start: Long?, end: Long?) {
        _uiState.update {
            it.copy(
                startDate = start,
                endDate = end?.let { e ->
                    val cal = Calendar.getInstance().apply {
                        timeInMillis = e
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }
                    cal.timeInMillis
                }
            )
        }
    }

    fun loadHistory() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val state = _uiState.value
            _uiState.update { it.copy(isLoading = true, currentPage = 0, history = emptyList()) }

            // Load local as placeholder/immediate view
            loadLocalHistory()

            if (state.isOnline && state.selectedBranchId != localBranchId) {
                // If remote is needed, show loading again and fetch
                _uiState.update { it.copy(isLoading = true) }
                loadRemoteHistory()
            }
        }
    }

    fun loadMoreHistory() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMore) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            val nextPage = state.currentPage + 1
            
            val from = state.startDate ?: 0L
            val to = state.endDate ?: System.currentTimeMillis()

            val result = reportRepository.getInventoryAdjustmentPage(
                branchId = state.selectedBranchId,
                from = from,
                to = to,
                page = nextPage,
                size = PAGE_SIZE
            )

            result.fold(
                onSuccess = { pageResponse ->
                    val newRows = pageResponse.items.map { item ->
                        val isAdd = item.adjustmentAmount >= 0.0
                        AdjustmentHistoryRow(
                            adjustmentId = item.adjustmentId,
                            ingredientName = item.ingredientName,
                            adjustmentType = if (isAdd) "Add" else "Reduce",
                            quantity = abs(item.adjustmentAmount),
                            reason = item.reason,
                            dateTime = item.dateTime,
                            branchId = state.selectedBranchId ?: 0,
                            branchName = branches.firstOrNull { it.branchId == state.selectedBranchId }?.branchName ?: "Remote Branch",
                            userName = item.userName
                        )
                    }
                    _uiState.update {
                        it.copy(
                            history = it.history + newRows,
                            currentPage = nextPage,
                            hasMore = pageResponse.hasNext,
                            isLoadingMore = false
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isLoadingMore = false, error = error.message) }
                }
            )
        }
    }

    private suspend fun loadLocalHistory() {
        val state = _uiState.value
        val from = state.startDate ?: 0L
        val to = state.endDate ?: System.currentTimeMillis()

        val items = if (state.selectedBranchId == null) {
            adjustmentDao.getAdjustmentsByDateRangeAllBranches(from, to)
        } else {
            adjustmentDao.getAdjustmentsByDateRange(state.selectedBranchId, from, to)
        }
        
        val rows = withContext(Dispatchers.Default) {
            val ingredientMap = ingredients.associateBy { it.ingredientId }
            val branchMap = branches.associateBy { it.branchId }
            val userMap = users.associateBy { it.userId }

            items.map { adjustment ->
                val isAdd = adjustment.adjustmentAmount >= 0.0
                AdjustmentHistoryRow(
                    adjustmentId = adjustment.adjustmentId,
                    ingredientName = ingredientMap[adjustment.ingredientId]?.ingredientName ?: "Unknown ingredient",
                    adjustmentType = if (isAdd) "Add" else "Reduce",
                    quantity = abs(adjustment.adjustmentAmount),
                    reason = adjustment.reason,
                    dateTime = adjustment.dateTime,
                    branchId = adjustment.branchId,
                    branchName = branchMap[adjustment.branchId]?.branchName ?: "Branch ${adjustment.branchId}",
                    userName = userMap[adjustment.userId]?.name ?: "User ${adjustment.userId}"
                )
            }.sortedByDescending { it.dateTime }
        }

        _uiState.update {
            it.copy(
                history = rows,
                isLoading = false,
                hasMore = false,
                totalAdjustments = rows.size,
                netAdjustmentQuantity = rows.sumOf { r -> if (r.adjustmentType == "Add") r.quantity else -r.quantity }
            )
        }
    }

    private suspend fun loadRemoteHistory() {
        val state = _uiState.value
        val from = state.startDate ?: 0L
        val to = state.endDate ?: System.currentTimeMillis()

        val summaryResult = reportRepository.getInventoryAdjustmentSummary(
            branchId = state.selectedBranchId,
            from = from,
            to = to
        )

        val pageResult = reportRepository.getInventoryAdjustmentPage(
            branchId = state.selectedBranchId,
            from = from,
            to = to,
            page = 0,
            size = PAGE_SIZE
        )

        summaryResult.fold(
            onSuccess = { summary ->
                _uiState.update {
                    it.copy(
                        totalAdjustments = summary.totalAdjustments,
                        netAdjustmentQuantity = summary.netAdjustmentQuantity
                    )
                }
            },
            onFailure = { error ->
                _uiState.update { it.copy(error = error.message) }
            }
        )

        pageResult.fold(
            onSuccess = { pageResponse ->
                val rows = pageResponse.items.map { item ->
                    val isAdd = item.adjustmentAmount >= 0.0
                    AdjustmentHistoryRow(
                        adjustmentId = item.adjustmentId,
                        ingredientName = item.ingredientName,
                        adjustmentType = if (isAdd) "Add" else "Reduce",
                        quantity = abs(item.adjustmentAmount),
                        reason = item.reason,
                        dateTime = item.dateTime,
                        branchId = state.selectedBranchId ?: 0,
                        branchName = branches.firstOrNull { it.branchId == state.selectedBranchId }?.branchName ?: "Remote Branch",
                        userName = item.userName
                    )
                }
                _uiState.update {
                    it.copy(
                        history = rows,
                        isLoading = false,
                        currentPage = 0,
                        hasMore = pageResponse.hasNext
                    )
                }
            },
            onFailure = { error ->
                _uiState.update { it.copy(isLoading = false) }
                loadLocalHistory()
            }
        )
    }

    private fun rebuildIngredients() {
        val ingredientMap = ingredients.associateBy { it.ingredientId }

        val ingredientRows = inventoryItems
            .filter { it.branchId == localBranchId }
            .mapNotNull { inventory ->
                val ingredient = ingredientMap[inventory.ingredientId] ?: return@mapNotNull null
                AdjustmentIngredientRow(
                    ingredientId = inventory.ingredientId,
                    branchId = inventory.branchId,
                    ingredientName = ingredient.ingredientName,
                    currentStock = inventory.currentStock,
                    unitType = ingredient.unitType
                )
            }.sortedBy { it.ingredientName.lowercase() }

        _uiState.update { it.copy(ingredients = ingredientRows) }
    }

    fun submitAdjustment(ingredient: AdjustmentIngredientRow?, type: String, quantityText: String, reason: String) {
        if (ingredient == null) { setError("Select an ingredient."); return }
        val quantity = quantityText.toDoubleOrNull() ?: run { setError("Enter a valid quantity."); return }
        if (quantity <= 0.0) { setError("Enter a valid quantity."); return }
        if (type == "Reduce" && ingredient.currentStock < quantity) { setError("Insufficient stock."); return }

        viewModelScope.launch {
            val userId = sessionManager.getUserId()
            val signedAmount = if (type == "Add") quantity else -quantity
            val result = adjustmentRepository.adjustInventory(ingredient.ingredientId, localBranchId, userId, signedAmount, reason.ifBlank { "Inventory adjustment" })
            if (result.isSuccess) {
                _uiState.update { it.copy(successMessage = "Adjustment saved.", error = null) }
                loadHistory()
            } else {
                _uiState.update { it.copy(error = result.exceptionOrNull()?.message ?: "Failed to save adjustment.", successMessage = null) }
            }
        }
    }

    fun clearMessages() { _uiState.update { it.copy(error = null, successMessage = null) } }
    private fun setError(message: String) { _uiState.update { it.copy(error = message, successMessage = null) } }
    private fun isAdminUser(): Boolean {
        return sessionManager.isAdmin()
    }
}
