package com.example.fruitylicious.data.local.dao

import androidx.room.*
import com.example.fruitylicious.data.local.entity.TransactionItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionItemDao {

    // ─── Insert / Update / Delete ───────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: TransactionItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<TransactionItemEntity>)

    @Update
    suspend fun update(item: TransactionItemEntity)

    @Delete
    suspend fun delete(item: TransactionItemEntity)

    @Query("DELETE FROM transaction_items WHERE transaction_item_id = :itemId")
    suspend fun deleteById(itemId: String)

    @Query("DELETE FROM transaction_items WHERE transaction_id = :transactionId")
    suspend fun deleteByTransactionId(transactionId: String)

    @Query("DELETE FROM transaction_items")
    suspend fun deleteAll()

    // ─── Queries ────────────────────────────────────────────────────────────

    @Query("SELECT * FROM transaction_items WHERE transaction_item_id = :itemId")
    suspend fun getById(itemId: String): TransactionItemEntity?

    @Query("SELECT * FROM transaction_items WHERE transaction_id = :transactionId")
    fun getByTransactionId(transactionId: String): Flow<List<TransactionItemEntity>>

    @Query("SELECT * FROM transaction_items WHERE product_id = :productId ORDER BY last_modified DESC")
    fun getByProductId(productId: String): Flow<List<TransactionItemEntity>>

    @Query("SELECT * FROM transaction_items")
    fun getAll(): Flow<List<TransactionItemEntity>>

    /** Total quantity sold per product — useful for bestseller reports */
    @Query("SELECT SUM(quantity) FROM transaction_items WHERE product_id = :productId")
    suspend fun getTotalQuantitySold(productId: String): Int?

    @Query("SELECT * FROM transaction_items WHERE is_synced = 0")
    suspend fun getUnsynced(): List<TransactionItemEntity>

    @Query("UPDATE transaction_items SET is_synced = 1, synced_at = :syncedAt WHERE transaction_item_id = :itemId")
    suspend fun markSynced(itemId: String, syncedAt: Long)
}