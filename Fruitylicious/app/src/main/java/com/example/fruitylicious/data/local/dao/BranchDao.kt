package com.example.fruitylicious.data.local.dao

import androidx.room.*
import com.example.fruitylicious.data.local.entity.BranchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BranchDao {

    // ─── Insert / Update / Delete ───────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(branch: BranchEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(branches: List<BranchEntity>)

    @Update
    suspend fun update(branch: BranchEntity)

    @Delete
    suspend fun delete(branch: BranchEntity)

    @Query("DELETE FROM branches WHERE branch_id = :branchId")
    suspend fun deleteById(branchId: String)

    @Query("DELETE FROM branches")
    suspend fun deleteAll()

    // ─── Queries ────────────────────────────────────────────────────────────

    @Query("SELECT * FROM branches WHERE branch_id = :branchId")
    suspend fun getById(branchId: String): BranchEntity?

    @Query("SELECT * FROM branches ORDER BY branch_name ASC")
    fun getAll(): Flow<List<BranchEntity>>

    @Query("SELECT * FROM branches WHERE is_synced = 0")
    suspend fun getUnsynced(): List<BranchEntity>

    @Query("UPDATE branches SET is_synced = 1, synced_at = :syncedAt WHERE branch_id = :branchId")
    suspend fun markSynced(branchId: String, syncedAt: Long)
}