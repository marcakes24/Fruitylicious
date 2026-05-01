package com.example.fruitylicious.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.fruitylicious.data.local.entity.RestockLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RestockLogDao {

    @Query("SELECT * FROM restock_logs WHERE branchId = :branchId ORDER BY dateTime DESC")
    fun observeRestockLogsByBranch(branchId: Int): Flow<List<RestockLogEntity>>

    @Query("SELECT * FROM restock_logs WHERE branchId = :branchId ORDER BY dateTime DESC")
    suspend fun getRestockLogsByBranch(branchId: Int): List<RestockLogEntity>

    @Query(
        """
        SELECT * FROM restock_logs
        WHERE branchId = :branchId
        AND dateTime BETWEEN :from AND :to
        ORDER BY dateTime DESC
        """
    )
    fun observeRestockLogsByDateRange(branchId: Int, from: Long, to: Long): Flow<List<RestockLogEntity>>

    @Query("SELECT * FROM restock_logs WHERE isSynced = 0")
    suspend fun getUnsyncedRestockLogs(): List<RestockLogEntity>

    @Upsert
    suspend fun upsertRestockLog(restockLog: RestockLogEntity)

    @Upsert
    suspend fun upsertRestockLogs(restockLogs: List<RestockLogEntity>)

    @Query("UPDATE restock_logs SET isSynced = 1, syncedAt = :syncedAt WHERE restockId = :restockId")
    suspend fun markSynced(restockId: String, syncedAt: Long)

    @Query("DELETE FROM restock_logs WHERE restockId = :restockId")
    suspend fun deleteRestockLog(restockId: String)

    @Query("SELECT * FROM restock_logs ORDER BY dateTime DESC")
    fun observeAllRestockLogs(): Flow<List<RestockLogEntity>>

    @Query(
        """
    SELECT COALESCE(SUM(quantityAdded), 0)
    FROM restock_logs
    WHERE dateTime BETWEEN :from AND :to
    AND (:branchId IS NULL OR branchId = :branchId)
    """
    )
    suspend fun getTotalRestockedUnits(
        branchId: Int?,
        from: Long,
        to: Long
    ): Double

    @Query(
        """
    SELECT 
        i.ingredientName AS ingredientName,
        COUNT(*) AS restockCount,
        COALESCE(AVG(r.quantityAdded), 0) AS avgUnits
    FROM restock_logs r
    INNER JOIN ingredients i ON r.ingredientId = i.ingredientId
    WHERE (:branchId IS NULL OR r.branchId = :branchId)
    GROUP BY r.ingredientId
    ORDER BY restockCount DESC, ingredientName ASC
    """
    )
    suspend fun getRestockFrequencyReport(
        branchId: Int?
    ): List<RestockFrequencyRow>
}