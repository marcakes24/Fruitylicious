package com.example.fruitylicious.data.local.dao

import androidx.room.*
import com.example.fruitylicious.data.local.entity.IngredientEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IngredientDao {

    // ─── Insert / Update / Delete ───────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(ingredient: IngredientEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(ingredients: List<IngredientEntity>)

    @Update
    suspend fun update(ingredient: IngredientEntity)

    @Delete
    suspend fun delete(ingredient: IngredientEntity)

    @Query("DELETE FROM ingredients WHERE ingredient_id = :ingredientId")
    suspend fun deleteById(ingredientId: String)

    @Query("DELETE FROM ingredients")
    suspend fun deleteAll()

    // ─── Queries ────────────────────────────────────────────────────────────

    @Query("SELECT * FROM ingredients WHERE ingredient_id = :ingredientId")
    suspend fun getById(ingredientId: String): IngredientEntity?

    @Query("SELECT * FROM ingredients ORDER BY ingredient_name ASC")
    fun getAll(): Flow<List<IngredientEntity>>

    @Query("SELECT * FROM ingredients WHERE is_packaging = 0 ORDER BY ingredient_name ASC")
    fun getRawIngredients(): Flow<List<IngredientEntity>>

    @Query("SELECT * FROM ingredients WHERE is_packaging = 1 ORDER BY ingredient_name ASC")
    fun getPackagingItems(): Flow<List<IngredientEntity>>

    @Query("SELECT * FROM ingredients WHERE unit_type = :unitType ORDER BY ingredient_name ASC")
    fun getByUnitType(unitType: String): Flow<List<IngredientEntity>>

    @Query("SELECT * FROM ingredients WHERE is_synced = 0")
    suspend fun getUnsynced(): List<IngredientEntity>

    @Query("UPDATE ingredients SET is_synced = 1, synced_at = :syncedAt WHERE ingredient_id = :ingredientId")
    suspend fun markSynced(ingredientId: String, syncedAt: Long)
}