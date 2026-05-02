package com.example.fruitylicious.ui.admin.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitylicious.data.local.dao.BranchDao
import com.example.fruitylicious.data.local.dao.RestockFrequencyRow
import com.example.fruitylicious.data.local.dao.RestockLogDao
import com.example.fruitylicious.data.repository.ReportRepository
import com.example.fruitylicious.util.NetworkMonitor
import com.example.fruitylicious.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RestockReportUiState(
    val totalToday: Double = 0.0,
    val frequencyItems: List<RestockFrequencyRow> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class RestockReportViewModel @Inject constructor(
    private val restockLogDao: RestockLogDao,
    private val branchDao: BranchDao,
    private val reportRepository: ReportRepository,
    private val networkMonitor: NetworkMonitor,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(RestockReportUiState())
    val uiState: StateFlow<RestockReportUiState> = _uiState.asStateFlow()

    fun loadReport(
        branchId: Int?
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null
                )
            }

            val localBranchId = sessionManager.getBranchId()
            val isOnline = networkMonitor.isOnline()
            val isAdmin = isAdminUser()

            val todayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val now = System.currentTimeMillis()

            when {
                !isAdmin -> {
                    loadLocalReport(
                        branchId = localBranchId,
                        from = todayStart,
                        to = now
                    )
                }

                !isOnline -> {
                    loadLocalReport(
                        branchId = localBranchId,
                        from = todayStart,
                        to = now
                    )
                }

                branchId == null -> {
                    loadRemoteAllBranchesReport(
                        from = todayStart,
                        to = now
                    )
                }

                else -> {
                    loadRemoteBranchReport(
                        branchId = branchId,
                        from = todayStart,
                        to = now
                    )
                }
            }
        }
    }

    private suspend fun loadLocalReport(
        branchId: Int,
        from: Long,
        to: Long
    ) {
        try {
            val totalToday = restockLogDao.getTotalRestockedUnits(
                branchId = branchId,
                from = from,
                to = to
            )

            val frequencyItems = restockLogDao.getRestockFrequencyReport(
                branchId = branchId
            )

            _uiState.update {
                it.copy(
                    totalToday = totalToday,
                    frequencyItems = frequencyItems,
                    isLoading = false,
                    error = null
                )
            }
        } catch (exception: Exception) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = exception.message ?: "Failed to load local restock report."
                )
            }
        }
    }

    private suspend fun loadRemoteBranchReport(
        branchId: Int,
        from: Long,
        to: Long
    ) {
        val result = reportRepository.getRestockReport(
            branchId = branchId,
            from = from,
            to = to
        )

        result.fold(
            onSuccess = { reportDto ->
                applyRemoteItems(
                    totalRestock = reportDto.totalRestockQuantity,
                    items = reportDto.items
                )
            },
            onFailure = { exception ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load remote restock report."
                    )
                }
            }
        )
    }

    private suspend fun loadRemoteAllBranchesReport(
        from: Long,
        to: Long
    ) {
        try {
            val branches = branchDao.getAllBranches()

            if (branches.isEmpty()) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "No branches found."
                    )
                }
                return
            }

            val allItems = mutableListOf<com.example.fruitylicious.data.remote.dto.RestockReportItemDto>()
            var totalRestock = 0.0
            var firstError: String? = null

            for (branch in branches) {
                val result = reportRepository.getRestockReport(
                    branchId = branch.branchId,
                    from = from,
                    to = to
                )

                result.fold(
                    onSuccess = { report ->
                        totalRestock += report.totalRestockQuantity
                        allItems.addAll(report.items)
                    },
                    onFailure = { exception ->
                        if (firstError == null) {
                            firstError = exception.message
                        }
                    }
                )
            }

            applyRemoteItems(
                totalRestock = totalRestock,
                items = allItems,
                warning = firstError
            )
        } catch (exception: Exception) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = exception.message ?: "Failed to load combined restock report."
                )
            }
        }
    }

    private fun applyRemoteItems(
        totalRestock: Double,
        items: List<com.example.fruitylicious.data.remote.dto.RestockReportItemDto>,
        warning: String? = null
    ) {
        val frequencyRows = items
            .groupBy { it.ingredientName }
            .map { (ingredientName, groupedItems) ->
                val totalQuantity = groupedItems.sumOf { it.quantityAdded }
                val count = groupedItems.size

                RestockFrequencyRow(
                    ingredientName = ingredientName,
                    restockCount = count,
                    avgUnits = if (count > 0) {
                        totalQuantity / count.toDouble()
                    } else {
                        0.0
                    }
                )
            }
            .sortedByDescending { it.restockCount }

        _uiState.update {
            it.copy(
                totalToday = totalRestock,
                frequencyItems = frequencyRows,
                isLoading = false,
                error = warning
            )
        }
    }

    private fun isAdminUser(): Boolean {
        val role = sessionManager.getRole()

        return role.equals("admin", ignoreCase = true) ||
                role.equals("owner", ignoreCase = true)
    }
}