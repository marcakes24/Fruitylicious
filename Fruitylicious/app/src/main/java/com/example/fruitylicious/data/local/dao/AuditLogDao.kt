package com.example.fruitylicious.data.local.dao

import androidx.room.*
import com.example.fruitylicious.data.local.entity.AuditLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditLogDao {

    // ─── Insert / Update / Delete ───────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: AuditLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<AuditLogEntity>)

    @Delete
    suspend fun delete(log: AuditLogEntity)

    @Query("DELETE FROM audit_logs WHERE log_id = :logId")
    suspend fun deleteById(logId: String)

    @Query("DELETE FROM audit_logs")
    suspend fun deleteAll()

    @Query("SELECT * FROM audit_logs WHERE log_id = :logId")
    suspend fun getById(logId: String): AuditLogEntity?

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    fun getAll(): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs WHERE user_id = :userId ORDER BY timestamp DESC")
    fun getByUser(userId: String): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs WHERE branch_id = :branchId ORDER BY timestamp DESC")
    fun getByBranch(branchId: String): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs WHERE table_affected = :tableName ORDER BY timestamp DESC")
    fun getByTable(tableName: String): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs WHERE 'action' = :action ORDER BY timestamp DESC")
    fun getByAction(action: String): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs WHERE timestamp BETWEEN :from AND :to ORDER BY timestamp DESC")
    fun getByDateRange(from: Long, to: Long): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs WHERE is_synced = 0")
    suspend fun getUnsynced(): List<AuditLogEntity>

    @Query("UPDATE audit_logs SET is_synced = 1, synced_at = :syncedAt WHERE log_id = :logId")
    suspend fun markSynced(logId: String, syncedAt: Long)
}