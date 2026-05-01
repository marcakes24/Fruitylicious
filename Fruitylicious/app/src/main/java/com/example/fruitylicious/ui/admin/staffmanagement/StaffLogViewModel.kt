package com.example.fruitylicious.ui.admin.staffmanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitylicious.data.local.dao.StaffLogDao
import com.example.fruitylicious.data.local.entity.StaffLogEntity
import com.example.fruitylicious.data.local.dao.UserDao
import com.example.fruitylicious.data.local.entity.UserEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StaffLogRow(
    val logId: String,
    val userId: Int,
    val staffName: String,
    val username: String,
    val branchId: Int,
    val clockIn: Long,
    val clockOut: Long?
)

data class StaffLogUiState(
    val logs: List<StaffLogRow> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class StaffLogViewModel @Inject constructor(
    private val staffLogDao: StaffLogDao,
    private val userDao: UserDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(StaffLogUiState())
    val uiState: StateFlow<StaffLogUiState> = _uiState.asStateFlow()

    private var staffLogs: List<StaffLogEntity> = emptyList()
    private var users: List<UserEntity> = emptyList()

    init {
        observeStaffLogs()
        observeUsers()
    }

    private fun observeStaffLogs() {
        viewModelScope.launch {
            staffLogDao.observeAllStaffLogs().collectLatest { logs ->
                staffLogs = logs
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

        val rows = staffLogs.map { log ->
            val user = userMap[log.userId]

            StaffLogRow(
                logId = log.logId,
                userId = log.userId,
                staffName = user?.name ?: "Unknown Staff",
                username = user?.username ?: "unknown",
                branchId = log.branchId,
                clockIn = log.clockIn,
                clockOut = log.clockOut
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
}
