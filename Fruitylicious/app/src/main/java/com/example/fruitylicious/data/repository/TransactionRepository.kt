package com.example.fruitylicious.data.repository

import androidx.room.withTransaction
import com.example.fruitylicious.data.local.dao.AuditLogDao
import com.example.fruitylicious.data.local.dao.InventoryDao
import com.example.fruitylicious.data.local.dao.ProductRecipeDao
import com.example.fruitylicious.data.local.dao.TransactionDao
import com.example.fruitylicious.data.local.dao.TransactionItemDao
import com.example.fruitylicious.data.local.db.PosDatabase
import com.example.fruitylicious.data.local.entity.AuditLogEntity
import com.example.fruitylicious.data.local.entity.TransactionEntity
import com.example.fruitylicious.data.local.entity.TransactionItemEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class CartItem(
    val productId: Int,
    val productName: String,
    val quantity: Int,
    val unitPrice: Double,
    val subtotal: Double
)

@Singleton
class TransactionRepository @Inject constructor(
    private val database: PosDatabase,
    private val transactionDao: TransactionDao,
    private val transactionItemDao: TransactionItemDao,
    private val productRecipeDao: ProductRecipeDao,
    private val inventoryDao: InventoryDao,
    private val auditLogDao: AuditLogDao
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

    fun observeTransactionsByDateRange(branchId: Int, from: Long, to: Long): Flow<List<TransactionEntity>> {
        return transactionDao.observeTransactionsByDateRange(branchId, from, to)
    }

    fun observeCompletedSalesTotal(branchId: Int, from: Long, to: Long): Flow<Double> {
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

        val now = System.currentTimeMillis()
        val requiredByIngredient = mutableMapOf<Int, Double>()

        for (cartItem in cartItems) {
            if (cartItem.quantity <= 0) {
                return Result.failure(IllegalArgumentException("Item quantity must be greater than zero."))
            }

            val recipes = productRecipeDao.getRecipesForProduct(cartItem.productId)

            for (recipe in recipes) {
                val totalRequired = recipe.quantityRequired * cartItem.quantity
                requiredByIngredient[recipe.ingredientId] =
                    (requiredByIngredient[recipe.ingredientId] ?: 0.0) + totalRequired
            }
        }

        for ((ingredientId, requiredQuantity) in requiredByIngredient) {
            val inventory = inventoryDao.getInventoryItem(ingredientId, branchId)
                ?: return Result.failure(IllegalStateException("Missing inventory for ingredient $ingredientId."))

            if (inventory.currentStock < requiredQuantity) {
                return Result.failure(IllegalStateException("Insufficient stock for ingredient $ingredientId."))
            }
        }

        val transactionId = UUID.randomUUID().toString()
        val totalAmount = cartItems.sumOf { it.subtotal }

        database.withTransaction {
            transactionDao.upsertTransaction(
                TransactionEntity(
                    transactionId = transactionId,
                    userId = userId,
                    branchId = branchId,
                    totalAmount = totalAmount,
                    paymentType = paymentType.trim(),
                    dateTime = now,
                    status = "completed",
                    lastModified = now,
                    isSynced = false,
                    syncedAt = null
                )
            )

            val transactionItems = cartItems.map {
                TransactionItemEntity(
                    transactionItemId = UUID.randomUUID().toString(),
                    transactionId = transactionId,
                    productId = it.productId,
                    quantity = it.quantity,
                    subtotal = it.subtotal,
                    lastModified = now,
                    isSynced = false,
                    syncedAt = null
                )
            }

            transactionItemDao.upsertTransactionItems(transactionItems)

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

        return Result.success(transactionId)
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

    suspend fun markTransactionSynced(transactionId: String, syncedAt: Long) {
        transactionDao.markSynced(transactionId, syncedAt)
    }

    suspend fun markTransactionItemSynced(transactionItemId: String, syncedAt: Long) {
        transactionItemDao.markSynced(transactionItemId, syncedAt)
    }
}