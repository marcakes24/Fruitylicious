package com.example.fruitylicious.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.fruitylicious.data.local.entity.AuditLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditLogDao {

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    fun observeAllAuditLogs(): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs WHERE branchId = :branchId ORDER BY timestamp DESC")
    fun observeAuditLogsByBranch(branchId: Int): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs WHERE logId = :logId")
    suspend fun getAuditLogById(logId: String): AuditLogEntity?

    @Query("SELECT * FROM audit_logs WHERE isSynced = 0")
    suspend fun getUnsyncedAuditLogs(): List<AuditLogEntity>

    @Upsert
    suspend fun upsertAuditLog(auditLog: AuditLogEntity)

    @Upsert
    suspend fun upsertAuditLogs(auditLogs: List<AuditLogEntity>)

    @Query(
        """
        UPDATE audit_logs
        SET isSynced = 1, syncedAt = :syncedAt
        WHERE logId = :logId
        """
    )
    suspend fun markSynced(
        logId: String,
        syncedAt: Long
    )
}