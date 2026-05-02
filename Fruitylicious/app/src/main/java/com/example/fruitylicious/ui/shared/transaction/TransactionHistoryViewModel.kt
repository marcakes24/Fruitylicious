package com.example.fruitylicious.ui.shared.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.example.fruitylicious.data.local.dao.AuditLogDao
import com.example.fruitylicious.data.local.dao.ProductDao
import com.example.fruitylicious.data.local.dao.TransactionDao
import com.example.fruitylicious.data.local.dao.TransactionItemAddonDao
import com.example.fruitylicious.data.local.dao.TransactionItemDao
import com.example.fruitylicious.data.local.dao.UserDao
import com.example.fruitylicious.data.local.db.PosDatabase
import com.example.fruitylicious.data.local.entity.AuditLogEntity
import com.example.fruitylicious.data.local.entity.ProductEntity
import com.example.fruitylicious.data.local.entity.TransactionEntity
import com.example.fruitylicious.data.local.entity.TransactionItemAddonEntity
import com.example.fruitylicious.data.local.entity.TransactionItemEntity
import com.example.fruitylicious.data.local.entity.UserEntity
import com.example.fruitylicious.util.BranchConfig
import com.example.fruitylicious.util.SessionManager
import com.example.fruitylicious.data.local.dao.BranchDao
import com.example.fruitylicious.data.local.entity.BranchEntity
import com.example.fruitylicious.data.repository.ReportRepository
import com.example.fruitylicious.util.NetworkMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

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

    private val _uiState = MutableStateFlow(
        TransactionHistoryUiState(
            isAdmin = sessionManager.getRole()?.equals("admin", ignoreCase = true) == true ||
                    sessionManager.getRole()?.equals("owner", ignoreCase = true) == true,
            localBranchId = sessionManager.getBranchId(),
            selectedBranchId = sessionManager.getBranchId()
        )
    )
    val uiState: StateFlow<TransactionHistoryUiState> = _uiState.asStateFlow()

    private var localTransactions: List<TransactionEntity> = emptyList()
    private var remoteTransactions: List<TransactionHistoryRow> = emptyList()
    private var transactionItems: List<TransactionItemEntity> = emptyList()
    private var transactionAddons: List<TransactionItemAddonEntity> = emptyList()
    private var products: List<ProductEntity> = emptyList()
    private var users: List<UserEntity> = emptyList()

    init {
        observeTransactions()
        observeItems()
        observeAddons()
        observeProducts()
        observeUsers()
        observeBranches()
        observeNetworkStatus()
    }

    private fun observeBranches() {
        viewModelScope.launch {
            branchDao.observeAllBranches().collectLatest { branchList ->
                _uiState.update { it.copy(branches = branchList) }
            }
        }
    }

    private fun observeNetworkStatus() {
        viewModelScope.launch {
            networkMonitor.observeNetworkStatus().collectLatest { online ->
                _uiState.update { state ->
                    val canAccess = state.isAdmin && online
                    val newSelectedId = if (!canAccess && state.selectedBranchId != state.localBranchId) {
                        state.localBranchId
                    } else {
                        state.selectedBranchId
                    }

                    state.copy(
                        isOnline = online,
                        canAccessCrossBranch = canAccess,
                        selectedBranchId = newSelectedId
                    )
                }
                loadRemoteIfNecessary()
            }
        }
    }

    fun onBranchSelected(branchId: Int?) {
        val state = _uiState.value
        if (branchId != state.localBranchId && !state.canAccessCrossBranch) {
            return
        }
        _uiState.update { it.copy(selectedBranchId = branchId) }
        loadRemoteIfNecessary()
        rebuildRows()
    }

    private fun loadRemoteIfNecessary() {
        val state = _uiState.value
        if (state.selectedBranchId != state.localBranchId && state.isOnline) {
            fetchRemoteTransactions(state.selectedBranchId)
        } else {
            remoteTransactions = emptyList()
            rebuildRows()
        }
    }

    private fun fetchRemoteTransactions(requestedBranchId: Int?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            val now = System.currentTimeMillis()
            val monthAgo = now - 30L * 24L * 60L * 60L * 1000L
            
            val result = if (requestedBranchId == null) {
                reportRepository.getTransactionReport(0, monthAgo, now)
            } else {
                reportRepository.getTransactionReport(requestedBranchId, monthAgo, now)
            }

            result.onSuccess { reportDto ->
                remoteTransactions = reportDto.transactions.map { item ->
                    TransactionHistoryRow(
                        transactionId = item.transactionId,
                        displayId = buildDisplayId(item.transactionId),
                        staffName = item.userName,
                        username = "",
                        branchId = item.branchId,
                        totalAmount = item.totalAmount,
                        paymentType = item.paymentType,
                        status = item.status,
                        dateTime = item.dateTime,
                        items = item.items.map { line ->
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
                rebuildRows()
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun observeTransactions() {
        viewModelScope.launch {
            transactionDao.observeAllTransactions().collectLatest { items ->
                localTransactions = items
                rebuildRows()
            }
        }
    }

    private fun observeItems() {
        viewModelScope.launch {
            transactionItemDao.observeAllTransactionItems().collectLatest { items ->
                transactionItems = items
                rebuildRows()
            }
        }
    }

    private fun observeAddons() {
        viewModelScope.launch {
            transactionItemAddonDao.observeAllTransactionItemAddons().collectLatest { items ->
                transactionAddons = items
                rebuildRows()
            }
        }
    }

    private fun observeProducts() {
        viewModelScope.launch {
            productDao.observeProducts().collectLatest { items ->
                products = items
                rebuildRows()
            }
        }
    }

    private fun observeUsers() {
        viewModelScope.launch {
            userDao.observeUsers().collectLatest { items ->
                users = items
                rebuildRows()
            }
        }
    }

    private fun rebuildRows() {
        val state = _uiState.value
        
        if (state.selectedBranchId != state.localBranchId && state.isOnline) {
            _uiState.update {
                it.copy(
                    transactions = remoteTransactions,
                    isLoading = false,
                    error = null
                )
            }
            return
        }

        val userMap = users.associateBy { it.userId }
        val productMap = products.associateBy { it.productId }
        val itemsByTransaction = transactionItems.groupBy { it.transactionId }
        val addonsByItem = transactionAddons.groupBy { it.transactionItemId }

        val rows = localTransactions.map { transaction ->
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
        }.sortedByDescending { it.dateTime }

        _uiState.update {
            it.copy(
                transactions = rows,
                isLoading = false,
                error = null
            )
        }
    }

    fun voidTransaction(transactionId: String) {
        if (sessionManager.getRole()?.equals("admin", ignoreCase = true) != true) {
            _uiState.update { it.copy(error = "Only admins can void transactions.") }
            return
        }

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val userId = sessionManager.getUserId()
            val branchId = branchConfig.branchId

            database.withTransaction {
                transactionDao.voidTransaction(
                    transactionId = transactionId,
                    lastModified = now
                )

                auditLogDao.upsertAuditLog(
                    AuditLogEntity(
                        logId = UUID.randomUUID().toString(),
                        userId = userId,
                        branchId = branchId,
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
        }
    }

    fun clearMessages() {
        _uiState.update {
            it.copy(error = null, successMessage = null)
        }
    }

    private fun buildDisplayId(transactionId: String): String {
        return if (transactionId.length <= 6) {
            transactionId.uppercase()
        } else {
            "TX-${transactionId.takeLast(6).uppercase()}"
        }
    }
}