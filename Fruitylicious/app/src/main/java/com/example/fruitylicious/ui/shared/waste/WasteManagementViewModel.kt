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
import com.example.fruitylicious.data.local.entity.InventoryEntity
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

    private var localInventory: List<InventoryEntity> = emptyList()
    private var localIngredients: List<IngredientEntity> = emptyList()
    private var localWasteLogs: List<WasteLogEntity> = emptyList()
    private var branches: List<BranchEntity> = emptyList()

    init {
        observeBranches()
        observeNetwork()
        observeInventory()
        observeIngredients()
        observeWasteLogs()
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
            val role = sessionManager.getRole()

            if (role?.equals("admin", ignoreCase = true) == true ||
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
                            "You must clock in before recording waste."
                        } else {
                            null
                        }
                    )
                }
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

    private fun observeIngredients() {
        viewModelScope.launch {
            ingredientDao.observeIngredients().collectLatest { items ->
                localIngredients = items
                rebuildIngredientRows()
                loadHistory()
            }
        }
    }

    private fun observeWasteLogs() {
        viewModelScope.launch {
            wasteLogDao.observeWasteLogsByBranch(localBranchId).collectLatest { items ->
                localWasteLogs = items
                loadHistory()
            }
        }
    }

    private fun rebuildIngredientRows() {
        val ingredientMap = localIngredients.associateBy { it.ingredientId }

        val ingredientRows = localInventory.mapNotNull { item ->
            val ingredient = ingredientMap[item.ingredientId] ?: return@mapNotNull null

            WasteIngredientRow(
                ingredientId = item.ingredientId,
                branchId = item.branchId,
                ingredientName = ingredient.ingredientName,
                currentStock = item.currentStock,
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

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMore) return
        
        // Only for local history in this simple implementation
        if (state.isAdmin && state.isOnline && state.selectedBranchId != localBranchId) return

        _uiState.update { it.copy(isLoadingMore = true) }

        viewModelScope.launch {
            val nextPage = state.currentPage + 1
            val offset = nextPage * PAGE_SIZE
            
            val newEntities = if (state.selectedBranchId == null) {
                wasteLogDao.getAllWasteLogsPaged(PAGE_SIZE, offset)
            } else {
                wasteLogDao.getWasteLogsByBranchPaged(state.selectedBranchId, PAGE_SIZE, offset)
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

        when {
            state.selectedBranchId == localBranchId -> {
                loadLocalHistory(localBranchId)
            }

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

        val historyRows = localWasteLogs
            .filter { it.branchId == branchId }
            .take(PAGE_SIZE)
            .map { log ->
                val ingredient = ingredientMap[log.ingredientId]

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

            val result = reportRepository.getWasteReport(
                branchId = branchId,
                from = from,
                to = to
            )

            result.fold(
                onSuccess = { report ->
                    val rows = report.items.map { item ->
                        WasteHistoryRow(
                            wasteId = item.wasteId,
                            ingredientName = item.ingredientName,
                            quantity = item.quantity,
                            unitType = item.unitType,
                            reason = item.reason,
                            branchId = report.branchId ?: branchId,
                            branchName = report.branchName ?: "Branch $branchId",
                            dateTime = item.dateTime,
                            imagePath = ImageStorage.saveBase64Image(
                                context = context,
                                base64Value = item.image,
                                folder = "waste"
                            )
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
                            error = error.message ?: "Failed to load remote waste history."
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
            val allRows = mutableListOf<WasteHistoryRow>()
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
                val result = reportRepository.getWasteReport(
                    branchId = branch.branchId,
                    from = from,
                    to = to
                )

                result.fold(
                    onSuccess = { report ->
                        val rows = report.items.map { item ->
                            WasteHistoryRow(
                                wasteId = item.wasteId,
                                ingredientName = item.ingredientName,
                                quantity = item.quantity,
                                unitType = item.unitType,
                                reason = item.reason,
                                branchId = report.branchId ?: branch.branchId,
                                branchName = report.branchName ?: branch.branchName,
                                dateTime = item.dateTime,
                                imagePath = ImageStorage.saveBase64Image(
                                    context = context,
                                    base64Value = item.image,
                                    folder = "waste"
                                )
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
            val branchId = localBranchId

            val result = wasteRepository.logWaste(
                ingredientId = ingredient.ingredientId,
                branchId = branchId,
                userId = userId,
                quantity = quantity,
                image = imagePath,
                reason = reason.ifBlank { "Waste entry" }
            )

            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        successMessage = "Waste entry saved.",
                        error = null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        error = result.exceptionOrNull()?.message ?: "Failed to save waste entry.",
                        successMessage = null
                    )
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
        val role = sessionManager.getRole()

        return role.equals("admin", ignoreCase = true) ||
                role.equals("owner", ignoreCase = true)
    }
}