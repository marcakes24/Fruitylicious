package com.example.fruitylicious.domain.usecase.restock

import com.example.fruitylicious.data.repository.RestockRepository
import javax.inject.Inject

class RestockUseCase @Inject constructor(
    private val restockRepository: RestockRepository
) {

    suspend operator fun invoke(
        ingredientId: Int,
        branchId: Int,
        userId: Int,
        quantityAdded: Double,
        supplier: String
    ): Result<Unit> {
        return restockRepository.restock(
            ingredientId = ingredientId,
            branchId = branchId,
            userId = userId,
            quantityAdded = quantityAdded,
            supplier = supplier
        )
    }
}