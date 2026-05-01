package com.example.fruitylicious.ui.admin.system

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitylicious.data.local.dao.AuditLogDao
import com.example.fruitylicious.data.local.dao.UserDao
import com.example.fruitylicious.data.local.entity.AuditLogEntity
import com.example.fruitylicious.data.local.entity.UserEntity
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
    val timestamp: Long
)

data class AuditLogUiState(
    val logs: List<AuditLogRow> = emptyList(),
    val isAdmin: Boolean = false,
    val userBranchId: String = "B1",
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class AuditLogViewModel @Inject constructor(
    private val auditLogDao: AuditLogDao,
    private val userDao: UserDao,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AuditLogUiState(
            isAdmin = sessionManager.getRole()?.equals("admin", ignoreCase = true) == true,
            userBranchId = "B${sessionManager.getBranchId()}"
        )
    )
    val uiState: StateFlow<AuditLogUiState> = _uiState.asStateFlow()

    private var auditLogs: List<AuditLogEntity> = emptyList()
    private var users: List<UserEntity> = emptyList()

    init {
        observeAuditLogs()
        observeUsers()
    }

    private fun observeAuditLogs() {
        viewModelScope.launch {
            auditLogDao.observeAllAuditLogs().collectLatest { logs ->
                auditLogs = logs
                rebuildRows()
            }
        }
    }

    private fun observeUsers() {
        viewModelScope.launch {
            userDao.observeUsers().collectLatest { userList ->
                users = userList
                rebuildRows()
            }
        }
    }

    private fun rebuildRows() {
        val userMap = users.associateBy { it.userId }

        val rows = auditLogs.map { log ->
            val user = userMap[log.userId]

            AuditLogRow(
                logId = log.logId,
                action = extractActionTitle(log.action),
                description = log.action,
                userName = user?.name ?: "Unknown User",
                username = user?.username ?: "unknown",
                tableAffected = log.tableAffected,
                branchId = log.branchId,
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

    private fun extractActionTitle(action: String): String {
        return action
            .substringBefore(" ")
            .ifBlank { "activity" }
            .lowercase()
    }
}