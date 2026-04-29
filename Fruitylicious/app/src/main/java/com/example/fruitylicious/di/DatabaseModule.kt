package com.example.fruitylicious.di

import android.content.Context
import androidx.room.Room
import com.example.fruitylicious.data.local.dao.AuditLogDao
import com.example.fruitylicious.data.local.dao.BranchDao
import com.example.fruitylicious.data.local.dao.IngredientDao
import com.example.fruitylicious.data.local.dao.InventoryAdjustmentDao
import com.example.fruitylicious.data.local.dao.InventoryDao
import com.example.fruitylicious.data.local.dao.ProductDao
import com.example.fruitylicious.data.local.dao.ProductRecipeDao
import com.example.fruitylicious.data.local.dao.RestockLogDao
import com.example.fruitylicious.data.local.dao.StaffLogDao
import com.example.fruitylicious.data.local.dao.TransactionDao
import com.example.fruitylicious.data.local.dao.TransactionItemDao
import com.example.fruitylicious.data.local.dao.UserDao
import com.example.fruitylicious.data.local.dao.WasteLogDao
import com.example.fruitylicious.data.local.db.PosDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun providePosDatabase(
        @ApplicationContext context: Context
    ): PosDatabase {
        return Room.databaseBuilder(
            context,
            PosDatabase::class.java,
            "fruitylicious_pos.db"
        )
            .fallbackToDestructiveMigration(false)
            .build()
    }

    @Provides
    fun provideBranchDao(database: PosDatabase): BranchDao {
        return database.branchDao()
    }

    @Provides
    fun provideUserDao(database: PosDatabase): UserDao {
        return database.userDao()
    }

    @Provides
    fun provideProductDao(database: PosDatabase): ProductDao {
        return database.productDao()
    }

    @Provides
    fun provideIngredientDao(database: PosDatabase): IngredientDao {
        return database.ingredientDao()
    }

    @Provides
    fun provideProductRecipeDao(database: PosDatabase): ProductRecipeDao {
        return database.productRecipeDao()
    }

    @Provides
    fun provideInventoryDao(database: PosDatabase): InventoryDao {
        return database.inventoryDao()
    }

    @Provides
    fun provideRestockLogDao(database: PosDatabase): RestockLogDao {
        return database.restockLogDao()
    }

    @Provides
    fun provideInventoryAdjustmentDao(database: PosDatabase): InventoryAdjustmentDao {
        return database.inventoryAdjustmentDao()
    }

    @Provides
    fun provideWasteLogDao(database: PosDatabase): WasteLogDao {
        return database.wasteLogDao()
    }

    @Provides
    fun provideTransactionDao(database: PosDatabase): TransactionDao {
        return database.transactionDao()
    }

    @Provides
    fun provideTransactionItemDao(database: PosDatabase): TransactionItemDao {
        return database.transactionItemDao()
    }

    @Provides
    fun provideAuditLogDao(database: PosDatabase): AuditLogDao {
        return database.auditLogDao()
    }

    @Provides
    fun provideStaffLogDao(database: PosDatabase): StaffLogDao {
        return database.staffLogDao()
    }
}
