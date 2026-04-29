package com.example.fruitylicious.data.repository

import com.example.fruitylicious.data.local.dao.InventoryDao
import com.example.fruitylicious.data.local.entity.InventoryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InventoryRepository @Inject constructor(
    private val inventoryDao: InventoryDao
) {

    fun observeInventory(branchId: Int): Flow<List<InventoryEntity>> {
        return inventoryDao.observeInventoryByBranch(branchId)
    }

    fun observeInventoryItem(ingredientId: Int, branchId: Int): Flow<InventoryEntity?> {
        return inventoryDao.observeInventoryItem(ingredientId, branchId)
    }

    fun observeLowStockItems(branchId: Int): Flow<List<InventoryEntity>> {
        return inventoryDao.observeLowStockItems(branchId)
    }

    suspend fun getInventory(branchId: Int): List<InventoryEntity> {
        return inventoryDao.getInventoryByBranch(branchId)
    }

    suspend fun getInventoryItem(ingredientId: Int, branchId: Int): InventoryEntity? {
        return inventoryDao.getInventoryItem(ingredientId, branchId)
    }

    suspend fun getLowStockItems(branchId: Int): List<InventoryEntity> {
        return inventoryDao.getLowStockItems(branchId)
    }

    suspend fun upsertInventoryItem(
        ingredientId: Int,
        branchId: Int,
        currentStock: Double
    ): Result<Unit> {
        if (ingredientId <= 0) {
            return Result.failure(IllegalArgumentException("Ingredient is required."))
        }

        if (branchId <= 0) {
            return Result.failure(IllegalArgumentException("Branch is required."))
        }

        if (currentStock < 0.0) {
            return Result.failure(IllegalArgumentException("Stock cannot be negative."))
        }

        val now = System.currentTimeMillis()

        inventoryDao.upsertInventoryItem(
            InventoryEntity(
                ingredientId = ingredientId,
                branchId = branchId,
                currentStock = currentStock,
                lastModified = now,
                isSynced = false,
                syncedAt = null
            )
        )

        return Result.success(Unit)
    }

    suspend fun addStock(
        ingredientId: Int,
        branchId: Int,
        amount: Double
    ): Result<Unit> {
        if (amount <= 0.0) {
            return Result.failure(IllegalArgumentException("Amount must be greater than zero."))
        }

        inventoryDao.addStock(
            ingredientId = ingredientId,
            branchId = branchId,
            amount = amount,
            lastModified = System.currentTimeMillis()
        )

        return Result.success(Unit)
    }

    suspend fun deductStock(
        ingredientId: Int,
        branchId: Int,
        amount: Double
    ): Result<Unit> {
        if (amount <= 0.0) {
            return Result.failure(IllegalArgumentException("Amount must be greater than zero."))
        }

        val item = inventoryDao.getInventoryItem(ingredientId, branchId)
            ?: return Result.failure(IllegalStateException("Inventory item not found."))

        if (item.currentStock < amount) {
            return Result.failure(IllegalStateException("Insufficient stock."))
        }

        inventoryDao.deductStock(
            ingredientId = ingredientId,
            branchId = branchId,
            amount = amount,
            lastModified = System.currentTimeMillis()
        )

        return Result.success(Unit)
    }

    suspend fun setStock(
        ingredientId: Int,
        branchId: Int,
        currentStock: Double
    ): Result<Unit> {
        if (currentStock < 0.0) {
            return Result.failure(IllegalArgumentException("Stock cannot be negative."))
        }

        inventoryDao.setStock(
            ingredientId = ingredientId,
            branchId = branchId,
            currentStock = currentStock,
            lastModified = System.currentTimeMillis()
        )

        return Result.success(Unit)
    }

    suspend fun getUnsyncedInventory(): List<InventoryEntity> {
        return inventoryDao.getUnsyncedInventory()
    }

    suspend fun markSynced(ingredientId: Int, branchId: Int, syncedAt: Long) {
        inventoryDao.markSynced(ingredientId, branchId, syncedAt)
    }

    suspend fun savePulledInventory(inventory: List<InventoryEntity>) {
        inventoryDao.upsertInventoryItems(inventory.map { it.copy(isSynced = true, syncedAt = it.syncedAt ?: System.currentTimeMillis()) })
    }
}