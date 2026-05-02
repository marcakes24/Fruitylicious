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
    val localBranchId: Int = 1,
    val userBranchId: String = "B1",
    val isLoading: Boolean = true,
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

    private var localAuditLogs: List<AuditLogEntity> = emptyList()
    private var localUsers: List<UserEntity> = emptyList()
    private var branches: List<BranchEntity> = emptyList()

    init {
        observeBranches()
        observeNetwork()
        observeLocalUsers()
        observeLocalAuditLogs()
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

        loadLogs()
    }

    fun refresh() {
        _uiState.update {
            it.copy(
                isLoading = true,
                error = null
            )
        }

        loadLogs()
    }

    private fun observeBranches() {
        viewModelScope.launch {
            branchDao.observeAllBranches().collectLatest { branchList ->
                branches = branchList

                _uiState.update {
                    it.copy(branches = branchList)
                }

                loadLogs()
            }
        }
    }

    private fun observeNetwork() {
        viewModelScope.launch {
            networkMonitor.observeNetworkStatus().collectLatest { online ->
                _uiState.update { current ->
                    val forcedBranch = if (!online || !current.isAdmin) {
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

    private fun observeLocalUsers() {
        viewModelScope.launch {
            userDao.observeUsers().collectLatest { userList ->
                localUsers = userList
                loadLogs()
            }
        }
    }

    private fun observeLocalAuditLogs() {
        viewModelScope.launch {
            auditLogDao.observeAuditLogsByBranch(localBranchId).collectLatest { logs ->
                localAuditLogs = logs
                loadLogs()
            }
        }
    }

    private fun loadLogs() {
        val state = _uiState.value

        when {
            !state.isAdmin -> {
                loadLocalLogs(localBranchId)
            }

            !state.isOnline -> {
                loadLocalLogs(localBranchId)
            }

            state.selectedBranchId == null -> {
                loadRemoteAllBranches()
            }

            else -> {
                loadRemoteBranch(state.selectedBranchId)
            }
        }
    }

    private fun loadLocalLogs(branchId: Int) {
        val userMap = localUsers.associateBy { it.userId }
        val branchName = branches.firstOrNull { it.branchId == branchId }?.branchName ?: "Branch $branchId"

        val rows = localAuditLogs
            .filter { it.branchId == branchId }
            .map { log ->
                val user = userMap[log.userId]

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

        _uiState.update {
            it.copy(
                logs = rows,
                isLoading = false,
                error = null
            )
        }
    }

    private fun loadRemoteBranch(branchId: Int) {
        viewModelScope.launch {
            val from = 0L
            val to = System.currentTimeMillis()

            val result = reportRepository.getAuditLogsReport(
                branchId = branchId,
                from = from,
                to = to
            )

            result.fold(
                onSuccess = { report ->
                    val rows = report.logs.map { log ->
                        AuditLogRow(
                            logId = log.logId,
                            action = extractActionTitle(log.action),
                            description = log.action,
                            userName = log.userName,
                            username = "unknown",
                            tableAffected = log.tableAffected,
                            branchId = report.branchId ?: branchId,
                            branchName = report.branchName ?: "Branch $branchId",
                            timestamp = log.timestamp
                        )
                    }

                    _uiState.update {
                        it.copy(
                            logs = rows,
                            isLoading = false,
                            error = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to load remote audit logs."
                        )
                    }
                }
            )
        }
    }

    private fun loadRemoteAllBranches() {
        viewModelScope.launch {
            val from = 0L
            val to = System.currentTimeMillis()

            val allRows = mutableListOf<AuditLogRow>()
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
                val result = reportRepository.getAuditLogsReport(
                    branchId = branch.branchId,
                    from = from,
                    to = to
                )

                result.fold(
                    onSuccess = { report ->
                        val rows = report.logs.map { log ->
                            AuditLogRow(
                                logId = log.logId,
                                action = extractActionTitle(log.action),
                                description = log.action,
                                userName = log.userName,
                                username = "unknown",
                                tableAffected = log.tableAffected,
                                branchId = report.branchId ?: branch.branchId,
                                branchName = report.branchName ?: branch.branchName,
                                timestamp = log.timestamp
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
                    logs = allRows.sortedByDescending { row -> row.timestamp },
                    isLoading = false,
                    error = firstError
                )
            }
        }
    }

    private fun isAdminUser(): Boolean {
        val role = sessionManager.getRole()

        return role.equals("admin", ignoreCase = true) ||
                role.equals("owner", ignoreCase = true)
    }

    private fun extractActionTitle(action: String): String {
        return action
            .substringBefore(" ")
            .ifBlank { "activity" }
            .lowercase()
    }
}