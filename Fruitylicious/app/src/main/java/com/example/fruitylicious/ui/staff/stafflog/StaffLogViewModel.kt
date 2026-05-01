package com.example.fruitylicious.ui.staff.stafflog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitylicious.data.local.dao.AuditLogDao
import com.example.fruitylicious.data.local.dao.StaffLogDao
import com.example.fruitylicious.data.local.entity.AuditLogEntity
import com.example.fruitylicious.data.local.entity.StaffLogEntity
import com.example.fruitylicious.util.BranchConfig
import com.example.fruitylicious.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StaffTimeLogRow(
    val logId: String,
    val userId: Int,
    val branchId: Int,
    val clockIn: Long,
    val clockOut: Long?
)

data class StaffTimeLogUiState(
    val staffName: String = "",
    val username: String = "",
    val branchId: Int = 1,
    val branchName: String = "",
    val logs: List<StaffTimeLogRow> = emptyList(),
    val hasActiveLog: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class StaffTimeLogViewModel @Inject constructor(
    private val staffLogDao: StaffLogDao,
    private val auditLogDao: AuditLogDao,
    private val sessionManager: SessionManager,
    private val branchConfig: BranchConfig
) : ViewModel() {

    private val userId = sessionManager.getUserId()
    private val staffName = sessionManager.getUserName()
    private val username = sessionManager.getUsername()
    private val branchId = branchConfig.branchId
    private val branchName = branchConfig.branchName

    private val _uiState = MutableStateFlow(
        StaffTimeLogUiState(
            staffName = staffName,
            username = username,
            branchId = branchId,
            branchName = branchName
        )
    )

    val uiState: StateFlow<StaffTimeLogUiState> = _uiState.asStateFlow()

    init {
        observeLogs()
    }

    private fun observeLogs() {
        viewModelScope.launch {
            staffLogDao.observeLogsByUser(userId).collectLatest { logs ->
                val rows = logs.map { log ->
                    StaffTimeLogRow(
                        logId = log.logId,
                        userId = log.userId,
                        branchId = log.branchId,
                        clockIn = log.clockIn,
                        clockOut = log.clockOut
                    )
                }

                _uiState.update {
                    it.copy(
                        logs = rows,
                        hasActiveLog = rows.any { row -> row.clockOut == null },
                        isLoading = false,
                        error = null
                    )
                }
            }
        }
    }

    fun clockIn() {
        viewModelScope.launch {
            val activeLog = staffLogDao.getActiveLogForUser(userId)

            if (activeLog != null) {
                _uiState.update {
                    it.copy(
                        error = "You are already clocked in.",
                        successMessage = null
                    )
                }
                return@launch
            }

            val now = System.currentTimeMillis()
            val logId = UUID.randomUUID().toString()

            staffLogDao.upsertStaffLog(
                StaffLogEntity(
                    logId = logId,
                    userId = userId,
                    branchId = branchId,
                    clockIn = now,
                    clockOut = null,
                    image = null,
                    lastModified = now,
                    isSynced = false,
                    syncedAt = null
                )
            )

            auditLogDao.upsertAuditLog(
                AuditLogEntity(
                    logId = UUID.randomUUID().toString(),
                    userId = userId,
                    branchId = branchId,
                    action = "Clocked in.",
                    tableAffected = "staff_logs",
                    timestamp = now,
                    lastModified = now,
                    isSynced = false,
                    syncedAt = null
                )
            )

            _uiState.update {
                it.copy(
                    successMessage = "Clocked in successfully.",
                    error = null
                )
            }
        }
    }

    fun clockOut() {
        viewModelScope.launch {
            val activeLog = staffLogDao.getActiveLogForUser(userId)

            if (activeLog == null) {
                _uiState.update {
                    it.copy(
                        error = "No active clock-in found.",
                        successMessage = null
                    )
                }
                return@launch
            }

            val now = System.currentTimeMillis()

            staffLogDao.upsertStaffLog(
                activeLog.copy(
                    clockOut = now,
                    lastModified = now,
                    isSynced = false,
                    syncedAt = null
                )
            )

            auditLogDao.upsertAuditLog(
                AuditLogEntity(
                    logId = UUID.randomUUID().toString(),
                    userId = userId,
                    branchId = branchId,
                    action = "Clocked out.",
                    tableAffected = "staff_logs",
                    timestamp = now,
                    lastModified = now,
                    isSynced = false,
                    syncedAt = null
                )
            )

            _uiState.update {
                it.copy(
                    successMessage = "Clocked out successfully.",
                    error = null
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.update {
            it.copy(
                error = null,
                successMessage = null
            )
        }
    }
}