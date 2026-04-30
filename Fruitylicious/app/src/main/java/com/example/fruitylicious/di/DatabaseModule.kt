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
import androidx.room.RoomDatabase
import com.example.fruitylicious.data.local.dao.ProductVariantDao
import com.example.fruitylicious.data.local.dao.TransactionItemAddonDao
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
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    super.onCreate(db)
                    seedDatabase(db)
                }

                override fun onOpen(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    super.onOpen(db)
                    seedDatabase(db)
                }

                private fun seedDatabase(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    val now = System.currentTimeMillis()

                    db.execSQL(
                        """
                    INSERT OR IGNORE INTO branches (
                        branchId,
                        branchName,
                        address,
                        contactNumber,
                        lastModified,
                        isSynced,
                        syncedAt
                    ) VALUES (
                        1,
                        'Branch 1',
                        'Default Branch Address',
                        'N/A',
                        $now,
                        1,
                        $now
                    )
                    """.trimIndent()
                    )

                    db.execSQL(
                        """
                    INSERT OR IGNORE INTO users (
                        userId,
                        name,
                        role,
                        username,
                        password,
                        lastModified,
                        isSynced,
                        syncedAt
                    ) VALUES (
                        1,
                        'Default Admin',
                        'admin',
                        'admin',
                        'admin123',
                        $now,
                        1,
                        $now
                    )
                    """.trimIndent()
                    )

                    db.execSQL(
                        """
                    INSERT OR IGNORE INTO users (
                        userId,
                        name,
                        role,
                        username,
                        password,
                        lastModified,
                        isSynced,
                        syncedAt
                    ) VALUES (
                        2,
                        'Default Staff',
                        'staff',
                        'staff',
                        'staff123',
                        $now,
                        1,
                        $now
                    )
                    """.trimIndent()
                    )
                }
            })
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

    @Provides
    fun provideTransactionItemAddonDao(database: PosDatabase): TransactionItemAddonDao {
        return database.transactionItemAddonDao()
    }

    @Provides
    fun provideProductVariantDao(database: PosDatabase): ProductVariantDao {
        return database.productVariantDao()
    }
}
