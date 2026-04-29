package com.example.fruitylicious.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitylicious.data.local.dao.ProductDao
import com.example.fruitylicious.data.local.dao.TransactionDao
import com.example.fruitylicious.data.local.dao.TransactionItemDao
import com.example.fruitylicious.data.local.dao.UserDao
import com.example.fruitylicious.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class TransactionDetailData(
    val transactionId: String,
    val staffName: String,
    val date: String,
    val time: String,
    val amount: String,
    val status: String,
    val items: List<TransactionItemDetail>
)

data class TransactionItemDetail(
    val productName: String,
    val quantity: Int,
    val subtotal: Double
)

class TransactionViewModel(
    private val transactionDao: TransactionDao,
    private val transactionItemDao: TransactionItemDao,
    private val userDao: UserDao,
    private val productDao: ProductDao
) : ViewModel() {

    private val _transactions = MutableStateFlow<List<TransactionEntity>>(emptyList())
    val transactions: StateFlow<List<TransactionEntity>> = _transactions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _selectedTransactionDetails = MutableStateFlow<TransactionDetailData?>(null)
    val selectedTransactionDetails: StateFlow<TransactionDetailData?> = _selectedTransactionDetails.asStateFlow()

    // Keep track of all transactions for search
    private var allTransactions: List<TransactionEntity> = emptyList()

    init {
        loadAllTransactions()
    }

    fun loadAllTransactions() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                transactionDao.getAll().collect { list ->
                    allTransactions = list.sortedByDescending { it.dateTime }
                    _transactions.value = allTransactions
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = "Error loading transactions: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun loadTransactionsByDateRange(fromDate: Long, toDate: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                transactionDao.getByDateRange(fromDate, toDate).collect { list ->
                    allTransactions = list
                    _transactions.value = list.sortedByDescending { it.dateTime }
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = "Error loading transactions: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun searchTransactions(query: String) {
        if (query.isEmpty()) {
            _transactions.value = allTransactions
            return
        }
        
        val filtered = allTransactions.filter { transaction ->
            transaction.transactionId.contains(query, ignoreCase = true) ||
            transaction.paymentType.contains(query, ignoreCase = true)
        }
        _transactions.value = filtered
    }

    fun loadTransactionDetails(transactionId: String) {
        viewModelScope.launch {
            try {
                // Get transaction
                val transaction = transactionDao.getById(transactionId) ?: return@launch
                
                // Get user/staff info
                val user = if (transaction.userId != null) {
                    userDao.getById(transaction.userId)
                } else {
                    null
                }
                
                // Get transaction items
                var itemDetails = listOf<TransactionItemDetail>()
                transactionItemDao.getByTransactionId(transactionId).collect { items ->
                    itemDetails = items.mapNotNull { item ->
                        val product = productDao.getById(item.productId)
                        if (product != null) {
                            TransactionItemDetail(
                                productName = product.productName,
                                quantity = item.quantity,
                                subtotal = item.subtotal
                            )
                        } else null
                    }
                }
                
                // Format date and time
                val dateFormat = SimpleDateFormat("MMMM dd, yyyy - hh:mm a", Locale.getDefault())
                val formattedDateTime = dateFormat.format(Date(transaction.dateTime))
                
                val detailData = TransactionDetailData(
                    transactionId = transaction.transactionId,
                    staffName = user?.name ?: "Unknown Staff",
                    date = formattedDateTime,
                    time = "",
                    amount = "₱%.2f".format(transaction.totalAmount),
                    status = transaction.status.uppercase(),
                    items = itemDetails
                )
                
                _selectedTransactionDetails.value = detailData
            } catch (e: Exception) {
                _error.value = "Error loading transaction details: ${e.message}"
            }
        }
    }

    fun voidTransaction(transactionId: String) {
        viewModelScope.launch {
            try {
                transactionDao.voidTransaction(transactionId)
                // Reload transactions to reflect the change
                loadAllTransactions()
                _selectedTransactionDetails.value = null
            } catch (e: Exception) {
                _error.value = "Error voiding transaction: ${e.message}"
            }
        }
    }

    fun clearSelectedTransaction() {
        _selectedTransactionDetails.value = null
    }

    fun clearError() {
        _error.value = null
    }
}

