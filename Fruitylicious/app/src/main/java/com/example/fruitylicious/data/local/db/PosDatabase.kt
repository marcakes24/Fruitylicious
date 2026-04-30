package com.example.fruitylicious.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.fruitylicious.data.local.dao.AuditLogDao
import com.example.fruitylicious.data.local.dao.BranchDao
import com.example.fruitylicious.data.local.dao.IngredientDao
import com.example.fruitylicious.data.local.dao.InventoryAdjustmentDao
import com.example.fruitylicious.data.local.dao.InventoryDao
import com.example.fruitylicious.data.local.dao.ProductDao
import com.example.fruitylicious.data.local.dao.ProductRecipeDao
import com.example.fruitylicious.data.local.dao.ProductVariantDao
import com.example.fruitylicious.data.local.dao.RestockLogDao
import com.example.fruitylicious.data.local.dao.StaffLogDao
import com.example.fruitylicious.data.local.dao.TransactionDao
import com.example.fruitylicious.data.local.dao.TransactionItemDao
import com.example.fruitylicious.data.local.dao.UserDao
import com.example.fruitylicious.data.local.dao.WasteLogDao
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
import com.example.fruitylicious.data.local.dao.TransactionItemAddonDao
import com.example.fruitylicious.data.local.entity.TransactionItemAddonEntity
import com.example.fruitylicious.data.local.entity.ProductVariantEntity


@Database(
    entities = [
        BranchEntity::class,
        UserEntity::class,
        ProductEntity::class,
        ProductVariantEntity::class,
        IngredientEntity::class,
        ProductRecipeEntity::class,
        InventoryEntity::class,
        RestockLogEntity::class,
        InventoryAdjustmentEntity::class,
        WasteLogEntity::class,
        TransactionEntity::class,
        TransactionItemEntity::class,
        TransactionItemAddonEntity::class,
        AuditLogEntity::class,
        StaffLogEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class PosDatabase : RoomDatabase() {
    abstract fun branchDao(): BranchDao
    abstract fun userDao(): UserDao
    abstract fun productDao(): ProductDao
    abstract fun productVariantDao(): ProductVariantDao
    abstract fun ingredientDao(): IngredientDao
    abstract fun productRecipeDao(): ProductRecipeDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun restockLogDao(): RestockLogDao
    abstract fun inventoryAdjustmentDao(): InventoryAdjustmentDao
    abstract fun wasteLogDao(): WasteLogDao
    abstract fun transactionDao(): TransactionDao
    abstract fun transactionItemDao(): TransactionItemDao
    abstract fun transactionItemAddonDao(): TransactionItemAddonDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun staffLogDao(): StaffLogDao
}