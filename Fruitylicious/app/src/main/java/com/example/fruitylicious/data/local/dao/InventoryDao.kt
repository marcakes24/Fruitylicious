package com.example.fruitylicious.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.fruitylicious.data.local.entity.InventoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {

    @Query("SELECT * FROM inventory WHERE branchId = :branchId ORDER BY ingredientId ASC")
    fun observeInventoryByBranch(branchId: Int): Flow<List<InventoryEntity>>

    @Query("SELECT * FROM inventory WHERE branchId = :branchId ORDER BY ingredientId ASC")
    suspend fun getInventoryByBranch(branchId: Int): List<InventoryEntity>

    @Query("SELECT * FROM inventory WHERE ingredientId = :ingredientId AND branchId = :branchId LIMIT 1")
    fun observeInventoryItem(ingredientId: Int, branchId: Int): Flow<InventoryEntity?>

    @Query("SELECT * FROM inventory WHERE ingredientId = :ingredientId AND branchId = :branchId LIMIT 1")
    suspend fun getInventoryItem(ingredientId: Int, branchId: Int): InventoryEntity?

    @Query(
        """
        SELECT inventory.*
        FROM inventory
        INNER JOIN ingredients ON inventory.ingredientId = ingredients.ingredientId
        WHERE inventory.branchId = :branchId
        AND inventory.currentStock <= ingredients.lowStockThreshold
        ORDER BY ingredients.ingredientName ASC
        """
    )
    fun observeLowStockItems(branchId: Int): Flow<List<InventoryEntity>>

    @Query(
        """
        SELECT inventory.*
        FROM inventory
        INNER JOIN ingredients ON inventory.ingredientId = ingredients.ingredientId
        WHERE inventory.branchId = :branchId
        AND inventory.currentStock <= ingredients.lowStockThreshold
        ORDER BY ingredients.ingredientName ASC
        """
    )
    suspend fun getLowStockItems(branchId: Int): List<InventoryEntity>

    @Query("SELECT * FROM inventory WHERE isSynced = 0")
    suspend fun getUnsyncedInventory(): List<InventoryEntity>

    @Upsert
    suspend fun upsertInventoryItem(inventory: InventoryEntity)

    @Upsert
    suspend fun upsertInventoryItems(inventory: List<InventoryEntity>)

    @Query(
        """
        UPDATE inventory
        SET currentStock = currentStock + :amount,
            lastModified = :lastModified,
            isSynced = 0,
            syncedAt = NULL
        WHERE ingredientId = :ingredientId AND branchId = :branchId
        """
    )
    suspend fun addStock(
        ingredientId: Int,
        branchId: Int,
        amount: Double,
        lastModified: Long
    )

    @Query(
        """
        UPDATE inventory
        SET currentStock = currentStock - :amount,
            lastModified = :lastModified,
            isSynced = 0,
            syncedAt = NULL
        WHERE ingredientId = :ingredientId AND branchId = :branchId
        """
    )
    suspend fun deductStock(
        ingredientId: Int,
        branchId: Int,
        amount: Double,
        lastModified: Long
    )

    @Query(
        """
        UPDATE inventory
        SET currentStock = :currentStock,
            lastModified = :lastModified,
            isSynced = 0,
            syncedAt = NULL
        WHERE ingredientId = :ingredientId AND branchId = :branchId
        """
    )
    suspend fun setStock(
        ingredientId: Int,
        branchId: Int,
        currentStock: Double,
        lastModified: Long
    )

    @Query("UPDATE inventory SET isSynced = 1, syncedAt = :syncedAt WHERE ingredientId = :ingredientId AND branchId = :branchId")
    suspend fun markSynced(ingredientId: Int, branchId: Int, syncedAt: Long)

    @Query("DELETE FROM inventory WHERE ingredientId = :ingredientId AND branchId = :branchId")
    suspend fun deleteInventoryItem(ingredientId: Int, branchId: Int)
}