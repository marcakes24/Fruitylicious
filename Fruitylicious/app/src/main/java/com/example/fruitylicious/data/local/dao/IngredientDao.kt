package com.example.fruitylicious.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.fruitylicious.data.local.entity.IngredientEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IngredientDao {

    @Query("SELECT * FROM ingredients WHERE isDeleted = 0 ORDER BY ingredientName ASC")
    fun observeAllIngredients(): Flow<List<IngredientEntity>>

    @Query("SELECT * FROM ingredients WHERE isDeleted = 0 ORDER BY ingredientName ASC")
    suspend fun getAllIngredients(): List<IngredientEntity>


    @Query("SELECT * FROM ingredients WHERE ingredientId = :ingredientId AND isDeleted = 0 LIMIT 1")
    fun observeIngredient(ingredientId: Int): Flow<IngredientEntity?>

    @Query("SELECT * FROM ingredients WHERE ingredientId = :ingredientId AND isDeleted = 0")
    suspend fun getIngredientById(ingredientId: Int): IngredientEntity?

    @Query("SELECT * FROM ingredients WHERE isPackaging = :isPackaging AND isDeleted = 0 ORDER BY ingredientName ASC")
    fun observeIngredientsByPackaging(isPackaging: Boolean): Flow<List<IngredientEntity>>

    @Query("SELECT * FROM ingredients WHERE ingredientName LIKE '%' || :query || '%' AND isDeleted = 0 ORDER BY ingredientName ASC")
    fun searchIngredients(query: String): Flow<List<IngredientEntity>>

    @Query("SELECT * FROM ingredients WHERE isSynced = 0")
    suspend fun getUnsyncedIngredients(): List<IngredientEntity>

    @Upsert
    suspend fun upsertIngredient(ingredient: IngredientEntity)

    @Upsert
    suspend fun upsertIngredients(ingredients: List<IngredientEntity>)

    @Query("UPDATE ingredients SET isSynced = 1, syncedAt = :syncedAt WHERE ingredientId = :ingredientId")
    suspend fun markSynced(ingredientId: Int, syncedAt: Long)

    @Query(
        """
        UPDATE ingredients
        SET image = :imagePath,
            lastModified = :lastModified,
            isSynced = 0,
            syncedAt = NULL
        WHERE ingredientId = :ingredientId
        """
    )
    suspend fun updateIngredientImage(
        ingredientId: Int,
        imagePath: String?,
        lastModified: Long
    )

    @Query("SELECT * FROM ingredients WHERE isDeleted = 0 ORDER BY ingredientName ASC")
    fun observeIngredients(): Flow<List<IngredientEntity>>

    @Query(
        """
    UPDATE ingredients
    SET isDeleted = 1,
        deletedAt = :now,
        lastModified = :now,
        isSynced = 0,
        syncedAt = NULL
    WHERE ingredientId = :ingredientId
    """
    )
    suspend fun softDeleteIngredient(
        ingredientId: Int,
        now: Long
    )
}