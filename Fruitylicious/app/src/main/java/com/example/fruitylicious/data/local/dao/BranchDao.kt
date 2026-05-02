package com.example.fruitylicious.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.fruitylicious.data.local.entity.BranchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BranchDao {
    @Upsert
    suspend fun upsertBranches(branches: List<BranchEntity>)

    @Query("UPDATE branches SET isSynced = 1, syncedAt = :syncedAt WHERE branchId = :branchId")
    suspend fun markSynced(branchId: Int, syncedAt: Long)

    @Query("SELECT * FROM branches WHERE isSynced = 0")
    suspend fun getUnsyncedBranches(): List<BranchEntity>

    @Query("SELECT * FROM branches ORDER BY branchId ASC")
    fun observeAllBranches(): Flow<List<BranchEntity>>

    @Query("SELECT * FROM branches ORDER BY branchId ASC")
    suspend fun getAllBranches(): List<BranchEntity>
}
