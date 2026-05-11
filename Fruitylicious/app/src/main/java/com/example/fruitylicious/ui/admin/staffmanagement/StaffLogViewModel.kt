package com.example.fruitylicious.ui.admin.staffmanagement

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitylicious.data.local.dao.BranchDao
import com.example.fruitylicious.data.local.dao.StaffLogDao
import com.example.fruitylicious.data.local.dao.UserDao
import com.example.fruitylicious.data.local.entity.BranchEntity
import com.example.fruitylicious.data.local.entity.StaffLogEntity
import com.example.fruitylicious.data.local.entity.UserEntity
import com.example.fruitylicious.data.repository.ReportRepository
import com.example.fruitylicious.util.BranchConfig
import com.example.fruitylicious.util.ImageStorage
import com.example.fruitylicious.util.NetworkMonitor
import com.example.fruitylicious.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    val localBranchId: Int = 0,
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

    private var branches: List<BranchEntity> = emptyList()
    private val refreshTrigger = MutableStateFlow(0)

    init {
        observeBranches()
        observeNetwork()
        observeLocalData()

        // Reactive history loading: only one central point for loading
        viewModelScope.launch {
            combine(
                _uiState.map { it.isOnline }.distinctUntilChanged(),
                _uiState.map { it.selectedBranchId }.distinctUntilChanged(),
                refreshTrigger
            ) { online, branchId, trigger ->
                Triple(online, branchId, trigger)
            }.collectLatest { (online, branchId, _) ->
                // ONLY load if online AND selected branch is NOT the local branch
                if (online && branchId != localBranchId) {
                    delay(300) // Debounce branch selection
                    loadLogs()
                } else {
                    // For local branch, observeLocalData handles everything
                }
            }
        }
    }

    private fun observeLocalData() {
        viewModelScope.launch {
            combine(
                staffLogDao.observeAllStaffLogs(),
                userDao.observeUsers(),
                refreshTrigger
            ) { logs, users, _ ->
                logs to users
            }
                .flowOn(Dispatchers.Default)
                .map { (logs, users) ->
                    buildLogRows(logs, users)
                }
                .collectLatest { rows ->
                    _uiState.update { state ->
                        // Always allow local data to be the baseline
                        // This ensures synced images for other branches are visible if remote fetch hasn't finished
                        val isRemoteFetchInProgress = state.isOnline && state.selectedBranchId != localBranchId && state.isLoading

                        if (state.selectedBranchId == localBranchId || !state.isOnline || state.error != null || (state.logs.isEmpty() && !isRemoteFetchInProgress)) {
                            state.copy(
                                logs = rows,
                                isLoading = if (state.selectedBranchId != localBranchId && state.isOnline && state.error == null) state.isLoading else false,
                                hasMore = if (state.selectedBranchId == localBranchId) rows.size >= (state.currentPage + 1) * PAGE_SIZE else state.hasMore
                            )
                        } else {
                            state
                        }
                    }
                }
        }
    }

    private fun buildLogRows(entities: List<StaffLogEntity>, users: List<UserEntity>): List<StaffLogRow> {
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
    }

    fun onBranchSelected(branchId: Int?) {
        val state = _uiState.value

        val finalBranchId = if (state.isAdmin && state.isOnline) {
            branchId
        } else {
            localBranchId
        }

        if (state.selectedBranchId == finalBranchId) return

        _uiState.update {
            it.copy(
                selectedBranchId = finalBranchId,
                isLoading = true,
                currentPage = 0,
                hasMore = true,
                error = null
            )
        }

        refreshTrigger.value += 1
    }

    fun refresh() {
        _uiState.update {
            it.copy(
                isLoading = true,
                error = null,
                currentPage = 0
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
                _uiState.update { it.copy(isLoading = true, error = null) }
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
                    val newRows = withContext(Dispatchers.IO) {
                        // Fetch local logs for this branch to merge existing images
                        val localLogs = staffLogDao.getStaffLogsByBranch(state.selectedBranchId ?: 0)
                        val localImageMap = localLogs.associate { it.logId to it.image }

                        pageResponse.items.map { log ->
                            val localImagePath = localImageMap[log.logId]
                            val hasLocalImage = localImagePath != null && ImageStorage.getImageFile(context, localImagePath).exists()

                            // Only save if we don't already have it locally
                            val finalImagePath = if (hasLocalImage) {
                                localImagePath
                            } else {
                                ImageStorage.saveBase64Image(
                                    context = context,
                                    base64Value = log.image,
                                    folder = "staff_logs"
                                )
                            }

                            StaffLogRow(
                                logId = log.logId,
                                userId = log.userId,
                                staffName = log.userName,
                                username = "unknown",
                                branchId = log.branchId ?: state.selectedBranchId ?: 0,
                                branchName = log.branchName ?: branches.firstOrNull { it.branchId == log.branchId }?.branchName ?: "Remote Branch",
                                clockIn = log.clockIn,
                                clockOut = log.clockOut,
                                imagePath = finalImagePath
                            )
                        }
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
                            isLoadingMore = false
                        )
                    }
                    refreshTrigger.value += 1
                }
            )
        }
    }

    private fun isAdminUser(): Boolean {
        return sessionManager.isAdmin()
    }
}
