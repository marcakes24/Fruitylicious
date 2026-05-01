package com.example.fruitylicious.data.repository

import com.example.fruitylicious.data.local.dao.AuditLogDao
import com.example.fruitylicious.data.local.entity.AuditLogEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuditLogRepository @Inject constructor(
    private val auditLogDao: AuditLogDao
) {

    fun observeAuditLogs(branchId: Int): Flow<List<AuditLogEntity>> {
        return auditLogDao.observeAuditLogsByBranch(branchId)
    }

    suspend fun log(
        userId: Int,
        branchId: Int,
        action: String,
        tableAffected: String
    ): Result<Unit> {
        if (action.trim().isBlank()) {
            return Result.failure(IllegalArgumentException("Action is required."))
        }

        if (tableAffected.trim().isBlank()) {
            return Result.failure(IllegalArgumentException("Table affected is required."))
        }

        val now = System.currentTimeMillis()

        auditLogDao.upsertAuditLog(
            AuditLogEntity(
                logId = UUID.randomUUID().toString(),
                userId = userId,
                branchId = branchId,
                action = action.trim(),
                tableAffected = tableAffected.trim(),
                timestamp = now,
                lastModified = now,
                isSynced = false,
                syncedAt = null
            )
        )

        return Result.success(Unit)
    }

    suspend fun getUnsyncedAuditLogs(): List<AuditLogEntity> {
        return auditLogDao.getUnsyncedAuditLogs()
    }

    suspend fun markSynced(logId: String, syncedAt: Long) {
        auditLogDao.markSynced(logId, syncedAt)
    }
}