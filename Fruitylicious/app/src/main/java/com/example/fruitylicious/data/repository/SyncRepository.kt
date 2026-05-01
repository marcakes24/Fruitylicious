package com.example.fruitylicious.data.repository

import androidx.room.withTransaction
import com.example.fruitylicious.data.local.dao.AuditLogDao
import com.example.fruitylicious.data.local.dao.BranchDao
import com.example.fruitylicious.data.local.dao.IngredientDao
import com.example.fruitylicious.data.local.dao.InventoryAdjustmentDao
import com.example.fruitylicious.data.local.dao.InventoryDao
import com.example.fruitylicious.data.local.dao.ProductDao
import com.example.fruitylicious.data.local.dao.ProductRecipeDao
import com.example.fruitylicious.data.local.dao.ProductVariantDao
import com.example.fruitylicious.data.local.dao.RestockLogDao
import com.example.fruitylicious.data.local.dao.StaffLogDao
import com.example.fruitylicious.data.local.dao.TransactionDao
import com.example.fruitylicious.data.local.dao.TransactionItemAddonDao
import com.example.fruitylicious.data.local.dao.TransactionItemDao
import com.example.fruitylicious.data.local.dao.UserDao
import com.example.fruitylicious.data.local.dao.WasteLogDao
import com.example.fruitylicious.data.local.db.PosDatabase
import com.example.fruitylicious.data.remote.api.SyncApi
import com.example.fruitylicious.data.remote.dto.PushRequestDto
import com.example.fruitylicious.data.remote.dto.SyncRecordResultDto
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
    private val database: PosDatabase,
    private val syncApi: SyncApi,
    private val branchDao: BranchDao,
    private val userDao: UserDao,
    private val productDao: ProductDao,
    private val productVariantDao: ProductVariantDao,
    private val ingredientDao: IngredientDao,
    private val productRecipeDao: ProductRecipeDao,
    private val inventoryDao: InventoryDao,
    private val restockLogDao: RestockLogDao,
    private val inventoryAdjustmentDao: InventoryAdjustmentDao,
    private val wasteLogDao: WasteLogDao,
    private val transactionDao: TransactionDao,
    private val transactionItemDao: TransactionItemDao,
    private val transactionItemAddonDao: TransactionItemAddonDao,
    private val auditLogDao: AuditLogDao,
    private val staffLogDao: StaffLogDao
) {

    suspend fun sync(lastPulledAt: Long): SyncResult {
        val pushResult = pushUnsynced()

        if (!pushResult.success) {
            return pushResult
        }

        val pullResult = pullUpdates(lastPulledAt)

        if (!pullResult.success) {
            return SyncResult(
                success = false,
                pushedCount = pushResult.pushedCount,
                pulledCount = 0,
                message = "Push succeeded, but pull failed: ${pullResult.message}"
            )
        }

        return SyncResult(
            success = true,
            pushedCount = pushResult.pushedCount,
            pulledCount = pullResult.pulledCount,
            message = "Sync completed. Pushed ${pushResult.pushedCount}, pulled ${pullResult.pulledCount}."
        )
    }

    suspend fun pushUnsynced(): SyncResult {
        return try {
            val branches = branchDao.getUnsyncedBranches()
            val users = userDao.getUnsyncedUsers()
            val products = productDao.getUnsyncedProducts()
            val productVariants = productVariantDao.getUnsyncedVariants()
            val ingredients = ingredientDao.getUnsyncedIngredients()
            val recipes = productRecipeDao.getUnsyncedRecipes()
            val inventory = inventoryDao.getUnsyncedInventory()
            val restockLogs = restockLogDao.getUnsyncedRestockLogs()
            val adjustments = inventoryAdjustmentDao.getUnsyncedAdjustments()
            val wasteLogs = wasteLogDao.getUnsyncedWasteLogs()
            val transactions = transactionDao.getUnsyncedTransactions()
            val transactionItems = transactionItemDao.getUnsyncedTransactionItems()
            val transactionItemAddons = transactionItemAddonDao.getUnsyncedTransactionItemAddons()
            val staffLogs = staffLogDao.getUnsyncedStaffLogs()
            val auditLogs = auditLogDao.getUnsyncedAuditLogs()

            val totalCount =
                branches.size +
                        users.size +
                        products.size +
                        productVariants.size +
                        ingredients.size +
                        recipes.size +
                        inventory.size +
                        restockLogs.size +
                        adjustments.size +
                        wasteLogs.size +
                        transactions.size +
                        transactionItems.size +
                        transactionItemAddons.size +
                        staffLogs.size +
                        auditLogs.size

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
                    branches = branches,
                    users = users,
                    products = products,
                    productVariants = productVariants,
                    ingredients = ingredients,
                    productRecipes = recipes,
                    inventory = inventory,
                    restockLogs = restockLogs,
                    inventoryAdjustments = adjustments,
                    wasteLogs = wasteLogs,
                    transactions = transactions,
                    transactionItems = transactionItems,
                    transactionItemAddons = transactionItemAddons,
                    staffLogs = staffLogs,
                    auditLogs = auditLogs
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
                ?: return SyncResult(
                    success = false,
                    pushedCount = 0,
                    pulledCount = 0,
                    message = "Push failed: empty server response."
                )

            val syncedAt = System.currentTimeMillis()

            val pushedCount =
                markBranchesSynced(body.branches.orEmpty(), syncedAt) +
                        markUsersSynced(body.users.orEmpty(), syncedAt) +
                        markProductsSynced(body.products.orEmpty(), syncedAt) +
                        markProductVariantsSynced(body.productVariants.orEmpty(), syncedAt) +
                        markIngredientsSynced(body.ingredients.orEmpty(), syncedAt) +
                        markRecipesSynced(body.productRecipes.orEmpty(), syncedAt) +
                        markInventorySynced(body.inventory.orEmpty(), syncedAt) +
                        markRestockLogsSynced(body.restockLogs.orEmpty(), syncedAt) +
                        markAdjustmentsSynced(body.inventoryAdjustments.orEmpty(), syncedAt) +
                        markWasteLogsSynced(body.wasteLogs.orEmpty(), syncedAt) +
                        markTransactionsSynced(body.transactions.orEmpty(), syncedAt) +
                        markTransactionItemsSynced(body.transactionItems.orEmpty(), syncedAt) +
                        markTransactionItemAddonsSynced(body.transactionItemAddons.orEmpty(), syncedAt) +
                        markStaffLogsSynced(body.staffLogs.orEmpty(), syncedAt) +
                        markAuditLogsSynced(body.auditLogs.orEmpty(), syncedAt)

            SyncResult(
                success = true,
                pushedCount = pushedCount,
                pulledCount = 0,
                message = "Push completed. Pushed $pushedCount of $totalCount local records."
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
            val pulledProductVariants = body.productVariants.orEmpty()
            val pulledIngredients = body.ingredients.orEmpty()
            val pulledRecipes = body.productRecipes.orEmpty()
            val pulledInventory = body.inventory.orEmpty()
            val pulledRestockLogs = body.restockLogs.orEmpty()
            val pulledAdjustments = body.inventoryAdjustments.orEmpty()
            val pulledWasteLogs = body.wasteLogs.orEmpty()
            val pulledTransactions = body.transactions.orEmpty()
            val pulledTransactionItems = body.transactionItems.orEmpty()
            val pulledTransactionItemAddons = body.transactionItemAddons.orEmpty()
            val pulledStaffLogs = body.staffLogs.orEmpty()
            val pulledAuditLogs = body.auditLogs.orEmpty()

            database.withTransaction {
                branchDao.upsertBranches(
                    pulledBranches.map {
                        it.copy(isSynced = true, syncedAt = pulledAt)
                    }
                )

                userDao.upsertUsers(
                    pulledUsers.map {
                        it.copy(isSynced = true, syncedAt = pulledAt)
                    }
                )

                productDao.upsertProducts(
                    pulledProducts.map {
                        it.copy(isSynced = true, syncedAt = pulledAt)
                    }
                )

                productVariantDao.upsertVariants(
                    pulledProductVariants.map {
                        it.copy(isSynced = true, syncedAt = pulledAt)
                    }
                )

                ingredientDao.upsertIngredients(
                    pulledIngredients.map {
                        it.copy(isSynced = true, syncedAt = pulledAt)
                    }
                )

                productRecipeDao.upsertRecipes(
                    pulledRecipes.map {
                        it.copy(isSynced = true, syncedAt = pulledAt)
                    }
                )

                inventoryDao.upsertInventoryItems(
                    pulledInventory.map {
                        it.copy(isSynced = true, syncedAt = pulledAt)
                    }
                )

                restockLogDao.upsertRestockLogs(
                    pulledRestockLogs.map {
                        it.copy(isSynced = true, syncedAt = pulledAt)
                    }
                )

                pulledAdjustments.forEach {
                    inventoryAdjustmentDao.upsertAdjustment(
                        it.copy(isSynced = true, syncedAt = pulledAt)
                    )
                }

                wasteLogDao.upsertWasteLogs(
                    pulledWasteLogs.map {
                        it.copy(isSynced = true, syncedAt = pulledAt)
                    }
                )

                transactionDao.upsertTransactions(
                    pulledTransactions.map {
                        it.copy(isSynced = true, syncedAt = pulledAt)
                    }
                )

                pulledTransactionItems.forEach {
                    transactionItemDao.upsertTransactionItem(
                        it.copy(isSynced = true, syncedAt = pulledAt)
                    )
                }

                transactionItemAddonDao.upsertAddons(
                    pulledTransactionItemAddons.map {
                        it.copy(isSynced = true, syncedAt = pulledAt)
                    }
                )

                pulledStaffLogs.forEach {
                    staffLogDao.upsertStaffLog(
                        it.copy(isSynced = true, syncedAt = pulledAt)
                    )
                }

                pulledAuditLogs.forEach {
                    auditLogDao.upsertAuditLog(
                        it.copy(isSynced = true, syncedAt = pulledAt)
                    )
                }
            }

            val pulledCount =
                pulledBranches.size +
                        pulledUsers.size +
                        pulledProducts.size +
                        pulledProductVariants.size +
                        pulledIngredients.size +
                        pulledRecipes.size +
                        pulledInventory.size +
                        pulledRestockLogs.size +
                        pulledAdjustments.size +
                        pulledWasteLogs.size +
                        pulledTransactions.size +
                        pulledTransactionItems.size +
                        pulledTransactionItemAddons.size +
                        pulledStaffLogs.size +
                        pulledAuditLogs.size

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

    private fun successIds(results: List<SyncRecordResultDto>): Set<String> {
        return results
            .filter { it.success }
            .map { it.recordId }
            .toSet()
    }

    private suspend fun markBranchesSynced(results: List<SyncRecordResultDto>, syncedAt: Long): Int {
        val ids = successIds(results)
        ids.forEach { branchDao.markSynced(it.toInt(), syncedAt) }
        return ids.size
    }

    private suspend fun markUsersSynced(results: List<SyncRecordResultDto>, syncedAt: Long): Int {
        val ids = successIds(results)
        ids.forEach { userDao.markSynced(it.toInt(), syncedAt) }
        return ids.size
    }

    private suspend fun markProductsSynced(results: List<SyncRecordResultDto>, syncedAt: Long): Int {
        val ids = successIds(results)
        ids.forEach { productDao.markSynced(it.toInt(), syncedAt) }
        return ids.size
    }

    private suspend fun markProductVariantsSynced(results: List<SyncRecordResultDto>, syncedAt: Long): Int {
        val ids = successIds(results)
        ids.forEach { productVariantDao.markSynced(it.toInt(), syncedAt) }
        return ids.size
    }

    private suspend fun markIngredientsSynced(results: List<SyncRecordResultDto>, syncedAt: Long): Int {
        val ids = successIds(results)
        ids.forEach { ingredientDao.markSynced(it.toInt(), syncedAt) }
        return ids.size
    }

    private suspend fun markRecipesSynced(results: List<SyncRecordResultDto>, syncedAt: Long): Int {
        val ids = successIds(results)
        ids.forEach { productRecipeDao.markSynced(it.toInt(), syncedAt) }
        return ids.size
    }

    private suspend fun markInventorySynced(results: List<SyncRecordResultDto>, syncedAt: Long): Int {
        val ids = successIds(results)

        ids.forEach { recordId ->
            val parts = recordId.split(":")
            val ingredientId = parts.getOrNull(0)?.toIntOrNull()
            val branchId = parts.getOrNull(1)?.toIntOrNull()

            if (ingredientId != null && branchId != null) {
                inventoryDao.markSynced(
                    ingredientId = ingredientId,
                    branchId = branchId,
                    syncedAt = syncedAt
                )
            }
        }

        return ids.size
    }

    private suspend fun markRestockLogsSynced(results: List<SyncRecordResultDto>, syncedAt: Long): Int {
        val ids = successIds(results)
        ids.forEach { restockLogDao.markSynced(it, syncedAt) }
        return ids.size
    }

    private suspend fun markAdjustmentsSynced(results: List<SyncRecordResultDto>, syncedAt: Long): Int {
        val ids = successIds(results)
        ids.forEach { inventoryAdjustmentDao.markSynced(it, syncedAt) }
        return ids.size
    }

    private suspend fun markWasteLogsSynced(results: List<SyncRecordResultDto>, syncedAt: Long): Int {
        val ids = successIds(results)
        ids.forEach { wasteLogDao.markSynced(it, syncedAt) }
        return ids.size
    }

    private suspend fun markTransactionsSynced(results: List<SyncRecordResultDto>, syncedAt: Long): Int {
        val ids = successIds(results)
        ids.forEach { transactionDao.markSynced(it, syncedAt) }
        return ids.size
    }

    private suspend fun markTransactionItemsSynced(results: List<SyncRecordResultDto>, syncedAt: Long): Int {
        val ids = successIds(results)
        ids.forEach { transactionItemDao.markSynced(it, syncedAt) }
        return ids.size
    }

    private suspend fun markTransactionItemAddonsSynced(results: List<SyncRecordResultDto>, syncedAt: Long): Int {
        val ids = successIds(results)
        ids.forEach { transactionItemAddonDao.markSynced(it, syncedAt) }
        return ids.size
    }

    private suspend fun markStaffLogsSynced(results: List<SyncRecordResultDto>, syncedAt: Long): Int {
        val ids = successIds(results)
        ids.forEach { staffLogDao.markSynced(it, syncedAt) }
        return ids.size
    }

    private suspend fun markAuditLogsSynced(results: List<SyncRecordResultDto>, syncedAt: Long): Int {
        val ids = successIds(results)
        ids.forEach { auditLogDao.markSynced(it, syncedAt) }
        return ids.size
    }
}