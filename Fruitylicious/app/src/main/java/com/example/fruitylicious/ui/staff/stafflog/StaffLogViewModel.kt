package com.example.fruitylicious.ui.staff.stafflog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitylicious.data.local.entity.StaffLogEntity
import com.example.fruitylicious.data.repository.StaffLogRepository
import com.example.fruitylicious.util.BranchConfig
import com.example.fruitylicious.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StaffLogUiState(
    val logs: List<StaffLogEntity> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class StaffLogViewModel @Inject constructor(
    private val staffLogRepository: StaffLogRepository,
    sessionManager: SessionManager,
    branchConfig: BranchConfig
) : ViewModel() {

    private val branchId = sessionManager.getBranchId().takeIf { it > 0 } ?: branchConfig.branchId

    private val _uiState = MutableStateFlow(StaffLogUiState())
    val uiState: StateFlow<StaffLogUiState> = _uiState.asStateFlow()

    init {
        observeLogs()
    }

    private fun observeLogs() {
        viewModelScope.launch {
            staffLogRepository.observeStaffLogs(branchId).collectLatest { logs ->
                _uiState.update {
                    it.copy(
                        logs = logs,
                        isLoading = false
                    )
                }
            }
        }
    }
}