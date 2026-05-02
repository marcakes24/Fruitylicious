package com.example.fruitylicious.ui.staff.stafflog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

data class StaffLogEntry(
    val logId: String,
    val name: String,
    val username: String,
    val branch: String,
    val clockIn: String,
    val clockOut: String,
    val date: String,
    val clockInMillis: Long
)

data class StaffLogUiState(
    val logs: List<StaffLogEntry> = emptyList(),
    val userName: String = "",
    val username: String = "",
    val branchName: String = "",
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class StaffLogTimeInViewModel @Inject constructor(
    private val staffLogRepository: StaffLogRepository,
    private val sessionManager: SessionManager,
    private val branchConfig: BranchConfig
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        StaffLogUiState(
            userName = sessionManager.getUserName().ifBlank { "Staff" },
            username = sessionManager.getUsername().ifBlank { "staff" },
            branchName = branchConfig.branchName
        )
    )

    val uiState: StateFlow<StaffLogUiState> = _uiState.asStateFlow()

    init {
        observeStaffLogs()
    }

    private fun observeStaffLogs() {
        viewModelScope.launch {
            val userId = sessionManager.getUserId()
            val branchId = sessionManager.getBranchId()

            staffLogRepository.observeStaffLogsByUser(userId)
                .collectLatest { logs ->
                    val rows = logs
                        .filter { it.branchId == branchId }
                        .sortedByDescending { it.clockIn }
                        .map { log ->
                            StaffLogEntry(
                                logId = log.logId,
                                name = sessionManager.getUserName().ifBlank { "Staff" },
                                username = "@${sessionManager.getUsername().ifBlank { "staff" }}",
                                branch = "B${log.branchId}",
                                clockIn = formatTime(log.clockIn),
                                clockOut = log.clockOut?.let { formatTime(it) } ?: "Active",
                                date = formatDate(log.clockIn),
                                clockInMillis = log.clockIn
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
    }

    private fun formatDate(timestamp: Long): String {
        return SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(timestamp))
    }

    private fun formatTime(timestamp: Long): String {
        return SimpleDateFormat("hh:mm a", Locale.US).format(Date(timestamp))
    }
}