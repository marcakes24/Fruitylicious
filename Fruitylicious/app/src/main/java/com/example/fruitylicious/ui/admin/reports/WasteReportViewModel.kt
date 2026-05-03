package com.example.fruitylicious.ui.admin.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitylicious.data.local.dao.BranchDao
import com.example.fruitylicious.data.local.dao.WasteItemRow
import com.example.fruitylicious.data.local.dao.WasteLogDao
import com.example.fruitylicious.data.local.dao.WasteReasonRow
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

data class WasteReportUiState(
    val totalWaste: Double = 0.0,
    val mostWasted: String = "—",
    val mostWastedQty: Double = 0.0,
    val reasonData: List<WasteReasonRow> = emptyList(),
    val wasteByItem: List<WasteItemRow> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class WasteReportViewModel @Inject constructor(
    private val wasteLogDao: WasteLogDao,
    private val branchDao: BranchDao,
    private val reportRepository: ReportRepository,
    private val networkMonitor: NetworkMonitor,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(WasteReportUiState())
    val uiState: StateFlow<WasteReportUiState> = _uiState.asStateFlow()

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
            val range = getCurrentWeekRange()

            when {
                branchId == localBranchId -> {
                    loadLocalReport(
                        branchId = localBranchId,
                        from = range.first,
                        to = range.second
                    )
                }

                !isAdmin -> {
                    loadLocalReport(
                        branchId = localBranchId,
                        from = range.first,
                        to = range.second
                    )
                }

                !isOnline -> {
                    loadLocalReport(
                        branchId = localBranchId,
                        from = range.first,
                        to = range.second
                    )
                }

                branchId == null -> {
                    loadRemoteAllBranchesReport(
                        from = range.first,
                        to = range.second
                    )
                }

                else -> {
                    loadRemoteBranchReport(
                        branchId = branchId,
                        from = range.first,
                        to = range.second
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
            val totalWaste = wasteLogDao.getTotalWasteQuantity(
                branchId = branchId,
                from = from,
                to = to
            )

            val reasonData = wasteLogDao.getWasteReasonReport(
                branchId = branchId,
                from = from,
                to = to
            )

            val wasteByItem = wasteLogDao.getWasteByItemReport(
                branchId = branchId,
                from = from,
                to = to
            )

            val topItem = wasteByItem.maxByOrNull {
                it.totalQuantity
            }

            _uiState.update {
                it.copy(
                    totalWaste = totalWaste,
                    mostWasted = topItem?.ingredientName ?: "—",
                    mostWastedQty = topItem?.totalQuantity ?: 0.0,
                    reasonData = reasonData,
                    wasteByItem = wasteByItem,
                    isLoading = false,
                    error = null
                )
            }
        } catch (exception: Exception) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = exception.message ?: "Failed to load local waste report."
                )
            }
        }
    }

    private suspend fun loadRemoteBranchReport(
        branchId: Int,
        from: Long,
        to: Long
    ) {
        val result = reportRepository.getWasteReport(
            branchId = branchId,
            from = from,
            to = to
        )

        result.fold(
            onSuccess = { reportDto ->
                applyRemoteItems(
                    totalWaste = reportDto.totalWasteQuantity,
                    items = reportDto.items
                )
            },
            onFailure = { exception ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load remote waste report."
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

            val branchList = branches.ifEmpty {
                emptyList()
            }

            if (branchList.isEmpty()) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "No branches found."
                    )
                }
                return
            }

            val allItems = mutableListOf<com.example.fruitylicious.data.remote.dto.WasteReportItemDto>()
            var totalWaste = 0.0
            var firstError: String? = null

            for (branch in branchList) {
                val result = reportRepository.getWasteReport(
                    branchId = branch.branchId,
                    from = from,
                    to = to
                )

                result.fold(
                    onSuccess = { report ->
                        totalWaste += report.totalWasteQuantity
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
                totalWaste = totalWaste,
                items = allItems,
                warning = firstError
            )
        } catch (exception: Exception) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = exception.message ?: "Failed to load combined waste report."
                )
            }
        }
    }

    private fun applyRemoteItems(
        totalWaste: Double,
        items: List<com.example.fruitylicious.data.remote.dto.WasteReportItemDto>,
        warning: String? = null
    ) {
        val reasonRows = items
            .groupBy { it.reason.ifBlank { "Unspecified" } }
            .map { (reason, groupedItems) ->
                WasteReasonRow(
                    reason = reason,
                    count = groupedItems.size
                )
            }
            .sortedByDescending { it.count }

        val itemRows = items
            .groupBy { it.ingredientName }
            .map { (ingredientName, groupedItems) ->
                WasteItemRow(
                    ingredientName = ingredientName,
                    totalQuantity = groupedItems.sumOf { it.quantity }
                )
            }
            .sortedByDescending { it.totalQuantity }

        val topItem = itemRows.maxByOrNull {
            it.totalQuantity
        }

        _uiState.update {
            it.copy(
                totalWaste = totalWaste,
                mostWasted = topItem?.ingredientName ?: "—",
                mostWastedQty = topItem?.totalQuantity ?: 0.0,
                reasonData = reasonRows,
                wasteByItem = itemRows,
                isLoading = false,
                error = warning
            )
        }
    }

    private fun getCurrentWeekRange(): Pair<Long, Long> {
        val start = Calendar.getInstance()

        while (start.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            start.add(Calendar.DAY_OF_YEAR, -1)
        }

        start.set(Calendar.HOUR_OF_DAY, 0)
        start.set(Calendar.MINUTE, 0)
        start.set(Calendar.SECOND, 0)
        start.set(Calendar.MILLISECOND, 0)

        val end = System.currentTimeMillis()

        return start.timeInMillis to end
    }

    private fun isAdminUser(): Boolean {
        val role = sessionManager.getRole()

        return role.equals("admin", ignoreCase = true) ||
                role.equals("owner", ignoreCase = true)
    }
}