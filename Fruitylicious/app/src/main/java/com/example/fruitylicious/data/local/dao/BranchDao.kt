package com.example.fruitylicious.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.fruitylicious.data.local.entity.BranchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BranchDao {

    @Query("SELECT * FROM branches ORDER BY branchName ASC")
    fun observeAllBranches(): Flow<List<BranchEntity>>

    @Query("SELECT * FROM branches ORDER BY branchName ASC")
    suspend fun getAllBranches(): List<BranchEntity>

    @Query("SELECT * FROM branches WHERE branchId = :branchId LIMIT 1")
    fun observeBranch(branchId: Int): Flow<BranchEntity?>

    @Query("SELECT * FROM branches WHERE branchId = :branchId LIMIT 1")
    suspend fun getBranchById(branchId: Int): BranchEntity?

    @Query("SELECT * FROM branches WHERE isSynced = 0")
    suspend fun getUnsyncedBranches(): List<BranchEntity>

    @Upsert
    suspend fun upsertBranch(branch: BranchEntity)

    @Upsert
    suspend fun upsertBranches(branches: List<BranchEntity>)

    @Query("UPDATE branches SET isSynced = 1, syncedAt = :syncedAt WHERE branchId = :branchId")
    suspend fun markSynced(branchId: Int, syncedAt: Long)

    @Query("DELETE FROM branches WHERE branchId = :branchId")
    suspend fun deleteBranch(branchId: Int)
}