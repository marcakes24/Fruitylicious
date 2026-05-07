package com.example.fruitylicious.domain.usecase.admin

import com.example.fruitylicious.data.local.entity.IngredientEntity
import com.example.fruitylicious.data.repository.IngredientRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ManageIngredientUseCase @Inject constructor(
    private val ingredientRepository: IngredientRepository
) {

    fun observeIngredients(): Flow<List<IngredientEntity>> {
        return ingredientRepository.observeIngredients()
    }

    fun observeIngredient(ingredientId: Int): Flow<IngredientEntity?> {
        return ingredientRepository.observeIngredient(ingredientId)
    }

    suspend fun getIngredient(ingredientId: Int): IngredientEntity? {
        return ingredientRepository.getIngredient(ingredientId)
    }

    suspend fun saveIngredient(
        ingredientId: Int,
        image: String?,
        ingredientName: String,
        unitType: String,
        isPackaging: Boolean,
        lowStockThreshold: Double
    ): Result<Unit> {
        return ingredientRepository.saveIngredient(
            ingredientId = ingredientId,
            image = image,
            ingredientName = ingredientName,
            unitType = unitType,
            isPackaging = isPackaging,
            lowStockThreshold = lowStockThreshold
        )
    }

    suspend fun deleteIngredient(ingredientId: Int): Result<Unit> {
        return ingredientRepository.deleteIngredient(ingredientId)
    }

    suspend fun updateIngredientImage(ingredientId: Int, imagePath: String?) {
        ingredientRepository.updateIngredientImage(ingredientId, imagePath)
    }
}