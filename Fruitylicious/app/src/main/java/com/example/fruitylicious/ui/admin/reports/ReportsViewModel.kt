package com.example.fruitylicious.ui.admin.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitylicious.data.remote.dto.InventoryReportDto
import com.example.fruitylicious.data.remote.dto.RestockReportDto
import com.example.fruitylicious.data.remote.dto.SalesReportDto
import com.example.fruitylicious.data.remote.dto.TransactionReportDto
import com.example.fruitylicious.data.remote.dto.WasteReportDto
import com.example.fruitylicious.data.repository.ReportRepository
import com.example.fruitylicious.util.BranchConfig
import com.example.fruitylicious.util.DateTimeUtil
import com.example.fruitylicious.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReportsUiState(
    val branchId: Int = 0,
    val branchName: String = "",
    val from: Long = DateTimeUtil.startOfToday(),
    val to: Long = DateTimeUtil.endOfToday(),
    val salesReport: SalesReportDto? = null,
    val inventoryReport: InventoryReportDto? = null,
    val wasteReport: WasteReportDto? = null,
    val restockReport: RestockReportDto? = null,
    val transactionReport: TransactionReportDto? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val reportRepository: ReportRepository,
    sessionManager: SessionManager,
    branchConfig: BranchConfig
) : ViewModel() {

    private val branchId: Int =
        sessionManager.getBranchId().takeIf { it > 0 } ?: branchConfig.branchId

    private val _uiState = MutableStateFlow(
        ReportsUiState(
            branchId = branchId,
            branchName = branchConfig.branchName
        )
    )
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    fun setDateRange(from: Long, to: Long) {
        _uiState.update {
            it.copy(
                from = from,
                to = to,
                error = null
            )
        }
    }

    fun loadSalesReport(combined: Boolean = false) {
        viewModelScope.launch {
            val state = _uiState.value
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = if (combined) {
                reportRepository.getCombinedSalesReport(state.from, state.to)
            } else {
                reportRepository.getSalesReport(state.branchId, state.from, state.to)
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    salesReport = result.getOrNull(),
                    error = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun loadInventoryReport() {
        viewModelScope.launch {
            val state = _uiState.value
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = reportRepository.getInventoryReport(state.branchId)

            _uiState.update {
                it.copy(
                    isLoading = false,
                    inventoryReport = result.getOrNull(),
                    error = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun loadWasteReport() {
        viewModelScope.launch {
            val state = _uiState.value
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = reportRepository.getWasteReport(state.branchId, state.from, state.to)

            _uiState.update {
                it.copy(
                    isLoading = false,
                    wasteReport = result.getOrNull(),
                    error = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun loadRestockReport() {
        viewModelScope.launch {
            val state = _uiState.value
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = reportRepository.getRestockReport(state.branchId, state.from, state.to)

            _uiState.update {
                it.copy(
                    isLoading = false,
                    restockReport = result.getOrNull(),
                    error = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun loadTransactionReport() {
        viewModelScope.launch {
            val state = _uiState.value
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = reportRepository.getTransactionReport(state.branchId, state.from, state.to)

            _uiState.update {
                it.copy(
                    isLoading = false,
                    transactionReport = result.getOrNull(),
                    error = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun clearError() {
        _uiState.update {
            it.copy(error = null)
        }
    }
}