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
import com.example.fruitylicious.data.remote.dto.SyncRecordResultDto
import com.example.fruitylicious.util.BranchConfig
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.getOrNull

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
        return pushUnsynced()
    }

    suspend fun pushUnsynced(): SyncResult {
        return try {
            val inventory = inventoryDao.getUnsyncedInventory()
            val restockLogs = restockLogDao.getUnsyncedRestockLogs()
            val adjustments = inventoryAdjustmentDao.getUnsyncedAdjustments()
            val wasteLogs = wasteLogDao.getUnsyncedWasteLogs()
            val transactions = transactionDao.getUnsyncedTransactions()
            val transactionItems = transactionItemDao.getUnsyncedTransactionItems()
            val auditLogs = auditLogDao.getUnsyncedAuditLogs()
            val staffLogs = staffLogDao.getUnsyncedStaffLogs()

            val totalCount =
                inventory.size +
                        restockLogs.size +
                        adjustments.size +
                        wasteLogs.size +
                        transactions.size +
                        transactionItems.size +
                        auditLogs.size +
                        staffLogs.size

            println(
                "FRUITY_PUSH_COUNT: inventory=${inventory.size}, " +
                        "restockLogs=${restockLogs.size}, " +
                        "adjustments=${adjustments.size}, " +
                        "wasteLogs=${wasteLogs.size}, " +
                        "transactions=${transactions.size}, " +
                        "transactionItems=${transactionItems.size}, " +
                        "auditLogs=${auditLogs.size}, " +
                        "staffLogs=${staffLogs.size}, " +
                        "total=$totalCount"
            )

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

            println("FRUITY_PUSH_RESPONSE: code=${response.code()}, successful=${response.isSuccessful}")

            if (!response.isSuccessful) {
                return SyncResult(
                    success = false,
                    pushedCount = 0,
                    pulledCount = 0,
                    message = "Push failed: ${response.code()} ${response.message()}"
                )
            }

            val body = response.body()

            val syncedAt = System.currentTimeMillis()

            val inventoryResults = body?.inventory.orEmpty()
            val restockResults = body?.restockLogs.orEmpty()
            val adjustmentResults = body?.inventoryAdjustments.orEmpty()
            val wasteResults = body?.wasteLogs.orEmpty()
            val transactionResults = body?.transactions.orEmpty()
            val transactionItemResults = body?.transactionItems.orEmpty()
            val auditResults = body?.auditLogs.orEmpty()
            val staffResults = body?.staffLogs.orEmpty()

            inventory.forEachIndexed { index, item ->
                if (isPushSuccess(inventoryResults, index)) {
                    inventoryDao.markSynced(
                        ingredientId = item.ingredientId,
                        branchId = item.branchId,
                        syncedAt = syncedAt
                    )
                }
            }

            restockLogs.forEachIndexed { index, item ->
                if (isPushSuccess(restockResults, index)) {
                    restockLogDao.markSynced(
                        restockId = item.restockId,
                        syncedAt = syncedAt
                    )
                }
            }

            adjustments.forEachIndexed { index, item ->
                if (isPushSuccess(adjustmentResults, index)) {
                    inventoryAdjustmentDao.markSynced(
                        adjustmentId = item.adjustmentId,
                        syncedAt = syncedAt
                    )
                }
            }

            wasteLogs.forEachIndexed { index, item ->
                if (isPushSuccess(wasteResults, index)) {
                    wasteLogDao.markSynced(
                        wasteId = item.wasteId,
                        syncedAt = syncedAt
                    )
                }
            }

            transactions.forEachIndexed { index, item ->
                if (isPushSuccess(transactionResults, index)) {
                    transactionDao.markSynced(
                        transactionId = item.transactionId,
                        syncedAt = syncedAt
                    )
                }
            }

            transactionItems.forEachIndexed { index, item ->
                if (isPushSuccess(transactionItemResults, index)) {
                    transactionItemDao.markSynced(
                        transactionItemId = item.transactionItemId,
                        syncedAt = syncedAt
                    )
                }
            }

            auditLogs.forEachIndexed { index, item ->
                if (isPushSuccess(auditResults, index)) {
                    auditLogDao.markSynced(
                        logId = item.logId,
                        syncedAt = syncedAt
                    )
                }
            }

            staffLogs.forEachIndexed { index, item ->
                if (isPushSuccess(staffResults, index)) {
                    staffLogDao.markSynced(
                        logId = item.logId,
                        syncedAt = syncedAt
                    )
                }
            }

            val pushedCount =
                countSuccess(inventoryResults, inventory.size) +
                        countSuccess(restockResults, restockLogs.size) +
                        countSuccess(adjustmentResults, adjustments.size) +
                        countSuccess(wasteResults, wasteLogs.size) +
                        countSuccess(transactionResults, transactions.size) +
                        countSuccess(transactionItemResults, transactionItems.size) +
                        countSuccess(auditResults, auditLogs.size) +
                        countSuccess(staffResults, staffLogs.size)

            SyncResult(
                success = true,
                pushedCount = pushedCount,
                pulledCount = 0,
                message = "Push completed. Pushed $pushedCount of $totalCount local records."
            )
        } catch (exception: Exception) {
            println("FRUITY_PUSH_ERROR: ${exception.message}")

            SyncResult(
                success = false,
                pushedCount = 0,
                pulledCount = 0,
                message = exception.message ?: "Push failed."
            )
        }
    }

    suspend fun pullUpdates(since: String): SyncResult {
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
                ?: return SyncResult(
                    success = false,
                    pushedCount = 0,
                    pulledCount = 0,
                    message = "Pull failed: empty server response."
                )

            val pulledAt = System.currentTimeMillis()

            val pulledBranches = body.branches.orEmpty()
            val pulledUsers = body.users.orEmpty()
            val pulledProducts = body.products.orEmpty()
            val pulledIngredients = body.ingredients.orEmpty()
            val pulledRecipes = body.recipes.orEmpty()

            branchDao.upsertBranches(
                pulledBranches.map {
                    it.copy(
                        isSynced = true,
                        syncedAt = pulledAt
                    )
                }
            )

            userDao.upsertUsers(
                pulledUsers.map {
                    it.copy(
                        isSynced = true,
                        syncedAt = pulledAt
                    )
                }
            )

            productDao.upsertProducts(
                pulledProducts.map {
                    it.copy(
                        isSynced = true,
                        syncedAt = pulledAt
                    )
                }
            )

            ingredientDao.upsertIngredients(
                pulledIngredients.map {
                    it.copy(
                        isSynced = true,
                        syncedAt = pulledAt
                    )
                }
            )

            productRecipeDao.upsertRecipes(
                pulledRecipes.map {
                    it.copy(
                        isSynced = true,
                        syncedAt = pulledAt
                    )
                }
            )

            val pulledCount =
                pulledBranches.size +
                        pulledUsers.size +
                        pulledProducts.size +
                        pulledIngredients.size +
                        pulledRecipes.size

            SyncResult(
                success = true,
                pushedCount = 0,
                pulledCount = pulledCount,
                message = "Pull completed. Pulled $pulledCount records."
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

    private fun isPushSuccess(
        results: List<SyncRecordResultDto>,
        index: Int
    ): Boolean {
        return if (results.isEmpty()) {
            true
        } else {
            results.getOrNull(index)?.success == true
        }
    }

    private fun countSuccess(
        results: List<SyncRecordResultDto>,
        submittedCount: Int
    ): Int {
        return if (results.isEmpty()) {
            submittedCount
        } else {
            results.count { it.success }
        }
    }
}