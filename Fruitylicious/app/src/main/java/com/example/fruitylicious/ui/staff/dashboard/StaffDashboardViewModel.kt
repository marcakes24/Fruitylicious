package com.example.fruitylicious.ui.staff.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitylicious.domain.usecase.auth.LogoutUseCase
import com.example.fruitylicious.domain.usecase.inventory.CheckLowStockUseCase
import com.example.fruitylicious.domain.usecase.staff.ClockInUseCase
import com.example.fruitylicious.domain.usecase.staff.ClockOutUseCase
import com.example.fruitylicious.data.repository.StaffLogRepository
import com.example.fruitylicious.util.BranchConfig
import com.example.fruitylicious.util.DateTimeUtil
import com.example.fruitylicious.util.NetworkMonitor
import com.example.fruitylicious.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StaffDashboardUiState(
    val branchName: String = "",
    val userName: String = "",
    val userId: Int = 0,
    val branchId: Int = 0,
    val isOnline: Boolean = false,
    val lastSyncAt: Long = 0L,
    val lastSyncSuccessful: Boolean = false,
    val lastSyncMessage: String = "Not synced yet.",
    val currentTimeText: String = "",
    val isClockedIn: Boolean = false,
    val isClockActionLoading: Boolean = false,
    val lowStockCount: Int = 0,
    val error: String? = null
)

@HiltViewModel
class StaffDashboardViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val branchConfig: BranchConfig,
    private val networkMonitor: NetworkMonitor,
    private val checkLowStockUseCase: CheckLowStockUseCase,
    private val clockInUseCase: ClockInUseCase,
    private val clockOutUseCase: ClockOutUseCase,
    private val staffLogRepository: StaffLogRepository,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    private val branchId: Int = sessionManager.getBranchId().takeIf { it > 0 } ?: branchConfig.branchId
    private val userId: Int = sessionManager.getUserId()

    private val _uiState = MutableStateFlow(
        StaffDashboardUiState(
            branchName = branchConfig.branchName,
            userName = sessionManager.getUserName(),
            userId = userId,
            branchId = branchId,
            lastSyncAt = sessionManager.getLastSyncAt(),
            lastSyncSuccessful = sessionManager.wasLastSyncSuccessful(),
            lastSyncMessage = sessionManager.getLastSyncMessage(),
            currentTimeText = DateTimeUtil.formatDateTime(System.currentTimeMillis())
        )
    )
    val uiState: StateFlow<StaffDashboardUiState> = _uiState.asStateFlow()

    init {
        observeNetwork()
        observeLowStock()
        startClock()
        refreshClockStatus()
    }

    private fun observeNetwork() {
        viewModelScope.launch {
            networkMonitor.observeNetworkStatus().collectLatest { isOnline ->
                _uiState.update {
                    it.copy(
                        isOnline = isOnline,
                        lastSyncAt = sessionManager.getLastSyncAt(),
                        lastSyncSuccessful = sessionManager.wasLastSyncSuccessful(),
                        lastSyncMessage = sessionManager.getLastSyncMessage()
                    )
                }
            }
        }
    }

    private fun observeLowStock() {
        viewModelScope.launch {
            checkLowStockUseCase(branchId).collectLatest { lowStock ->
                _uiState.update {
                    it.copy(lowStockCount = lowStock.size)
                }
            }
        }
    }

    private fun startClock() {
        viewModelScope.launch {
            while (true) {
                _uiState.update {
                    it.copy(currentTimeText = DateTimeUtil.formatDateTime(System.currentTimeMillis()))
                }
                delay(60_000L)
            }
        }
    }

    fun refreshClockStatus() {
        viewModelScope.launch {
            val openLog = staffLogRepository.getOpenStaffLog(userId, branchId)
            _uiState.update {
                it.copy(isClockedIn = openLog != null)
            }
        }
    }

    fun clockIn(image: String?) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isClockActionLoading = true, error = null)
            }

            val result = clockInUseCase(
                userId = userId,
                branchId = branchId,
                image = image
            )

            _uiState.update {
                it.copy(
                    isClockActionLoading = false,
                    isClockedIn = result.isSuccess,
                    error = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun clockOut() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isClockActionLoading = true, error = null)
            }

            val result = clockOutUseCase(
                userId = userId,
                branchId = branchId
            )

            _uiState.update {
                it.copy(
                    isClockActionLoading = false,
                    isClockedIn = if (result.isSuccess) false else it.isClockedIn,
                    error = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun logout() {
        logoutUseCase()
    }
}