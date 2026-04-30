package com.example.fruitylicious.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.fruitylicious.data.local.entity.ProductRecipeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductRecipeDao {

    @Query("SELECT * FROM product_recipes ORDER BY productId ASC")
    fun observeRecipes(): Flow<List<ProductRecipeEntity>>

    @Query("SELECT * FROM product_recipes ORDER BY productId ASC")
    suspend fun getRecipes(): List<ProductRecipeEntity>

    @Query("SELECT * FROM product_recipes WHERE recipeId = :recipeId")
    suspend fun getRecipeById(recipeId: Int): ProductRecipeEntity?

    @Query("SELECT * FROM product_recipes WHERE productId = :productId ORDER BY recipeId ASC")
    fun observeRecipesForProduct(productId: Int): Flow<List<ProductRecipeEntity>>

    @Query(
        """
        SELECT * FROM product_recipes
        WHERE productId = :productId AND variantId IS NULL
        ORDER BY recipeId ASC
        """
    )
    suspend fun getRecipesForProduct(productId: Int): List<ProductRecipeEntity>

    @Query("SELECT * FROM product_recipes WHERE variantId = :variantId ORDER BY recipeId ASC")
    fun observeRecipesForVariant(variantId: Int): Flow<List<ProductRecipeEntity>>

    @Query("SELECT * FROM product_recipes WHERE variantId = :variantId ORDER BY recipeId ASC")
    suspend fun getRecipesForVariant(variantId: Int): List<ProductRecipeEntity>

    @Query("SELECT * FROM product_recipes WHERE ingredientId = :ingredientId ORDER BY recipeId ASC")
    fun observeRecipesUsingIngredient(ingredientId: Int): Flow<List<ProductRecipeEntity>>

    @Upsert
    suspend fun upsertRecipe(recipe: ProductRecipeEntity)

    @Upsert
    suspend fun upsertRecipes(recipes: List<ProductRecipeEntity>)

    @Query("DELETE FROM product_recipes WHERE recipeId = :recipeId")
    suspend fun deleteRecipe(recipeId: Int)

    @Query("DELETE FROM product_recipes WHERE productId = :productId")
    suspend fun deleteRecipesForProduct(productId: Int)

    @Query("DELETE FROM product_recipes WHERE variantId = :variantId")
    suspend fun deleteRecipesForVariant(variantId: Int)

    @Query("SELECT * FROM product_recipes WHERE isSynced = 0")
    suspend fun getUnsyncedRecipes(): List<ProductRecipeEntity>

    @Query("UPDATE product_recipes SET isSynced = 1, syncedAt = :syncedAt WHERE recipeId = :recipeId")
    suspend fun markSynced(recipeId: Int, syncedAt: Long)
}