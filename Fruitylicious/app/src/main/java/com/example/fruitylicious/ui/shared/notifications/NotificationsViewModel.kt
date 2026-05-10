package com.example.fruitylicious.ui.shared.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitylicious.data.local.dao.BranchDao
import com.example.fruitylicious.data.local.dao.IngredientDao
import com.example.fruitylicious.data.local.dao.InventoryDao
import com.example.fruitylicious.data.local.entity.BranchEntity
import com.example.fruitylicious.data.local.entity.IngredientEntity
import com.example.fruitylicious.data.local.entity.InventoryEntity
import com.example.fruitylicious.data.remote.dto.InventoryReportItemDto
import com.example.fruitylicious.data.repository.ReportRepository
import com.example.fruitylicious.data.repository.StaffLogRepository
import com.example.fruitylicious.util.BranchConfig
import com.example.fruitylicious.util.NetworkMonitor
import com.example.fruitylicious.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationRow(
    val ingredientId: Int,
    val branchId: Int,
    val branchName: String,
    val name: String,
    val imageUrl: String?,
    val currentStock: Double,
    val unitType: String,
    val lowStockThreshold: Double,
    val status: String,
    val progress: Float
)

data class NotificationsUiState(
    val notifications: List<NotificationRow> = emptyList(),
    val branches: List<BranchEntity> = emptyList(),
    val selectedBranchId: Int? = null,
    val isAdmin: Boolean = false,
    val isOnline: Boolean = false,
    val isClockedIn: Boolean = false,
    val localBranchId: Int = 0,
    val userBranchId: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val inventoryDao: InventoryDao,
    private val ingredientDao: IngredientDao,
    private val branchDao: BranchDao,
    private val reportRepository: ReportRepository,
    private val staffLogRepository: StaffLogRepository,
    private val sessionManager: SessionManager,
    private val branchConfig: BranchConfig,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val localBranchId = sessionManager.getBranchId().takeIf { it > 0 } ?: branchConfig.branchId

    private val _uiState = MutableStateFlow(
        NotificationsUiState(
            isAdmin = isAdminUser(),
            selectedBranchId = localBranchId,
            localBranchId = localBranchId,
            userBranchId = "B$localBranchId"
        )
    )

    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    private var branches: List<BranchEntity> = emptyList()
    private val refreshTrigger = MutableStateFlow(0)
    private var loadJob: kotlinx.coroutines.Job? = null

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
                    loadNotifications()
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
                buildNotificationRows(inventory, ingredients)
            }.collect { rows ->
                _uiState.update { state ->
                    // Avoid showing local data as a "flickering" placeholder if we are currently loading remote data
                    val isActivelyLoadingRemote = state.isOnline && state.selectedBranchId != localBranchId && state.isLoading

                    if (state.selectedBranchId == localBranchId || !state.isOnline || state.error != null || (state.notifications.isEmpty() && !isActivelyLoadingRemote)) {
                        state.copy(
                            notifications = rows,
                            isLoading = if (state.selectedBranchId != localBranchId && state.isOnline && state.error == null) state.isLoading else false
                        )
                    } else {
                        state
                    }
                }
            }
        }
    }

    private fun buildNotificationRows(
        inventory: List<InventoryEntity>,
        ingredients: List<IngredientEntity>
    ): List<NotificationRow> {
        val state = _uiState.value
        val ingredientMap = ingredients.associateBy { it.ingredientId }
        
        val filteredInventory = if (state.selectedBranchId == null) {
            inventory
        } else {
            inventory.filter { it.branchId == state.selectedBranchId }
        }

        return filteredInventory.mapNotNull { inv ->
            val ingredient = ingredientMap[inv.ingredientId] ?: return@mapNotNull null
            val branchName = branches.firstOrNull { it.branchId == inv.branchId }?.branchName ?: "Branch ${inv.branchId}"

            buildNotificationRow(
                ingredientId = ingredient.ingredientId,
                branchId = inv.branchId,
                branchName = branchName,
                name = ingredient.ingredientName,
                imageUrl = ingredient.image,
                currentStock = inv.currentStock,
                unitType = ingredient.unitType,
                lowStockThreshold = ingredient.lowStockThreshold
            )
        }.sortedWith(notificationSorter())
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
                // Removed: notifications = emptyList() to prevent flickering
            )
        }

        refreshTrigger.value += 1
    }

    private fun loadNotifications() {
        val state = _uiState.value
        if (state.isOnline && state.selectedBranchId != localBranchId) {
            loadRemoteNotifications(state.selectedBranchId)
        } else {
            loadJob?.cancel()
        }
    }

    fun refresh() {
        _uiState.update {
            it.copy(
                isLoading = true,
                error = null,
                notifications = emptyList()
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
                _uiState.update { it.copy(isClockedIn = hasActiveLog) }
            }
        }
    }

    private fun loadRemoteNotifications(branchId: Int?) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }

                if (branchId == null) {
                    loadRemoteAllBranchesNotificationsInternal()
                } else {
                    loadRemoteBranchNotificationsInternal(branchId)
                }
            } catch (t: Throwable) {
                if (t is kotlinx.coroutines.CancellationException) {
                    return@launch
                }

                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        error = t.message ?: "Failed to load remote notifications."
                    )
                }
            }
        }
    }

    private suspend fun loadRemoteBranchNotificationsInternal(branchId: Int) {
        val result = reportRepository.getInventoryReport(branchId)

        result.fold(
            onSuccess = { report ->
                val rows = report.items.mapNotNull { item ->
                    item.toNotificationRow(
                        branchId = report.branchId ?: branchId,
                        branchName = report.branchName ?: "Branch $branchId"
                    )
                }.sortedWith(notificationSorter())

                _uiState.update {
                    it.copy(
                        notifications = rows,
                        isLoading = false,
                        error = null
                    )
                }
            },
            onFailure = { exception ->
                _uiState.update {
                    it.copy(
                        isLoading = false
                    )
                }
                refreshTrigger.value += 1
            }
        )
    }

    private suspend fun loadRemoteAllBranchesNotificationsInternal() {
        val allRows = mutableListOf<NotificationRow>()
        var firstError: String? = null

        try {
            // Use supervisorScope to isolate sibling failures and handle cancellation better
            kotlinx.coroutines.supervisorScope {
                val deferredResults = branches.map { branch ->
                    async {
                        branch to reportRepository.getInventoryReport(branch.branchId)
                    }
                }

                val results = deferredResults.awaitAll()

                results.forEach { (branch, result) ->
                    result.fold(
                        onSuccess = { report ->
                            allRows.addAll(report.items.mapNotNull { item ->
                                item.toNotificationRow(
                                    branchId = branch.branchId,
                                    branchName = branch.branchName
                                )
                            })
                        },
                        onFailure = { exception ->
                            if (firstError == null) firstError = exception.message
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
            _uiState.update {
                it.copy(
                    notifications = allRows.sortedWith(notificationSorter()),
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

    private fun InventoryReportItemDto.toNotificationRow(
        branchId: Int,
        branchName: String
    ): NotificationRow? {
        // We use local images if available
        val localImage = null // Could lookup from localIngredients if needed, but not critical for remote view
        
        return buildNotificationRow(
            ingredientId = ingredientId,
            branchId = branchId,
            branchName = branchName,
            name = ingredientName,
            imageUrl = localImage,
            currentStock = currentStock,
            unitType = unitType,
            lowStockThreshold = lowStockThreshold
        )
    }

    private fun buildNotificationRow(
        ingredientId: Int,
        branchId: Int,
        branchName: String,
        name: String,
        imageUrl: String?,
        currentStock: Double,
        unitType: String,
        lowStockThreshold: Double
    ): NotificationRow? {
        if (lowStockThreshold <= 0.0) return null

        val status = when {
            currentStock <= lowStockThreshold -> "Critical"
            currentStock <= lowStockThreshold * 2 -> "Warning"
            else -> null
        } ?: return null

        val progress = (currentStock / lowStockThreshold).toFloat().coerceIn(0f, 1f)

        return NotificationRow(
            ingredientId = ingredientId,
            branchId = branchId,
            branchName = branchName,
            name = name,
            imageUrl = imageUrl,
            currentStock = currentStock,
            unitType = unitType,
            lowStockThreshold = lowStockThreshold,
            status = status,
            progress = progress
        )
    }

    private fun notificationSorter(): Comparator<NotificationRow> {
        return compareBy<NotificationRow> {
            when (it.status) {
                "Critical" -> 0
                "Warning" -> 1
                else -> 2
            }
        }.thenBy { it.branchId }.thenBy { it.name.lowercase() }
    }

    private fun isAdminUser(): Boolean {
        return sessionManager.isAdmin()
    }
}
