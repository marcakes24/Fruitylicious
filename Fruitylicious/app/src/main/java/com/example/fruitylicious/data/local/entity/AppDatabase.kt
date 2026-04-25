package com.example.fruitylicious.data.local.entity

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.fruitylicious.data.local.dao.*

@Database(
    entities = [
        ProductEntity::class,
        IngredientEntity::class,
        BranchEntity::class,
        UserEntity::class,
        ProductRecipeEntity::class,
        InventoryEntity::class,
        RestockLogEntity::class,
        InventoryAdjustmentEntity::class,
        AuditLogEntity::class,
        WasteLogEntity::class,
        TransactionEntity::class,
        TransactionItemEntity::class,
        StaffLogEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao
    abstract fun ingredientDao(): IngredientDao
    abstract fun branchDao(): BranchDao
    abstract fun userDao(): UserDao
    abstract fun productRecipeDao(): ProductRecipeDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun restockLogDao(): RestockLogDao
    abstract fun inventoryAdjustmentDao(): InventoryAdjustmentDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun wasteLogDao(): WasteLogDao
    abstract fun transactionDao(): TransactionDao
    abstract fun transactionItemDao(): TransactionItemDao
    abstract fun staffLogDao(): StaffLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}