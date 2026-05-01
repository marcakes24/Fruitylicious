package com.example.fruitylicious.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.fruitylicious.data.local.entity.IngredientEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IngredientDao {

    @Query("SELECT * FROM ingredients ORDER BY ingredientName ASC")
    fun observeAllIngredients(): Flow<List<IngredientEntity>>

    @Query("SELECT * FROM ingredients ORDER BY ingredientName ASC")
    suspend fun getAllIngredients(): List<IngredientEntity>

    @Query("SELECT * FROM ingredients WHERE ingredientId = :ingredientId LIMIT 1")
    fun observeIngredient(ingredientId: Int): Flow<IngredientEntity?>

    @Query("SELECT * FROM ingredients WHERE ingredientId = :ingredientId")
    suspend fun getIngredientById(ingredientId: Int): IngredientEntity?

    @Query("SELECT * FROM ingredients WHERE isPackaging = :isPackaging ORDER BY ingredientName ASC")
    fun observeIngredientsByPackaging(isPackaging: Boolean): Flow<List<IngredientEntity>>

    @Query("SELECT * FROM ingredients WHERE ingredientName LIKE '%' || :query || '%' ORDER BY ingredientName ASC")
    fun searchIngredients(query: String): Flow<List<IngredientEntity>>

    @Query("SELECT * FROM ingredients WHERE isSynced = 0")
    suspend fun getUnsyncedIngredients(): List<IngredientEntity>

    @Upsert
    suspend fun upsertIngredient(ingredient: IngredientEntity)

    @Upsert
    suspend fun upsertIngredients(ingredients: List<IngredientEntity>)

    @Query("UPDATE ingredients SET isSynced = 1, syncedAt = :syncedAt WHERE ingredientId = :ingredientId")
    suspend fun markSynced(ingredientId: Int, syncedAt: Long)

    @Query("DELETE FROM ingredients WHERE ingredientId = :ingredientId")
    suspend fun deleteIngredient(ingredientId: Int)

    @Query("SELECT * FROM ingredients ORDER BY ingredientName ASC")
    fun observeIngredients(): Flow<List<IngredientEntity>>
}