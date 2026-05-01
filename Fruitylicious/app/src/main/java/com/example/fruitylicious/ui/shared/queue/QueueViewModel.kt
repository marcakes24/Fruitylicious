package com.example.fruitylicious.ui.shared.queue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.example.fruitylicious.data.local.dao.AuditLogDao
import com.example.fruitylicious.data.local.dao.ProductDao
import com.example.fruitylicious.data.local.dao.TransactionDao
import com.example.fruitylicious.data.local.dao.TransactionItemDao
import com.example.fruitylicious.data.local.db.PosDatabase
import com.example.fruitylicious.data.local.entity.AuditLogEntity
import com.example.fruitylicious.data.local.entity.ProductEntity
import com.example.fruitylicious.data.local.entity.TransactionEntity
import com.example.fruitylicious.data.local.entity.TransactionItemEntity
import com.example.fruitylicious.util.BranchConfig
import com.example.fruitylicious.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class QueueOrderLine(
    val productName: String,
    val sizeName: String,
    val quantity: Int,
    val subtotal: Double
)

data class QueueOrderRow(
    val transactionId: String,
    val displayId: String,
    val queueNumber: String,
    val customerName: String,
    val branchId: Int,
    val paymentType: String,
    val status: String,
    val totalAmount: Double,
    val dateTime: Long,
    val items: List<QueueOrderLine>
)

data class QueueUiState(
    val orders: List<QueueOrderRow> = emptyList(),
    val selectedStatus: String = "pending",
    val isLoading: Boolean = true,
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class QueueViewModel @Inject constructor(
    private val database: PosDatabase,
    private val transactionDao: TransactionDao,
    private val transactionItemDao: TransactionItemDao,
    private val productDao: ProductDao,
    private val auditLogDao: AuditLogDao,
    private val sessionManager: SessionManager,
    private val branchConfig: BranchConfig
) : ViewModel() {

    private val _uiState = MutableStateFlow(QueueUiState())
    val uiState: StateFlow<QueueUiState> = _uiState.asStateFlow()

    private var transactions: List<TransactionEntity> = emptyList()
    private var transactionItems: List<TransactionItemEntity> = emptyList()
    private var products: List<ProductEntity> = emptyList()

    init {
        observeTransactions()
        observeItems()
        observeProducts()
    }

    private fun observeTransactions() {
        viewModelScope.launch {
            transactionDao.observeQueueTransactions().collectLatest { items ->
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

    private fun observeProducts() {
        viewModelScope.launch {
            productDao.observeProducts().collectLatest { items ->
                products = items
                rebuildRows()
            }
        }
    }

    private fun rebuildRows() {
        val productMap = products.associateBy { it.productId }
        val itemsByTransaction = transactionItems.groupBy { it.transactionId }

        val rows = transactions.map { transaction ->
            val itemRows = itemsByTransaction[transaction.transactionId]
                .orEmpty()
                .map { item ->
                    val product = productMap[item.productId]

                    QueueOrderLine(
                        productName = product?.productName ?: "Unknown Product",
                        sizeName = item.sizeName ?: "",
                        quantity = item.quantity,
                        subtotal = item.subtotal
                    )
                }

            QueueOrderRow(
                transactionId = transaction.transactionId,
                displayId = buildDisplayId(transaction.transactionId),
                queueNumber = buildQueueNumber(transaction.transactionId),
                customerName = "Walk-in Customer",
                branchId = transaction.branchId,
                paymentType = transaction.paymentType,
                status = transaction.status.lowercase(),
                totalAmount = transaction.totalAmount,
                dateTime = transaction.dateTime,
                items = itemRows
            )
        }.sortedByDescending { it.dateTime }

        _uiState.update {
            it.copy(
                orders = rows,
                isLoading = false,
                error = null
            )
        }
    }

    fun selectStatus(status: String) {
        _uiState.update {
            it.copy(selectedStatus = status)
        }
    }

    fun advanceOrder(order: QueueOrderRow) {
        val nextStatus = nextStatus(order.status) ?: return

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val userId = sessionManager.getUserId()
            val branchId = order.branchId.takeIf { it > 0 } ?: branchConfig.branchId

            database.withTransaction {
                transactionDao.updateTransactionStatus(
                    transactionId = order.transactionId,
                    status = nextStatus,
                    lastModified = now
                )

                auditLogDao.upsertAuditLog(
                    AuditLogEntity(
                        logId = UUID.randomUUID().toString(),
                        userId = userId,
                        branchId = branchId,
                        action = "Updated order ${order.displayId} status to $nextStatus.",
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
                    successMessage = "Order updated.",
                    error = null
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.update {
            it.copy(
                error = null,
                successMessage = null
            )
        }
    }

    private fun nextStatus(status: String): String? {
        return when (status.lowercase()) {
            "pending" -> "preparing"
            "preparing" -> "ready"
            "ready" -> "completed"
            else -> null
        }
    }

    private fun buildDisplayId(transactionId: String): String {
        return if (transactionId.length <= 6) {
            transactionId.uppercase()
        } else {
            "TX-${transactionId.takeLast(6).uppercase()}"
        }
    }

    private fun buildQueueNumber(transactionId: String): String {
        return transactionId
            .takeLast(2)
            .filter { it.isDigit() }
            .ifBlank { "1" }
    }
}