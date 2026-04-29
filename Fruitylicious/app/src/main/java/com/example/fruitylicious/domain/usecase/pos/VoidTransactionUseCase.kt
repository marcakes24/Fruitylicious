package com.example.fruitylicious.domain.usecase.pos

import com.example.fruitylicious.data.repository.TransactionRepository
import javax.inject.Inject

class VoidTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {

    suspend operator fun invoke(
        transactionId: String,
        userId: Int,
        branchId: Int
    ): Result<Unit> {
        return transactionRepository.voidTransaction(
            transactionId = transactionId,
            userId = userId,
            branchId = branchId
        )
    }
}