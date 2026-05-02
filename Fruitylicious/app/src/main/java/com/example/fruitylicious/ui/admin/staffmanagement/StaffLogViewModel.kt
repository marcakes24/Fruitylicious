package com.example.fruitylicious.ui.admin.staffmanagement
import android.content.Context
import com.example.fruitylicious.data.repository.ReportRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitylicious.data.local.dao.BranchDao
import com.example.fruitylicious.data.local.dao.StaffLogDao
import com.example.fruitylicious.data.local.dao.UserDao
import com.example.fruitylicious.data.local.entity.BranchEntity
import com.example.fruitylicious.data.local.entity.StaffLogEntity
import com.example.fruitylicious.data.local.entity.UserEntity
import com.example.fruitylicious.util.BranchConfig
import com.example.fruitylicious.util.ImageStorage
import com.example.fruitylicious.util.NetworkMonitor
import com.example.fruitylicious.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StaffLogRow(
    val logId: String,
    val userId: Int,
    val staffName: String,
    val username: String,
    val branchId: Int,
    val branchName: String,
    val clockIn: Long,
    val clockOut: Long?,
    val imagePath: String? = null
)

data class StaffLogUiState(
    val logs: List<StaffLogRow> = emptyList(),
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
class StaffLogViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val staffLogDao: StaffLogDao,
    private val userDao: UserDao,
    private val branchDao: BranchDao,
    private val reportRepository: ReportRepository,
    private val sessionManager: SessionManager,
    private val branchConfig: BranchConfig,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val localBranchId = branchConfig.branchId

    private val _uiState = MutableStateFlow(
        StaffLogUiState(
            isAdmin = isAdminUser(),
            selectedBranchId = localBranchId,
            localBranchId = localBranchId,
            userBranchId = "B$localBranchId"
        )
    )

    val uiState: StateFlow<StaffLogUiState> = _uiState.asStateFlow()

    private var localStaffLogs: List<StaffLogEntity> = emptyList()
    private var localUsers: List<UserEntity> = emptyList()
    private var branches: List<BranchEntity> = emptyList()

    init {
        observeBranches()
        observeNetwork()
        observeLocalUsers()
        observeLocalStaffLogs()
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
                    val forcedBranchId = if (!online || !current.isAdmin) {
                        localBranchId
                    } else {
                        current.selectedBranchId
                    }

                    current.copy(
                        isOnline = online,
                        selectedBranchId = forcedBranchId
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

    private fun observeLocalStaffLogs() {
        viewModelScope.launch {
            staffLogDao.observeStaffLogsByBranch(localBranchId).collectLatest { logs ->
                localStaffLogs = logs
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
        val branchName = branches.firstOrNull { it.branchId == branchId }?.branchName
            ?: "Branch $branchId"

        val rows = localStaffLogs
            .filter { it.branchId == branchId }
            .map { log ->
                val user = userMap[log.userId]

                StaffLogRow(
                    logId = log.logId,
                    userId = log.userId,
                    staffName = user?.name ?: "Unknown Staff",
                    username = user?.username ?: "unknown",
                    branchId = log.branchId,
                    branchName = branchName,
                    clockIn = log.clockIn,
                    clockOut = log.clockOut,
                    imagePath = log.image
                )
            }
            .sortedByDescending { it.clockIn }

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

            val result = reportRepository.getStaffLogsReport(
                branchId = branchId,
                from = from,
                to = to
            )

            result.fold(
                onSuccess = { report ->
                    val rows = report.logs.map { log ->
                        StaffLogRow(
                            logId = log.logId,
                            userId = log.userId,
                            staffName = log.userName,
                            username = "unknown",
                            branchId = report.branchId ?: branchId,
                            branchName = report.branchName ?: "Branch $branchId",
                            clockIn = log.clockIn,
                            clockOut = log.clockOut,
                            imagePath = ImageStorage.saveBase64Image(
                                context = context,
                                base64Value = log.image,
                                folder = "staff_logs"
                            )
                        )
                    }.sortedByDescending { it.clockIn }

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
                            logs = emptyList(),
                            isLoading = false,
                            error = error.message ?: "Failed to load remote staff logs."
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

            val allRows = mutableListOf<StaffLogRow>()
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
                val result = reportRepository.getStaffLogsReport(
                    branchId = branch.branchId,
                    from = from,
                    to = to
                )

                result.fold(
                    onSuccess = { report ->
                        val rows = report.logs.map { log ->
                            StaffLogRow(
                                logId = log.logId,
                                userId = log.userId,
                                staffName = log.userName,
                                username = "unknown",
                                branchId = report.branchId ?: branch.branchId,
                                branchName = report.branchName ?: branch.branchName,
                                clockIn = log.clockIn,
                                clockOut = log.clockOut,
                                imagePath = ImageStorage.saveBase64Image(
                                    context = context,
                                    base64Value = log.image,
                                    folder = "staff_logs"
                                )
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
                    logs = allRows.sortedByDescending { row -> row.clockIn },
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
}
