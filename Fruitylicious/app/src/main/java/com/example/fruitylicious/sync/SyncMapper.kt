package com.example.fruitylicious.sync

import com.example.fruitylicious.data.local.entity.AuditLogEntity
import com.example.fruitylicious.data.local.entity.BranchEntity
import com.example.fruitylicious.data.local.entity.IngredientEntity
import com.example.fruitylicious.data.local.entity.InventoryAdjustmentEntity
import com.example.fruitylicious.data.local.entity.InventoryEntity
import com.example.fruitylicious.data.local.entity.ProductEntity
import com.example.fruitylicious.data.local.entity.ProductRecipeEntity
import com.example.fruitylicious.data.local.entity.RestockLogEntity
import com.example.fruitylicious.data.local.entity.StaffLogEntity
import com.example.fruitylicious.data.local.entity.TransactionEntity
import com.example.fruitylicious.data.local.entity.TransactionItemEntity
import com.example.fruitylicious.data.local.entity.UserEntity
import com.example.fruitylicious.data.local.entity.WasteLogEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncMapper @Inject constructor() {

    fun inventoryRecordId(entity: InventoryEntity): String {
        return "${entity.ingredientId}:${entity.branchId}"
    }

    fun branchRecordId(entity: BranchEntity): String {
        return entity.branchId.toString()
    }

    fun userRecordId(entity: UserEntity): String {
        return entity.userId.toString()
    }

    fun productRecordId(entity: ProductEntity): String {
        return entity.productId.toString()
    }

    fun ingredientRecordId(entity: IngredientEntity): String {
        return entity.ingredientId.toString()
    }

    fun recipeRecordId(entity: ProductRecipeEntity): String {
        return entity.recipeId.toString()
    }

    fun restockRecordId(entity: RestockLogEntity): String {
        return entity.restockId
    }

    fun adjustmentRecordId(entity: InventoryAdjustmentEntity): String {
        return entity.adjustmentId
    }

    fun wasteRecordId(entity: WasteLogEntity): String {
        return entity.wasteId
    }

    fun transactionRecordId(entity: TransactionEntity): String {
        return entity.transactionId
    }

    fun transactionItemRecordId(entity: TransactionItemEntity): String {
        return entity.transactionItemId
    }

    fun auditLogRecordId(entity: AuditLogEntity): String {
        return entity.logId
    }

    fun staffLogRecordId(entity: StaffLogEntity): String {
        return entity.logId
    }

    fun syncedInventory(entity: InventoryEntity, syncedAt: Long): InventoryEntity {
        return entity.copy(isSynced = true, syncedAt = syncedAt)
    }

    fun syncedBranch(entity: BranchEntity, syncedAt: Long): BranchEntity {
        return entity.copy(isSynced = true, syncedAt = syncedAt)
    }

    fun syncedUser(entity: UserEntity, syncedAt: Long): UserEntity {
        return entity.copy(isSynced = true, syncedAt = syncedAt)
    }

    fun syncedProduct(entity: ProductEntity, syncedAt: Long): ProductEntity {
        return entity.copy(isSynced = true, syncedAt = syncedAt)
    }

    fun syncedIngredient(entity: IngredientEntity, syncedAt: Long): IngredientEntity {
        return entity.copy(isSynced = true, syncedAt = syncedAt)
    }

    fun syncedRecipe(entity: ProductRecipeEntity, syncedAt: Long): ProductRecipeEntity {
        return entity.copy(isSynced = true, syncedAt = syncedAt)
    }
}