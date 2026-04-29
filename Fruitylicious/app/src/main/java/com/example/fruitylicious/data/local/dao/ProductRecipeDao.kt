package com.example.fruitylicious.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.fruitylicious.data.local.entity.ProductRecipeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductRecipeDao {

    @Query("SELECT * FROM product_recipes ORDER BY productId ASC, ingredientId ASC")
    fun observeAllRecipes(): Flow<List<ProductRecipeEntity>>

    @Query("SELECT * FROM product_recipes ORDER BY productId ASC, ingredientId ASC")
    suspend fun getAllRecipes(): List<ProductRecipeEntity>

    @Query("SELECT * FROM product_recipes WHERE recipeId = :recipeId LIMIT 1")
    suspend fun getRecipeById(recipeId: Int): ProductRecipeEntity?

    @Query("SELECT * FROM product_recipes WHERE productId = :productId ORDER BY ingredientId ASC")
    fun observeRecipesForProduct(productId: Int): Flow<List<ProductRecipeEntity>>

    @Query("SELECT * FROM product_recipes WHERE productId = :productId ORDER BY ingredientId ASC")
    suspend fun getRecipesForProduct(productId: Int): List<ProductRecipeEntity>

    @Query("SELECT * FROM product_recipes WHERE ingredientId = :ingredientId ORDER BY productId ASC")
    fun observeRecipesUsingIngredient(ingredientId: Int): Flow<List<ProductRecipeEntity>>

    @Query("SELECT * FROM product_recipes WHERE isSynced = 0")
    suspend fun getUnsyncedRecipes(): List<ProductRecipeEntity>

    @Upsert
    suspend fun upsertRecipe(recipe: ProductRecipeEntity)

    @Upsert
    suspend fun upsertRecipes(recipes: List<ProductRecipeEntity>)

    @Query("UPDATE product_recipes SET isSynced = 1, syncedAt = :syncedAt WHERE recipeId = :recipeId")
    suspend fun markSynced(recipeId: Int, syncedAt: Long)

    @Query("DELETE FROM product_recipes WHERE recipeId = :recipeId")
    suspend fun deleteRecipe(recipeId: Int)

    @Query("DELETE FROM product_recipes WHERE productId = :productId")
    suspend fun deleteRecipesForProduct(productId: Int)
}