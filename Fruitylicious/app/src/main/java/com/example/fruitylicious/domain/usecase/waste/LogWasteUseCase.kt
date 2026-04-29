package com.example.fruitylicious.domain.usecase.waste

import com.example.fruitylicious.data.repository.WasteRepository
import javax.inject.Inject

class LogWasteUseCase @Inject constructor(
    private val wasteRepository: WasteRepository
) {

    suspend operator fun invoke(
        ingredientId: Int,
        branchId: Int,
        userId: Int,
        quantity: Double,
        image: String?,
        reason: String
    ): Result<Unit> {
        return wasteRepository.logWaste(
            ingredientId = ingredientId,
            branchId = branchId,
            userId = userId,
            quantity = quantity,
            image = image,
            reason = reason
        )
    }
}