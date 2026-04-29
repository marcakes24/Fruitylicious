package com.example.fruitylicious.data.repository

import com.example.fruitylicious.data.local.dao.AuditLogDao
import com.example.fruitylicious.data.local.dao.BranchDao
import com.example.fruitylicious.data.local.dao.IngredientDao
import com.example.fruitylicious.data.local.dao.InventoryAdjustmentDao
import com.example.fruitylicious.data.local.dao.InventoryDao
import com.example.fruitylicious.data.local.dao.ProductDao
import com.example.fruitylicious.data.local.dao.ProductRecipeDao
import com.example.fruitylicious.data.local.dao.RestockLogDao
import com.example.fruitylicious.data.local.dao.StaffLogDao
import com.example.fruitylicious.data.local.dao.TransactionDao
import com.example.fruitylicious.data.local.dao.TransactionItemDao
import com.example.fruitylicious.data.local.dao.UserDao
import com.example.fruitylicious.data.local.dao.WasteLogDao
import com.example.fruitylicious.data.remote.api.SyncApi
import com.example.fruitylicious.data.remote.dto.PushRequestDto
import com.example.fruitylicious.util.BranchConfig
import javax.inject.Inject
import javax.inject.Singleton

data class SyncResult(
    val success: Boolean,
    val pushedCount: Int,
    val pulledCount: Int,
    val message: String
)

