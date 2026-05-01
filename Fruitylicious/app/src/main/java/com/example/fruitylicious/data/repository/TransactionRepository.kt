package com.example.fruitylicious.data.repository

import androidx.room.withTransaction
import com.example.fruitylicious.data.local.dao.AuditLogDao
import com.example.fruitylicious.data.local.dao.IngredientDao
import com.example.fruitylicious.data.local.dao.InventoryDao
import com.example.fruitylicious.data.local.dao.ProductRecipeDao
import com.example.fruitylicious.data.local.dao.TransactionDao
import com.example.fruitylicious.data.local.dao.TransactionItemAddonDao
import com.example.fruitylicious.data.local.dao.TransactionItemDao
import com.example.fruitylicious.data.local.db.PosDatabase
import com.example.fruitylicious.data.local.entity.AuditLogEntity
import com.example.fruitylicious.data.local.entity.TransactionEntity
import com.example.fruitylicious.data.local.entity.TransactionItemAddonEntity
import com.example.fruitylicious.data.local.entity.TransactionItemEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class CartAddon(
    val addonProductId: Int,
    val addonName: String,
    val quantity: Int,
    val unitPrice: Double,
    val subtotal: Double
)

data class CartItem(
    val cartLineId: String = UUID.randomUUID().toString(),
    val productId: Int,
    val variantId: Int,
    val productName: String,
    val sizeName: String,
    val quantity: Int,
    val unitPrice: Double,
    val subtotal: Double,
    val addons: List<CartAddon> = emptyList()
)

