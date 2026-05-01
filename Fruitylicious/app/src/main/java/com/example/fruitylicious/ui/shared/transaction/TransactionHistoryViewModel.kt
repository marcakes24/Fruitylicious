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
    val userBranchId: String = "B1",
    val isLoading: Boolean = true,
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
    private val branchConfig: BranchConfig
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        TransactionHistoryUiState(
            isAdmin = sessionManager.getRole()?.equals("admin", ignoreCase = true) == true,
            userBranchId = "B${sessionManager.getBranchId()}"
        )
    )
    val uiState: StateFlow<TransactionHistoryUiState> = _uiState.asStateFlow()

    private var transactions: List<TransactionEntity> = emptyList()
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
    }

    private fun observeTransactions() {
        viewModelScope.launch {
            transactionDao.observeAllTransactions().collectLatest { items ->
                transactions = items
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
        val userMap = users.associateBy { it.userId }
        val productMap = products.associateBy { it.productId }
        val itemsByTransaction = transactionItems.groupBy { it.transactionId }
        val addonsByItem = transactionAddons.groupBy { it.transactionItemId }

        val rows = transactions.map { transaction ->
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