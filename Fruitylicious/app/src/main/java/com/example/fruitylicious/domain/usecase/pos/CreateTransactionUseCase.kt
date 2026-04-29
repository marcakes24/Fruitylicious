package com.example.fruitylicious.domain.usecase.pos

import com.example.fruitylicious.data.repository.CartItem
import com.example.fruitylicious.data.repository.TransactionRepository
import javax.inject.Inject

class CreateTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {

    suspend operator fun invoke(
        userId: Int,
        branchId: Int,
        cartItems: List<CartItem>,
        paymentType: String
    ): Result<String> {
        return transactionRepository.createTransaction(
            userId = userId,
            branchId = branchId,
            cartItems = cartItems,
            paymentType = paymentType
        )
    }
}