@Singleton
class TransactionRepository @Inject constructor(
    private val database: PosDatabase,
    private val transactionDao: TransactionDao,
    private val transactionItemDao: TransactionItemDao,
    private val transactionItemAddonDao: TransactionItemAddonDao,
    private val productRecipeDao: ProductRecipeDao,
    private val inventoryDao: InventoryDao,
    private val auditLogDao: AuditLogDao,
    private val ingredientDao: IngredientDao,
) {

    fun observeTransactions(branchId: Int): Flow<List<TransactionEntity>> {
        return transactionDao.observeTransactionsByBranch(branchId)
    }

    fun observeTransaction(transactionId: String): Flow<TransactionEntity?> {
        return transactionDao.observeTransaction(transactionId)
    }

    fun observeItemsForTransaction(transactionId: String): Flow<List<TransactionItemEntity>> {
        return transactionItemDao.observeItemsForTransaction(transactionId)
    }

    fun observeTransactionsByDateRange(
        branchId: Int,
        from: Long,
        to: Long
    ): Flow<List<TransactionEntity>> {
        return transactionDao.observeTransactionsByDateRange(branchId, from, to)
    }

    fun observeCompletedSalesTotal(
        branchId: Int,
        from: Long,
        to: Long
    ): Flow<Double> {
        return transactionDao.observeCompletedSalesTotal(branchId, from, to)
    }

    suspend fun getTransactions(branchId: Int): List<TransactionEntity> {
        return transactionDao.getTransactionsByBranch(branchId)
    }

    suspend fun getTransaction(transactionId: String): TransactionEntity? {
        return transactionDao.getTransactionById(transactionId)
    }

    suspend fun getItemsForTransaction(transactionId: String): List<TransactionItemEntity> {
        return transactionItemDao.getItemsForTransaction(transactionId)
    }

    suspend fun createTransaction(
        userId: Int,
        branchId: Int,
        cartItems: List<CartItem>,
        paymentType: String
    ): Result<String> {
        if (cartItems.isEmpty()) {
            return Result.failure(IllegalArgumentException("Cart is empty."))
        }

        if (paymentType.trim().isBlank()) {
            return Result.failure(IllegalArgumentException("Payment type is required."))
        }

        return try {
            val now = System.currentTimeMillis()
            val transactionId = UUID.randomUUID().toString()
            val totalAmount = cartItems.sumOf { it.subtotal }

            val requiredByIngredient = mutableMapOf<Int, Double>()

            for (cartItem in cartItems) {
                if (cartItem.quantity <= 0) {
                    return Result.failure(
                        IllegalArgumentException("Item quantity must be greater than zero.")
                    )
                }

                val baseRecipes = productRecipeDao.getRecipesForVariant(cartItem.variantId)

                for (recipe in baseRecipes) {
                    val ingredient = ingredientDao.getIngredientById(recipe.ingredientId)
                        ?: return Result.failure(
                            IllegalStateException("Ingredient ${recipe.ingredientId} not found.")
                        )

                    val deductionPerItem = computeInventoryDeduction(
                        recipeQuantity = recipe.quantityRequired,
                        unitType = ingredient.unitType,
                        estimatedWeightPerUnit = ingredient.estimatedWeightPerUnit,
                        ingredientName = ingredient.ingredientName
                    )

                    val totalRequired = deductionPerItem * cartItem.quantity

                    requiredByIngredient[recipe.ingredientId] =
                        (requiredByIngredient[recipe.ingredientId] ?: 0.0) + totalRequired
                }

                for (addon in cartItem.addons) {
                    val addonRecipes = productRecipeDao.getRecipesForProduct(addon.addonProductId)

                    for (recipe in addonRecipes) {
                        val ingredient = ingredientDao.getIngredientById(recipe.ingredientId)
                            ?: return Result.failure(
                                IllegalStateException("Ingredient ${recipe.ingredientId} not found.")
                            )

                        val deductionPerAddon = computeInventoryDeduction(
                            recipeQuantity = recipe.quantityRequired,
                            unitType = ingredient.unitType,
                            estimatedWeightPerUnit = ingredient.estimatedWeightPerUnit,
                            ingredientName = ingredient.ingredientName
                        )

                        val totalRequired = deductionPerAddon * addon.quantity * cartItem.quantity

                        requiredByIngredient[recipe.ingredientId] =
                            (requiredByIngredient[recipe.ingredientId] ?: 0.0) + totalRequired
                    }
                }
            }

            for ((ingredientId, requiredQuantity) in requiredByIngredient) {
                val inventory = inventoryDao.getInventoryItem(
                    ingredientId = ingredientId,
                    branchId = branchId
                )

                if (inventory == null) {
                    return Result.failure(
                        IllegalStateException("Inventory item not found for ingredient $ingredientId.")
                    )
                }

                if (inventory.currentStock < requiredQuantity) {
                    return Result.failure(
                        IllegalStateException("Insufficient stock for ingredient $ingredientId.")
                    )
                }
            }

            database.withTransaction {
                transactionDao.upsertTransaction(
                    TransactionEntity(
                        transactionId = transactionId,
                        userId = userId,
                        branchId = branchId,
                        totalAmount = totalAmount,
                        paymentType = paymentType.trim(),

                        // Important for Queue:
                        status = "pending",

                        dateTime = now,
                        lastModified = now,
                        isSynced = false,
                        syncedAt = null
                    )
                )

                for (cartItem in cartItems) {
                    val transactionItemId = UUID.randomUUID().toString()

                    transactionItemDao.upsertTransactionItem(
                        TransactionItemEntity(
                            transactionItemId = transactionItemId,
                            transactionId = transactionId,
                            productId = cartItem.productId,
                            variantId = cartItem.variantId,
                            sizeName = cartItem.sizeName,
                            quantity = cartItem.quantity,
                            subtotal = cartItem.subtotal,
                            lastModified = now,
                            isSynced = false,
                            syncedAt = null
                        )
                    )

                    val addonEntities = cartItem.addons.map { addon ->
                        val totalAddonQuantity = addon.quantity * cartItem.quantity

                        TransactionItemAddonEntity(
                            transactionItemAddonId = UUID.randomUUID().toString(),
                            transactionItemId = transactionItemId,
                            addonProductId = addon.addonProductId,
                            quantity = totalAddonQuantity,
                            subtotal = addon.unitPrice * totalAddonQuantity,
                            lastModified = now,
                            isSynced = false,
                            syncedAt = null
                        )
                    }

                    if (addonEntities.isNotEmpty()) {
                        transactionItemAddonDao.upsertAddons(addonEntities)
                    }
                }

                for ((ingredientId, requiredQuantity) in requiredByIngredient) {
                    inventoryDao.deductStock(
                        ingredientId = ingredientId,
                        branchId = branchId,
                        amount = requiredQuantity,
                        lastModified = now
                    )
                }

                auditLogDao.upsertAuditLog(
                    AuditLogEntity(
                        logId = UUID.randomUUID().toString(),
                        userId = userId,
                        branchId = branchId,
                        action = "Created transaction $transactionId with total amount $totalAmount.",
                        tableAffected = "transactions",
                        timestamp = now,
                        lastModified = now,
                        isSynced = false,
                        syncedAt = null
                    )
                )
            }

            Result.success(transactionId)
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    suspend fun voidTransaction(
        transactionId: String,
        userId: Int,
        branchId: Int
    ): Result<Unit> {
        val transaction = transactionDao.getTransactionById(transactionId)
            ?: return Result.failure(IllegalStateException("Transaction not found."))

        if (transaction.status == "void") {
            return Result.failure(IllegalStateException("Transaction is already void."))
        }

        val now = System.currentTimeMillis()

        database.withTransaction {
            transactionDao.voidTransaction(transactionId, now)

            auditLogDao.upsertAuditLog(
                AuditLogEntity(
                    logId = UUID.randomUUID().toString(),
                    userId = userId,
                    branchId = branchId,
                    action = "Voided transaction $transactionId. Inventory was not restored.",
                    tableAffected = "transactions",
                    timestamp = now,
                    lastModified = now,
                    isSynced = false,
                    syncedAt = null
                )
            )
        }

        return Result.success(Unit)
    }

    suspend fun getUnsyncedTransactions(): List<TransactionEntity> {
        return transactionDao.getUnsyncedTransactions()
    }

    suspend fun getUnsyncedTransactionItems(): List<TransactionItemEntity> {
        return transactionItemDao.getUnsyncedTransactionItems()
    }

    suspend fun getUnsyncedTransactionItemAddons(): List<TransactionItemAddonEntity> {
        return transactionItemAddonDao.getUnsyncedTransactionItemAddons()
    }

    suspend fun markTransactionSynced(
        transactionId: String,
        syncedAt: Long
    ) {
        transactionDao.markSynced(transactionId, syncedAt)
    }

    suspend fun markTransactionItemSynced(
        transactionItemId: String,
        syncedAt: Long
    ) {
        transactionItemDao.markSynced(transactionItemId, syncedAt)
    }

    suspend fun markTransactionItemAddonSynced(
        transactionItemAddonId: String,
        syncedAt: Long
    ) {
        transactionItemAddonDao.markSynced(transactionItemAddonId, syncedAt)
    }

    fun observeAddonsForTransactionItem(
        transactionItemId: String
    ): Flow<List<TransactionItemAddonEntity>> {
        return transactionItemAddonDao.observeAddonsForTransactionItem(transactionItemId)
    }

    suspend fun getAddonsForTransactionItem(
        transactionItemId: String
    ): List<TransactionItemAddonEntity> {
        return transactionItemAddonDao.getAddonsForTransactionItem(transactionItemId)
    }

    fun observeAllTransactionsByDateRange(
        from: Long,
        to: Long
    ): Flow<List<TransactionEntity>> {
        return transactionDao.observeAllTransactionsByDateRange(from, to)
    }

    private fun computeInventoryDeduction(
        recipeQuantity: Double,
        unitType: String,
        estimatedWeightPerUnit: Double,
        ingredientName: String
    ): Double {
        return if (unitType.equals("pcs", ignoreCase = true)) {
            if (estimatedWeightPerUnit <= 0.0) {
                throw IllegalStateException(
                    "$ingredientName uses pcs but estimated weight per unit is not set."
                )
            }

            recipeQuantity / estimatedWeightPerUnit
        } else {
            recipeQuantity
        }
    }
}