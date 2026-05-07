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
        SELECT COALESCE(SUM(totalAmount), 0.0)
        FROM transactions
        WHERE branchId = :branchId
        AND status = 'completed'
        AND dateTime BETWEEN :from AND :to
        """
    )
    fun observeCompletedSalesTotal(branchId: Int, from: Long, to: Long): Flow<Double>

    @Query("SELECT * FROM transactions WHERE isSynced = 0")
    suspend fun getUnsyncedTransactions(): List<TransactionEntity>

    @Upsert
    suspend fun upsertTransaction(transaction: TransactionEntity)

    @Upsert
    suspend fun upsertTransactions(transactions: List<TransactionEntity>)

    @Query("UPDATE transactions SET isSynced = 1, syncedAt = :syncedAt WHERE transactionId = :transactionId")
    suspend fun markSynced(transactionId: String, syncedAt: Long)

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

    @Query(
        """
    SELECT COALESCE(SUM(totalAmount), 0)
    FROM transactions
    WHERE status = 'completed'
    AND dateTime BETWEEN :from AND :to
    AND (:branchId IS NULL OR branchId = :branchId)
    """
    )

    suspend fun getSalesTotal(
        branchId: Int?,
        from: Long,
        to: Long
    ): Double

    @Query(
        """
    SELECT COALESCE(SUM(totalAmount), 0)
    FROM transactions
    WHERE status = 'completed'
    AND paymentType = :paymentType
    AND dateTime BETWEEN :from AND :to
    AND (:branchId IS NULL OR branchId = :branchId)
    """
    )
    suspend fun getPaymentTotal(
        branchId: Int?,
        paymentType: String,
        from: Long,
        to: Long
    ): Double

    @Query(
        """
    SELECT 
        p.productName AS productName,
        SUM(ti.quantity) AS qty,
        SUM(ti.subtotal) AS totalAmount,
        CAST(SUM(CASE WHEN t.branchId = 1 THEN ti.quantity ELSE 0 END) AS INTEGER) AS b1Qty,
        CAST(SUM(CASE WHEN t.branchId = 2 THEN ti.quantity ELSE 0 END) AS INTEGER) AS b2Qty,
        SUM(CASE WHEN t.branchId = 1 THEN ti.subtotal ELSE 0.0 END) AS b1Amount,
        SUM(CASE WHEN t.branchId = 2 THEN ti.subtotal ELSE 0.0 END) AS b2Amount
    FROM transaction_items ti
    INNER JOIN transactions t ON ti.transactionId = t.transactionId
    INNER JOIN products p ON ti.productId = p.productId
    WHERE t.status = 'completed'
    AND t.dateTime BETWEEN :from AND :to
    AND (:branchId IS NULL OR t.branchId = :branchId)
    GROUP BY p.productName
    ORDER BY totalAmount DESC
    """
    )
    suspend fun getSalesBreakdown(
        branchId: Int?,
        from: Long,
        to: Long
    ): List<SalesBreakdownRow>

    @Query(
        """
    SELECT 
        p.productName AS productName,
        SUM(ti.quantity) AS totalQty
    FROM transaction_items ti
    INNER JOIN transactions t ON ti.transactionId = t.transactionId
    INNER JOIN products p ON ti.productId = p.productId
    WHERE t.status = 'completed'
    AND t.dateTime BETWEEN :from AND :to
    AND (:branchId IS NULL OR t.branchId = :branchId)
    GROUP BY p.productName
    ORDER BY totalQty DESC
    LIMIT 5
    """
    )
    suspend fun getTopSellingItems(
        branchId: Int?,
        from: Long,
        to: Long
    ): List<TopSellingItemRow>

    @Query(
        """
    SELECT COUNT(*)
    FROM transactions
    WHERE status = 'completed'
    AND dateTime BETWEEN :from AND :to
    AND (:branchId IS NULL OR branchId = :branchId)
    """
    )
    suspend fun getTransactionCount(
        branchId: Int?,
        from: Long,
        to: Long
    ): Int

    @Query("SELECT * FROM transactions ORDER BY dateTime DESC")
    fun observeAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY dateTime DESC LIMIT :limit OFFSET :offset")
    suspend fun getTransactionsPaged(limit: Int, offset: Int): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE branchId = :branchId ORDER BY dateTime DESC LIMIT :limit OFFSET :offset")
    suspend fun getTransactionsByBranchPaged(branchId: Int, limit: Int, offset: Int): List<TransactionEntity>

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
    suspend fun voidTransaction(
        transactionId: String,
        lastModified: Long
    )

    @Query(
        """
    SELECT * FROM transactions
    WHERE status IN ('pending', 'preparing', 'ready', 'completed')
    AND dateTime > :since
    ORDER BY dateTime DESC
    """
    )
    fun observeQueueTransactions(since: Long): Flow<List<TransactionEntity>>

    @Query(
        """
    UPDATE transactions
    SET status = :status,
        lastModified = :lastModified,
        isSynced = 0,
        syncedAt = NULL
    WHERE transactionId = :transactionId
    """
    )
    suspend fun updateTransactionStatus(
        transactionId: String,
        status: String,
        lastModified: Long
    )

    @Query(
        """
    SELECT COALESCE(SUM(totalAmount), 0)
    FROM transactions
    WHERE status != 'void'
    AND branchId = :branchId
    AND dateTime BETWEEN :from AND :to
    """
    )
    suspend fun getTodaySalesForBranch(
        branchId: Int,
        from: Long,
        to: Long
    ): Double

    @Query(
        """
    SELECT COUNT(*)
    FROM transactions
    WHERE status != 'void'
    AND branchId = :branchId
    AND dateTime BETWEEN :from AND :to
    """
    )
    suspend fun getTodayTransactionCountForBranch(
        branchId: Int,
        from: Long,
        to: Long
    ): Int

    @Query(
        """
    SELECT COALESCE(SUM(totalAmount), 0)
    FROM transactions
    WHERE status != 'void'
    AND branchId = :branchId
    AND paymentType = :paymentType
    AND dateTime BETWEEN :from AND :to
    """
    )
    suspend fun getTodayPaymentTotalForBranch(
        branchId: Int,
        paymentType: String,
        from: Long,
        to: Long
    ): Double

    @Query(
        """
    SELECT COUNT(*)
    FROM transactions
    WHERE branchId = :branchId
    AND status IN ('pending', 'preparing', 'ready')
    """
    )
    suspend fun getActiveQueueCountForBranch(
        branchId: Int
    ): Int
}