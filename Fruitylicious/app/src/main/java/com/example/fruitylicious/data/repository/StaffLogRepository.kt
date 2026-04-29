package com.example.fruitylicious.data.repository

import androidx.room.withTransaction
import com.example.fruitylicious.data.local.dao.AuditLogDao
import com.example.fruitylicious.data.local.dao.StaffLogDao
import com.example.fruitylicious.data.local.db.PosDatabase
import com.example.fruitylicious.data.local.entity.AuditLogEntity
import com.example.fruitylicious.data.local.entity.StaffLogEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StaffLogRepository @Inject constructor(
    private val database: PosDatabase,
    private val staffLogDao: StaffLogDao,
    private val auditLogDao: AuditLogDao
) {

    fun observeStaffLogs(branchId: Int): Flow<List<StaffLogEntity>> {
        return staffLogDao.observeStaffLogsByBranch(branchId)
    }

    fun observeStaffLogsByUser(userId: Int): Flow<List<StaffLogEntity>> {
        return staffLogDao.observeStaffLogsByUser(userId)
    }

    fun observeStaffLogsByDateRange(branchId: Int, from: Long, to: Long): Flow<List<StaffLogEntity>> {
        return staffLogDao.observeStaffLogsByDateRange(branchId, from, to)
    }

    suspend fun getStaffLogs(branchId: Int): List<StaffLogEntity> {
        return staffLogDao.getStaffLogsByBranch(branchId)
    }

    suspend fun getOpenStaffLog(userId: Int, branchId: Int): StaffLogEntity? {
        return staffLogDao.getOpenStaffLog(userId, branchId)
    }

    suspend fun clockIn(
        userId: Int,
        branchId: Int,
        image: String?
    ): Result<Unit> {
        val openLog = staffLogDao.getOpenStaffLog(userId, branchId)

        if (openLog != null) {
            return Result.failure(IllegalStateException("Staff is already clocked in."))
        }

        val now = System.currentTimeMillis()
        val logId = UUID.randomUUID().toString()

        database.withTransaction {
            staffLogDao.upsertStaffLog(
                StaffLogEntity(
                    logId = logId,
                    userId = userId,
                    branchId = branchId,
                    image = image?.trim()?.ifBlank { null },
                    clockIn = now,
                    clockOut = null,
                    lastModified = now,
                    isSynced = false,
                    syncedAt = null
                )
            )

            auditLogDao.upsertAuditLog(
                AuditLogEntity(
                    logId = UUID.randomUUID().toString(),
                    userId = userId,
                    branchId = branchId,
                    action = "Clocked in.",
                    tableAffected = "staff_logs",
                    timestamp = now,
                    lastModified = now,
                    isSynced = false,
                    syncedAt = null
                )
            )
        }

        return Result.success(Unit)
    }

    suspend fun clockOut(userId: Int, branchId: Int): Result<Unit> {
        val openLog = staffLogDao.getOpenStaffLog(userId, branchId)
            ?: return Result.failure(IllegalStateException("No active clock-in found."))

        val now = System.currentTimeMillis()

        database.withTransaction {
            staffLogDao.clockOut(
                logId = openLog.logId,
                clockOut = now,
                lastModified = now
            )

            auditLogDao.upsertAuditLog(
                AuditLogEntity(
                    logId = UUID.randomUUID().toString(),
                    userId = userId,
                    branchId = branchId,
                    action = "Clocked out.",
                    tableAffected = "staff_logs",
                    timestamp = now,
                    lastModified = now,
                    isSynced = false,
                    syncedAt = null
                )
            )
        }

        return Result.success(Unit)
    }

    suspend fun getUnsyncedStaffLogs(): List<StaffLogEntity> {
        return staffLogDao.getUnsyncedStaffLogs()
    }

    suspend fun markSynced(logId: String, syncedAt: Long) {
        staffLogDao.markSynced(logId, syncedAt)
    }
}