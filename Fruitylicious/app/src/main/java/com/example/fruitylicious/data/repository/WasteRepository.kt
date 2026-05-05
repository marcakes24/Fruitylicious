package com.example.fruitylicious.data.repository

import androidx.room.withTransaction
import com.example.fruitylicious.data.local.dao.AuditLogDao
import com.example.fruitylicious.data.local.dao.BranchDao
import com.example.fruitylicious.data.local.dao.IngredientDao
import com.example.fruitylicious.data.local.dao.InventoryDao
import com.example.fruitylicious.data.local.dao.UserDao
import com.example.fruitylicious.data.local.dao.WasteLogDao
import com.example.fruitylicious.data.local.db.PosDatabase
import com.example.fruitylicious.data.local.entity.AuditLogEntity
import com.example.fruitylicious.data.local.entity.BranchEntity
import com.example.fruitylicious.data.local.entity.IngredientEntity
import com.example.fruitylicious.data.local.entity.UserEntity
import com.example.fruitylicious.data.local.entity.WasteLogEntity
import com.example.fruitylicious.sync.AutoSyncManager
import com.example.fruitylicious.util.SessionManager
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WasteRepository @Inject constructor(
    private val database: PosDatabase,
    private val wasteLogDao: WasteLogDao,
    private val inventoryDao: InventoryDao,
    private val auditLogDao: AuditLogDao,
    private val userDao: UserDao,
    private val branchDao: BranchDao,
    private val ingredientDao: IngredientDao,
    private val sessionManager: SessionManager,
    private val autoSyncManager: AutoSyncManager
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
        image: String,
        reason: String
    ): Result<Unit> {
        if (ingredientId <= 0) {
            return Result.failure(IllegalArgumentException("Ingredient is required."))
        }

        if (quantity <= 0.0) {
            return Result.failure(IllegalArgumentException("Quantity must be greater than zero."))
        }

        if (image.trim().isBlank()) {
            return Result.failure(IllegalArgumentException("Image is required."))
        }

        if (reason.trim().isBlank()) {
            return Result.failure(IllegalArgumentException("Reason is required."))
        }

        val now = System.currentTimeMillis()
        val wasteId = UUID.randomUUID().toString()

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
                            estimatedWeightPerUnit = 1.0,
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

                if (currentStock < quantity) {
                    throw IllegalStateException("Insufficient stock.")
                }

                // 4. Update Inventory
                inventoryDao.deductStock(
                    ingredientId = ingredientId,
                    branchId = branchId,
                    amount = quantity,
                    lastModified = now
                )

                // 5. Insert Waste Log
                wasteLogDao.upsertWasteLog(
                    WasteLogEntity(
                        wasteId = wasteId,
                        ingredientId = ingredientId,
                        branchId = branchId,
                        userId = userId,
                        quantity = quantity,
                        image = image,
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
                        action = "Logged waste for ingredient $ingredientId: quantity $quantity. Reason: ${reason.trim()}.",
                        tableAffected = "waste_logs",
                        timestamp = now,
                        lastModified = now,
                        isSynced = false,
                        syncedAt = null
                    )
                )
            }
            autoSyncManager.requestSync("waste_saved")
            return Result.success(Unit)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            return Result.failure(e)
        }
    }

    suspend fun getUnsyncedWasteLogs(): List<WasteLogEntity> {
        return wasteLogDao.getUnsyncedWasteLogs()
    }

    suspend fun markSynced(wasteId: String, syncedAt: Long) {
        wasteLogDao.markSynced(wasteId, syncedAt)
    }
}
