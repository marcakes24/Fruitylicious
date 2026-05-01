package com.example.fruitylicious.data.repository

import androidx.room.withTransaction
import com.example.fruitylicious.data.local.dao.AuditLogDao
import com.example.fruitylicious.data.local.dao.InventoryDao
import com.example.fruitylicious.data.local.dao.WasteLogDao
import com.example.fruitylicious.data.local.db.PosDatabase
import com.example.fruitylicious.data.local.entity.AuditLogEntity
import com.example.fruitylicious.data.local.entity.WasteLogEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WasteRepository @Inject constructor(
    private val database: PosDatabase,
    private val wasteLogDao: WasteLogDao,
    private val inventoryDao: InventoryDao,
    private val auditLogDao: AuditLogDao
) {

    fun observeWasteLogs(branchId: Int): Flow<List<WasteLogEntity>> {
        return wasteLogDao.observeWasteLogsByBranch(branchId)
    }

    suspend fun getWasteLogs(branchId: Int): List<WasteLogEntity> {
        return wasteLogDao.getWasteLogsByBranch(branchId)
    }

    suspend fun logWaste(
        ingredientId: Int,
        branchId: Int,
        userId: Int,
        quantity: Double,
        image: String?,
        reason: String
    ): Result<Unit> {
        if (ingredientId <= 0) {
            return Result.failure(IllegalArgumentException("Ingredient is required."))
        }

        if (quantity <= 0.0) {
            return Result.failure(IllegalArgumentException("Quantity must be greater than zero."))
        }

        if (reason.trim().isBlank()) {
            return Result.failure(IllegalArgumentException("Reason is required."))
        }

        val inventory = inventoryDao.getInventoryItem(ingredientId, branchId)
            ?: return Result.failure(IllegalStateException("Inventory item not found."))

        if (inventory.currentStock < quantity) {
            return Result.failure(IllegalStateException("Insufficient stock."))
        }

        val now = System.currentTimeMillis()
        val wasteId = UUID.randomUUID().toString()

        database.withTransaction {
            wasteLogDao.upsertWasteLog(
                WasteLogEntity(
                    wasteId = wasteId,
                    ingredientId = ingredientId,
                    branchId = branchId,
                    userId = userId,
                    quantity = quantity,
                    image = image?.trim()?.ifBlank { null },
                    reason = reason.trim(),
                    dateTime = now,
                    lastModified = now,
                    isSynced = false,
                    syncedAt = null
                )
            )

            inventoryDao.deductStock(
                ingredientId = ingredientId,
                branchId = branchId,
                amount = quantity,
                lastModified = now
            )

            auditLogDao.upsertAuditLog(
                AuditLogEntity(
                    logId = UUID.randomUUID().toString(),
                    userId = userId,
                    branchId = branchId,
                    action = "Logged waste $wasteId for ingredient $ingredientId with quantity $quantity.",
                    tableAffected = "waste_logs",
                    timestamp = now,
                    lastModified = now,
                    isSynced = false,
                    syncedAt = null
                )
            )
        }

        return Result.success(Unit)
    }

    suspend fun getUnsyncedWasteLogs(): List<WasteLogEntity> {
        return wasteLogDao.getUnsyncedWasteLogs()
    }

    suspend fun markSynced(wasteId: String, syncedAt: Long) {
        wasteLogDao.markSynced(wasteId, syncedAt)
    }
}
