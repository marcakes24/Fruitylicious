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
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val currentPage: Int = 0,
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

    private val PAGE_SIZE = 20

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
                currentPage = 0,
                hasMore = true,
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
                    val forcedBranchId = if (!current.isAdmin) {
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

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMore) return
        
        if (state.isAdmin && state.isOnline && state.selectedBranchId != localBranchId) {
            loadRemoteLogsPage(state.currentPage + 1)
            return
        }

        _uiState.update { it.copy(isLoadingMore = true) }

        viewModelScope.launch {
            val nextPage = state.currentPage + 1
            val offset = nextPage * PAGE_SIZE
            
            val newEntities = if (state.selectedBranchId == null) {
                staffLogDao.getStaffLogsPaged(PAGE_SIZE, offset)
            } else {
                staffLogDao.getStaffLogsByBranchPaged(state.selectedBranchId, PAGE_SIZE, offset)
            }
            
            if (newEntities.isEmpty()) {
                _uiState.update { it.copy(isLoadingMore = false, hasMore = false) }
                return@launch
            }
            
            val userMap = localUsers.associateBy { it.userId }
            val newRows = newEntities.map { log ->
                val user = userMap[log.userId]
                val branchName = branches.firstOrNull { it.branchId == log.branchId }?.branchName
                    ?: "Branch ${log.branchId}"

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

            _uiState.update { 
                it.copy(
                    logs = it.logs + newRows,
                    currentPage = nextPage,
                    isLoadingMore = false,
                    hasMore = newRows.size == PAGE_SIZE
                )
            }
        }
    }

    private fun loadLogs() {
        val state = _uiState.value

        when {
            state.selectedBranchId == localBranchId -> {
                loadLocalLogs(localBranchId)
            }

            !state.isAdmin -> {
                loadLocalLogs(localBranchId)
            }

            !state.isOnline -> {
                loadLocalLogs(localBranchId)
            }

            else -> {
                loadRemoteLogsPage(0)
            }
        }
    }

    private fun loadLocalLogs(branchId: Int) {
        val userMap = localUsers.associateBy { it.userId }
        val branchName = branches.firstOrNull { it.branchId == branchId }?.branchName
            ?: "Branch $branchId"

        val rows = localStaffLogs
            .filter { it.branchId == branchId }
            .take(PAGE_SIZE)
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
                hasMore = rows.size >= PAGE_SIZE,
                currentPage = 0,
                error = null
            )
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

            val result = reportRepository.getStaffLogsPage(
                branchId = state.selectedBranchId,
                from = monthAgo,
                to = now,
                page = page,
                size = PAGE_SIZE
            )

            result.fold(
                onSuccess = { pageResponse ->
                    val newRows = pageResponse.items.map { log ->
                        StaffLogRow(
                            logId = log.logId,
                            userId = log.userId,
                            staffName = log.userName,
                            username = "unknown",
                            branchId = log.branchId ?: state.selectedBranchId ?: 0,
                            branchName = log.branchName ?: branches.firstOrNull { it.branchId == log.branchId }?.branchName ?: "Remote Branch",
                            clockIn = log.clockIn,
                            clockOut = log.clockOut,
                            imagePath = ImageStorage.saveBase64Image(
                                context = context,
                                base64Value = log.image,
                                folder = "staff_logs"
                            )
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
                            error = error.message ?: "Failed to load remote staff logs."
                        )
                    }
                }
            )
        }
    }

    private fun loadRemoteBranch(branchId: Int) {
        loadRemoteLogsPage(0)
    }

    private fun loadRemoteAllBranches() {
        loadRemoteLogsPage(0)
    }

    private fun isAdminUser(): Boolean {
        return sessionManager.isAdmin()
    }
}
