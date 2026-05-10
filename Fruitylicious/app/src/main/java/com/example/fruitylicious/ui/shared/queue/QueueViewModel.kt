package com.example.fruitylicious.ui.shared.queue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.example.fruitylicious.data.local.dao.AuditLogDao
import com.example.fruitylicious.data.local.dao.ProductDao
import com.example.fruitylicious.data.local.dao.TransactionDao
import com.example.fruitylicious.data.local.dao.TransactionItemAddonDao
import com.example.fruitylicious.data.local.dao.TransactionItemDao
import com.example.fruitylicious.data.local.db.PosDatabase
import com.example.fruitylicious.data.local.entity.AuditLogEntity
import com.example.fruitylicious.data.repository.StaffLogRepository
import com.example.fruitylicious.util.BranchConfig
import com.example.fruitylicious.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class QueueOrderLine(
    val productName: String,
    val sizeName: String,
    val quantity: Int,
    val subtotal: Double,
    val addons: List<String> = emptyList()
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
    val isClockedIn: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class QueueViewModel @Inject constructor(
    private val database: PosDatabase,
    private val transactionDao: TransactionDao,
    private val transactionItemDao: TransactionItemDao,
    private val transactionItemAddonDao: TransactionItemAddonDao,
    private val productDao: ProductDao,
    private val auditLogDao: AuditLogDao,
    private val staffLogRepository: StaffLogRepository,
    private val sessionManager: SessionManager,
    private val branchConfig: BranchConfig
) : ViewModel() {

    private val _uiState = MutableStateFlow(QueueUiState())
    val uiState: StateFlow<QueueUiState> = _uiState.asStateFlow()

    init {
        observeAll()
        observeClockInStatus()
    }

    private fun observeAll() {
        val since = System.currentTimeMillis() - (24 * 60 * 60 * 1000L) // Last 24 hours
        
        viewModelScope.launch {
            combine(
                transactionDao.observeQueueTransactions(since),
                transactionItemDao.observeAllTransactionItems(),
                transactionItemAddonDao.observeAllTransactionItemAddons(),
                productDao.observeProducts()
            ) { transactions, items, addons, products ->
                val currentBranchId = sessionManager.getBranchId().takeIf { it > 0 } ?: branchConfig.branchId
                val productMap = products.associateBy { it.productId }
                val itemsByTransaction = items.groupBy { it.transactionId }
                val addonsByItem = addons.groupBy { it.transactionItemId }

                transactions
                    .filter { it.branchId == currentBranchId }
                    .map { transaction ->
                    val itemRows = itemsByTransaction[transaction.transactionId]
                        .orEmpty()
                        .map { item ->
                            val product = productMap[item.productId]
                            val itemAddons = addonsByItem[item.transactionItemId]
                                .orEmpty()
                                .map { addon ->
                                    val addonProduct = productMap[addon.addonProductId]
                                    addonProduct?.productName ?: "Unknown Addon"
                                }

                            QueueOrderLine(
                                productName = product?.productName ?: "Unknown Product",
                                sizeName = item.sizeName ?: "",
                                quantity = item.quantity,
                                subtotal = item.subtotal,
                                addons = itemAddons
                            )
                        }

                    QueueOrderRow(
                        transactionId = transaction.transactionId,
                        displayId = buildDisplayId(transaction.transactionId),
                        queueNumber = buildQueueNumber(transaction.transactionId),
                        customerName = transaction.transactionName ?: "Walk-in Customer",
                        branchId = transaction.branchId,
                        paymentType = transaction.paymentType,
                        status = transaction.status.lowercase(),
                        totalAmount = transaction.totalAmount,
                        dateTime = transaction.dateTime,
                        items = itemRows
                    )
                }.sortedByDescending { it.dateTime }
            }.onStart { 
                _uiState.update { it.copy(isLoading = true) }
            }.collect { rows ->
                _uiState.update {
                    it.copy(
                        orders = rows,
                        isLoading = false,
                        error = null
                    )
                }
            }
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
            try {
                // Update selected tab immediately for a snappier feel
                _uiState.update { 
                    it.copy(
                        selectedStatus = nextStatus,
                        isLoading = true, 
                        successMessage = null, 
                        error = null
                    ) 
                }
                
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
                        successMessage = "Order moved to ${nextStatus.replaceFirstChar { it.uppercase() }}.",
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to update order: ${e.message}"
                    )
                }
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

    private fun observeClockInStatus() {
        viewModelScope.launch {
            if (sessionManager.isAdmin()) {
                _uiState.update { it.copy(isClockedIn = true) }
                return@launch
            }

            val userId = sessionManager.getUserId()
            staffLogRepository.observeStaffLogsByUser(userId).collectLatest { logs ->
                val hasActiveLog = logs.any { it.clockOut == null }
                _uiState.update { it.copy(isClockedIn = hasActiveLog) }
            }
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
