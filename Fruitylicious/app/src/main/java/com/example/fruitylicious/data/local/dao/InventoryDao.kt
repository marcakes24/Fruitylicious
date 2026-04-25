package com.example.fruitylicious.data.local.dao

import androidx.room.*
import com.example.fruitylicious.data.local.entity.InventoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {

    // ─── Insert / Update / Delete ───────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(inventory: InventoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(inventories: List<InventoryEntity>)

    @Update
    suspend fun update(inventory: InventoryEntity)

    @Query("DELETE FROM inventory WHERE ingredient_id = :ingredientId AND branch_id = :branchId")
    suspend fun delete(ingredientId: String, branchId: String)

    @Query("DELETE FROM inventory WHERE branch_id = :branchId")
    suspend fun deleteByBranch(branchId: String)

    @Query("DELETE FROM inventory")
    suspend fun deleteAll()

    // ─── Queries ────────────────────────────────────────────────────────────

    @Query("SELECT * FROM inventory WHERE ingredient_id = :ingredientId AND branch_id = :branchId")
    suspend fun getByKey(ingredientId: String, branchId: String): InventoryEntity?

    @Query("SELECT * FROM inventory WHERE branch_id = :branchId ORDER BY ingredient_id ASC")
    fun getByBranch(branchId: String): Flow<List<InventoryEntity>>

    @Query("SELECT * FROM inventory WHERE ingredient_id = :ingredientId")
    fun getByIngredient(ingredientId: String): Flow<List<InventoryEntity>>

    @Query("SELECT * FROM inventory")
    fun getAll(): Flow<List<InventoryEntity>>

    /** Returns stock entries where current_stock is at or below a threshold — useful for low-stock alerts */
    @Query("SELECT * FROM inventory WHERE branch_id = :branchId AND current_stock <= :threshold")
    fun getLowStock(branchId: String, threshold: Double): Flow<List<InventoryEntity>>

    @Query("UPDATE inventory SET current_stock = current_stock + :amount, last_modified = :lastModified WHERE ingredient_id = :ingredientId AND branch_id = :branchId")
    suspend fun addStock(ingredientId: String, branchId: String, amount: Double, lastModified: Long = System.currentTimeMillis())

    @Query("UPDATE inventory SET current_stock = current_stock - :amount, last_modified = :lastModified WHERE ingredient_id = :ingredientId AND branch_id = :branchId")
    suspend fun deductStock(ingredientId: String, branchId: String, amount: Double, lastModified: Long = System.currentTimeMillis())

    @Query("SELECT * FROM inventory WHERE is_synced = 0")
    suspend fun getUnsynced(): List<InventoryEntity>

    @Query("UPDATE inventory SET is_synced = 1, synced_at = :syncedAt WHERE ingredient_id = :ingredientId AND branch_id = :branchId")
    suspend fun markSynced(ingredientId: String, branchId: String, syncedAt: Long)
}