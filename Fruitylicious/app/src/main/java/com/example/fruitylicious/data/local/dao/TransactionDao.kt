package com.example.fruitylicious.data.local.dao

import androidx.room.*
import com.example.fruitylicious.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    // ─── Insert / Update / Delete ───────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE transaction_id = :transactionId")
    suspend fun deleteById(transactionId: String)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    // ─── Queries ────────────────────────────────────────────────────────────

    @Query("SELECT * FROM transactions WHERE transaction_id = :transactionId")
    suspend fun getById(transactionId: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE branch_id = :branchId ORDER BY date_time DESC")
    fun getByBranch(branchId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE user_id = :userId ORDER BY date_time DESC")
    fun getByUser(userId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE status = :status ORDER BY date_time DESC")
    fun getByStatus(status: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE branch_id = :branchId AND status = 'completed' ORDER BY date_time DESC")
    fun getCompletedByBranch(branchId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE date_time BETWEEN :from AND :to ORDER BY date_time DESC")
    fun getByDateRange(from: Long, to: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE branch_id = :branchId AND date_time BETWEEN :from AND :to ORDER BY date_time DESC")
    fun getByBranchAndDateRange(branchId: String, from: Long, to: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY date_time DESC")
    fun getAll(): Flow<List<TransactionEntity>>

    /** Total sales amount for a branch within a date range */
    @Query("SELECT SUM(total_amount) FROM transactions WHERE branch_id = :branchId AND status = 'completed' AND date_time BETWEEN :from AND :to")
    suspend fun getTotalSales(branchId: String, from: Long, to: Long): Double?

    /** Void a transaction by updating its status */
    @Query("UPDATE transactions SET status = 'void', last_modified = :lastModified WHERE transaction_id = :transactionId")
    suspend fun voidTransaction(transactionId: String, lastModified: Long = System.currentTimeMillis())

    @Query("SELECT * FROM transactions WHERE is_synced = 0")
    suspend fun getUnsynced(): List<TransactionEntity>

    @Query("UPDATE transactions SET is_synced = 1, synced_at = :syncedAt WHERE transaction_id = :transactionId")
    suspend fun markSynced(transactionId: String, syncedAt: Long)
}