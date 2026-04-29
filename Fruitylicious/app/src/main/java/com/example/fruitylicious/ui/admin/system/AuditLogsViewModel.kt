package com.example.fruitylicious.ui.admin.system

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitylicious.data.local.entity.AuditLogEntity
import com.example.fruitylicious.data.local.entity.UserEntity
import com.example.fruitylicious.data.repository.AuditLogRepository
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

enum class AuditSyncFilter {
    ALL,
    SYNCED_ONLY,
    UNSYNCED_ONLY
}

data class AuditLogsUiState(
    val logs: List<AuditLogEntity> = emptyList(),
    val filteredLogs: List<AuditLogEntity> = emptyList(),
    val users: List<UserEntity> = emptyList(),
    val userNames: Map<Int, String> = emptyMap(),

    val selectedSyncFilter: AuditSyncFilter = AuditSyncFilter.ALL,
    val selectedUserId: Int? = null,
    val selectedUserName: String = "All users",
    val actionQuery: String = "",
    val tableQuery: String = "",

    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class AuditLogsViewModel @Inject constructor(
    private val auditLogRepository: AuditLogRepository,
    private val userRepository: UserRepository,
    sessionManager: SessionManager,
    branchConfig: BranchConfig
) : ViewModel() {

    private val branchId: Int =
        sessionManager.getBranchId().takeIf { it > 0 } ?: branchConfig.branchId

    private val _uiState = MutableStateFlow(AuditLogsUiState())
    val uiState: StateFlow<AuditLogsUiState> = _uiState.asStateFlow()

    init {
        observeUsers()
        observeAuditLogs()
    }

    private fun observeUsers() {
        viewModelScope.launch {
            userRepository.observeUsers().collectLatest { users ->
                _uiState.update { state ->
                    state.copy(
                        users = users,
                        userNames = users.associate { user ->
                            user.userId to user.name
                        },
                        selectedUserName = if (state.selectedUserId == null) {
                            "All users"
                        } else {
                            users.firstOrNull { it.userId == state.selectedUserId }?.name
                                ?: state.selectedUserName
                        }
                    )
                }
            }
        }
    }

    private fun observeAuditLogs() {
        viewModelScope.launch {
            auditLogRepository.observeAuditLogs(branchId).collectLatest { logs ->
                _uiState.update { state ->
                    state.copy(
                        logs = logs,
                        filteredLogs = applyFilters(
                            logs = logs,
                            syncFilter = state.selectedSyncFilter,
                            selectedUserId = state.selectedUserId,
                            actionQuery = state.actionQuery,
                            tableQuery = state.tableQuery
                        ),
                        isLoading = false,
                        error = null
                    )
                }
            }
        }
    }

    fun setSyncFilter(filter: AuditSyncFilter) {
        _uiState.update { state ->
            state.copy(
                selectedSyncFilter = filter,
                filteredLogs = applyFilters(
                    logs = state.logs,
                    syncFilter = filter,
                    selectedUserId = state.selectedUserId,
                    actionQuery = state.actionQuery,
                    tableQuery = state.tableQuery
                )
            )
        }
    }

    fun toggleUnsyncedOnly() {
        _uiState.update { state ->
            val nextFilter = if (state.selectedSyncFilter == AuditSyncFilter.UNSYNCED_ONLY) {
                AuditSyncFilter.ALL
            } else {
                AuditSyncFilter.UNSYNCED_ONLY
            }

            state.copy(
                selectedSyncFilter = nextFilter,
                filteredLogs = applyFilters(
                    logs = state.logs,
                    syncFilter = nextFilter,
                    selectedUserId = state.selectedUserId,
                    actionQuery = state.actionQuery,
                    tableQuery = state.tableQuery
                )
            )
        }
    }

    fun showAll() {
        _uiState.update { state ->
            state.copy(
                selectedSyncFilter = AuditSyncFilter.ALL,
                filteredLogs = applyFilters(
                    logs = state.logs,
                    syncFilter = AuditSyncFilter.ALL,
                    selectedUserId = state.selectedUserId,
                    actionQuery = state.actionQuery,
                    tableQuery = state.tableQuery
                )
            )
        }
    }

    fun showSyncedOnly() {
        setSyncFilter(AuditSyncFilter.SYNCED_ONLY)
    }

    fun showUnsyncedOnly() {
        setSyncFilter(AuditSyncFilter.UNSYNCED_ONLY)
    }

    fun onActionQueryChanged(value: String) {
        _uiState.update { state ->
            state.copy(
                actionQuery = value,
                filteredLogs = applyFilters(
                    logs = state.logs,
                    syncFilter = state.selectedSyncFilter,
                    selectedUserId = state.selectedUserId,
                    actionQuery = value,
                    tableQuery = state.tableQuery
                )
            )
        }
    }

    fun onTableQueryChanged(value: String) {
        _uiState.update { state ->
            state.copy(
                tableQuery = value,
                filteredLogs = applyFilters(
                    logs = state.logs,
                    syncFilter = state.selectedSyncFilter,
                    selectedUserId = state.selectedUserId,
                    actionQuery = state.actionQuery,
                    tableQuery = value
                )
            )
        }
    }

    fun onUserSelected(userId: Int?) {
        _uiState.update { state ->
            val selectedName = if (userId == null) {
                "All users"
            } else {
                state.users.firstOrNull { it.userId == userId }?.name ?: "User $userId"
            }

            state.copy(
                selectedUserId = userId,
                selectedUserName = selectedName,
                filteredLogs = applyFilters(
                    logs = state.logs,
                    syncFilter = state.selectedSyncFilter,
                    selectedUserId = userId,
                    actionQuery = state.actionQuery,
                    tableQuery = state.tableQuery
                )
            )
        }
    }

    fun clearFilters() {
        _uiState.update { state ->
            state.copy(
                selectedSyncFilter = AuditSyncFilter.ALL,
                selectedUserId = null,
                selectedUserName = "All users",
                actionQuery = "",
                tableQuery = "",
                filteredLogs = state.logs
            )
        }
    }

    fun clearError() {
        _uiState.update {
            it.copy(error = null)
        }
    }

    private fun applyFilters(
        logs: List<AuditLogEntity>,
        syncFilter: AuditSyncFilter,
        selectedUserId: Int?,
        actionQuery: String,
        tableQuery: String
    ): List<AuditLogEntity> {
        val cleanActionQuery = actionQuery.trim()
        val cleanTableQuery = tableQuery.trim()

        return logs
            .filter { log ->
                when (syncFilter) {
                    AuditSyncFilter.ALL -> true
                    AuditSyncFilter.SYNCED_ONLY -> log.isSynced
                    AuditSyncFilter.UNSYNCED_ONLY -> !log.isSynced
                }
            }
            .filter { log ->
                selectedUserId == null || log.userId == selectedUserId
            }
            .filter { log ->
                cleanActionQuery.isBlank() ||
                        log.action.contains(cleanActionQuery, ignoreCase = true)
            }
            .filter { log ->
                cleanTableQuery.isBlank() ||
                        log.tableAffected.contains(cleanTableQuery, ignoreCase = true)
            }
            .sortedByDescending { it.timestamp }
    }
}