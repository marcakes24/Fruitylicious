package com.example.fruitylicious.data.repository

import androidx.room.withTransaction
import com.example.fruitylicious.data.local.dao.AuditLogDao
import com.example.fruitylicious.data.local.dao.BranchDao
import com.example.fruitylicious.data.local.dao.IngredientDao
import com.example.fruitylicious.data.local.dao.InventoryAdjustmentDao
import com.example.fruitylicious.data.local.dao.InventoryDao
import com.example.fruitylicious.data.local.dao.UserDao
import com.example.fruitylicious.data.local.db.PosDatabase
import com.example.fruitylicious.data.local.entity.AuditLogEntity
import com.example.fruitylicious.data.local.entity.BranchEntity
import com.example.fruitylicious.data.local.entity.IngredientEntity
import com.example.fruitylicious.data.local.entity.InventoryAdjustmentEntity
import com.example.fruitylicious.data.local.entity.UserEntity
import com.example.fruitylicious.sync.AutoSyncManager
import com.example.fruitylicious.util.SessionManager
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdjustmentRepository @Inject constructor(
    private val database: PosDatabase,
    private val inventoryAdjustmentDao: InventoryAdjustmentDao,
    private val inventoryDao: InventoryDao,
    private val auditLogDao: AuditLogDao,
    private val userDao: UserDao,
    private val branchDao: BranchDao,
    private val ingredientDao: IngredientDao,
    private val sessionManager: SessionManager,
    private val autoSyncManager: AutoSyncManager
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

        val now = System.currentTimeMillis()
        val adjustmentId = UUID.randomUUID().toString()

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

                // 3. Ensure Ingredient exists
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

                val existingInventory = inventoryDao.getInventoryItem(ingredientId, branchId)
                val currentStock = existingInventory?.currentStock ?: 0.0
                val newStock = currentStock + adjustmentAmount

                if (newStock < 0.0) {
                    throw IllegalStateException("Adjustment would result in negative stock.")
                }

                // 4. Update Inventory
                if (existingInventory == null) {
                    inventoryDao.upsertInventoryItem(
                        com.example.fruitylicious.data.local.entity.InventoryEntity(
                            ingredientId = ingredientId,
                            branchId = branchId,
                            currentStock = newStock,
                            lastModified = now,
                            isSynced = false,
                            syncedAt = null
                        )
                    )
                } else {
                    inventoryDao.setStock(
                        ingredientId = ingredientId,
                        branchId = branchId,
                        currentStock = newStock,
                        lastModified = now
                    )
                }

                // 5. Insert Adjustment Log
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

                // 6. Audit Log
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
            autoSyncManager.requestSync("inventory_adjustment_saved")
            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    suspend fun getUnsyncedAdjustments(): List<InventoryAdjustmentEntity> {
        return inventoryAdjustmentDao.getUnsyncedAdjustments()
    }

    suspend fun markSynced(adjustmentId: String, syncedAt: Long) {
        inventoryAdjustmentDao.markSynced(adjustmentId, syncedAt)
    }
}