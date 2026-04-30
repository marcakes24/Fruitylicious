package com.example.fruitylicious.data.repository

import com.example.fruitylicious.data.local.dao.ProductRecipeDao
import com.example.fruitylicious.data.local.entity.ProductRecipeEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecipeRepository @Inject constructor(
    private val productRecipeDao: ProductRecipeDao
) {

    fun observeRecipes(): Flow<List<ProductRecipeEntity>> {
        return productRecipeDao.observeRecipes()
    }

    fun observeRecipesForProduct(productId: Int): Flow<List<ProductRecipeEntity>> {
        return productRecipeDao.observeRecipesForProduct(productId)
    }

    fun observeRecipesForVariant(variantId: Int): Flow<List<ProductRecipeEntity>> {
        return productRecipeDao.observeRecipesForVariant(variantId)
    }

    fun observeRecipesUsingIngredient(ingredientId: Int): Flow<List<ProductRecipeEntity>> {
        return productRecipeDao.observeRecipesUsingIngredient(ingredientId)
    }

    suspend fun getRecipes(): List<ProductRecipeEntity> {
        return productRecipeDao.getRecipes()
    }

    suspend fun getRecipe(recipeId: Int): ProductRecipeEntity? {
        return productRecipeDao.getRecipeById(recipeId)
    }

    suspend fun getRecipesForProduct(productId: Int): List<ProductRecipeEntity> {
        return productRecipeDao.getRecipesForProduct(productId)
    }

    suspend fun getRecipesForVariant(variantId: Int): List<ProductRecipeEntity> {
        return productRecipeDao.getRecipesForVariant(variantId)
    }

    suspend fun saveRecipe(
        recipeId: Int,
        productId: Int,
        variantId: Int?,
        ingredientId: Int,
        quantityRequired: Double
    ): Result<Unit> {
        if (productId <= 0) {
            return Result.failure(IllegalArgumentException("Product is required."))
        }

        if (ingredientId <= 0) {
            return Result.failure(IllegalArgumentException("Ingredient is required."))
        }

        if (quantityRequired <= 0.0) {
            return Result.failure(IllegalArgumentException("Quantity required must be greater than zero."))
        }

        val now = System.currentTimeMillis()

        productRecipeDao.upsertRecipe(
            ProductRecipeEntity(
                recipeId = recipeId,
                productId = productId,
                variantId = variantId,
                ingredientId = ingredientId,
                quantityRequired = quantityRequired,
                lastModified = now,
                isSynced = false,
                syncedAt = null
            )
        )

        return Result.success(Unit)
    }

    suspend fun deleteRecipe(recipeId: Int): Result<Unit> {
        productRecipeDao.deleteRecipe(recipeId)
        return Result.success(Unit)
    }

    suspend fun deleteRecipesForProduct(productId: Int): Result<Unit> {
        productRecipeDao.deleteRecipesForProduct(productId)
        return Result.success(Unit)
    }

    suspend fun deleteRecipesForVariant(variantId: Int): Result<Unit> {
        productRecipeDao.deleteRecipesForVariant(variantId)
        return Result.success(Unit)
    }

    suspend fun getUnsyncedRecipes(): List<ProductRecipeEntity> {
        return productRecipeDao.getUnsyncedRecipes()
    }

    suspend fun markSynced(recipeId: Int, syncedAt: Long) {
        productRecipeDao.markSynced(recipeId, syncedAt)
    }

    suspend fun savePulledRecipes(recipes: List<ProductRecipeEntity>) {
        val syncedAt = System.currentTimeMillis()

        productRecipeDao.upsertRecipes(
            recipes.map {
                it.copy(
                    isSynced = true,
                    syncedAt = it.syncedAt ?: syncedAt
                )
            }
        )
    }
}