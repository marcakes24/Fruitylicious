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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
    val isLoading: Boolean = true,
    val isAdmin: Boolean = false,
    val isOnline: Boolean = false,
    val localBranchId: Int = 1,
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class InventoryAdjustmentViewModel @Inject constructor(
    private val adjustmentRepository: AdjustmentRepository,
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
            localBranchId = localBranchId
        )
    )
    val uiState: StateFlow<InventoryAdjustmentUiState> = _uiState.asStateFlow()

    private var inventoryItems: List<InventoryEntity> = emptyList()
    private var ingredients: List<IngredientEntity> = emptyList()
    private var adjustments: List<InventoryAdjustmentEntity> = emptyList()
    private var branches: List<BranchEntity> = emptyList()
    private var users: List<UserEntity> = emptyList()

    init {
        observeBranches()
        observeNetwork()
        observeInventory()
        observeIngredients()
        observeAdjustments()
        observeUsers()
    }

    private fun observeBranches() {
        viewModelScope.launch {
            branchDao.observeAllBranches().collectLatest { items ->
                branches = items
                _uiState.update { it.copy(branches = items) }
                rebuildState()
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
                rebuildState()
            }
        }
    }

    private fun observeInventory() {
        viewModelScope.launch {
            inventoryDao.observeAllInventory().collectLatest { items ->
                inventoryItems = items
                rebuildState()
            }
        }
    }

    private fun observeIngredients() {
        viewModelScope.launch {
            ingredientDao.observeIngredients().collectLatest { items ->
                ingredients = items
                rebuildState()
            }
        }
    }

    private fun observeAdjustments() {
        viewModelScope.launch {
            adjustmentDao.observeAllAdjustments().collectLatest { items ->
                adjustments = items
                rebuildState()
            }
        }
    }

    private fun observeUsers() {
        viewModelScope.launch {
            userDao.observeAllUsers().collectLatest { items ->
                users = items
                rebuildState()
            }
        }
    }

    fun selectBranch(branchId: Int?) {
        val state = _uiState.value
        val finalBranchId = if (state.isAdmin && state.isOnline) branchId else localBranchId
        _uiState.update { it.copy(selectedBranchId = finalBranchId) }
        rebuildState()
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        rebuildState()
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
        rebuildState()
    }

    private fun rebuildState() {
        val currentState = _uiState.value
        val ingredientMap = ingredients.associateBy { it.ingredientId }
        val branchMap = branches.associateBy { it.branchId }
        val userMap = users.associateBy { it.userId }

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

        val historyRows = adjustments.filter { adjustment ->
            val branchMatches = currentState.selectedBranchId == null || adjustment.branchId == currentState.selectedBranchId
            val ingredientName = ingredientMap[adjustment.ingredientId]?.ingredientName ?: ""
            val userName = userMap[adjustment.userId]?.name ?: "User ${adjustment.userId}"
            val branchName = branchMap[adjustment.branchId]?.branchName ?: "Branch ${adjustment.branchId}"
            
            val query = currentState.searchQuery.trim()
            val searchMatches = query.isBlank() || 
                    ingredientName.contains(query, ignoreCase = true) ||
                    userName.contains(query, ignoreCase = true) ||
                    adjustment.reason.contains(query, ignoreCase = true) ||
                    branchName.contains(query, ignoreCase = true)

            val dateMatches = (currentState.startDate == null || adjustment.dateTime >= currentState.startDate) &&
                             (currentState.endDate == null || adjustment.dateTime <= currentState.endDate)

            branchMatches && searchMatches && dateMatches
        }.map { adjustment ->
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

        _uiState.update { it.copy(ingredients = ingredientRows, history = historyRows, isLoading = false) }
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
            } else {
                _uiState.update { it.copy(error = result.exceptionOrNull()?.message ?: "Failed to save adjustment.", successMessage = null) }
            }
        }
    }

    fun clearMessages() { _uiState.update { it.copy(error = null, successMessage = null) } }
    private fun setError(message: String) { _uiState.update { it.copy(error = message, successMessage = null) } }
    private fun isAdminUser(): Boolean {
        val role = sessionManager.getRole()
        return role.equals("admin", ignoreCase = true) || role.equals("owner", ignoreCase = true)
    }
}
