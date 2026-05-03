package com.example.fruitylicious.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.fruitylicious.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    @Query("SELECT * FROM products WHERE isDeleted = 0 ORDER BY productName ASC")
    fun observeAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE isDeleted = 0 ORDER BY productName ASC")
    suspend fun getAllProducts(): List<ProductEntity>

    @Query("SELECT * FROM products WHERE isAddon = 0 AND isDeleted = 0 ORDER BY productName ASC")
    fun observeMainProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE isAddon = 1 AND isDeleted = 0 ORDER BY productName ASC")
    fun observeAddons(): Flow<List<ProductEntity>>
    @Query("SELECT * FROM products WHERE productId = :productId AND isDeleted = 0 LIMIT 1")
    fun observeProduct(productId: Int): Flow<ProductEntity?>

    @Query("SELECT * FROM products WHERE productId = :productId AND isDeleted = 0 LIMIT 1")
    suspend fun getProductById(productId: Int): ProductEntity?

    @Query("SELECT * FROM products WHERE productName LIKE '%' || :query || '%' AND isDeleted = 0 ORDER BY productName ASC")
    fun searchProducts(query: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE isSynced = 0")
    suspend fun getUnsyncedProducts(): List<ProductEntity>

    @Upsert
    suspend fun upsertProduct(product: ProductEntity)

    @Upsert
    suspend fun upsertProducts(products: List<ProductEntity>)

    @Query("UPDATE products SET isSynced = 1, syncedAt = :syncedAt WHERE productId = :productId")
    suspend fun markSynced(productId: Int, syncedAt: Long)

    @Query(
        """
        UPDATE products
        SET image = :imagePath,
            lastModified = :lastModified,
            isSynced = 0,
            syncedAt = NULL
        WHERE productId = :productId
        """
    )
    suspend fun updateProductImage(
        productId: Int,
        imagePath: String?,
        lastModified: Long
    )

    @Query("SELECT * FROM products WHERE isDeleted = 0 ORDER BY productName ASC")
    fun observeProducts(): Flow<List<ProductEntity>>

    @Query(
        """
    UPDATE products
    SET isDeleted = 1,
        deletedAt = :now,
        lastModified = :now,
        isSynced = 0,
        syncedAt = NULL
    WHERE productId = :productId
    """
    )
    suspend fun softDeleteProduct(
        productId: Int,
        now: Long
    )
}