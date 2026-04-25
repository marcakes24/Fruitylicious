package com.example.fruitylicious.data.local.dao

import androidx.room.*
import com.example.fruitylicious.data.local.entity.InventoryAdjustmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryAdjustmentDao {

    // ─── Insert / Update / Delete ───────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(adjustment: InventoryAdjustmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(adjustments: List<InventoryAdjustmentEntity>)

    @Update
    suspend fun update(adjustment: InventoryAdjustmentEntity)

    @Delete
    suspend fun delete(adjustment: InventoryAdjustmentEntity)

    @Query("DELETE FROM inventory_adjustments WHERE adjustment_id = :adjustmentId")
    suspend fun deleteById(adjustmentId: String)

    @Query("DELETE FROM inventory_adjustments")
    suspend fun deleteAll()

    // ─── Queries ────────────────────────────────────────────────────────────

    @Query("SELECT * FROM inventory_adjustments WHERE adjustment_id = :adjustmentId")
    suspend fun getById(adjustmentId: String): InventoryAdjustmentEntity?

    @Query("SELECT * FROM inventory_adjustments WHERE branch_id = :branchId ORDER BY date_time DESC")
    fun getByBranch(branchId: String): Flow<List<InventoryAdjustmentEntity>>

    @Query("SELECT * FROM inventory_adjustments WHERE ingredient_id = :ingredientId ORDER BY date_time DESC")
    fun getByIngredient(ingredientId: String): Flow<List<InventoryAdjustmentEntity>>

    @Query("SELECT * FROM inventory_adjustments WHERE user_id = :userId ORDER BY date_time DESC")
    fun getByUser(userId: String): Flow<List<InventoryAdjustmentEntity>>

    @Query("SELECT * FROM inventory_adjustments WHERE date_time BETWEEN :from AND :to ORDER BY date_time DESC")
    fun getByDateRange(from: Long, to: Long): Flow<List<InventoryAdjustmentEntity>>

    @Query("SELECT * FROM inventory_adjustments ORDER BY date_time DESC")
    fun getAll(): Flow<List<InventoryAdjustmentEntity>>

    @Query("SELECT * FROM inventory_adjustments WHERE is_synced = 0")
    suspend fun getUnsynced(): List<InventoryAdjustmentEntity>

    @Query("UPDATE inventory_adjustments SET is_synced = 1, synced_at = :syncedAt WHERE adjustment_id = :adjustmentId")
    suspend fun markSynced(adjustmentId: String, syncedAt: Long)
}