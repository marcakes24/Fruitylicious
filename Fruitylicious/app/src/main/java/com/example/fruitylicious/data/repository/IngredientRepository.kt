package com.example.fruitylicious.data.repository

import com.example.fruitylicious.data.local.dao.IngredientDao
import com.example.fruitylicious.data.local.entity.IngredientEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IngredientRepository @Inject constructor(
    private val ingredientDao: IngredientDao
) {

    fun observeIngredients(): Flow<List<IngredientEntity>> {
        return ingredientDao.observeAllIngredients()
    }

    fun observeIngredient(ingredientId: Int): Flow<IngredientEntity?> {
        return ingredientDao.observeIngredient(ingredientId)
    }

    fun observeIngredientsByPackaging(isPackaging: Boolean): Flow<List<IngredientEntity>> {
        return ingredientDao.observeIngredientsByPackaging(isPackaging)
    }

    fun searchIngredients(query: String): Flow<List<IngredientEntity>> {
        return ingredientDao.searchIngredients(query.trim())
    }

    suspend fun getIngredients(): List<IngredientEntity> {
        return ingredientDao.getAllIngredients()
    }

    suspend fun getIngredient(ingredientId: Int): IngredientEntity? {
        return ingredientDao.getIngredientById(ingredientId)
    }

    suspend fun saveIngredient(
        ingredientId: Int,
        image: String?,
        ingredientName: String,
        unitType: String,
        estimatedWeightPerUnit: Double,
        isPackaging: Boolean,
        lowStockThreshold: Double
    ): Result<Unit> {
        val cleanName = ingredientName.trim()
        val cleanUnit = unitType.trim()

        if (cleanName.isBlank()) {
            return Result.failure(IllegalArgumentException("Ingredient name is required."))
        }

        if (cleanUnit.isBlank()) {
            return Result.failure(IllegalArgumentException("Unit type is required."))
        }

        if (estimatedWeightPerUnit < 0.0) {
            return Result.failure(IllegalArgumentException("Estimated weight cannot be negative."))
        }

        if (lowStockThreshold < 0.0) {
            return Result.failure(IllegalArgumentException("Low stock threshold cannot be negative."))
        }

        val now = System.currentTimeMillis()

        ingredientDao.upsertIngredient(
            IngredientEntity(
                ingredientId = ingredientId,
                image = image?.trim()?.ifBlank { null },
                ingredientName = cleanName,
                unitType = cleanUnit,
                estimatedWeightPerUnit = estimatedWeightPerUnit,
                isPackaging = isPackaging,
                lowStockThreshold = lowStockThreshold,
                lastModified = now,
                isSynced = false,
                syncedAt = null
            )
        )

        return Result.success(Unit)
    }

    suspend fun deleteIngredient(ingredientId: Int): Result<Unit> {
        ingredientDao.deleteIngredient(ingredientId)
        return Result.success(Unit)
    }

    suspend fun getUnsyncedIngredients(): List<IngredientEntity> {
        return ingredientDao.getUnsyncedIngredients()
    }

    suspend fun markSynced(ingredientId: Int, syncedAt: Long) {
        ingredientDao.markSynced(ingredientId, syncedAt)
    }

    suspend fun savePulledIngredients(ingredients: List<IngredientEntity>) {
        ingredientDao.upsertIngredients(ingredients.map { it.copy(isSynced = true, syncedAt = it.syncedAt ?: System.currentTimeMillis()) })
    }
}