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

    @Query("SELECT * FROM transaction_items WHERE isSynced = 0")
    suspend fun getUnsyncedTransactionItems(): List<TransactionItemEntity>

    @Upsert
    suspend fun upsertTransactionItem(transactionItem: TransactionItemEntity)

    @Query("UPDATE transaction_items SET isSynced = 1, syncedAt = :syncedAt WHERE transactionItemId = :transactionItemId")
    suspend fun markSynced(transactionItemId: String, syncedAt: Long)

    @Query("SELECT * FROM transaction_items ORDER BY transactionId ASC")
    fun observeAllTransactionItems(): Flow<List<TransactionItemEntity>>
}