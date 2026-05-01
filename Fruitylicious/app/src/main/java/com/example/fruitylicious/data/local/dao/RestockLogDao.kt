package com.example.fruitylicious.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.fruitylicious.data.local.entity.RestockLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RestockLogDao {

    @Query("SELECT * FROM restock_logs WHERE branchId = :branchId ORDER BY dateTime DESC")
    fun observeRestockLogsByBranch(branchId: Int): Flow<List<RestockLogEntity>>

    @Query("SELECT * FROM restock_logs WHERE branchId = :branchId ORDER BY dateTime DESC")
    suspend fun getRestockLogsByBranch(branchId: Int): List<RestockLogEntity>

    @Query("SELECT * FROM restock_logs WHERE restockId = :restockId LIMIT 1")
    suspend fun getRestockLogById(restockId: String): RestockLogEntity?

    @Query("SELECT * FROM restock_logs WHERE userId = :userId ORDER BY dateTime DESC")
    fun observeRestockLogsByUser(userId: Int): Flow<List<RestockLogEntity>>

    @Query(
        """
        SELECT * FROM restock_logs
        WHERE branchId = :branchId
        AND dateTime BETWEEN :from AND :to
        ORDER BY dateTime DESC
        """
    )
    fun observeRestockLogsByDateRange(branchId: Int, from: Long, to: Long): Flow<List<RestockLogEntity>>

    @Query(
        """
        SELECT * FROM restock_logs
        WHERE branchId = :branchId
        AND dateTime BETWEEN :from AND :to
        ORDER BY dateTime DESC
        """
    )
    suspend fun getRestockLogsByDateRange(branchId: Int, from: Long, to: Long): List<RestockLogEntity>

    @Query("SELECT * FROM restock_logs WHERE isSynced = 0")
    suspend fun getUnsyncedRestockLogs(): List<RestockLogEntity>

    @Upsert
    suspend fun upsertRestockLog(restockLog: RestockLogEntity)

    @Upsert
    suspend fun upsertRestockLogs(restockLogs: List<RestockLogEntity>)

    @Query("UPDATE restock_logs SET isSynced = 1, syncedAt = :syncedAt WHERE restockId = :restockId")
    suspend fun markSynced(restockId: String, syncedAt: Long)

    @Query("DELETE FROM restock_logs WHERE restockId = :restockId")
    suspend fun deleteRestockLog(restockId: String)

    @Query("SELECT * FROM restock_logs ORDER BY dateTime DESC")
    fun observeAllRestockLogs(): Flow<List<RestockLogEntity>>
}