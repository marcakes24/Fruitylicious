package com.example.fruitylicious.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.fruitylicious.data.local.entity.WasteLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WasteLogDao {

    @Query("SELECT * FROM waste_logs WHERE branchId = :branchId ORDER BY dateTime DESC")
    fun observeWasteLogsByBranch(branchId: Int): Flow<List<WasteLogEntity>>

    @Query("SELECT * FROM waste_logs WHERE branchId = :branchId ORDER BY dateTime DESC")
    suspend fun getWasteLogsByBranch(branchId: Int): List<WasteLogEntity>

    @Query("SELECT * FROM waste_logs WHERE wasteId = :wasteId LIMIT 1")
    suspend fun getWasteLogById(wasteId: String): WasteLogEntity?

    @Query("SELECT * FROM waste_logs WHERE userId = :userId ORDER BY dateTime DESC")
    fun observeWasteLogsByUser(userId: Int): Flow<List<WasteLogEntity>>

    @Query(
        """
        SELECT * FROM waste_logs
        WHERE branchId = :branchId
        AND dateTime BETWEEN :from AND :to
        ORDER BY dateTime DESC
        """
    )
    fun observeWasteLogsByDateRange(branchId: Int, from: Long, to: Long): Flow<List<WasteLogEntity>>

    @Query(
        """
        SELECT * FROM waste_logs
        WHERE branchId = :branchId
        AND dateTime BETWEEN :from AND :to
        ORDER BY dateTime DESC
        """
    )
    suspend fun getWasteLogsByDateRange(branchId: Int, from: Long, to: Long): List<WasteLogEntity>

    @Query("SELECT * FROM waste_logs WHERE isSynced = 0")
    suspend fun getUnsyncedWasteLogs(): List<WasteLogEntity>

    @Upsert
    suspend fun upsertWasteLog(wasteLog: WasteLogEntity)

    @Upsert
    suspend fun upsertWasteLogs(wasteLogs: List<WasteLogEntity>)

    @Query("UPDATE waste_logs SET isSynced = 1, syncedAt = :syncedAt WHERE wasteId = :wasteId")
    suspend fun markSynced(wasteId: String, syncedAt: Long)

    @Query("DELETE FROM waste_logs WHERE wasteId = :wasteId")
    suspend fun deleteWasteLog(wasteId: String)
}
