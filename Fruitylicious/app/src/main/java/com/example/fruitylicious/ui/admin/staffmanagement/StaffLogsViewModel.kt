package com.example.fruitylicious.ui.admin.staffmanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitylicious.data.local.entity.StaffLogEntity
import com.example.fruitylicious.data.repository.StaffLogRepository
import com.example.fruitylicious.data.repository.UserRepository
import com.example.fruitylicious.util.BranchConfig
import com.example.fruitylicious.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StaffLogsUiState(
    val logs: List<StaffLogEntity> = emptyList(),
    val filteredLogs: List<StaffLogEntity> = emptyList(),
    val userNames: Map<Int, String> = emptyMap(),
    val showActiveOnly: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class StaffLogsViewModel @Inject constructor(
    private val staffLogRepository: StaffLogRepository,
    private val userRepository: UserRepository,
    sessionManager: SessionManager,
    branchConfig: BranchConfig
) : ViewModel() {

    private val branchId = sessionManager.getBranchId().takeIf { it > 0 } ?: branchConfig.branchId

    private val _uiState = MutableStateFlow(StaffLogsUiState())
    val uiState: StateFlow<StaffLogsUiState> = _uiState.asStateFlow()

    init {
        observeStaffLogs()
        observeUsers()
    }

    private fun observeStaffLogs() {
        viewModelScope.launch {
            staffLogRepository.observeStaffLogs(branchId).collectLatest { logs ->
                _uiState.update { state ->
                    val filtered = applyFilter(logs, state.showActiveOnly)
                    state.copy(
                        logs = logs,
                        filteredLogs = filtered,
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun observeUsers() {
        viewModelScope.launch {
            userRepository.observeUsers().collectLatest { users ->
                _uiState.update {
                    it.copy(
                        userNames = users.associate { user -> user.userId to user.name }
                    )
                }
            }
        }
    }

    fun toggleActiveOnly() {
        _uiState.update { state ->
            val nextValue = !state.showActiveOnly
            state.copy(
                showActiveOnly = nextValue,
                filteredLogs = applyFilter(state.logs, nextValue)
            )
        }
    }

    fun showAll() {
        _uiState.update { state ->
            state.copy(
                showActiveOnly = false,
                filteredLogs = applyFilter(state.logs, false)
            )
        }
    }

    private fun applyFilter(
        logs: List<StaffLogEntity>,
        activeOnly: Boolean
    ): List<StaffLogEntity> {
        return if (activeOnly) {
            logs.filter { it.clockOut == null }
        } else {
            logs
        }
    }
}