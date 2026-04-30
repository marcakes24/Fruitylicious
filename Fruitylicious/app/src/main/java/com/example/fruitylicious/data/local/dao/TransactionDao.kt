package com.example.fruitylicious.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.example.fruitylicious.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions WHERE branchId = :branchId ORDER BY dateTime DESC")
    fun observeTransactionsByBranch(branchId: Int): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE branchId = :branchId ORDER BY dateTime DESC")
    suspend fun getTransactionsByBranch(branchId: Int): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE transactionId = :transactionId LIMIT 1")
    fun observeTransaction(transactionId: String): Flow<TransactionEntity?>

    @Query("SELECT * FROM transactions WHERE transactionId = :transactionId LIMIT 1")
    suspend fun getTransactionById(transactionId: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY dateTime DESC")
    fun observeTransactionsByUser(userId: Int): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT * FROM transactions
        WHERE branchId = :branchId
        AND dateTime BETWEEN :from AND :to
        ORDER BY dateTime DESC
        """
    )
    fun observeTransactionsByDateRange(branchId: Int, from: Long, to: Long): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT * FROM transactions
        WHERE branchId = :branchId
        AND dateTime BETWEEN :from AND :to
        ORDER BY dateTime DESC
        """
    )
    suspend fun getTransactionsByDateRange(branchId: Int, from: Long, to: Long): List<TransactionEntity>

    @Query(
        """
        SELECT COALESCE(SUM(totalAmount), 0.0)
        FROM transactions
        WHERE branchId = :branchId
        AND status = 'completed'
        AND dateTime BETWEEN :from AND :to
        """
    )
    fun observeCompletedSalesTotal(branchId: Int, from: Long, to: Long): Flow<Double>

    @Query(
        """
        SELECT COALESCE(SUM(totalAmount), 0.0)
        FROM transactions
        WHERE branchId = :branchId
        AND status = 'completed'
        AND dateTime BETWEEN :from AND :to
        """
    )
    suspend fun getCompletedSalesTotal(branchId: Int, from: Long, to: Long): Double

    @Query("SELECT * FROM transactions WHERE isSynced = 0")
    suspend fun getUnsyncedTransactions(): List<TransactionEntity>

    @Upsert
    suspend fun upsertTransaction(transaction: TransactionEntity)

    @Upsert
    suspend fun upsertTransactions(transactions: List<TransactionEntity>)

    @Query(
        """
        UPDATE transactions
        SET status = 'void',
            lastModified = :lastModified,
            isSynced = 0,
            syncedAt = NULL
        WHERE transactionId = :transactionId
        """
    )
    suspend fun voidTransaction(transactionId: String, lastModified: Long)

    @Query("UPDATE transactions SET isSynced = 1, syncedAt = :syncedAt WHERE transactionId = :transactionId")
    suspend fun markSynced(transactionId: String, syncedAt: Long)

    @Query("DELETE FROM transactions WHERE transactionId = :transactionId")
    suspend fun deleteTransaction(transactionId: String)

    @Transaction
    suspend fun upsertTransactionAndRun(
        transaction: TransactionEntity,
        block: suspend () -> Unit
    ) {
        upsertTransaction(transaction)
        block()
    }

    @Query(
        """
    SELECT * FROM transactions
    WHERE dateTime BETWEEN :from AND :to
    ORDER BY dateTime DESC
    """
    )
    fun observeAllTransactionsByDateRange(
        from: Long,
        to: Long
    ): Flow<List<TransactionEntity>>
}