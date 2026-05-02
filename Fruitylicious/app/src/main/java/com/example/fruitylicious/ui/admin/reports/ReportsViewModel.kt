package com.example.fruitylicious.ui.admin.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitylicious.data.local.dao.BranchDao
import com.example.fruitylicious.data.local.entity.BranchEntity
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

data class ReportsUiState(
    val adminName: String = "",
    val isAdmin: Boolean = false,
    val localBranchId: Int = 1,
    val selectedBranchId: Int? = 1,
    val branches: List<BranchEntity> = emptyList(),
    val isOnline: Boolean = false,
    val canAccessCrossBranch: Boolean = false,
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    sessionManager: SessionManager,
    private val branchDao: BranchDao,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ReportsUiState(
            adminName = sessionManager.getUserName().ifBlank { "Admin User" },
            isAdmin = (sessionManager.getRole()?.equals("admin", ignoreCase = true) == true ||
                    sessionManager.getRole()?.equals("owner", ignoreCase = true) == true),
            localBranchId = sessionManager.getBranchId(),
            selectedBranchId = sessionManager.getBranchId()
        )
    )
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    init {
        observeBranches()
        observeNetworkStatus()
    }

    private fun observeBranches() {
        viewModelScope.launch {
            branchDao.observeAllBranches().collectLatest { branchList ->
                _uiState.update { it.copy(branches = branchList) }
            }
        }
    }

    private fun observeNetworkStatus() {
        viewModelScope.launch {
            networkMonitor.observeNetworkStatus().collectLatest { online ->
                _uiState.update { state ->
                    val canAccess = state.isAdmin && online
                    val newSelectedId = if (!canAccess && state.selectedBranchId != state.localBranchId) {
                        state.localBranchId
                    } else {
                        state.selectedBranchId
                    }

                    state.copy(
                        isOnline = online,
                        canAccessCrossBranch = canAccess,
                        selectedBranchId = newSelectedId
                    )
                }
            }
        }
    }

    fun onBranchSelected(branchId: Int?) {
        val state = _uiState.value
        if (branchId != state.localBranchId && !state.canAccessCrossBranch) {
            // Restriction applies
            return
        }
        _uiState.update { it.copy(selectedBranchId = branchId) }
    }
}
