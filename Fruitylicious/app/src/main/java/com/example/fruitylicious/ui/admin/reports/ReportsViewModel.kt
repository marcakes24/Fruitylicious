package com.example.fruitylicious.ui.admin.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitylicious.data.local.dao.BranchDao
import com.example.fruitylicious.data.local.entity.BranchEntity
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

data class ReportsUiState(
    val adminName: String = "",
    val isAdmin: Boolean = false,
    val localBranchId: Int = 1,
    val selectedBranchId: Int? = 1,
    val branches: List<BranchEntity> = emptyList(),
    val isOnline: Boolean = false,
    val canAccessCrossBranch: Boolean = false,
    val isRemoteAccessLocked: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val branchDao: BranchDao,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val localBranchId = sessionManager.getBranchId()
    private var lockoutJob: kotlinx.coroutines.Job? = null

    private val _uiState = MutableStateFlow(
        ReportsUiState(
            adminName = sessionManager.getUserName().ifBlank { "Owner User" },
            isAdmin = isAdminUser(),
            localBranchId = localBranchId,
            selectedBranchId = localBranchId
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
                _uiState.update {
                    it.copy(
                        branches = branchList
                    )
                }
            }
        }
    }

    private fun observeNetworkStatus() {
        viewModelScope.launch {
            networkMonitor.observeNetworkStatus().collectLatest { online ->
                _uiState.update { state ->
                    val canAccessCrossBranch = state.isAdmin

                    val selectedBranchId = if (canAccessCrossBranch) {
                        state.selectedBranchId
                    } else {
                        state.localBranchId
                    }

                    state.copy(
                        isOnline = online,
                        canAccessCrossBranch = canAccessCrossBranch,
                        selectedBranchId = selectedBranchId
                    )
                }
            }
        }
    }

    fun onBranchSelected(branchId: Int?) {
        val state = _uiState.value
        val finalBranchId = if (state.canAccessCrossBranch) {
            branchId
        } else {
            state.localBranchId
        }

        _uiState.update {
            it.copy(
                selectedBranchId = finalBranchId,
                isLoading = true
            )
        }

        viewModelScope.launch {
            kotlinx.coroutines.delay(500)
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun setRemoteError(errorMessage: String?) {
        _uiState.update { it.copy(error = errorMessage, isRemoteAccessLocked = errorMessage != null) }
        if (errorMessage != null) {
            startLockoutTimer()
        }
    }

    private fun startLockoutTimer() {
        lockoutJob?.cancel()
        lockoutJob = viewModelScope.launch {
            kotlinx.coroutines.delay(5 * 60 * 1000L)
            _uiState.update { it.copy(isRemoteAccessLocked = false) }
        }
    }

    private fun isAdminUser(): Boolean {
        return sessionManager.isAdmin()
    }
}