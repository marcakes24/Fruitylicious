package com.example.fruitylicious.data.repository

import androidx.room.withTransaction
import com.example.fruitylicious.data.local.dao.AuditLogDao
import com.example.fruitylicious.data.local.dao.BranchDao
import com.example.fruitylicious.data.local.dao.IngredientDao
import com.example.fruitylicious.data.local.dao.InventoryDao
import com.example.fruitylicious.data.local.dao.RestockLogDao
import com.example.fruitylicious.data.local.dao.UserDao
import com.example.fruitylicious.data.local.db.PosDatabase
import com.example.fruitylicious.data.local.entity.AuditLogEntity
import com.example.fruitylicious.data.local.entity.BranchEntity
import com.example.fruitylicious.data.local.entity.IngredientEntity
import com.example.fruitylicious.data.local.entity.InventoryEntity
import com.example.fruitylicious.data.local.entity.RestockLogEntity
import com.example.fruitylicious.data.local.entity.UserEntity
import com.example.fruitylicious.sync.AutoSyncManager
import com.example.fruitylicious.util.SessionManager
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RestockRepository @Inject constructor(
    private val database: PosDatabase,
    private val restockLogDao: RestockLogDao,
    private val inventoryDao: InventoryDao,
    private val auditLogDao: AuditLogDao,
    private val userDao: UserDao,
    private val branchDao: BranchDao,
    private val ingredientDao: IngredientDao,
    private val sessionManager: SessionManager,
    private val autoSyncManager: AutoSyncManager
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

        val now = System.currentTimeMillis()
        val restockId = UUID.randomUUID().toString()

        try {
            database.withTransaction {
                // 1. Ensure User exists
                val existingUser = userDao.getUserById(userId)
                if (existingUser == null) {
                    userDao.upsertUser(
                        UserEntity(
                            userId = userId,
                            name = sessionManager.getUserName(),
                            role = sessionManager.getRole() ?: "STAFF",
                            username = sessionManager.getUsername(),
                            password = "",
                            lastModified = now,
                            isSynced = true,
                            syncedAt = now
                        )
                    )
                }

                // 2. Ensure Branch exists
                val branches = branchDao.getAllBranches()
                if (branches.none { it.branchId == branchId }) {
                    branchDao.upsertBranches(
                        listOf(
                            BranchEntity(
                                branchId = branchId,
                                branchName = "Branch $branchId",
                                address = "",
                                contactNumber = "",
                                lastModified = now,
                                isSynced = true,
                                syncedAt = now
                            )
                        )
                    )
                }

                // 3. Ensure Ingredient exists (crucial for restock)
                val existingIngredient = ingredientDao.getIngredientById(ingredientId)
                if (existingIngredient == null) {
                    ingredientDao.upsertIngredient(
                        IngredientEntity(
                            ingredientId = ingredientId,
                            image = null,
                            ingredientName = "Ingredient $ingredientId",
                            unitType = "unit",
                            isPackaging = false,
                            lowStockThreshold = 0.0,
                            lastModified = now,
                            isSynced = true,
                            syncedAt = now
                        )
                    )
                }

                // 4. Ensure Inventory record exists
                val existingInventory = inventoryDao.getInventoryItem(ingredientId, branchId)
                if (existingInventory == null) {
                    inventoryDao.upsertInventoryItem(
                        InventoryEntity(
                            ingredientId = ingredientId,
                            branchId = branchId,
                            currentStock = quantityAdded,
                            lastModified = now,
                            isSynced = false,
                            syncedAt = null
                        )
                    )
                } else {
                    inventoryDao.addStock(
                        ingredientId = ingredientId,
                        branchId = branchId,
                        amount = quantityAdded,
                        lastModified = now
                    )
                }

                // 5. Insert Restock Log
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

                // 6. Audit Log
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
            autoSyncManager.requestSync("restock_saved")
            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    suspend fun getUnsyncedRestockLogs(): List<RestockLogEntity> {
        return restockLogDao.getUnsyncedRestockLogs()
    }

    suspend fun markSynced(restockId: String, syncedAt: Long) {
        restockLogDao.markSynced(restockId, syncedAt)
    }
}