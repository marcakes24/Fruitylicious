package com.example.fruitylicious.ui.shared.waste

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.example.fruitylicious.data.local.dao.AuditLogDao
import com.example.fruitylicious.data.local.dao.BranchDao
import com.example.fruitylicious.data.local.dao.IngredientDao
import com.example.fruitylicious.data.local.dao.InventoryDao
import com.example.fruitylicious.data.local.dao.WasteLogDao
import com.example.fruitylicious.data.local.db.PosDatabase
import com.example.fruitylicious.data.local.entity.AuditLogEntity
import com.example.fruitylicious.data.local.entity.BranchEntity
import com.example.fruitylicious.data.local.entity.IngredientEntity
import com.example.fruitylicious.data.local.entity.InventoryEntity
import com.example.fruitylicious.data.local.entity.WasteLogEntity
import com.example.fruitylicious.data.repository.ReportRepository
import com.example.fruitylicious.data.repository.StaffLogRepository
import com.example.fruitylicious.data.repository.SyncRepository
import com.example.fruitylicious.util.BranchConfig
import com.example.fruitylicious.util.ImageStorage
import com.example.fruitylicious.util.NetworkMonitor
import com.example.fruitylicious.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
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
    val isClockedIn: Boolean = true,
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class WasteManagementViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: PosDatabase,
    private val inventoryDao: InventoryDao,
    private val ingredientDao: IngredientDao,
    private val wasteLogDao: WasteLogDao,
    private val auditLogDao: AuditLogDao,
    private val branchDao: BranchDao,
    private val reportRepository: ReportRepository,
    private val staffLogRepository: StaffLogRepository,
    private val sessionManager: SessionManager,
    private val branchConfig: BranchConfig,
    private val networkMonitor: NetworkMonitor,
    private val syncRepository: SyncRepository
) : ViewModel() {

    private val localBranchId = branchConfig.branchId

    private val _uiState = MutableStateFlow(
        WasteManagementUiState(
            isAdmin = isAdminUser(),
            selectedBranchId = localBranchId,
            localBranchId = localBranchId,
            userBranchId = "B$localBranchId"
        )
    )

    val uiState: StateFlow<WasteManagementUiState> = _uiState.asStateFlow()

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

        val historyRows = localWasteLogs
            .filter { it.branchId == branchId }
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
        imagePath: String?
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
            val now = System.currentTimeMillis()
            val userId = sessionManager.getUserId()
            val branchId = localBranchId

            database.withTransaction {
                inventoryDao.deductStock(
                    ingredientId = ingredient.ingredientId,
                    branchId = branchId,
                    amount = quantity,
                    lastModified = now
                )

                wasteLogDao.upsertWasteLog(
                    WasteLogEntity(
                        wasteId = UUID.randomUUID().toString(),
                        ingredientId = ingredient.ingredientId,
                        branchId = branchId,
                        userId = userId,
                        quantity = quantity,
                        reason = reason.ifBlank { "Waste entry" },
                        image = imagePath,
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
                        action = "Recorded waste for ${ingredient.ingredientName}: $quantity ${ingredient.unitType}.",
                        tableAffected = "waste_logs",
                        timestamp = now,
                        lastModified = now,
                        isSynced = false,
                        syncedAt = null
                    )
                )
            }

            _uiState.update {
                it.copy(
                    successMessage = "Waste entry saved.",
                    error = null
                )
            }

            if (_uiState.value.isOnline) {
                syncRepository.pushUnsynced()
                loadHistory()
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