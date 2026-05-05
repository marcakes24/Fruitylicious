package com.example.fruitylicious.data.repository

import androidx.room.withTransaction
import com.example.fruitylicious.data.local.dao.ProductDao
import com.example.fruitylicious.data.local.dao.ProductRecipeDao
import com.example.fruitylicious.data.local.dao.ProductVariantDao
import com.example.fruitylicious.data.local.db.PosDatabase
import com.example.fruitylicious.data.local.entity.ProductEntity
import com.example.fruitylicious.sync.AutoSyncManager
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductRepository @Inject constructor(
    private val database: PosDatabase,
    private val productDao: ProductDao,
    private val productVariantDao: ProductVariantDao,
    private val productRecipeDao: ProductRecipeDao,
    private val autoSyncManager: AutoSyncManager
) {

    fun observeProducts(): Flow<List<ProductEntity>> {
        return productDao.observeAllProducts()
    }

    fun observeMainProducts(): Flow<List<ProductEntity>> {
        return productDao.observeMainProducts()
    }

    fun observeAddons(): Flow<List<ProductEntity>> {
        return productDao.observeAddons()
    }

    fun observeProduct(productId: Int): Flow<ProductEntity?> {
        return productDao.observeProduct(productId)
    }

    fun searchProducts(query: String): Flow<List<ProductEntity>> {
        return productDao.searchProducts(query.trim())
    }

    suspend fun getProducts(): List<ProductEntity> {
        return productDao.getAllProducts()
    }

    suspend fun getProduct(productId: Int): ProductEntity? {
        return productDao.getProductById(productId)
    }

    suspend fun saveProduct(
        productId: Int,
        image: String?,
        productName: String,
        isAddon: Boolean,
        price: Double
    ): Result<Unit> {
        val cleanName = productName.trim()

        if (cleanName.isBlank()) {
            return Result.failure(IllegalArgumentException("Product name is required."))
        }

        if (price < 0.0) {
            return Result.failure(IllegalArgumentException("Price cannot be negative."))
        }

        val now = System.currentTimeMillis()

        productDao.upsertProduct(
            ProductEntity(
                productId = productId,
                image = image?.trim()?.ifBlank { null },
                productName = cleanName,
                isAddon = isAddon,
                price = price,
                lastModified = now,
                isSynced = false,
                syncedAt = null
            )
        )

        autoSyncManager.requestSync("product_changed")
        return Result.success(Unit)
    }

    suspend fun deleteProduct(productId: Int): Result<Unit> {
        val now = System.currentTimeMillis()
        database.withTransaction {
            productDao.softDeleteProduct(productId, now)
            productVariantDao.softDeleteVariantsByProduct(productId, now)
            productRecipeDao.softDeleteRecipesByProduct(productId, now)
        }
        autoSyncManager.requestSync("product_deleted")
        return Result.success(Unit)
    }

    suspend fun updateProductImage(
        productId: Int,
        imagePath: String?,
        lastModified: Long
    ) {
        productDao.updateProductImage(productId, imagePath, lastModified)
    }

    suspend fun getUnsyncedProducts(): List<ProductEntity> {
        return productDao.getUnsyncedProducts()
    }

    suspend fun markSynced(productId: Int, syncedAt: Long) {
        productDao.markSynced(productId, syncedAt)
    }

    suspend fun savePulledProducts(products: List<ProductEntity>) {
        productDao.upsertProducts(products.map { it.copy(isSynced = true, syncedAt = it.syncedAt ?: System.currentTimeMillis()) })
    }
}