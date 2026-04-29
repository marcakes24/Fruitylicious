package com.example.fruitylicious.ui.admin.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitylicious.data.repository.IngredientRepository
import com.example.fruitylicious.data.repository.ProductRepository
import com.example.fruitylicious.domain.usecase.auth.LogoutUseCase
import com.example.fruitylicious.domain.usecase.inventory.CheckLowStockUseCase
import com.example.fruitylicious.util.BranchConfig
import com.example.fruitylicious.util.NetworkMonitor
import com.example.fruitylicious.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminDashboardUiState(
    val userName: String = "",
    val selectedBranchId: Int = 0,
    val selectedBranchName: String = "",
    val isOnline: Boolean = false,
    val lastSyncAt: Long = 0L,
    val lastSyncSuccessful: Boolean = false,
    val lastSyncMessage: String = "Not synced yet.",
    val lowStockCount: Int = 0,
    val productCount: Int = 0,
    val ingredientCount: Int = 0
)

@HiltViewModel
class AdminDashboardViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val branchConfig: BranchConfig,
    private val networkMonitor: NetworkMonitor,
    private val checkLowStockUseCase: CheckLowStockUseCase,
    private val productRepository: ProductRepository,
    private val ingredientRepository: IngredientRepository,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    private val branchId = sessionManager.getBranchId().takeIf { it > 0 } ?: branchConfig.branchId

    private val _uiState = MutableStateFlow(
        AdminDashboardUiState(
            userName = sessionManager.getUserName(),
            selectedBranchId = branchId,
            selectedBranchName = branchConfig.branchName,
            lastSyncAt = sessionManager.getLastSyncAt(),
            lastSyncSuccessful = sessionManager.wasLastSyncSuccessful(),
            lastSyncMessage = sessionManager.getLastSyncMessage()
        )
    )
    val uiState: StateFlow<AdminDashboardUiState> = _uiState.asStateFlow()

    init {
        observeNetwork()
        observeLowStock()
        observeCounts()
    }

    private fun observeNetwork() {
        viewModelScope.launch {
            networkMonitor.observeNetworkStatus().collectLatest { isOnline ->
                _uiState.update {
                    it.copy(
                        isOnline = isOnline,
                        lastSyncAt = sessionManager.getLastSyncAt(),
                        lastSyncSuccessful = sessionManager.wasLastSyncSuccessful(),
                        lastSyncMessage = sessionManager.getLastSyncMessage()
                    )
                }
            }
        }
    }

    private fun observeLowStock() {
        viewModelScope.launch {
            checkLowStockUseCase(branchId).collectLatest { lowStock ->
                _uiState.update {
                    it.copy(lowStockCount = lowStock.size)
                }
            }
        }
    }

    private fun observeCounts() {
        viewModelScope.launch {
            productRepository.observeProducts().collectLatest { products ->
                _uiState.update {
                    it.copy(productCount = products.size)
                }
            }
        }

        viewModelScope.launch {
            ingredientRepository.observeIngredients().collectLatest { ingredients ->
                _uiState.update {
                    it.copy(ingredientCount = ingredients.size)
                }
            }
        }
    }

    fun logout() {
        logoutUseCase()
    }
}