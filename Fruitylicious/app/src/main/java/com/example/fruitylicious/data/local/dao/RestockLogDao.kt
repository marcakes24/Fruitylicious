package com.example.fruitylicious.data.local.dao

import androidx.room.*
import com.example.fruitylicious.data.local.entity.RestockLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RestockLogDao {

    // ─── Insert / Update / Delete ───────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: RestockLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<RestockLogEntity>)

    @Update
    suspend fun update(log: RestockLogEntity)

    @Delete
    suspend fun delete(log: RestockLogEntity)

    @Query("DELETE FROM restock_logs WHERE restock_id = :restockId")
    suspend fun deleteById(restockId: String)

    @Query("DELETE FROM restock_logs")
    suspend fun deleteAll()

    // ─── Queries ────────────────────────────────────────────────────────────

    @Query("SELECT * FROM restock_logs WHERE restock_id = :restockId")
    suspend fun getById(restockId: String): RestockLogEntity?

    @Query("SELECT * FROM restock_logs WHERE branch_id = :branchId ORDER BY date_time DESC")
    fun getByBranch(branchId: String): Flow<List<RestockLogEntity>>

    @Query("SELECT * FROM restock_logs WHERE ingredient_id = :ingredientId ORDER BY date_time DESC")
    fun getByIngredient(ingredientId: String): Flow<List<RestockLogEntity>>

    @Query("SELECT * FROM restock_logs WHERE branch_id = :branchId AND ingredient_id = :ingredientId ORDER BY date_time DESC")
    fun getByBranchAndIngredient(branchId: String, ingredientId: String): Flow<List<RestockLogEntity>>

    @Query("SELECT * FROM restock_logs WHERE date_time BETWEEN :from AND :to ORDER BY date_time DESC")
    fun getByDateRange(from: Long, to: Long): Flow<List<RestockLogEntity>>

    @Query("SELECT * FROM restock_logs ORDER BY date_time DESC")
    fun getAll(): Flow<List<RestockLogEntity>>

    @Query("SELECT * FROM restock_logs WHERE is_synced = 0")
    suspend fun getUnsynced(): List<RestockLogEntity>

    @Query("UPDATE restock_logs SET is_synced = 1, synced_at = :syncedAt WHERE restock_id = :restockId")
    suspend fun markSynced(restockId: String, syncedAt: Long)
}