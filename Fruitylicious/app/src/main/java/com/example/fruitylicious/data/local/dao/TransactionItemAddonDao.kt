package com.example.fruitylicious.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.fruitylicious.data.local.entity.TransactionItemAddonEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionItemAddonDao {

    @Query(
        """
        SELECT * FROM transaction_item_addons
        WHERE transactionItemId = :transactionItemId
        ORDER BY transactionItemAddonId ASC
        """
    )
    fun observeAddonsForTransactionItem(
        transactionItemId: String
    ): Flow<List<TransactionItemAddonEntity>>

    @Query(
        """
        SELECT * FROM transaction_item_addons
        WHERE transactionItemId = :transactionItemId
        ORDER BY transactionItemAddonId ASC
        """
    )
    suspend fun getAddonsForTransactionItem(
        transactionItemId: String
    ): List<TransactionItemAddonEntity>

    @Query("SELECT * FROM transaction_item_addons WHERE isSynced = 0")
    suspend fun getUnsyncedTransactionItemAddons(): List<TransactionItemAddonEntity>

    @Upsert
    suspend fun upsertAddon(addon: TransactionItemAddonEntity)

    @Upsert
    suspend fun upsertAddons(addons: List<TransactionItemAddonEntity>)

    @Query(
        """
        UPDATE transaction_item_addons
        SET isSynced = 1, syncedAt = :syncedAt
        WHERE transactionItemAddonId = :transactionItemAddonId
        """
    )
    suspend fun markSynced(
        transactionItemAddonId: String,
        syncedAt: Long
    )

    @Query("DELETE FROM transaction_item_addons WHERE transactionItemAddonId = :transactionItemAddonId")
    suspend fun deleteAddon(transactionItemAddonId: String)

    @Query("DELETE FROM transaction_item_addons WHERE transactionItemId = :transactionItemId")
    suspend fun deleteAddonsForTransactionItem(transactionItemId: String)

    @Query("SELECT * FROM transaction_item_addons ORDER BY transactionItemId ASC")
    fun observeAllTransactionItemAddons(): Flow<List<TransactionItemAddonEntity>>
}