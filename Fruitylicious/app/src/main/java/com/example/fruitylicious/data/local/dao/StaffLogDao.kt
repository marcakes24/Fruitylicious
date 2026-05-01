package com.example.fruitylicious.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.fruitylicious.data.local.entity.StaffLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StaffLogDao {

    @Query("SELECT * FROM staff_logs WHERE branchId = :branchId ORDER BY clockIn DESC")
    fun observeStaffLogsByBranch(branchId: Int): Flow<List<StaffLogEntity>>

    @Query("SELECT * FROM staff_logs WHERE branchId = :branchId ORDER BY clockIn DESC")
    suspend fun getStaffLogsByBranch(branchId: Int): List<StaffLogEntity>

    @Query("SELECT * FROM staff_logs WHERE userId = :userId ORDER BY clockIn DESC")
    fun observeStaffLogsByUser(userId: Int): Flow<List<StaffLogEntity>>

    @Query(
        """
        SELECT * FROM staff_logs
        WHERE userId = :userId
        AND branchId = :branchId
        AND clockOut IS NULL
        ORDER BY clockIn DESC
        LIMIT 1
        """
    )
    suspend fun getOpenStaffLog(userId: Int, branchId: Int): StaffLogEntity?

    @Query(
        """
        SELECT * FROM staff_logs
        WHERE branchId = :branchId
        AND clockIn BETWEEN :from AND :to
        ORDER BY clockIn DESC
        """
    )
    fun observeStaffLogsByDateRange(branchId: Int, from: Long, to: Long): Flow<List<StaffLogEntity>>

    @Query("SELECT * FROM staff_logs WHERE isSynced = 0")
    suspend fun getUnsyncedStaffLogs(): List<StaffLogEntity>

    @Upsert
    suspend fun upsertStaffLog(staffLog: StaffLogEntity)

    @Query(
        """
        UPDATE staff_logs
        SET clockOut = :clockOut,
            lastModified = :lastModified,
            isSynced = 0,
            syncedAt = NULL
        WHERE logId = :logId
        """
    )
    suspend fun clockOut(logId: String, clockOut: Long, lastModified: Long)

    @Query("UPDATE staff_logs SET isSynced = 1, syncedAt = :syncedAt WHERE logId = :logId")
    suspend fun markSynced(logId: String, syncedAt: Long)

    @Query("SELECT * FROM staff_logs ORDER BY clockIn DESC")
    fun observeAllStaffLogs(): Flow<List<StaffLogEntity>>

    @Query("SELECT * FROM staff_logs WHERE userId = :userId ORDER BY clockIn DESC")
    fun observeLogsByUser(userId: Int): Flow<List<StaffLogEntity>>

    @Query(
        """
    SELECT * FROM staff_logs
    WHERE userId = :userId
    AND clockOut IS NULL
    ORDER BY clockIn DESC
    LIMIT 1
    """
    )
    suspend fun getActiveLogForUser(userId: Int): StaffLogEntity?
}