package com.example.fruitylicious.data.repository

import androidx.room.withTransaction
import com.example.fruitylicious.data.local.dao.ProductRecipeDao
import com.example.fruitylicious.data.local.dao.ProductVariantDao
import com.example.fruitylicious.data.local.db.PosDatabase
import com.example.fruitylicious.data.local.entity.ProductVariantEntity
import com.example.fruitylicious.sync.AutoSyncManager
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.absoluteValue

@Singleton
class ProductVariantRepository @Inject constructor(
    private val database: PosDatabase,
    private val productVariantDao: ProductVariantDao,
    private val productRecipeDao: ProductRecipeDao,
    private val autoSyncManager: AutoSyncManager
) {

    fun observeAllVariants(): Flow<List<ProductVariantEntity>> {
        return productVariantDao.observeAllVariants()
    }

    fun observeVariantsForProduct(productId: Int): Flow<List<ProductVariantEntity>> {
        return productVariantDao.observeVariantsForProduct(productId)
    }

    suspend fun getVariantsForProduct(productId: Int): List<ProductVariantEntity> {
        return productVariantDao.getVariantsForProduct(productId)
    }

    suspend fun getVariant(variantId: Int): ProductVariantEntity? {
        return productVariantDao.getVariantById(variantId)
    }

    suspend fun saveVariant(
        variantId: Int = generateVariantId(),
        productId: Int,
        sizeName: String,
        price: Double
    ): Result<Unit> {
        val cleanSizeName = sizeName.trim()

        if (productId <= 0) {
            return Result.failure(IllegalArgumentException("Product is required."))
        }

        if (cleanSizeName.isBlank()) {
            return Result.failure(IllegalArgumentException("Size name is required."))
        }

        if (price < 0.0) {
            return Result.failure(IllegalArgumentException("Price cannot be negative."))
        }

        val now = System.currentTimeMillis()

        productVariantDao.upsertVariant(
            ProductVariantEntity(
                variantId = variantId,
                productId = productId,
                sizeName = cleanSizeName,
                price = price,
                lastModified = now,
                isSynced = false,
                syncedAt = null
            )
        )

        autoSyncManager.requestSync("product_variant_changed")
        return Result.success(Unit)
    }

    suspend fun deleteVariant(variantId: Int): Result<Unit> {
        val now = System.currentTimeMillis()
        database.withTransaction {
            productVariantDao.softDeleteVariant(variantId, now)
            productRecipeDao.softDeleteRecipesByVariant(variantId, now)
        }
        autoSyncManager.requestSync("product_variant_deleted")
        return Result.success(Unit)
    }

    suspend fun deleteVariantsForProduct(productId: Int): Result<Unit> {
        val now = System.currentTimeMillis()
        productVariantDao.softDeleteVariantsByProduct(productId, now)
        autoSyncManager.requestSync("product_variant_deleted")
        return Result.success(Unit)
    }

    suspend fun getUnsyncedVariants(): List<ProductVariantEntity> {
        return productVariantDao.getUnsyncedVariants()
    }

    suspend fun markSynced(variantId: Int, syncedAt: Long) {
        productVariantDao.markSynced(variantId, syncedAt)
    }

    private fun generateVariantId(): Int {
        return System.currentTimeMillis().hashCode().absoluteValue
    }
}