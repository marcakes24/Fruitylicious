package com.example.fruitylicious.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.fruitylicious.data.local.entity.InventoryAdjustmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryAdjustmentDao {

    @Query("SELECT * FROM inventory_adjustments WHERE branchId = :branchId ORDER BY dateTime DESC")
    fun observeAdjustmentsByBranch(branchId: Int): Flow<List<InventoryAdjustmentEntity>>

    @Query("SELECT * FROM inventory_adjustments WHERE branchId = :branchId ORDER BY dateTime DESC")
    suspend fun getAdjustmentsByBranch(branchId: Int): List<InventoryAdjustmentEntity>

    @Query("SELECT * FROM inventory_adjustments WHERE adjustmentId = :adjustmentId LIMIT 1")
    suspend fun getAdjustmentById(adjustmentId: String): InventoryAdjustmentEntity?

    @Query("SELECT * FROM inventory_adjustments WHERE userId = :userId ORDER BY dateTime DESC")
    fun observeAdjustmentsByUser(userId: Int): Flow<List<InventoryAdjustmentEntity>>

    @Query(
        """
        SELECT * FROM inventory_adjustments
        WHERE branchId = :branchId
        AND dateTime BETWEEN :from AND :to
        ORDER BY dateTime DESC
        """
    )
    fun observeAdjustmentsByDateRange(
        branchId: Int,
        from: Long,
        to: Long
    ): Flow<List<InventoryAdjustmentEntity>>

    @Query(
        """
        SELECT * FROM inventory_adjustments
        WHERE branchId = :branchId
        AND dateTime BETWEEN :from AND :to
        ORDER BY dateTime DESC
        """
    )
    suspend fun getAdjustmentsByDateRange(
        branchId: Int,
        from: Long,
        to: Long
    ): List<InventoryAdjustmentEntity>

    @Query("SELECT * FROM inventory_adjustments WHERE isSynced = 0")
    suspend fun getUnsyncedAdjustments(): List<InventoryAdjustmentEntity>

    @Upsert
    suspend fun upsertAdjustment(adjustment: InventoryAdjustmentEntity)

    @Upsert
    suspend fun upsertAdjustments(adjustments: List<InventoryAdjustmentEntity>)

    @Query("UPDATE inventory_adjustments SET isSynced = 1, syncedAt = :syncedAt WHERE adjustmentId = :adjustmentId")
    suspend fun markSynced(adjustmentId: String, syncedAt: Long)

    @Query("DELETE FROM inventory_adjustments WHERE adjustmentId = :adjustmentId")
    suspend fun deleteAdjustment(adjustmentId: String)
}