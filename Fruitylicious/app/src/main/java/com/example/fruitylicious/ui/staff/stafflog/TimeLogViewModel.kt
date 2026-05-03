package com.example.fruitylicious.ui.shared

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitylicious.data.local.dao.StaffLogDao
import com.example.fruitylicious.data.local.dao.UserDao
import com.example.fruitylicious.data.repository.StaffLogRepository
import com.example.fruitylicious.util.BranchConfig
import com.example.fruitylicious.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AttendanceState {
    CLOCKED_OUT,
    CLOCKED_IN
}

data class TimeLogUiState(
    val fullName: String = "",
    val username: String = "",
    val attendanceState: AttendanceState = AttendanceState.CLOCKED_OUT,
    val clockedInSince: String = "",
    val activeLogId: String? = null,
    val activeImagePath: String? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class TimeLogViewModel @Inject constructor(
    private val staffLogRepository: StaffLogRepository,
    private val staffLogDao: StaffLogDao,
    private val userDao: UserDao,
    private val sessionManager: SessionManager,
    private val branchConfig: BranchConfig
) : ViewModel() {

    private val userId = sessionManager.getUserId()

    private val branchId = sessionManager.getBranchId()
        .takeIf { it > 0 }
        ?: branchConfig.branchId

    private val _uiState = MutableStateFlow(
        TimeLogUiState(
            fullName = sessionManager.getUserName(),
            username = "@${sessionManager.getUserName()}",
            isLoading = true
        )
    )

    val uiState: StateFlow<TimeLogUiState> = _uiState.asStateFlow()

    init {
        observeUser()
        observeStaffLogs()
    }

    private fun observeUser() {
        viewModelScope.launch {
            userDao.observeUserById(userId).collectLatest { user ->
                _uiState.update {
                    it.copy(
                        fullName = user?.name ?: sessionManager.getUserName(),
                        username = user?.username?.let { username ->
                            if (username.startsWith("@")) {
                                username
                            } else {
                                "@$username"
                            }
                        } ?: "@${sessionManager.getUserName()}"
                    )
                }
            }
        }
    }

    private fun observeStaffLogs() {
        viewModelScope.launch {
            staffLogDao.observeStaffLogsByUser(userId).collectLatest { logs ->
                val activeLog = logs.firstOrNull { it.clockOut == null }

                _uiState.update {
                    if (activeLog != null) {
                        it.copy(
                            attendanceState = AttendanceState.CLOCKED_IN,
                            clockedInSince = formatTime(activeLog.clockIn),
                            activeLogId = activeLog.logId,
                            activeImagePath = activeLog.image,
                            isLoading = false,
                            error = null
                        )
                    } else {
                        it.copy(
                            attendanceState = AttendanceState.CLOCKED_OUT,
                            clockedInSince = "",
                            activeLogId = null,
                            activeImagePath = null,
                            isLoading = false,
                            error = null
                        )
                    }
                }
            }
        }
    }

    fun clockInWithImage(
        imagePath: String?
    ) {
        viewModelScope.launch {
            val result = staffLogRepository.clockIn(
                userId = userId,
                branchId = branchId,
                image = imagePath
            )

            result.onFailure { e ->
                _uiState.update { it.copy(error = e.message ?: "Failed to clock in") }
            }
        }
    }

    fun clockOut() {
        viewModelScope.launch {
            val result = staffLogRepository.clockOut(
                userId = userId,
                branchId = branchId
            )

            result.onFailure { e ->
                _uiState.update { it.copy(error = e.message ?: "Failed to clock out") }
            }
        }
    }

    fun setError(message: String) {
        _uiState.update {
            it.copy(error = message)
        }
    }

    private fun formatTime(timestamp: Long): String {
        return SimpleDateFormat("hh:mm a", Locale.getDefault())
            .format(Date(timestamp))
    }
}