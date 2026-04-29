package com.example.fruitylicious.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.fruitylicious.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    @Query("SELECT * FROM products ORDER BY productName ASC")
    fun observeAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products ORDER BY productName ASC")
    suspend fun getAllProducts(): List<ProductEntity>

    @Query("SELECT * FROM products WHERE isAddon = 0 ORDER BY productName ASC")
    fun observeMainProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE isAddon = 1 ORDER BY productName ASC")
    fun observeAddons(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE productId = :productId LIMIT 1")
    fun observeProduct(productId: Int): Flow<ProductEntity?>

    @Query("SELECT * FROM products WHERE productId = :productId LIMIT 1")
    suspend fun getProductById(productId: Int): ProductEntity?

    @Query("SELECT * FROM products WHERE productName LIKE '%' || :query || '%' ORDER BY productName ASC")
    fun searchProducts(query: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE isSynced = 0")
    suspend fun getUnsyncedProducts(): List<ProductEntity>

    @Upsert
    suspend fun upsertProduct(product: ProductEntity)

    @Upsert
    suspend fun upsertProducts(products: List<ProductEntity>)

    @Query("UPDATE products SET isSynced = 1, syncedAt = :syncedAt WHERE productId = :productId")
    suspend fun markSynced(productId: Int, syncedAt: Long)

    @Query("DELETE FROM products WHERE productId = :productId")
    suspend fun deleteProduct(productId: Int)
}