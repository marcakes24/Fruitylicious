package com.example.fruitylicious.ui.shared.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.example.fruitylicious.data.local.dao.AuditLogDao
import com.example.fruitylicious.data.local.dao.BranchDao
import com.example.fruitylicious.data.local.dao.ProductDao
import com.example.fruitylicious.data.local.dao.TransactionDao
import com.example.fruitylicious.data.local.dao.TransactionItemAddonDao
import com.example.fruitylicious.data.local.dao.TransactionItemDao
import com.example.fruitylicious.data.local.dao.UserDao
import com.example.fruitylicious.data.local.db.PosDatabase
import com.example.fruitylicious.data.local.entity.AuditLogEntity
import com.example.fruitylicious.data.local.entity.BranchEntity
import com.example.fruitylicious.data.local.entity.ProductEntity
import com.example.fruitylicious.data.local.entity.TransactionEntity
import com.example.fruitylicious.data.local.entity.TransactionItemAddonEntity
import com.example.fruitylicious.data.local.entity.TransactionItemEntity
import com.example.fruitylicious.data.local.entity.UserEntity
import com.example.fruitylicious.data.repository.ReportRepository
import com.example.fruitylicious.util.BranchConfig
import com.example.fruitylicious.util.NetworkMonitor
import com.example.fruitylicious.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TransactionHistoryItemRow(
    val transactionItemId: String,
    val productName: String,
    val sizeName: String,
    val quantity: Int,
    val subtotal: Double,
    val addons: List<String>
)

data class TransactionHistoryRow(
    val transactionId: String,
    val displayId: String,
    val transactionName: String?,
    val staffName: String,
    val username: String,
    val branchId: Int,
    val totalAmount: Double,
    val paymentType: String,
    val status: String,
    val dateTime: Long,
    val items: List<TransactionHistoryItemRow>
)