@Singleton
class SyncRepository @Inject constructor(
    private val syncApi: SyncApi,
    private val branchConfig: BranchConfig,
    private val branchDao: BranchDao,
    private val userDao: UserDao,
    private val productDao: ProductDao,
    private val ingredientDao: IngredientDao,
    private val productRecipeDao: ProductRecipeDao,
    private val inventoryDao: InventoryDao,
    private val restockLogDao: RestockLogDao,
    private val inventoryAdjustmentDao: InventoryAdjustmentDao,
    private val wasteLogDao: WasteLogDao,
    private val transactionDao: TransactionDao,
    private val transactionItemDao: TransactionItemDao,
    private val auditLogDao: AuditLogDao,
    private val staffLogDao: StaffLogDao
) {

    suspend fun sync(lastPulledAt: Long): SyncResult {
        val pushResult = pushUnsynced()

        if (!pushResult.success) {
            return pushResult
        }

        val pullResult = pullUpdates(lastPulledAt)

        return SyncResult(
            success = pullResult.success,
            pushedCount = pushResult.pushedCount,
            pulledCount = pullResult.pulledCount,
            message = if (pullResult.success) {
                "Sync completed successfully."
            } else {
                pullResult.message
            }
        )
    }

    suspend fun pushUnsynced(): SyncResult {
        return try {
            val branches = branchDao.getUnsyncedBranches()
            val users = userDao.getUnsyncedUsers()
            val products = productDao.getUnsyncedProducts()
            val ingredients = ingredientDao.getUnsyncedIngredients()
            val recipes = productRecipeDao.getUnsyncedRecipes()
            val inventory = inventoryDao.getUnsyncedInventory()
            val restockLogs = restockLogDao.getUnsyncedRestockLogs()
            val adjustments = inventoryAdjustmentDao.getUnsyncedAdjustments()
            val wasteLogs = wasteLogDao.getUnsyncedWasteLogs()
            val transactions = transactionDao.getUnsyncedTransactions()
            val transactionItems = transactionItemDao.getUnsyncedTransactionItems()
            val auditLogs = auditLogDao.getUnsyncedAuditLogs()
            val staffLogs = staffLogDao.getUnsyncedStaffLogs()

            val totalCount =
                branches.size +
                        users.size +
                        products.size +
                        ingredients.size +
                        recipes.size +
                        inventory.size +
                        restockLogs.size +
                        adjustments.size +
                        wasteLogs.size +
                        transactions.size +
                        transactionItems.size +
                        auditLogs.size +
                        staffLogs.size

            if (totalCount == 0) {
                return SyncResult(
                    success = true,
                    pushedCount = 0,
                    pulledCount = 0,
                    message = "No local changes to push."
                )
            }

            val response = syncApi.push(
                PushRequestDto(
                    branchId = branchConfig.branchId,
                    pushedAt = System.currentTimeMillis(),
                    branches = branches,
                    users = users,
                    products = products,
                    ingredients = ingredients,
                    recipes = recipes,
                    inventory = inventory,
                    restockLogs = restockLogs,
                    inventoryAdjustments = adjustments,
                    wasteLogs = wasteLogs,
                    transactions = transactions,
                    transactionItems = transactionItems,
                    auditLogs = auditLogs,
                    staffLogs = staffLogs
                )
            )

            if (!response.isSuccessful) {
                return SyncResult(
                    success = false,
                    pushedCount = 0,
                    pulledCount = 0,
                    message = "Push failed: ${response.code()} ${response.message()}"
                )
            }

            val body = response.body()
                ?: return SyncResult(false, 0, 0, "Push failed: empty server response.")

            for (result in body.results) {
                if (result.success) {
                    when (result.tableName) {
                        "branches" -> branchDao.markSynced(result.recordId.toInt(), body.syncedAt)
                        "users" -> userDao.markSynced(result.recordId.toInt(), body.syncedAt)
                        "products" -> productDao.markSynced(result.recordId.toInt(), body.syncedAt)
                        "ingredients" -> ingredientDao.markSynced(result.recordId.toInt(), body.syncedAt)
                        "product_recipes" -> productRecipeDao.markSynced(result.recordId.toInt(), body.syncedAt)
                        "inventory" -> {
                            val ids = result.recordId.split(":")
                            if (ids.size == 2) {
                                inventoryDao.markSynced(ids[0].toInt(), ids[1].toInt(), body.syncedAt)
                            }
                        }
                        "restock_logs" -> restockLogDao.markSynced(result.recordId, body.syncedAt)
                        "inventory_adjustments" -> inventoryAdjustmentDao.markSynced(result.recordId, body.syncedAt)
                        "waste_logs" -> wasteLogDao.markSynced(result.recordId, body.syncedAt)
                        "transactions" -> transactionDao.markSynced(result.recordId, body.syncedAt)
                        "transaction_items" -> transactionItemDao.markSynced(result.recordId, body.syncedAt)
                        "audit_logs" -> auditLogDao.markSynced(result.recordId, body.syncedAt)
                        "staff_logs" -> staffLogDao.markSynced(result.recordId, body.syncedAt)
                    }
                }
            }

            SyncResult(
                success = body.success,
                pushedCount = body.results.count { it.success },
                pulledCount = 0,
                message = body.message ?: "Push completed."
            )
        } catch (exception: Exception) {
            SyncResult(
                success = false,
                pushedCount = 0,
                pulledCount = 0,
                message = exception.message ?: "Push failed."
            )
        }
    }

    suspend fun pullUpdates(since: Long): SyncResult {
        return try {
            val response = syncApi.pull(since)

            if (!response.isSuccessful) {
                return SyncResult(
                    success = false,
                    pushedCount = 0,
                    pulledCount = 0,
                    message = "Pull failed: ${response.code()} ${response.message()}"
                )
            }

            val body = response.body()
                ?: return SyncResult(false, 0, 0, "Pull failed: empty server response.")

            if (!body.success) {
                return SyncResult(false, 0, 0, body.message ?: "Pull failed.")
            }

            branchDao.upsertBranches(body.branches.map { it.copy(isSynced = true, syncedAt = body.pulledAt) })
            userDao.upsertUsers(body.users.map { it.copy(isSynced = true, syncedAt = body.pulledAt) })
            productDao.upsertProducts(body.products.map { it.copy(isSynced = true, syncedAt = body.pulledAt) })
            ingredientDao.upsertIngredients(body.ingredients.map { it.copy(isSynced = true, syncedAt = body.pulledAt) })
            productRecipeDao.upsertRecipes(body.recipes.map { it.copy(isSynced = true, syncedAt = body.pulledAt) })
            inventoryDao.upsertInventoryItems(body.inventory.map { it.copy(isSynced = true, syncedAt = body.pulledAt) })

            val pulledCount =
                body.branches.size +
                        body.users.size +
                        body.products.size +
                        body.ingredients.size +
                        body.recipes.size +
                        body.inventory.size

            SyncResult(
                success = true,
                pushedCount = 0,
                pulledCount = pulledCount,
                message = body.message ?: "Pull completed."
            )
        } catch (exception: Exception) {
            SyncResult(
                success = false,
                pushedCount = 0,
                pulledCount = 0,
                message = exception.message ?: "Pull failed."
            )
        }
    }
}