package com.example.fruitylicious.data.local.dao

import androidx.room.*
import com.example.fruitylicious.data.local.entity.WasteLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WasteLogDao {

    // ─── Insert / Update / Delete ───────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: WasteLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<WasteLogEntity>)

    @Update
    suspend fun update(log: WasteLogEntity)

    @Delete
    suspend fun delete(log: WasteLogEntity)

    @Query("DELETE FROM waste_logs WHERE waste_id = :wasteId")
    suspend fun deleteById(wasteId: String)

    @Query("DELETE FROM waste_logs")
    suspend fun deleteAll()

    // ─── Queries ────────────────────────────────────────────────────────────

    @Query("SELECT * FROM waste_logs WHERE waste_id = :wasteId")
    suspend fun getById(wasteId: String): WasteLogEntity?

    @Query("SELECT * FROM waste_logs WHERE branch_id = :branchId ORDER BY date_time DESC")
    fun getByBranch(branchId: String): Flow<List<WasteLogEntity>>

    @Query("SELECT * FROM waste_logs WHERE ingredient_id = :ingredientId ORDER BY date_time DESC")
    fun getByIngredient(ingredientId: String): Flow<List<WasteLogEntity>>

    @Query("SELECT * FROM waste_logs WHERE user_id = :userId ORDER BY date_time DESC")
    fun getByUser(userId: String): Flow<List<WasteLogEntity>>

    @Query("SELECT * FROM waste_logs WHERE date_time BETWEEN :from AND :to ORDER BY date_time DESC")
    fun getByDateRange(from: Long, to: Long): Flow<List<WasteLogEntity>>

    @Query("SELECT * FROM waste_logs ORDER BY date_time DESC")
    fun getAll(): Flow<List<WasteLogEntity>>

    /** Total waste quantity per ingredient in a branch — useful for waste reports */
    @Query("SELECT SUM(quantity) FROM waste_logs WHERE ingredient_id = :ingredientId AND branch_id = :branchId")
    suspend fun getTotalWaste(ingredientId: String, branchId: String): Double?

    @Query("SELECT * FROM waste_logs WHERE is_synced = 0")
    suspend fun getUnsynced(): List<WasteLogEntity>

    @Query("UPDATE waste_logs SET is_synced = 1, synced_at = :syncedAt WHERE waste_id = :wasteId")
    suspend fun markSynced(wasteId: String, syncedAt: Long)
}