data class TransactionHistoryUiState(
    val transactions: List<TransactionHistoryRow> = emptyList(),
    val isAdmin: Boolean = false,
    val localBranchId: Int = 1,
    val selectedBranchId: Int? = 1,
    val branches: List<BranchEntity> = emptyList(),
    val isOnline: Boolean = false,
    val canAccessCrossBranch: Boolean = false,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val currentPage: Int = 0,
    val isRemoteAccessLocked: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class TransactionHistoryViewModel @Inject constructor(
    private val database: PosDatabase,
    private val transactionDao: TransactionDao,
    private val transactionItemDao: TransactionItemDao,
    private val transactionItemAddonDao: TransactionItemAddonDao,
    private val productDao: ProductDao,
    private val userDao: UserDao,
    private val auditLogDao: AuditLogDao,
    private val sessionManager: SessionManager,
    private val branchConfig: BranchConfig,
    private val branchDao: BranchDao,
    private val reportRepository: ReportRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val localBranchId = branchConfig.branchId

    private val _uiState = MutableStateFlow(
        TransactionHistoryUiState(
            isAdmin = isAdminUser(),
            localBranchId = localBranchId,
            selectedBranchId = localBranchId
        )
    )

    val uiState: StateFlow<TransactionHistoryUiState> = _uiState.asStateFlow()

    private val PAGE_SIZE = 50
    private val refreshTrigger = MutableStateFlow(0)
    private var lockoutJob: kotlinx.coroutines.Job? = null
    private var loadJob: kotlinx.coroutines.Job? = null

    init {
        observeBranches()
        observeNetworkStatus()
        observeLocalData()
        
        // Reactive loading: only one central point for loading
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
                    kotlinx.coroutines.delay(300) // Debounce branch selection
                    loadTransactions()
                } else {
                    // For local branch, observeLocalData handles everything
                    loadJob?.cancel()
                    // Remove premature isLoading = false here, let observeLocalData handle it
                }
            }
        }
    }

    private fun observeLocalData() {
        viewModelScope.launch {
            combine(
                transactionDao.observeAllTransactions(),
                transactionItemDao.observeAllTransactionItems(),
                transactionItemAddonDao.observeAllTransactionItemAddons(),
                productDao.observeProducts(),
                userDao.observeUsers()
            ) { transactions, items, addons, products, users ->
                buildHistoryRows(transactions, items, addons, products, users)
            }.combine(refreshTrigger) { rows, _ ->
                rows
            }.collect { rows ->
                val state = _uiState.value
                
                // Show local data if:
                // 1. Local branch selected
                // 2. Offline
                // 3. Error fallback
                // 4. Loading remote data (placeholder)
                if (state.selectedBranchId == localBranchId || !state.isOnline || state.error != null || state.transactions.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            transactions = rows,
                            isLoading = if (state.selectedBranchId != localBranchId && state.isOnline && state.error == null) state.isLoading else false,
                            hasMore = rows.size >= (state.currentPage + 1) * PAGE_SIZE
                        )
                    }
                }
            }
        }
    }

    private fun buildHistoryRows(
        transactions: List<TransactionEntity>,
        items: List<TransactionItemEntity>,
        addons: List<TransactionItemAddonEntity>,
        products: List<ProductEntity>,
        users: List<UserEntity>
    ): List<TransactionHistoryRow> {
        val state = _uiState.value
        val userMap = users.associateBy { it.userId }
        val productMap = products.associateBy { it.productId }
        val itemsByTransaction = items.groupBy { it.transactionId }
        val addonsByItem = addons.groupBy { it.transactionItemId }

        return transactions
            .filter { transaction ->
                state.selectedBranchId == null || transaction.branchId == state.selectedBranchId
            }
            .take(PAGE_SIZE + (state.currentPage * PAGE_SIZE))
            .map { transaction ->
                val user = userMap[transaction.userId]

                val itemRows = itemsByTransaction[transaction.transactionId]
                    .orEmpty()
                    .map { item ->
                        val product = productMap[item.productId]

                        val addonNames = addonsByItem[item.transactionItemId]
                            .orEmpty()
                            .mapNotNull { addon ->
                                productMap[addon.addonProductId]?.productName
                            }

                        TransactionHistoryItemRow(
                            transactionItemId = item.transactionItemId,
                            productName = product?.productName ?: "Unknown Product",
                            sizeName = item.sizeName ?: "",
                            quantity = item.quantity,
                            subtotal = item.subtotal,
                            addons = addonNames
                        )
                    }

                TransactionHistoryRow(
                    transactionId = transaction.transactionId,
                    displayId = buildDisplayId(transaction.transactionId),
                    transactionName = transaction.transactionName,
                    staffName = user?.name ?: "Unknown Staff",
                    username = user?.username ?: "unknown",
                    branchId = transaction.branchId,
                    totalAmount = transaction.totalAmount,
                    paymentType = transaction.paymentType,
                    status = transaction.status,
                    dateTime = transaction.dateTime,
                    items = itemRows
                )
            }.sortedByDescending { it.dateTime }
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
                    val canAccess = state.isAdmin

                    val selected = if (!canAccess) {
                        localBranchId
                    } else {
                        state.selectedBranchId
                    }

                    state.copy(
                        isOnline = online,
                        canAccessCrossBranch = canAccess,
                        selectedBranchId = selected
                    )
                }
            }
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
                error = null,
                transactions = emptyList()
            )
        }

        refreshTrigger.value += 1
    }

    fun refresh() {
        _uiState.update {
            it.copy(
                isLoading = true,
                error = null,
                currentPage = 0,
                transactions = emptyList()
            )
        }

        loadTransactions()
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMore) return

        if (state.isOnline && state.selectedBranchId != localBranchId) {
            viewModelScope.launch {
                loadRemoteTransactionsPage(state.currentPage + 1)
            }
        } else {
            _uiState.update { 
                it.copy(
                    currentPage = it.currentPage + 1
                ) 
            }
            refreshTrigger.value += 1
        }
    }

    private fun loadTransactions() {
        val state = _uiState.value
        loadJob?.cancel()

        if (state.isOnline && state.selectedBranchId != localBranchId) {
            loadJob = viewModelScope.launch {
                loadRemoteTransactionsPage(0)
            }
        } else {
            // Local load is handled by observeLocalData
        }
    }

    private suspend fun loadRemoteTransactionsPage(page: Int) {
        if (page == 0) {
            _uiState.update { it.copy(isLoading = true, error = null, transactions = emptyList()) }
        } else {
            _uiState.update { it.copy(isLoadingMore = true) }
        }

        val now = System.currentTimeMillis()
        val monthAgo = now - 30L * 24L * 60L * 60L * 1000L

        val result = reportRepository.getTransactionPage(
            branchId = _uiState.value.selectedBranchId,
            from = monthAgo,
            to = now,
            page = page,
            size = PAGE_SIZE
        )

        result.fold(
            onSuccess = { pageResponse ->
                val newRows = pageResponse.items.map { it.toHistoryRow() }
                _uiState.update {
                    it.copy(
                        transactions = if (page == 0) newRows else it.transactions + newRows,
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
                        error = error.message ?: "Failed to load transactions.",
                        isRemoteAccessLocked = true
                    )
                }
                startLockoutTimer()
                refreshTrigger.value += 1
            }
        )
    }

    private fun startLockoutTimer() {
        lockoutJob?.cancel()
        lockoutJob = viewModelScope.launch {
            kotlinx.coroutines.delay(5 * 60 * 1000L) // 5 minutes
            _uiState.update { it.copy(isRemoteAccessLocked = false) }
        }
    }

    fun voidTransaction(transactionId: String) {
        if (!isAdminUser()) {
            _uiState.update {
                it.copy(error = "Only owners can void transactions.")
            }
            return
        }

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val userId = sessionManager.getUserId()

            database.withTransaction {
                transactionDao.voidTransaction(
                    transactionId = transactionId,
                    lastModified = now
                )

                auditLogDao.upsertAuditLog(
                    AuditLogEntity(
                        logId = UUID.randomUUID().toString(),
                        userId = userId,
                        branchId = localBranchId,
                        action = "Voided transaction $transactionId.",
                        tableAffected = "transactions",
                        timestamp = now,
                        lastModified = now,
                        isSynced = false,
                        syncedAt = null
                    )
                )
            }

            _uiState.update {
                it.copy(
                    successMessage = "Transaction voided.",
                    error = null
                )
            }

            loadTransactions()
        }
    }

    fun clearMessages() {
        _uiState.update {
            it.copy(error = null, successMessage = null)
        }
    }

    private fun com.example.fruitylicious.data.remote.dto.TransactionReportItemDto.toHistoryRow(): TransactionHistoryRow {
        return TransactionHistoryRow(
            transactionId = transactionId,
            displayId = buildDisplayId(transactionId),
            transactionName = transactionName,
            staffName = userName,
            username = "",
            branchId = branchId,
            totalAmount = totalAmount,
            paymentType = paymentType,
            status = status,
            dateTime = dateTime,
            items = items.map { line ->
                TransactionHistoryItemRow(
                    transactionItemId = "",
                    productName = line.productName,
                    sizeName = line.sizeName ?: "",
                    quantity = line.quantity,
                    subtotal = line.subtotal,
                    addons = line.addons
                )
            }
        )
    }

    private fun isAdminUser(): Boolean {
        return sessionManager.isAdmin()
    }

    private fun buildDisplayId(transactionId: String): String {
        return if (transactionId.length <= 6) {
            transactionId.uppercase()
        } else {
            "TX-${transactionId.takeLast(6).uppercase()}"
        }
    }
}
