package com.example.fruitylicious.data.repository

import com.example.fruitylicious.data.local.dao.ProductVariantDao
import com.example.fruitylicious.data.local.entity.ProductVariantEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.absoluteValue

@Singleton
class ProductVariantRepository @Inject constructor(
    private val productVariantDao: ProductVariantDao
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

        return Result.success(Unit)
    }

    suspend fun deleteVariant(variantId: Int): Result<Unit> {
        productVariantDao.deleteVariant(variantId)
        return Result.success(Unit)
    }

    suspend fun deleteVariantsForProduct(productId: Int): Result<Unit> {
        productVariantDao.deleteVariantsForProduct(productId)
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