package com.example.fruitylicious.data.local.dao

import androidx.room.*
import com.example.fruitylicious.data.local.entity.StaffLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StaffLogDao {

    // ─── Insert / Update / Delete ───────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: StaffLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<StaffLogEntity>)

    @Update
    suspend fun update(log: StaffLogEntity)

    @Delete
    suspend fun delete(log: StaffLogEntity)

    @Query("DELETE FROM staff_logs WHERE log_id = :logId")
    suspend fun deleteById(logId: String)

    @Query("DELETE FROM staff_logs")
    suspend fun deleteAll()

    // ─── Queries ────────────────────────────────────────────────────────────

    @Query("SELECT * FROM staff_logs WHERE log_id = :logId")
    suspend fun getById(logId: String): StaffLogEntity?

    @Query("SELECT * FROM staff_logs WHERE user_id = :userId ORDER BY clock_in DESC")
    fun getByUser(userId: String): Flow<List<StaffLogEntity>>

    @Query("SELECT * FROM staff_logs WHERE branch_id = :branchId ORDER BY clock_in DESC")
    fun getByBranch(branchId: String): Flow<List<StaffLogEntity>>

    @Query("SELECT * FROM staff_logs WHERE clock_in BETWEEN :from AND :to ORDER BY clock_in DESC")
    fun getByDateRange(from: Long, to: Long): Flow<List<StaffLogEntity>>

    @Query("SELECT * FROM staff_logs WHERE branch_id = :branchId AND clock_in BETWEEN :from AND :to ORDER BY clock_in DESC")
    fun getByBranchAndDateRange(branchId: String, from: Long, to: Long): Flow<List<StaffLogEntity>>

    /** Get the latest open (not yet clocked out) session for a user */
    @Query("SELECT * FROM staff_logs WHERE user_id = :userId AND clock_out IS NULL ORDER BY clock_in DESC LIMIT 1")
    suspend fun getActiveSession(userId: String): StaffLogEntity?

    /** Clock out — set the clock_out timestamp for an active session */
    @Query("UPDATE staff_logs SET clock_out = :clockOut, last_modified = :lastModified WHERE log_id = :logId")
    suspend fun clockOut(logId: String, clockOut: Long, lastModified: Long = System.currentTimeMillis())

    @Query("SELECT * FROM staff_logs ORDER BY clock_in DESC")
    fun getAll(): Flow<List<StaffLogEntity>>

    @Query("SELECT * FROM staff_logs WHERE is_synced = 0")
    suspend fun getUnsynced(): List<StaffLogEntity>

    @Query("UPDATE staff_logs SET is_synced = 1, synced_at = :syncedAt WHERE log_id = :logId")
    suspend fun markSynced(logId: String, syncedAt: Long)
}