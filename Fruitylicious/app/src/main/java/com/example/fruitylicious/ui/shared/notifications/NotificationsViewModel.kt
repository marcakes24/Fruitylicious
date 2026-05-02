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

data class NotificationRow(
    val ingredientId: Int,
    val branchId: Int,
    val branchName: String,
    val name: String,
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
    val localBranchId: Int = 1,
    val userBranchId: String = "B1",
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
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
        NotificationsUiState(
            isAdmin = isAdminUser(),
            selectedBranchId = localBranchId,
            localBranchId = localBranchId,
            userBranchId = "B$localBranchId"
        )
    )

    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    private var localInventoryItems: List<InventoryEntity> = emptyList()
    private var localIngredients: List<IngredientEntity> = emptyList()
    private var branches: List<BranchEntity> = emptyList()

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

        loadNotifications()
    }

    fun refresh() {
        _uiState.update {
            it.copy(
                isLoading = true,
                error = null
            )
        }

        loadNotifications()
    }

    private fun observeBranches() {
        viewModelScope.launch {
            branchDao.observeAllBranches().collectLatest { branchList ->
                branches = branchList

                _uiState.update {
                    it.copy(branches = branchList)
                }

                loadNotifications()
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

                loadNotifications()
            }
        }
    }

    private fun observeInventory() {
        viewModelScope.launch {
            inventoryDao.observeInventoryByBranch(localBranchId).collectLatest { items ->
                localInventoryItems = items

                if (!networkMonitor.isOnline() || !isAdminUser()) {
                    loadLocalNotifications(localBranchId)
                }
            }
        }
    }

    private fun observeIngredients() {
        viewModelScope.launch {
            ingredientDao.observeIngredients().collectLatest { items ->
                localIngredients = items

                if (!networkMonitor.isOnline() || !isAdminUser()) {
                    loadLocalNotifications(localBranchId)
                }
            }
        }
    }

    private fun loadNotifications() {
        val state = _uiState.value

        when {
            !state.isAdmin -> {
                loadLocalNotifications(localBranchId)
            }

            !state.isOnline -> {
                loadLocalNotifications(localBranchId)
            }

            state.selectedBranchId == null -> {
                loadRemoteAllBranchesNotifications()
            }

            else -> {
                loadRemoteBranchNotifications(state.selectedBranchId)
            }
        }
    }

    private fun loadLocalNotifications(branchId: Int) {
        val ingredientMap = localIngredients.associateBy { it.ingredientId }
        val branchName = branches.firstOrNull { it.branchId == branchId }?.branchName
            ?: "Branch $branchId"

        val rows = localInventoryItems
            .filter { it.branchId == branchId }
            .mapNotNull { inventory ->
                val ingredient = ingredientMap[inventory.ingredientId] ?: return@mapNotNull null

                buildNotificationRow(
                    ingredientId = ingredient.ingredientId,
                    branchId = inventory.branchId,
                    branchName = branchName,
                    name = ingredient.ingredientName,
                    currentStock = inventory.currentStock,
                    unitType = ingredient.unitType,
                    lowStockThreshold = ingredient.lowStockThreshold
                )
            }
            .sortedWith(notificationSorter())

        _uiState.update {
            it.copy(
                notifications = rows,
                isLoading = false,
                error = null
            )
        }
    }

    private fun loadRemoteBranchNotifications(branchId: Int) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, error = null)
            }

            val result = reportRepository.getInventoryReport(branchId)

            result.fold(
                onSuccess = { report ->
                    val rows = report.items
                        .mapNotNull { item ->
                            item.toNotificationRow(
                                branchId = report.branchId ?: branchId,
                                branchName = report.branchName ?: "Branch $branchId"
                            )
                        }
                        .sortedWith(notificationSorter())

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
                            notifications = emptyList(),
                            isLoading = false,
                            error = exception.message ?: "Failed to load remote notifications."
                        )
                    }
                }
            )
        }
    }

    private fun loadRemoteAllBranchesNotifications() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, error = null)
            }

            val branchList = branches.ifEmpty {
                branchDao.getAllBranches()
            }

            if (branchList.isEmpty()) {
                _uiState.update {
                    it.copy(
                        notifications = emptyList(),
                        isLoading = false,
                        error = "No branches found."
                    )
                }
                return@launch
            }

            val allRows = mutableListOf<NotificationRow>()
            var firstError: String? = null

            for (branch in branchList) {
                val result = reportRepository.getInventoryReport(branch.branchId)

                result.fold(
                    onSuccess = { report ->
                        val rows = report.items.mapNotNull { item ->
                            item.toNotificationRow(
                                branchId = report.branchId ?: branch.branchId,
                                branchName = report.branchName ?: branch.branchName
                            )
                        }

                        allRows.addAll(rows)
                    },
                    onFailure = { exception ->
                        if (firstError == null) {
                            firstError = exception.message
                        }
                    }
                )
            }

            _uiState.update {
                it.copy(
                    notifications = allRows.sortedWith(notificationSorter()),
                    isLoading = false,
                    error = firstError
                )
            }
        }
    }

    private fun InventoryReportItemDto.toNotificationRow(
        branchId: Int,
        branchName: String
    ): NotificationRow? {
        return buildNotificationRow(
            ingredientId = ingredientId,
            branchId = branchId,
            branchName = branchName,
            name = ingredientName,
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
        currentStock: Double,
        unitType: String,
        lowStockThreshold: Double
    ): NotificationRow? {
        if (lowStockThreshold <= 0.0) {
            return null
        }

        val status = when {
            currentStock <= lowStockThreshold -> "Critical"
            currentStock <= lowStockThreshold * 2 -> "Warning"
            else -> null
        } ?: return null

        val progress = (currentStock / lowStockThreshold)
            .toFloat()
            .coerceIn(0f, 1f)

        return NotificationRow(
            ingredientId = ingredientId,
            branchId = branchId,
            branchName = branchName,
            name = name,
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
        }.thenBy {
            it.branchId
        }.thenBy {
            it.name.lowercase()
        }
    }

    private fun isAdminUser(): Boolean {
        val role = sessionManager.getRole()

        return role.equals("admin", ignoreCase = true) ||
                role.equals("owner", ignoreCase = true)
    }
}