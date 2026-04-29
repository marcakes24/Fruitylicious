package com.example.fruitylicious.util

import com.example.fruitylicious.data.local.dao.AuditLogDao
import com.example.fruitylicious.data.local.entity.AuditLogEntity
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuditLogger @Inject constructor(
    private val auditLogDao: AuditLogDao,
    private val sessionManager: SessionManager,
    private val branchConfig: BranchConfig
) {

    suspend fun log(
        action: String,
        tableAffected: String,
        userId: Int = sessionManager.getUserId(),
        branchId: Int = sessionManager.getBranchId().takeIf { it > 0 } ?: branchConfig.branchId
    ) {
        val cleanAction = action.trim()
        val cleanTable = tableAffected.trim()

        if (cleanAction.isBlank() || cleanTable.isBlank()) {
            return
        }

        val now = System.currentTimeMillis()

        auditLogDao.upsertAuditLog(
            AuditLogEntity(
                logId = UUID.randomUUID().toString(),
                userId = userId,
                branchId = branchId,
                action = cleanAction,
                tableAffected = cleanTable,
                timestamp = now,
                lastModified = now,
                isSynced = false,
                syncedAt = null
            )
        )
    }
}