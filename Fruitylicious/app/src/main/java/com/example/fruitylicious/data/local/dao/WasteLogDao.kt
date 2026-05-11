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

    @Query("SELECT * FROM waste_logs ORDER BY dateTime DESC LIMIT :limit OFFSET :offset")
    suspend fun getAllWasteLogsPaged(limit: Int, offset: Int): List<WasteLogEntity>

    @Query("SELECT * FROM waste_logs WHERE branchId = :branchId ORDER BY dateTime DESC LIMIT :limit OFFSET :offset")
    suspend fun getWasteLogsByBranchPaged(branchId: Int, limit: Int, offset: Int): List<WasteLogEntity>

    @Query("SELECT * FROM waste_logs WHERE branchId = :branchId ORDER BY dateTime DESC")
    fun observeWasteLogsByBranch(branchId: Int): Flow<List<WasteLogEntity>>

    @Query("SELECT * FROM waste_logs WHERE branchId = :branchId ORDER BY dateTime DESC")
    suspend fun getWasteLogsByBranch(branchId: Int): List<WasteLogEntity>

    @Query("SELECT * FROM waste_logs WHERE wasteId = :wasteId")
    suspend fun getWasteLogById(wasteId: String): WasteLogEntity?

    @Query("SELECT * FROM waste_logs WHERE wasteId = :wasteId")
    fun getWasteLogByIdSync(wasteId: String): WasteLogEntity?

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
            UPPER(SUBSTR(reason, 1, 1)) || LOWER(SUBSTR(reason, 2)) AS reason,
            COUNT(*) AS count,
            SUM(CASE WHEN branchId = 1 THEN 1 ELSE 0 END) AS b1Count,
            SUM(CASE WHEN branchId = 2 THEN 1 ELSE 0 END) AS b2Count
        FROM waste_logs
        WHERE dateTime BETWEEN :from AND :to
        AND (:branchId IS NULL OR branchId = :branchId)
        GROUP BY LOWER(reason)
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
            COALESCE(SUM(w.quantity), 0) AS totalQuantity,
            i.unitType AS unitType,
            COALESCE(SUM(CASE WHEN w.branchId = 1 THEN w.quantity ELSE 0 END), 0) AS b1Qty,
            COALESCE(SUM(CASE WHEN w.branchId = 2 THEN w.quantity ELSE 0 END), 0) AS b2Qty
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

    @Query(
        """
        SELECT COUNT(*)
        FROM waste_logs
        WHERE dateTime BETWEEN :from AND :to
        AND (:branchId IS NULL OR branchId = :branchId)
        """
    )
    suspend fun getTotalWasteCount(
        branchId: Int?,
        from: Long,
        to: Long
    ): Int

    @Query(
        """
        SELECT 
            u.name AS staffName,
            COUNT(*) AS count
        FROM waste_logs w
        INNER JOIN users u ON w.userId = u.userId
        WHERE w.dateTime BETWEEN :from AND :to
        AND (:branchId IS NULL OR w.branchId = :branchId)
        GROUP BY w.userId
        ORDER BY count DESC
        """
    )
    suspend fun getStaffWasteActivity(
        branchId: Int?,
        from: Long,
        to: Long
    ): List<StaffWasteRow>
}