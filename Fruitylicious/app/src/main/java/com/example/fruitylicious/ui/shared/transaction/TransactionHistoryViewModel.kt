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

    private val PAGE_SIZE = 20

    private var localTransactions: List<TransactionEntity> = emptyList()
    private var localTransactionItems: List<TransactionItemEntity> = emptyList()
    private var localTransactionAddons: List<TransactionItemAddonEntity> = emptyList()
    private var products: List<ProductEntity> = emptyList()
    private var users: List<UserEntity> = emptyList()

    init {
        observeBranches()
        observeNetworkStatus()
        observeTransactions()
        observeItems()
        observeAddons()
        observeProducts()
        observeUsers()
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

                loadTransactions()
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

        _uiState.update {
            it.copy(
                selectedBranchId = finalBranchId,
                isLoading = true,
                error = null
            )
        }

        loadTransactions()
    }

    fun refresh() {
        _uiState.update {
            it.copy(
                isLoading = true,
                error = null
            )
        }

        loadTransactions()
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMore) return

        _uiState.update { it.copy(isLoadingMore = true) }
        
        viewModelScope.launch {
            val nextPage = state.currentPage + 1
            val offset = nextPage * PAGE_SIZE
            
            val newEntities = if (state.selectedBranchId == null) {
                transactionDao.getTransactionsPaged(PAGE_SIZE, offset)
            } else {
                transactionDao.getTransactionsByBranchPaged(state.selectedBranchId, PAGE_SIZE, offset)
            }
            
            if (newEntities.isEmpty()) {
                _uiState.update { it.copy(isLoadingMore = false, hasMore = false) }
                return@launch
            }
            
            val userMap = users.associateBy { it.userId }
            val productMap = products.associateBy { it.productId }
            val itemsByTransaction = localTransactionItems.groupBy { it.transactionId }
            val addonsByItem = localTransactionAddons.groupBy { it.transactionItemId }

            val newRows = newEntities.map { transaction ->
                val user = userMap[transaction.userId]
                val itemRows = itemsByTransaction[transaction.transactionId].orEmpty().map { item ->
                    val product = productMap[item.productId]
                    val addonNames = addonsByItem[item.transactionItemId].orEmpty().mapNotNull { addon ->
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
                    staffName = user?.name ?: "Unknown Staff",
                    username = user?.username ?: "unknown",
                    branchId = transaction.branchId,
                    totalAmount = transaction.totalAmount,
                    paymentType = transaction.paymentType,
                    status = transaction.status,
                    dateTime = transaction.dateTime,
                    items = itemRows
                )
            }

            _uiState.update { 
                it.copy(
                    transactions = it.transactions + newRows,
                    currentPage = nextPage,
                    isLoadingMore = false,
                    hasMore = newRows.size == PAGE_SIZE
                )
            }
        }
    }

    private fun loadTransactions() {
        val state = _uiState.value

        when {
            state.selectedBranchId == localBranchId -> {
                rebuildLocalRows()
            }

            !state.isAdmin -> {
                rebuildLocalRows()
            }

            !state.isOnline -> {
                rebuildLocalRows()
            }

            state.selectedBranchId == null -> {
                fetchRemoteCombinedTransactions()
            }

            else -> {
                fetchRemoteBranchTransactions(state.selectedBranchId)
            }
        }
    }

    private fun fetchRemoteBranchTransactions(branchId: Int) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, error = null)
            }

            val now = System.currentTimeMillis()
            val monthAgo = now - 30L * 24L * 60L * 60L * 1000L

            val result = reportRepository.getTransactionReport(
                branchId = branchId,
                from = monthAgo,
                to = now
            )

            result.fold(
                onSuccess = { report ->
                    _uiState.update {
                        it.copy(
                            transactions = report.transactions.map { item ->
                                item.toHistoryRow()
                            }.sortedByDescending { row -> row.dateTime },
                            isLoading = false,
                            error = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            transactions = emptyList(),
                            isLoading = false,
                            error = error.message ?: "Failed to load transactions."
                        )
                    }
                }
            )
        }
    }

    private fun fetchRemoteCombinedTransactions() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, error = null)
            }

            val now = System.currentTimeMillis()
            val monthAgo = now - 30L * 24L * 60L * 60L * 1000L

            val result = reportRepository.getCombinedTransactionReport(
                from = monthAgo,
                to = now
            )

            result.fold(
                onSuccess = { report ->
                    _uiState.update {
                        it.copy(
                            transactions = report.transactions.map { item ->
                                item.toHistoryRow()
                            }.sortedByDescending { row -> row.dateTime },
                            isLoading = false,
                            error = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            transactions = emptyList(),
                            isLoading = false,
                            error = error.message ?: "Failed to load combined transactions."
                        )
                    }
                }
            )
        }
    }

    private fun observeTransactions() {
        viewModelScope.launch {
            transactionDao.observeAllTransactions().collectLatest { items ->
                localTransactions = items
                loadTransactions()
            }
        }
    }

    private fun observeItems() {
        viewModelScope.launch {
            transactionItemDao.observeAllTransactionItems().collectLatest { items ->
                localTransactionItems = items
                loadTransactions()
            }
        }
    }

    private fun observeAddons() {
        viewModelScope.launch {
            transactionItemAddonDao.observeAllTransactionItemAddons().collectLatest { items ->
                localTransactionAddons = items
                loadTransactions()
            }
        }
    }

    private fun observeProducts() {
        viewModelScope.launch {
            productDao.observeProducts().collectLatest { items ->
                products = items
                loadTransactions()
            }
        }
    }

    private fun observeUsers() {
        viewModelScope.launch {
            userDao.observeUsers().collectLatest { items ->
                users = items
                loadTransactions()
            }
        }
    }

    private fun rebuildLocalRows() {
        val userMap = users.associateBy { it.userId }
        val productMap = products.associateBy { it.productId }
        val itemsByTransaction = localTransactionItems.groupBy { it.transactionId }
        val addonsByItem = localTransactionAddons.groupBy { it.transactionItemId }

        val rows = localTransactions
            .filter { transaction -> 
                _uiState.value.selectedBranchId == null || transaction.branchId == _uiState.value.selectedBranchId 
            }
            .take(PAGE_SIZE)
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
                    staffName = user?.name ?: "Unknown Staff",
                    username = user?.username ?: "unknown",
                    branchId = transaction.branchId,
                    totalAmount = transaction.totalAmount,
                    paymentType = transaction.paymentType,
                    status = transaction.status,
                    dateTime = transaction.dateTime,
                    items = itemRows
                )
            }
            .sortedByDescending { it.dateTime }

        _uiState.update {
            it.copy(
                transactions = rows,
                isLoading = false,
                hasMore = rows.size >= PAGE_SIZE,
                currentPage = 0,
                error = null
            )
        }
    }

    fun voidTransaction(transactionId: String) {
        if (!isAdminUser()) {
            _uiState.update {
                it.copy(error = "Only admins can void transactions.")
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
        val role = sessionManager.getRole()

        return role.equals("admin", ignoreCase = true) ||
                role.equals("owner", ignoreCase = true)
    }

    private fun buildDisplayId(transactionId: String): String {
        return if (transactionId.length <= 6) {
            transactionId.uppercase()
        } else {
            "TX-${transactionId.takeLast(6).uppercase()}"
        }
    }
}