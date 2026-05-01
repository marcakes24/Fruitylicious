package com.example.fruitylicious.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.fruitylicious.data.local.entity.WasteLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WasteLogDao {

    @Query("SELECT * FROM waste_logs ORDER BY dateTime DESC")
    fun observeAllWasteLogs(): Flow<List<WasteLogEntity>>

    @Query("SELECT * FROM waste_logs WHERE branchId = :branchId ORDER BY dateTime DESC")
    fun observeWasteLogsByBranch(branchId: Int): Flow<List<WasteLogEntity>>

    @Query("SELECT * FROM waste_logs WHERE branchId = :branchId ORDER BY dateTime DESC")
    suspend fun getWasteLogsByBranch(branchId: Int): List<WasteLogEntity>

    @Upsert
    suspend fun upsertWasteLog(wasteLog: WasteLogEntity)

    @Upsert
    suspend fun upsertWasteLogs(wasteLogs: List<WasteLogEntity>)

    @Query("DELETE FROM waste_logs WHERE wasteId = :wasteId")
    suspend fun deleteWasteLog(wasteId: String)

    @Query("SELECT * FROM waste_logs WHERE isSynced = 0")
    suspend fun getUnsyncedWasteLogs(): List<WasteLogEntity>

    @Query(
        """
        UPDATE waste_logs
        SET isSynced = 1, syncedAt = :syncedAt
        WHERE wasteId = :wasteId
        """
    )
    suspend fun markSynced(
        wasteId: String,
        syncedAt: Long
    )

    @Query(
        """
        SELECT COALESCE(SUM(quantity), 0)
        FROM waste_logs
        WHERE dateTime BETWEEN :from AND :to
        AND (:branchId IS NULL OR branchId = :branchId)
        """
    )
    suspend fun getTotalWasteQuantity(
        branchId: Int?,
        from: Long,
        to: Long
    ): Double

    @Query(
        """
        SELECT 
            reason AS reason,
            COUNT(*) AS count
        FROM waste_logs
        WHERE dateTime BETWEEN :from AND :to
        AND (:branchId IS NULL OR branchId = :branchId)
        GROUP BY reason
        ORDER BY count DESC
        """
    )
    suspend fun getWasteReasonReport(
        branchId: Int?,
        from: Long,
        to: Long
    ): List<WasteReasonRow>

    @Query(
        """
        SELECT 
            i.ingredientName AS ingredientName,
            COALESCE(SUM(w.quantity), 0) AS totalQuantity
        FROM waste_logs w
        INNER JOIN ingredients i ON w.ingredientId = i.ingredientId
        WHERE w.dateTime BETWEEN :from AND :to
        AND (:branchId IS NULL OR w.branchId = :branchId)
        GROUP BY w.ingredientId
        ORDER BY totalQuantity DESC
        """
    )
    suspend fun getWasteByItemReport(
        branchId: Int?,
        from: Long,
        to: Long
    ): List<WasteItemRow>
}