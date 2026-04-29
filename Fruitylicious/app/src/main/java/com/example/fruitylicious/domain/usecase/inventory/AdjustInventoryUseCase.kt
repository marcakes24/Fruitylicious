package com.example.fruitylicious.domain.usecase.inventory

import com.example.fruitylicious.data.repository.AdjustmentRepository
import javax.inject.Inject

class AdjustInventoryUseCase @Inject constructor(
    private val adjustmentRepository: AdjustmentRepository
) {

    suspend operator fun invoke(
        ingredientId: Int,
        branchId: Int,
        userId: Int,
        adjustmentAmount: Double,
        reason: String
    ): Result<Unit> {
        return adjustmentRepository.adjustInventory(
            ingredientId = ingredientId,
            branchId = branchId,
            userId = userId,
            adjustmentAmount = adjustmentAmount,
            reason = reason
        )
    }
}