package com.example.fruitylicious.data.repository

import androidx.room.withTransaction
import com.example.fruitylicious.data.local.dao.AuditLogDao
import com.example.fruitylicious.data.local.dao.InventoryAdjustmentDao
import com.example.fruitylicious.data.local.dao.InventoryDao
import com.example.fruitylicious.data.local.db.PosDatabase
import com.example.fruitylicious.data.local.entity.AuditLogEntity
import com.example.fruitylicious.data.local.entity.InventoryAdjustmentEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdjustmentRepository @Inject constructor(
    private val database: PosDatabase,
    private val inventoryAdjustmentDao: InventoryAdjustmentDao,
    private val inventoryDao: InventoryDao,
    private val auditLogDao: AuditLogDao
) {

    fun observeAdjustments(branchId: Int): Flow<List<InventoryAdjustmentEntity>> {
        return inventoryAdjustmentDao.observeAdjustmentsByBranch(branchId)
    }

    fun observeAdjustmentsByDateRange(branchId: Int, from: Long, to: Long): Flow<List<InventoryAdjustmentEntity>> {
        return inventoryAdjustmentDao.observeAdjustmentsByDateRange(branchId, from, to)
    }

    suspend fun getAdjustments(branchId: Int): List<InventoryAdjustmentEntity> {
        return inventoryAdjustmentDao.getAdjustmentsByBranch(branchId)
    }

    suspend fun adjustInventory(
        ingredientId: Int,
        branchId: Int,
        userId: Int,
        adjustmentAmount: Double,
        reason: String
    ): Result<Unit> {
        if (ingredientId <= 0) {
            return Result.failure(IllegalArgumentException("Ingredient is required."))
        }

        if (adjustmentAmount == 0.0) {
            return Result.failure(IllegalArgumentException("Adjustment amount cannot be zero."))
        }

        if (reason.trim().isBlank()) {
            return Result.failure(IllegalArgumentException("Reason is required."))
        }

        val inventory = inventoryDao.getInventoryItem(ingredientId, branchId)
            ?: return Result.failure(IllegalStateException("Inventory item not found."))

        val newStock = inventory.currentStock + adjustmentAmount

        if (newStock < 0.0) {
            return Result.failure(IllegalStateException("Adjustment would result in negative stock."))
        }

        val now = System.currentTimeMillis()
        val adjustmentId = UUID.randomUUID().toString()

        database.withTransaction {
            inventoryAdjustmentDao.upsertAdjustment(
                InventoryAdjustmentEntity(
                    adjustmentId = adjustmentId,
                    ingredientId = ingredientId,
                    branchId = branchId,
                    userId = userId,
                    adjustmentAmount = adjustmentAmount,
                    reason = reason.trim(),
                    dateTime = now,
                    lastModified = now,
                    isSynced = false,
                    syncedAt = null
                )
            )

            inventoryDao.setStock(
                ingredientId = ingredientId,
                branchId = branchId,
                currentStock = newStock,
                lastModified = now
            )

            auditLogDao.upsertAuditLog(
                AuditLogEntity(
                    logId = UUID.randomUUID().toString(),
                    userId = userId,
                    branchId = branchId,
                    action = "Adjusted ingredient $ingredientId by $adjustmentAmount. Reason: ${reason.trim()}.",
                    tableAffected = "inventory_adjustments",
                    timestamp = now,
                    lastModified = now,
                    isSynced = false,
                    syncedAt = null
                )
            )
        }

        return Result.success(Unit)
    }

    suspend fun getUnsyncedAdjustments(): List<InventoryAdjustmentEntity> {
        return inventoryAdjustmentDao.getUnsyncedAdjustments()
    }

    suspend fun markSynced(adjustmentId: String, syncedAt: Long) {
        inventoryAdjustmentDao.markSynced(adjustmentId, syncedAt)
    }
}