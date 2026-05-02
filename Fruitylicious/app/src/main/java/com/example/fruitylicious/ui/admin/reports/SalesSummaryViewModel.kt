package com.example.fruitylicious.ui.admin.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitylicious.data.local.dao.TransactionDao
import com.example.fruitylicious.data.local.entity.BranchEntity
import com.example.fruitylicious.data.repository.ReportRepository
import com.example.fruitylicious.util.BranchConfig
import com.example.fruitylicious.util.NetworkMonitor
import com.example.fruitylicious.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.fruitylicious.data.local.dao.BranchDao

data class SalesSummaryUiState(
    val selectedBranchId: Int? = 1,
    val isAdmin: Boolean = false,
    val localBranchId: Int = 1,
    val branches: List<BranchEntity> = emptyList(),
    val isOnline: Boolean = false,
    val canAccessCrossBranch: Boolean = false,
    val todaySales: Double = 0.0,
    val yesterdaySales: Double = 0.0,
    val weekData: List<Double> = List(7) { 0.0 },
    val monthTransactions: Int = 0,
    val monthTotal: Double = 0.0,
    val todayCash: Double = 0.0,
    val todayGcash: Double = 0.0,
    val monthLineData: List<Double> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SalesSummaryViewModel @Inject constructor(
    private val transactionDao: TransactionDao,
    private val sessionManager: SessionManager,
    private val branchConfig: BranchConfig,
    private val branchDao: BranchDao,
    private val reportRepository: ReportRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val localBranchId = branchConfig.branchId

    private val _uiState = MutableStateFlow(
        SalesSummaryUiState(
            isAdmin = isAdminUser(),
            localBranchId = localBranchId,
            selectedBranchId = localBranchId
        )
    )

    val uiState: StateFlow<SalesSummaryUiState> = _uiState.asStateFlow()

    init {
        observeBranches()
        observeNetworkStatus()
        loadSummary()
    }

    private fun observeBranches() {
        viewModelScope.launch {
            branchDao.observeAllBranches().collectLatest { branchList ->
                _uiState.update {
                    it.copy(branches = branchList)
                }
            }
        }
    }

    private fun observeNetworkStatus() {
        viewModelScope.launch {
            networkMonitor.observeNetworkStatus().collectLatest { online ->
                _uiState.update { state ->
                    val canAccess = state.isAdmin && online

                    val selectedBranchId = if (canAccess) {
                        state.selectedBranchId
                    } else {
                        state.localBranchId
                    }

                    state.copy(
                        isOnline = online,
                        canAccessCrossBranch = canAccess,
                        selectedBranchId = selectedBranchId
                    )
                }

                loadSummary()
            }
        }
    }

    fun onBranchSelected(branchId: Int?) {
        val state = _uiState.value

        val finalBranchId = if (state.canAccessCrossBranch) {
            branchId
        } else {
            state.localBranchId
        }

        _uiState.update {
            it.copy(selectedBranchId = finalBranchId)
        }

        loadSummary(finalBranchId)
    }

    fun loadSummary(
        branchId: Int? = _uiState.value.selectedBranchId
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null
                )
            }

            val isOnline = networkMonitor.isOnline()
            val isAdmin = isAdminUser()

            when {
                !isAdmin -> {
                    loadLocalSummary(localBranchId)
                }

                !isOnline -> {
                    loadLocalSummary(localBranchId)
                }

                branchId == null -> {
                    loadRemoteSummary(null)
                }

                else -> {
                    loadRemoteSummary(branchId)
                }
            }
        }
    }

    private suspend fun loadLocalSummary(
        branchId: Int
    ) {
        try {
            val now = Calendar.getInstance()

            val todayStart = Calendar.getInstance().apply {
                setStartOfDay()
            }

            val yesterdayStart = Calendar.getInstance().apply {
                setStartOfDay()
                add(Calendar.DAY_OF_YEAR, -1)
            }

            val yesterdayEnd = todayStart.timeInMillis - 1

            val todaySales = transactionDao.getSalesTotal(
                branchId = branchId,
                from = todayStart.timeInMillis,
                to = now.timeInMillis
            )

            val yesterdaySales = transactionDao.getSalesTotal(
                branchId = branchId,
                from = yesterdayStart.timeInMillis,
                to = yesterdayEnd
            )

            val todayCash = transactionDao.getPaymentTotal(
                branchId = branchId,
                paymentType = "Cash",
                from = todayStart.timeInMillis,
                to = now.timeInMillis
            )

            val todayGcash = transactionDao.getPaymentTotal(
                branchId = branchId,
                paymentType = "Gcash",
                from = todayStart.timeInMillis,
                to = now.timeInMillis
            )

            val weekStart = Calendar.getInstance().apply {
                setStartOfDay()

                while (get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
                    add(Calendar.DAY_OF_YEAR, -1)
                }
            }

            val weekData = MutableList(7) { 0.0 }

            repeat(7) { index ->
                val dayStart = weekStart.clone() as Calendar
                dayStart.add(Calendar.DAY_OF_YEAR, index)

                val dayEnd = dayStart.clone() as Calendar
                dayEnd.add(Calendar.DAY_OF_YEAR, 1)

                weekData[index] = transactionDao.getSalesTotal(
                    branchId = branchId,
                    from = dayStart.timeInMillis,
                    to = dayEnd.timeInMillis - 1
                )
            }

            val monthStart = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                setStartOfDay()
            }

            val monthTotal = transactionDao.getSalesTotal(
                branchId = branchId,
                from = monthStart.timeInMillis,
                to = now.timeInMillis
            )

            val monthTransactions = transactionDao.getTransactionCount(
                branchId = branchId,
                from = monthStart.timeInMillis,
                to = now.timeInMillis
            )

            val daysInMonthSoFar = now.get(Calendar.DAY_OF_MONTH)
            val monthLineData = MutableList(daysInMonthSoFar) { 0.0 }

            repeat(daysInMonthSoFar) { index ->
                val dayStart = monthStart.clone() as Calendar
                dayStart.add(Calendar.DAY_OF_MONTH, index)

                val dayEnd = dayStart.clone() as Calendar
                dayEnd.add(Calendar.DAY_OF_MONTH, 1)

                monthLineData[index] = transactionDao.getSalesTotal(
                    branchId = branchId,
                    from = dayStart.timeInMillis,
                    to = dayEnd.timeInMillis - 1
                )
            }

            _uiState.update {
                it.copy(
                    todaySales = todaySales,
                    yesterdaySales = yesterdaySales,
                    weekData = weekData,
                    monthTotal = monthTotal,
                    monthTransactions = monthTransactions,
                    todayCash = todayCash,
                    todayGcash = todayGcash,
                    monthLineData = monthLineData,
                    isLoading = false,
                    error = null
                )
            }
        } catch (exception: Exception) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = exception.message ?: "Failed to load local sales summary."
                )
            }
        }
    }

    private suspend fun loadRemoteSummary(
        branchId: Int?
    ) {
        try {
            val nowCalendar = Calendar.getInstance()

            val todayStart = Calendar.getInstance().apply {
                setStartOfDay()
            }

            val yesterdayStart = Calendar.getInstance().apply {
                setStartOfDay()
                add(Calendar.DAY_OF_YEAR, -1)
            }

            val yesterdayEnd = todayStart.timeInMillis - 1

            val monthStart = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                setStartOfDay()
            }

            val todayReport = getRemoteSalesReport(
                branchId = branchId,
                from = todayStart.timeInMillis,
                to = nowCalendar.timeInMillis
            )

            val yesterdayReport = getRemoteSalesReport(
                branchId = branchId,
                from = yesterdayStart.timeInMillis,
                to = yesterdayEnd
            )

            val monthReport = getRemoteSalesReport(
                branchId = branchId,
                from = monthStart.timeInMillis,
                to = nowCalendar.timeInMillis
            )

            val weekStart = Calendar.getInstance().apply {
                setStartOfDay()

                while (get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
                    add(Calendar.DAY_OF_YEAR, -1)
                }
            }

            val weekData = MutableList(7) { 0.0 }

            repeat(7) { index ->
                val dayStart = weekStart.clone() as Calendar
                dayStart.add(Calendar.DAY_OF_YEAR, index)

                val dayEnd = dayStart.clone() as Calendar
                dayEnd.add(Calendar.DAY_OF_YEAR, 1)

                val report = getRemoteSalesReport(
                    branchId = branchId,
                    from = dayStart.timeInMillis,
                    to = dayEnd.timeInMillis - 1
                )

                weekData[index] = report.totalSales
            }

            val daysInMonthSoFar = nowCalendar.get(Calendar.DAY_OF_MONTH)
            val monthLineData = MutableList(daysInMonthSoFar) { 0.0 }

            repeat(daysInMonthSoFar) { index ->
                val dayStart = monthStart.clone() as Calendar
                dayStart.add(Calendar.DAY_OF_MONTH, index)

                val dayEnd = dayStart.clone() as Calendar
                dayEnd.add(Calendar.DAY_OF_MONTH, 1)

                val report = getRemoteSalesReport(
                    branchId = branchId,
                    from = dayStart.timeInMillis,
                    to = dayEnd.timeInMillis - 1
                )

                monthLineData[index] = report.totalSales
            }

            _uiState.update {
                it.copy(
                    todaySales = todayReport.totalSales,
                    yesterdaySales = yesterdayReport.totalSales,
                    weekData = weekData,
                    monthTotal = monthReport.totalSales,
                    monthTransactions = monthReport.totalTransactions,
                    todayCash = todayReport.cashTotal,
                    todayGcash = todayReport.gcashTotal,
                    monthLineData = monthLineData,
                    isLoading = false,
                    error = null
                )
            }
        } catch (exception: Exception) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = exception.message ?: "Failed to load remote sales summary."
                )
            }
        }
    }

    private suspend fun getRemoteSalesReport(
        branchId: Int?,
        from: Long,
        to: Long
    ): com.example.fruitylicious.data.remote.dto.SalesReportDto {
        val result = if (branchId == null) {
            reportRepository.getCombinedSalesReport(
                from = from,
                to = to
            )
        } else {
            reportRepository.getSalesReport(
                branchId = branchId,
                from = from,
                to = to
            )
        }

        return result.getOrElse { error ->
            throw error
        }
    }

    private fun Calendar.setStartOfDay() {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    private fun isAdminUser(): Boolean {
        val role = sessionManager.getRole()

        return role.equals("admin", ignoreCase = true) ||
                role.equals("owner", ignoreCase = true)
    }
}