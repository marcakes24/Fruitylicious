package com.example.fruitylicious.ui.staff.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitylicious.data.local.entity.TransactionEntity
import com.example.fruitylicious.data.repository.TransactionRepository
import com.example.fruitylicious.domain.usecase.pos.VoidTransactionUseCase
import com.example.fruitylicious.util.BranchConfig
import com.example.fruitylicious.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransactionHistoryUiState(
    val transactions: List<TransactionEntity> = emptyList(),
    val isAdmin: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class TransactionHistoryViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val voidTransactionUseCase: VoidTransactionUseCase,
    private val sessionManager: SessionManager,
    private val branchConfig: BranchConfig
) : ViewModel() {

    private val branchId = sessionManager.getBranchId().takeIf { it > 0 } ?: branchConfig.branchId

    private val _uiState = MutableStateFlow(TransactionHistoryUiState(
        isAdmin = sessionManager.getRole()?.equals("admin", ignoreCase = true) == true
    ))
    val uiState: StateFlow<TransactionHistoryUiState> = _uiState.asStateFlow()

    init {
        observeTransactions()
    }

    private fun observeTransactions() {
        viewModelScope.launch {
            transactionRepository.observeTransactions(branchId).collectLatest { transactions ->
                _uiState.update {
                    it.copy(
                        transactions = transactions,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun voidTransaction(transactionId: String) {
        if (!_uiState.value.isAdmin) {
            _uiState.update { it.copy(error = "Only admins can void transactions.") }
            return
        }

        viewModelScope.launch {
            val result = voidTransactionUseCase(
                transactionId = transactionId,
                userId = sessionManager.getUserId(),
                branchId = branchId
            )

            _uiState.update {
                it.copy(error = result.exceptionOrNull()?.message)
            }
        }
    }
}
