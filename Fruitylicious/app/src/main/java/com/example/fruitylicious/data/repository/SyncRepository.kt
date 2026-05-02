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
import android.content.Context
import com.example.fruitylicious.util.ImageStorage
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @ApplicationContext private val context: Context,
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
    private val staffLogDao: StaffLogDao,
    private val auditLogDao: AuditLogDao
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
            val productsForPush = products.map { product ->
                val convertedImage = ImageStorage.imageFileToBase64(
                    context = context,
                    relativePath = product.image
                )

                product.copy(
                    image = convertedImage ?: product.image
                )
            }
            val productVariants = productVariantDao.getUnsyncedVariants()
            val ingredients = ingredientDao.getUnsyncedIngredients()
            val ingredientsForPush = ingredients.map { ingredient ->
                val convertedImage = ImageStorage.imageFileToBase64(
                    context = context,
                    relativePath = ingredient.image
                )

                ingredient.copy(
                    image = convertedImage ?: ingredient.image
                )
            }
            val productRecipes = productRecipeDao.getUnsyncedRecipes()
            val inventory = inventoryDao.getUnsyncedInventory()
            val restockLogs = restockLogDao.getUnsyncedRestockLogs()
            val inventoryAdjustments = inventoryAdjustmentDao.getUnsyncedAdjustments()
            val wasteLogs = wasteLogDao.getUnsyncedWasteLogs()
            val wasteLogsForPush = wasteLogs.map { wasteLog ->
                val convertedImage = ImageStorage.imageFileToBase64(
                    context = context,
                    relativePath = wasteLog.image
                )

                wasteLog.copy(
                    image = convertedImage ?: wasteLog.image
                )
            }
            val transactions = transactionDao.getUnsyncedTransactions()
            val transactionItems = transactionItemDao.getUnsyncedTransactionItems()
            val transactionItemAddons = transactionItemAddonDao.getUnsyncedTransactionItemAddons()
            val staffLogs = staffLogDao.getUnsyncedStaffLogs()
            val staffLogsForPush = staffLogs.map { staffLog ->
                val convertedImage = ImageStorage.imageFileToBase64(
                    context = context,
                    relativePath = staffLog.image
                )

                staffLog.copy(
                    image = convertedImage ?: staffLog.image
                )
            }
            val auditLogs = auditLogDao.getUnsyncedAuditLogs()

            val totalCount =
                branches.size +
                        users.size +
                        products.size +
                        productVariants.size +
                        ingredients.size +
                        productRecipes.size +
                        inventory.size +
                        restockLogs.size +
                        inventoryAdjustments.size +
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
                    products = productsForPush,
                    productVariants = productVariants,
                    ingredients = ingredientsForPush,
                    productRecipes = productRecipes,
                    inventory = inventory,
                    restockLogs = restockLogs,
                    inventoryAdjustments = inventoryAdjustments,
                    wasteLogs = wasteLogsForPush,
                    transactions = transactions,
                    transactionItems = transactionItems,
                    transactionItemAddons = transactionItemAddons,
                    staffLogs = staffLogsForPush,
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
                markBranchesSynced(body.branches, syncedAt) +
                        markUsersSynced(body.users, syncedAt) +
                        markProductsSynced(body.products, syncedAt) +
                        markProductVariantsSynced(body.productVariants, syncedAt) +
                        markIngredientsSynced(body.ingredients, syncedAt) +
                        markProductRecipesSynced(body.productRecipes, syncedAt) +
                        markInventorySynced(body.inventory, syncedAt) +
                        markRestockLogsSynced(body.restockLogs, syncedAt) +
                        markInventoryAdjustmentsSynced(body.inventoryAdjustments, syncedAt) +
                        markWasteLogsSynced(body.wasteLogs, syncedAt) +
                        markTransactionsSynced(body.transactions, syncedAt) +
                        markTransactionItemsSynced(body.transactionItems, syncedAt) +
                        markTransactionItemAddonsSynced(body.transactionItemAddons, syncedAt) +
                        markStaffLogsSynced(body.staffLogs, syncedAt) +
                        markAuditLogsSynced(body.auditLogs, syncedAt)

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

            database.withTransaction {
                branchDao.upsertBranches(
                    body.branches.map {
                        it.copy(isSynced = true, syncedAt = pulledAt)
                    }
                )

                userDao.upsertUsers(
                    body.users.map {
                        it.copy(isSynced = true, syncedAt = pulledAt)
                    }
                )

                val pulledProducts = body.products.map { product ->
                    val localImagePath = ImageStorage.saveBase64Image(
                        context = context,
                        base64Value = product.image,
                        folder = "products"
                    )

                    product.copy(
                        image = localImagePath ?: product.image,
                        isSynced = true,
                        syncedAt = pulledAt
                    )
                }

                productDao.upsertProducts(pulledProducts)

                productVariantDao.upsertVariants(
                    body.productVariants.map {
                        it.copy(isSynced = true, syncedAt = pulledAt)
                    }
                )

                val pulledIngredients = body.ingredients.map { ingredient ->
                    val localImagePath = ImageStorage.saveBase64Image(
                        context = context,
                        base64Value = ingredient.image,
                        folder = "ingredients"
                    )

                    ingredient.copy(
                        image = localImagePath ?: ingredient.image,
                        isSynced = true,
                        syncedAt = pulledAt
                    )
                }

                ingredientDao.upsertIngredients(pulledIngredients)

                productRecipeDao.upsertRecipes(
                    body.productRecipes.map {
                        it.copy(isSynced = true, syncedAt = pulledAt)
                    }
                )

                inventoryDao.upsertInventoryItems(
                    body.inventory.map {
                        it.copy(isSynced = true, syncedAt = pulledAt)
                    }
                )

                restockLogDao.upsertRestockLogs(
                    body.restockLogs.map {
                        it.copy(isSynced = true, syncedAt = pulledAt)
                    }
                )

                body.inventoryAdjustments.forEach {
                    inventoryAdjustmentDao.upsertAdjustment(
                        it.copy(isSynced = true, syncedAt = pulledAt)
                    )
                }

                val pulledWasteLogs = body.wasteLogs.map { wasteLog ->
                    val localImagePath = ImageStorage.saveBase64Image(
                        context = context,
                        base64Value = wasteLog.image,
                        folder = "waste"
                    )

                    wasteLog.copy(
                        image = localImagePath ?: wasteLog.image,
                        isSynced = true,
                        syncedAt = pulledAt
                    )
                }

                wasteLogDao.upsertWasteLogs(pulledWasteLogs)

                transactionDao.upsertTransactions(
                    body.transactions.map {
                        it.copy(isSynced = true, syncedAt = pulledAt)
                    }
                )

                body.transactionItems.forEach {
                    transactionItemDao.upsertTransactionItem(
                        it.copy(isSynced = true, syncedAt = pulledAt)
                    )
                }

                transactionItemAddonDao.upsertAddons(
                    body.transactionItemAddons.map {
                        it.copy(isSynced = true, syncedAt = pulledAt)
                    }
                )

                val pulledStaffLogs = body.staffLogs.map { staffLog ->
                    val localImagePath = ImageStorage.saveBase64Image(
                        context = context,
                        base64Value = staffLog.image,
                        folder = "staff_logs"
                    )

                    staffLog.copy(
                        image = localImagePath ?: staffLog.image,
                        isSynced = true,
                        syncedAt = pulledAt
                    )
                }

                staffLogDao.upsertStaffLogs(pulledStaffLogs)

                body.auditLogs.forEach {
                    auditLogDao.upsertAuditLog(
                        it.copy(isSynced = true, syncedAt = pulledAt)
                    )
                }
            }

            val pulledCount =
                body.branches.size +
                        body.users.size +
                        body.products.size +
                        body.productVariants.size +
                        body.ingredients.size +
                        body.productRecipes.size +
                        body.inventory.size +
                        body.restockLogs.size +
                        body.inventoryAdjustments.size +
                        body.wasteLogs.size +
                        body.transactions.size +
                        body.transactionItems.size +
                        body.transactionItemAddons.size +
                        body.staffLogs.size +
                        body.auditLogs.size

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

    private fun successIds(
        results: List<SyncRecordResultDto>
    ): Set<String> {
        return results
            .filter { it.success }
            .map { it.recordId }
            .toSet()
    }

    private suspend fun markBranchesSynced(
        results: List<SyncRecordResultDto>,
        syncedAt: Long
    ): Int {
        val ids = successIds(results)

        ids.forEach { id ->
            id.toIntOrNull()?.let {
                branchDao.markSynced(it, syncedAt)
            }
        }

        return ids.size
    }

    private suspend fun markUsersSynced(
        results: List<SyncRecordResultDto>,
        syncedAt: Long
    ): Int {
        val ids = successIds(results)

        ids.forEach { id ->
            id.toIntOrNull()?.let {
                userDao.markSynced(it, syncedAt)
            }
        }

        return ids.size
    }

    private suspend fun markProductsSynced(
        results: List<SyncRecordResultDto>,
        syncedAt: Long
    ): Int {
        val ids = successIds(results)

        ids.forEach { id ->
            id.toIntOrNull()?.let {
                productDao.markSynced(it, syncedAt)
            }
        }

        return ids.size
    }

    private suspend fun markProductVariantsSynced(
        results: List<SyncRecordResultDto>,
        syncedAt: Long
    ): Int {
        val ids = successIds(results)

        ids.forEach { id ->
            id.toIntOrNull()?.let {
                productVariantDao.markSynced(it, syncedAt)
            }
        }

        return ids.size
    }

    private suspend fun markIngredientsSynced(
        results: List<SyncRecordResultDto>,
        syncedAt: Long
    ): Int {
        val ids = successIds(results)

        ids.forEach { id ->
            id.toIntOrNull()?.let {
                ingredientDao.markSynced(it, syncedAt)
            }
        }

        return ids.size
    }

    private suspend fun markProductRecipesSynced(
        results: List<SyncRecordResultDto>,
        syncedAt: Long
    ): Int {
        val ids = successIds(results)

        ids.forEach { id ->
            id.toIntOrNull()?.let {
                productRecipeDao.markSynced(it, syncedAt)
            }
        }

        return ids.size
    }

    private suspend fun markInventorySynced(
        results: List<SyncRecordResultDto>,
        syncedAt: Long
    ): Int {
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

    private suspend fun markRestockLogsSynced(
        results: List<SyncRecordResultDto>,
        syncedAt: Long
    ): Int {
        val ids = successIds(results)

        ids.forEach {
            restockLogDao.markSynced(it, syncedAt)
        }

        return ids.size
    }

    private suspend fun markInventoryAdjustmentsSynced(
        results: List<SyncRecordResultDto>,
        syncedAt: Long
    ): Int {
        val ids = successIds(results)

        ids.forEach {
            inventoryAdjustmentDao.markSynced(it, syncedAt)
        }

        return ids.size
    }

    private suspend fun markWasteLogsSynced(
        results: List<SyncRecordResultDto>,
        syncedAt: Long
    ): Int {
        val ids = successIds(results)

        ids.forEach {
            wasteLogDao.markSynced(it, syncedAt)
        }

        return ids.size
    }

    private suspend fun markTransactionsSynced(
        results: List<SyncRecordResultDto>,
        syncedAt: Long
    ): Int {
        val ids = successIds(results)

        ids.forEach {
            transactionDao.markSynced(it, syncedAt)
        }

        return ids.size
    }

    private suspend fun markTransactionItemsSynced(
        results: List<SyncRecordResultDto>,
        syncedAt: Long
    ): Int {
        val ids = successIds(results)

        ids.forEach {
            transactionItemDao.markSynced(it, syncedAt)
        }

        return ids.size
    }

    private suspend fun markTransactionItemAddonsSynced(
        results: List<SyncRecordResultDto>,
        syncedAt: Long
    ): Int {
        val ids = successIds(results)

        ids.forEach {
            transactionItemAddonDao.markSynced(it, syncedAt)
        }

        return ids.size
    }

    private suspend fun markStaffLogsSynced(
        results: List<SyncRecordResultDto>,
        syncedAt: Long
    ): Int {
        val ids = successIds(results)

        ids.forEach {
            staffLogDao.markSynced(it, syncedAt)
        }

        return ids.size
    }

    private suspend fun markAuditLogsSynced(
        results: List<SyncRecordResultDto>,
        syncedAt: Long
    ): Int {
        val ids = successIds(results)

        ids.forEach {
            auditLogDao.markSynced(it, syncedAt)
        }

        return ids.size
    }
}