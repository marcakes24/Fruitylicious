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
import com.example.fruitylicious.data.remote.dto.HasUpdatesResponseDto
import com.example.fruitylicious.util.SessionManager
import android.content.Context
import android.util.Log
import com.example.fruitylicious.util.ImageStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
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
    private val auditLogDao: AuditLogDao,
    private val sessionManager: SessionManager
) {

    suspend fun sync(lastPulledAt: Long): SyncResult {
        return smartSync(lastPulledAt)
    }

    suspend fun smartSync(lastPulledAt: Long): SyncResult {
        return try {
            val localCount = getUnsyncedCount()
            val hasRemote = hasRemoteUpdates(lastPulledAt)

            Log.d("SyncRepository", "smartSync: localCount=$localCount, hasRemote=$hasRemote")

            if (localCount == 0 && !hasRemote) {
                return SyncResult(true, 0, 0, "No changes to sync.")
            }

            if (localCount > 0 && hasRemote) {
                val pushResult = pushUnsynced()
                if (!pushResult.success) return pushResult
                
                val pullResult = pullUpdates(lastPulledAt)
                return SyncResult(
                    success = pullResult.success,
                    pushedCount = pushResult.pushedCount,
                    pulledCount = pullResult.pulledCount,
                    message = if (pullResult.success)
                        "Smart sync: pushed ${pushResult.pushedCount}, pulled ${pullResult.pulledCount}."
                    else "Push ok, pull failed: ${pullResult.message}"
                )
            }

            if (localCount > 0) {
                return pushUnsynced()
            }

            if (hasRemote) {
                return pullUpdates(lastPulledAt)
            }

            SyncResult(true, 0, 0, "No changes to sync.")
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            SyncResult(false, 0, 0, e.message ?: "Smart sync failed.")
        }
    }

    suspend fun getUnsyncedCount(): Int {
        return branchDao.getUnsyncedBranches().size +
                userDao.getUnsyncedUsers().size +
                productDao.getUnsyncedProducts().size +
                productVariantDao.getUnsyncedVariants().size +
                ingredientDao.getUnsyncedIngredients().size +
                productRecipeDao.getUnsyncedRecipes().size +
                inventoryDao.getUnsyncedInventory().size +
                restockLogDao.getUnsyncedRestockLogs().size +
                inventoryAdjustmentDao.getUnsyncedAdjustments().size +
                wasteLogDao.getUnsyncedWasteLogs().size +
                transactionDao.getUnsyncedTransactions().size +
                transactionItemDao.getUnsyncedTransactionItems().size +
                transactionItemAddonDao.getUnsyncedTransactionItemAddons().size +
                staffLogDao.getUnsyncedStaffLogs().size +
                auditLogDao.getUnsyncedAuditLogs().size
    }

    suspend fun hasRemoteUpdates(since: Long): Boolean {
        return try {
            val response = syncApi.hasUpdates(since)
            if (response.isSuccessful) {
                response.body()?.hasUpdates ?: true
            } else {
                // Endpoint might not exist yet, fallback to true to pull normally
                true
            }
        } catch (e: Exception) {
            true // Fallback to true on network error/timeout
        }
    }

    suspend fun pushUnsynced(): SyncResult {
        return try {
            // Push order matters for FKs on backend
            val branches = branchDao.getUnsyncedBranches()
            val users = userDao.getUnsyncedUsers()
            
            val products = productDao.getUnsyncedProducts()
            val productsForPush = products.map { product ->
                product.copy(image = ImageStorage.imageFileToBase64(context, product.image) ?: product.image)
            }

            val ingredients = ingredientDao.getUnsyncedIngredients()
            val ingredientsForPush = ingredients.map { ingredient ->
                ingredient.copy(image = ImageStorage.imageFileToBase64(context, ingredient.image) ?: ingredient.image)
            }

            val productVariants = productVariantDao.getUnsyncedVariants()
            val productRecipes = productRecipeDao.getUnsyncedRecipes()
            val inventory = inventoryDao.getUnsyncedInventory()
            
            val transactions = transactionDao.getUnsyncedTransactions()
            val transactionItems = transactionItemDao.getUnsyncedTransactionItems()
            val transactionItemAddons = transactionItemAddonDao.getUnsyncedTransactionItemAddons()
            
            val restockLogs = restockLogDao.getUnsyncedRestockLogs()
            val inventoryAdjustments = inventoryAdjustmentDao.getUnsyncedAdjustments()
            
            val wasteLogs = wasteLogDao.getUnsyncedWasteLogs()
            val wasteLogsForPush = wasteLogs.map { wasteLog ->
                wasteLog.copy(image = ImageStorage.imageFileToBase64(context, wasteLog.image) ?: wasteLog.image)
            }

            val staffLogs = staffLogDao.getUnsyncedStaffLogs()
            val staffLogsForPush = staffLogs.map { staffLog ->
                staffLog.copy(image = ImageStorage.imageFileToBase64(context, staffLog.image) ?: staffLog.image)
            }

            val auditLogs = auditLogDao.getUnsyncedAuditLogs()

            val totalCount =
                branches.size + users.size + products.size + ingredients.size +
                productVariants.size + productRecipes.size + inventory.size +
                transactions.size + transactionItems.size + transactionItemAddons.size +
                restockLogs.size + inventoryAdjustments.size + wasteLogs.size +
                staffLogs.size + auditLogs.size

            if (totalCount == 0) {
                return SyncResult(true, 0, 0, "No local changes to push.")
            }

            val response = syncApi.push(
                PushRequestDto(
                    branches = branches,
                    users = users,
                    products = productsForPush,
                    ingredients = ingredientsForPush,
                    productVariants = productVariants,
                    productRecipes = productRecipes,
                    inventory = inventory,
                    transactions = transactions,
                    transactionItems = transactionItems,
                    transactionItemAddons = transactionItemAddons,
                    restockLogs = restockLogs,
                    inventoryAdjustments = inventoryAdjustments,
                    wasteLogs = wasteLogsForPush,
                    staffLogs = staffLogsForPush,
                    auditLogs = auditLogs
                )
            )

            if (!response.isSuccessful) {
                return SyncResult(false, 0, 0, "Push failed: ${response.code()} ${response.message()}")
            }

            val body = response.body() ?: return SyncResult(false, 0, 0, "Push failed: empty server response.")
            val syncedAt = System.currentTimeMillis()

            val pushedCount =
                markBranchesSynced(body.branches, syncedAt) +
                markUsersSynced(body.users, syncedAt) +
                markProductsSynced(body.products, syncedAt) +
                markIngredientsSynced(body.ingredients, syncedAt) +
                markProductVariantsSynced(body.productVariants, syncedAt) +
                markProductRecipesSynced(body.productRecipes, syncedAt) +
                markInventorySynced(body.inventory, syncedAt) +
                markTransactionsSynced(body.transactions, syncedAt) +
                markTransactionItemsSynced(body.transactionItems, syncedAt) +
                markTransactionItemAddonsSynced(body.transactionItemAddons, syncedAt) +
                markRestockLogsSynced(body.restockLogs, syncedAt) +
                markInventoryAdjustmentsSynced(body.inventoryAdjustments, syncedAt) +
                markWasteLogsSynced(body.wasteLogs, syncedAt) +
                markStaffLogsSynced(body.staffLogs, syncedAt) +
                markAuditLogsSynced(body.auditLogs, syncedAt)

            SyncResult(true, pushedCount, 0, "Push completed. Pushed $pushedCount of $totalCount local records.")
        } catch (exception: Exception) {
            if (exception is kotlinx.coroutines.CancellationException) throw exception
            SyncResult(false, 0, 0, exception.message ?: "Push failed.")
        }
    }

    suspend fun pullUpdates(since: Long): SyncResult {
        return try {
            val response = syncApi.pull(since)

            if (!response.isSuccessful) {
                return SyncResult(false, 0, 0, "Pull failed: ${response.code()} ${response.message()}")
            }

            val body = response.body() ?: return SyncResult(false, 0, 0, "Pull failed: empty server response.")
            val pulledAt = System.currentTimeMillis()

            database.withTransaction {
                // Upsert in parent-first order to satisfy local Room FKs if enabled
                branchDao.upsertBranches(body.branches.map { it.copy(isSynced = true, syncedAt = pulledAt) })
                userDao.upsertUsers(body.users.map { it.copy(isSynced = true, syncedAt = pulledAt) })

                productDao.upsertProducts(body.products.map { product ->
                    val localPath = ImageStorage.saveBase64Image(context, product.image, "products")
                    product.copy(image = localPath ?: product.image, isSynced = true, syncedAt = pulledAt)
                })

                ingredientDao.upsertIngredients(body.ingredients.map { ingredient ->
                    val localPath = ImageStorage.saveBase64Image(context, ingredient.image, "ingredients")
                    ingredient.copy(image = localPath ?: ingredient.image, isSynced = true, syncedAt = pulledAt)
                })

                productVariantDao.upsertVariants(body.productVariants.map { it.copy(isSynced = true, syncedAt = pulledAt) })
                productRecipeDao.upsertRecipes(body.productRecipes.map { it.copy(isSynced = true, syncedAt = pulledAt) })
                inventoryDao.upsertInventoryItems(body.inventory.map { it.copy(isSynced = true, syncedAt = pulledAt) })

                transactionDao.upsertTransactions(body.transactions.map { it.copy(isSynced = true, syncedAt = pulledAt) })
                body.transactionItems.forEach { transactionItemDao.upsertTransactionItem(it.copy(isSynced = true, syncedAt = pulledAt)) }
                transactionItemAddonDao.upsertAddons(body.transactionItemAddons.map { it.copy(isSynced = true, syncedAt = pulledAt) })

                restockLogDao.upsertRestockLogs(body.restockLogs.map { it.copy(isSynced = true, syncedAt = pulledAt) })
                body.inventoryAdjustments.forEach { inventoryAdjustmentDao.upsertAdjustment(it.copy(isSynced = true, syncedAt = pulledAt)) }

                wasteLogDao.upsertWasteLogs(body.wasteLogs.map { wasteLog ->
                    val localPath = ImageStorage.saveBase64Image(context, wasteLog.image, "waste")
                    wasteLog.copy(image = localPath ?: wasteLog.image, isSynced = true, syncedAt = pulledAt)
                })

                staffLogDao.upsertStaffLogs(body.staffLogs.map { staffLog ->
                    val localPath = ImageStorage.saveBase64Image(context, staffLog.image, "staff_logs")
                    staffLog.copy(image = localPath ?: staffLog.image, isSynced = true, syncedAt = pulledAt)
                })

                body.auditLogs.forEach { auditLogDao.upsertAuditLog(it.copy(isSynced = true, syncedAt = pulledAt)) }
            }

            sessionManager.saveLastPulledAt(body.serverTime)

            val pulledCount =
                body.branches.size + body.users.size + body.products.size + body.ingredients.size +
                body.productVariants.size + body.productRecipes.size + body.inventory.size +
                body.transactions.size + body.transactionItems.size + body.transactionItemAddons.size +
                body.restockLogs.size + body.inventoryAdjustments.size + body.wasteLogs.size +
                body.staffLogs.size + body.auditLogs.size

            SyncResult(true, 0, pulledCount, "Pull completed. Pulled $pulledCount records.")
        } catch (exception: Exception) {
            Log.e("SyncRepository", "Pull error", exception)
            if (exception is kotlinx.coroutines.CancellationException) throw exception
            SyncResult(false, 0, 0, exception.message ?: "Pull failed.")
        }
    }

    private fun successIds(results: List<SyncRecordResultDto>): Set<String> {
        return results.filter { it.success }.map { it.recordId }.toSet()
    }

    private suspend fun markBranchesSynced(results: List<SyncRecordResultDto>, syncedAt: Long): Int {
        val ids = successIds(results)
        ids.forEach { id -> id.toIntOrNull()?.let { branchDao.markSynced(it, syncedAt) } }
        return ids.size
    }

    private suspend fun markUsersSynced(results: List<SyncRecordResultDto>, syncedAt: Long): Int {
        val ids = successIds(results)
        ids.forEach { id -> id.toIntOrNull()?.let { userDao.markSynced(it, syncedAt) } }
        return ids.size
    }

    private suspend fun markProductsSynced(results: List<SyncRecordResultDto>, syncedAt: Long): Int {
        val ids = successIds(results)
        ids.forEach { id -> id.toIntOrNull()?.let { productDao.markSynced(it, syncedAt) } }
        return ids.size
    }

    private suspend fun markIngredientsSynced(results: List<SyncRecordResultDto>, syncedAt: Long): Int {
        val ids = successIds(results)
        ids.forEach { id -> id.toIntOrNull()?.let { ingredientDao.markSynced(it, syncedAt) } }
        return ids.size
    }

    private suspend fun markProductVariantsSynced(results: List<SyncRecordResultDto>, syncedAt: Long): Int {
        val ids = successIds(results)
        ids.forEach { id -> id.toIntOrNull()?.let { productVariantDao.markSynced(it, syncedAt) } }
        return ids.size
    }

    private suspend fun markProductRecipesSynced(results: List<SyncRecordResultDto>, syncedAt: Long): Int {
        val ids = successIds(results)
        ids.forEach { id -> id.toIntOrNull()?.let { productRecipeDao.markSynced(it, syncedAt) } }
        return ids.size
    }

    private suspend fun markInventorySynced(results: List<SyncRecordResultDto>, syncedAt: Long): Int {
        val ids = successIds(results)
        ids.forEach { recordId ->
            val parts = recordId.split(":")
            val ingredientId = parts.getOrNull(0)?.toIntOrNull()
            val branchId = parts.getOrNull(1)?.toIntOrNull()
            if (ingredientId != null && branchId != null) {
                inventoryDao.markSynced(ingredientId, branchId, syncedAt)
            }
        }
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

    private suspend fun markRestockLogsSynced(results: List<SyncRecordResultDto>, syncedAt: Long): Int {
        val ids = successIds(results)
        ids.forEach { restockLogDao.markSynced(it, syncedAt) }
        return ids.size
    }

    private suspend fun markInventoryAdjustmentsSynced(results: List<SyncRecordResultDto>, syncedAt: Long): Int {
        val ids = successIds(results)
        ids.forEach { inventoryAdjustmentDao.markSynced(it, syncedAt) }
        return ids.size
    }

    private suspend fun markWasteLogsSynced(results: List<SyncRecordResultDto>, syncedAt: Long): Int {
        val ids = successIds(results)
        ids.forEach { wasteLogDao.markSynced(it, syncedAt) }
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
