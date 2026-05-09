package com.example.fruitylicious.ui.admin.system

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitylicious.data.local.dao.AuditLogDao
import com.example.fruitylicious.data.local.dao.BranchDao
import com.example.fruitylicious.data.local.dao.UserDao
import com.example.fruitylicious.data.local.entity.AuditLogEntity
import com.example.fruitylicious.data.local.entity.BranchEntity
import com.example.fruitylicious.data.local.entity.UserEntity
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuditLogRow(
    val logId: String,
    val action: String,
    val description: String,
    val userName: String,
    val username: String,
    val tableAffected: String,
    val branchId: Int,
    val branchName: String,
    val timestamp: Long
)

data class AuditLogUiState(
    val logs: List<AuditLogRow> = emptyList(),
    val branches: List<BranchEntity> = emptyList(),
    val selectedBranchId: Int? = null,
    val isAdmin: Boolean = false,
    val isOnline: Boolean = false,
    val isRemoteAccessLocked: Boolean = false,
    val localBranchId: Int = 1,
    val userBranchId: String = "B1",
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val currentPage: Int = 0,
    val error: String? = null
)

@HiltViewModel
class AuditLogViewModel @Inject constructor(
    private val auditLogDao: AuditLogDao,
    private val userDao: UserDao,
    private val branchDao: BranchDao,
    private val reportRepository: ReportRepository,
    private val sessionManager: SessionManager,
    private val branchConfig: BranchConfig,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val localBranchId = branchConfig.branchId

    private val _uiState = MutableStateFlow(
        AuditLogUiState(
            isAdmin = isAdminUser(),
            selectedBranchId = localBranchId,
            localBranchId = localBranchId,
            userBranchId = "B$localBranchId"
        )
    )

    val uiState: StateFlow<AuditLogUiState> = _uiState.asStateFlow()

    private val PAGE_SIZE = 20
    private var lockoutJob: kotlinx.coroutines.Job? = null

    private var branches: List<BranchEntity> = emptyList()
    private val refreshTrigger = MutableStateFlow(0)

    init {
        observeBranches()
        observeNetwork()
        observeLocalData()
        loadLogs()
    }

    private fun observeLocalData() {
        viewModelScope.launch {
            combine(
                auditLogDao.observeAllAuditLogs(),
                userDao.observeUsers(),
                refreshTrigger
            ) { logs, users, _ ->
                buildLogRows(logs, users)
            }.collect { rows ->
                val state = _uiState.value
                // Use local data if:
                // 1. Local branch selected
                // 2. Offline
                // 3. Remote fetch failed (error != null)
                if (state.selectedBranchId == localBranchId || !state.isOnline || state.error != null) {
                    _uiState.update {
                        it.copy(
                            logs = rows,
                            isLoading = false,
                            hasMore = rows.size >= (state.currentPage + 1) * PAGE_SIZE,
                            error = state.error
                        )
                    }
                }
            }
        }
    }

    private fun buildLogRows(entities: List<AuditLogEntity>, users: List<UserEntity>): List<AuditLogRow> {
        val state = _uiState.value
        val userMap = users.associateBy { it.userId }
        
        return entities
            .filter { log ->
                state.selectedBranchId == null || log.branchId == state.selectedBranchId
            }
            .take(PAGE_SIZE + (state.currentPage * PAGE_SIZE))
            .map { log ->
                val user = userMap[log.userId]
                val branchName = branches.firstOrNull { it.branchId == log.branchId }?.branchName
                    ?: "Branch ${log.branchId}"

                AuditLogRow(
                    logId = log.logId,
                    action = extractActionTitle(log.action),
                    description = log.action,
                    userName = user?.name ?: "Unknown User",
                    username = user?.username ?: "unknown",
                    tableAffected = log.tableAffected,
                    branchId = log.branchId,
                    branchName = branchName,
                    timestamp = log.timestamp
                )
            }
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
                currentPage = 0,
                hasMore = true,
                error = null,
                logs = emptyList()
            )
        }

        refreshTrigger.value += 1
        loadLogs()
    }

    fun refresh() {
        _uiState.update {
            it.copy(
                isLoading = true,
                error = null,
                currentPage = 0,
                logs = emptyList()
            )
        }

        refreshTrigger.value += 1
        loadLogs()
    }

    private fun observeBranches() {
        viewModelScope.launch {
            branchDao.observeAllBranches().collectLatest { branchList ->
                branches = branchList

                _uiState.update {
                    it.copy(branches = branchList)
                }

                refreshTrigger.value += 1
                loadLogs()
            }
        }
    }

    private fun observeNetwork() {
        viewModelScope.launch {
            networkMonitor.observeNetworkStatus().collectLatest { online ->
                _uiState.update { current ->
                    val forcedBranch = if (!current.isAdmin) {
                        localBranchId
                    } else {
                        current.selectedBranchId
                    }

                    current.copy(
                        isOnline = online,
                        selectedBranchId = forcedBranch
                    )
                }

                loadLogs()
            }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMore) return
        
        if (state.isOnline && state.selectedBranchId != localBranchId) {
            loadRemoteLogsPage(state.currentPage + 1)
        } else {
            _uiState.update { 
                it.copy(currentPage = it.currentPage + 1)
            }
            refreshTrigger.value += 1
        }
    }

    private fun loadLogs() {
        val state = _uiState.value

        if (state.isOnline && state.selectedBranchId != localBranchId) {
            loadRemoteLogsPage(0)
        } else {
            // Local load is handled by observeLocalData
        }
    }

    private fun loadRemoteLogsPage(page: Int) {
        val state = _uiState.value
        viewModelScope.launch {
            if (page == 0) {
                _uiState.update { it.copy(isLoading = true, error = null, logs = emptyList()) }
            } else {
                _uiState.update { it.copy(isLoadingMore = true) }
            }

            val now = System.currentTimeMillis()
            val monthAgo = now - 30L * 24L * 60L * 60L * 1000L

            val result = reportRepository.getAuditLogsPage(
                branchId = state.selectedBranchId,
                from = monthAgo,
                to = now,
                page = page,
                size = PAGE_SIZE
            )

            result.fold(
                onSuccess = { pageResponse ->
                    val newRows = pageResponse.items.map { log ->
                        AuditLogRow(
                            logId = log.logId,
                            action = extractActionTitle(log.action),
                            description = log.action,
                            userName = log.userName,
                            username = "unknown",
                            tableAffected = log.tableAffected,
                            branchId = state.selectedBranchId ?: 0,
                            branchName = branches.firstOrNull { it.branchId == state.selectedBranchId }?.branchName ?: "Remote Branch",
                            timestamp = log.timestamp
                        )
                    }

                    _uiState.update {
                        it.copy(
                            logs = if (page == 0) newRows else it.logs + newRows,
                            isLoading = false,
                            isLoadingMore = false,
                            currentPage = page,
                            hasMore = pageResponse.hasNext,
                            error = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            error = error.message ?: "Failed to load remote audit logs.",
                            isRemoteAccessLocked = true
                        )
                    }
                    startLockoutTimer()
                    refreshTrigger.value += 1
                }
            )
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

    private fun extractActionTitle(action: String): String {
        return action
            .substringBefore(" ")
            .ifBlank { "activity" }
            .lowercase()
    }
}
