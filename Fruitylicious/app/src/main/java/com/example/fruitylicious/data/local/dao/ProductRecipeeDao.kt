package com.example.fruitylicious.data.local.dao

import androidx.room.*
import com.example.fruitylicious.data.local.entity.ProductRecipeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductRecipeDao {

    // ─── Insert / Update / Delete ───────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recipe: ProductRecipeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(recipes: List<ProductRecipeEntity>)

    @Update
    suspend fun update(recipe: ProductRecipeEntity)

    @Delete
    suspend fun delete(recipe: ProductRecipeEntity)

    @Query("DELETE FROM product_recipes WHERE recipe_id = :recipeId")
    suspend fun deleteById(recipeId: String)

    @Query("DELETE FROM product_recipes WHERE product_id = :productId")
    suspend fun deleteByProductId(productId: String)

    @Query("DELETE FROM product_recipes")
    suspend fun deleteAll()

    // ─── Queries ────────────────────────────────────────────────────────────

    @Query("SELECT * FROM product_recipes WHERE recipe_id = :recipeId")
    suspend fun getById(recipeId: String): ProductRecipeEntity?

    @Query("SELECT * FROM product_recipes WHERE product_id = :productId")
    fun getByProductId(productId: String): Flow<List<ProductRecipeEntity>>

    @Query("SELECT * FROM product_recipes WHERE ingredient_id = :ingredientId")
    fun getByIngredientId(ingredientId: String): Flow<List<ProductRecipeEntity>>

    @Query("SELECT * FROM product_recipes")
    fun getAll(): Flow<List<ProductRecipeEntity>>

    @Query("SELECT * FROM product_recipes WHERE is_synced = 0")
    suspend fun getUnsynced(): List<ProductRecipeEntity>

    @Query("UPDATE product_recipes SET is_synced = 1, synced_at = :syncedAt WHERE recipe_id = :recipeId")
    suspend fun markSynced(recipeId: String, syncedAt: Long)
}