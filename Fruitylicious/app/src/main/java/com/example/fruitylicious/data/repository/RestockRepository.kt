package com.example.fruitylicious.data.repository

import androidx.room.withTransaction
import com.example.fruitylicious.data.local.dao.AuditLogDao
import com.example.fruitylicious.data.local.dao.InventoryDao
import com.example.fruitylicious.data.local.dao.RestockLogDao
import com.example.fruitylicious.data.local.db.PosDatabase
import com.example.fruitylicious.data.local.entity.AuditLogEntity
import com.example.fruitylicious.data.local.entity.RestockLogEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RestockRepository @Inject constructor(
    private val database: PosDatabase,
    private val restockLogDao: RestockLogDao,
    private val inventoryDao: InventoryDao,
    private val auditLogDao: AuditLogDao
) {

    fun observeRestockLogs(branchId: Int): Flow<List<RestockLogEntity>> {
        return restockLogDao.observeRestockLogsByBranch(branchId)
    }

    fun observeRestockLogsByDateRange(branchId: Int, from: Long, to: Long): Flow<List<RestockLogEntity>> {
        return restockLogDao.observeRestockLogsByDateRange(branchId, from, to)
    }

    suspend fun getRestockLogs(branchId: Int): List<RestockLogEntity> {
        return restockLogDao.getRestockLogsByBranch(branchId)
    }

    suspend fun restock(
        ingredientId: Int,
        branchId: Int,
        userId: Int,
        quantityAdded: Double,
        supplier: String
    ): Result<Unit> {
        if (ingredientId <= 0) {
            return Result.failure(IllegalArgumentException("Ingredient is required."))
        }

        if (quantityAdded <= 0.0) {
            return Result.failure(IllegalArgumentException("Quantity added must be greater than zero."))
        }

        if (supplier.trim().isBlank()) {
            return Result.failure(IllegalArgumentException("Supplier is required."))
        }

        val inventory = inventoryDao.getInventoryItem(ingredientId, branchId)
            ?: return Result.failure(IllegalStateException("Inventory item not found."))

        val now = System.currentTimeMillis()
        val restockId = UUID.randomUUID().toString()

        database.withTransaction {
            restockLogDao.upsertRestockLog(
                RestockLogEntity(
                    restockId = restockId,
                    ingredientId = ingredientId,
                    branchId = branchId,
                    userId = userId,
                    quantityAdded = quantityAdded,
                    supplier = supplier.trim(),
                    dateTime = now,
                    lastModified = now,
                    isSynced = false,
                    syncedAt = null
                )
            )

            inventoryDao.addStock(
                ingredientId = inventory.ingredientId,
                branchId = inventory.branchId,
                amount = quantityAdded,
                lastModified = now
            )

            auditLogDao.upsertAuditLog(
                AuditLogEntity(
                    logId = UUID.randomUUID().toString(),
                    userId = userId,
                    branchId = branchId,
                    action = "Restocked ingredient $ingredientId with quantity $quantityAdded from ${supplier.trim()}.",
                    tableAffected = "restock_logs",
                    timestamp = now,
                    lastModified = now,
                    isSynced = false,
                    syncedAt = null
                )
            )
        }

        return Result.success(Unit)
    }

    suspend fun getUnsyncedRestockLogs(): List<RestockLogEntity> {
        return restockLogDao.getUnsyncedRestockLogs()
    }

    suspend fun markSynced(restockId: String, syncedAt: Long) {
        restockLogDao.markSynced(restockId, syncedAt)
    }
}