package com.example.fruitylicious.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.fruitylicious.data.local.entity.AuditLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditLogDao {

    @Query("SELECT * FROM audit_logs WHERE branchId = :branchId ORDER BY timestamp DESC")
    fun observeAuditLogsByBranch(branchId: Int): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs WHERE branchId = :branchId ORDER BY timestamp DESC")
    suspend fun getAuditLogsByBranch(branchId: Int): List<AuditLogEntity>

    @Query("SELECT * FROM audit_logs WHERE logId = :logId LIMIT 1")
    suspend fun getAuditLogById(logId: String): AuditLogEntity?

    @Query("SELECT * FROM audit_logs WHERE userId = :userId ORDER BY timestamp DESC")
    fun observeAuditLogsByUser(userId: Int): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs WHERE 'action' LIKE '%' || :action || '%' ORDER BY timestamp DESC")
    fun observeAuditLogsByAction(action: String): Flow<List<AuditLogEntity>>

    @Query(
        """
        SELECT * FROM audit_logs
        WHERE branchId = :branchId
        AND timestamp BETWEEN :from AND :to
        ORDER BY timestamp DESC
        """
    )
    fun observeAuditLogsByDateRange(branchId: Int, from: Long, to: Long): Flow<List<AuditLogEntity>>

    @Query(
        """
        SELECT * FROM audit_logs
        WHERE branchId = :branchId
        AND timestamp BETWEEN :from AND :to
        ORDER BY timestamp DESC
        """
    )
    suspend fun getAuditLogsByDateRange(branchId: Int, from: Long, to: Long): List<AuditLogEntity>

    @Query("SELECT * FROM audit_logs WHERE isSynced = 0")
    suspend fun getUnsyncedAuditLogs(): List<AuditLogEntity>

    @Upsert
    suspend fun upsertAuditLog(auditLog: AuditLogEntity)

    @Upsert
    suspend fun upsertAuditLogs(auditLogs: List<AuditLogEntity>)

    @Query("UPDATE audit_logs SET isSynced = 1, syncedAt = :syncedAt WHERE logId = :logId")
    suspend fun markSynced(logId: String, syncedAt: Long)

    @Query("DELETE FROM audit_logs WHERE logId = :logId")
    suspend fun deleteAuditLog(logId: String)
}