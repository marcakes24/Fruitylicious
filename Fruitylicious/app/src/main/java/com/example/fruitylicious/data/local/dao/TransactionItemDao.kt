package com.example.fruitylicious.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.fruitylicious.data.local.entity.TransactionItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionItemDao {

    @Query("SELECT * FROM transaction_items WHERE transactionId = :transactionId ORDER BY transactionItemId ASC")
    fun observeItemsForTransaction(transactionId: String): Flow<List<TransactionItemEntity>>

    @Query("SELECT * FROM transaction_items WHERE transactionId = :transactionId ORDER BY transactionItemId ASC")
    suspend fun getItemsForTransaction(transactionId: String): List<TransactionItemEntity>

    @Query("SELECT * FROM transaction_items WHERE transactionItemId = :transactionItemId LIMIT 1")
    suspend fun getTransactionItemById(transactionItemId: String): TransactionItemEntity?

    @Query("SELECT * FROM transaction_items WHERE productId = :productId ORDER BY transactionId DESC")
    fun observeItemsByProduct(productId: Int): Flow<List<TransactionItemEntity>>

    @Query(
        """
        SELECT transaction_items.*
        FROM transaction_items
        INNER JOIN transactions ON transaction_items.transactionId = transactions.transactionId
        WHERE transactions.branchId = :branchId
        AND transactions.dateTime BETWEEN :from AND :to
        ORDER BY transactions.dateTime DESC
        """
    )
    fun observeItemsByBranchAndDateRange(
        branchId: Int,
        from: Long,
        to: Long
    ): Flow<List<TransactionItemEntity>>

    @Query(
        """
        SELECT transaction_items.*
        FROM transaction_items
        INNER JOIN transactions ON transaction_items.transactionId = transactions.transactionId
        WHERE transactions.branchId = :branchId
        AND transactions.dateTime BETWEEN :from AND :to
        ORDER BY transactions.dateTime DESC
        """
    )
    suspend fun getItemsByBranchAndDateRange(
        branchId: Int,
        from: Long,
        to: Long
    ): List<TransactionItemEntity>

    @Query("SELECT * FROM transaction_items WHERE isSynced = 0")
    suspend fun getUnsyncedTransactionItems(): List<TransactionItemEntity>

    @Upsert
    suspend fun upsertTransactionItem(transactionItem: TransactionItemEntity)

    @Upsert
    suspend fun upsertTransactionItems(transactionItems: List<TransactionItemEntity>)

    @Query("UPDATE transaction_items SET isSynced = 1, syncedAt = :syncedAt WHERE transactionItemId = :transactionItemId")
    suspend fun markSynced(transactionItemId: String, syncedAt: Long)

    @Query("DELETE FROM transaction_items WHERE transactionItemId = :transactionItemId")
    suspend fun deleteTransactionItem(transactionItemId: String)

    @Query("DELETE FROM transaction_items WHERE transactionId = :transactionId")
    suspend fun deleteItemsForTransaction(transactionId: String)
}