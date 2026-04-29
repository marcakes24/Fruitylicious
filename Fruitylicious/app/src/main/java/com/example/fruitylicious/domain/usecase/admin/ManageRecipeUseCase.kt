package com.example.fruitylicious.domain.usecase.admin

import com.example.fruitylicious.data.local.entity.ProductRecipeEntity
import com.example.fruitylicious.data.repository.RecipeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ManageRecipeUseCase @Inject constructor(
    private val recipeRepository: RecipeRepository
) {

    fun observeRecipes(): Flow<List<ProductRecipeEntity>> {
        return recipeRepository.observeRecipes()
    }

    fun observeRecipesForProduct(productId: Int): Flow<List<ProductRecipeEntity>> {
        return recipeRepository.observeRecipesForProduct(productId)
    }

    suspend fun getRecipe(recipeId: Int): ProductRecipeEntity? {
        return recipeRepository.getRecipe(recipeId)
    }

    suspend fun saveRecipe(
        recipeId: Int,
        productId: Int,
        ingredientId: Int,
        quantityRequired: Double
    ): Result<Unit> {
        return recipeRepository.saveRecipe(
            recipeId = recipeId,
            productId = productId,
            ingredientId = ingredientId,
            quantityRequired = quantityRequired
        )
    }

    suspend fun deleteRecipe(recipeId: Int): Result<Unit> {
        return recipeRepository.deleteRecipe(recipeId)
    }

    suspend fun deleteRecipesForProduct(productId: Int): Result<Unit> {
        return recipeRepository.deleteRecipesForProduct(productId)
    }
}