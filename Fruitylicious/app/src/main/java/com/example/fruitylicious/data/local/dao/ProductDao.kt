package com.example.fruitylicious.data.local.dao

import androidx.room.*
import com.example.fruitylicious.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    // ─── Insert / Update / Delete ───────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<ProductEntity>)

    @Update
    suspend fun update(product: ProductEntity)

    @Delete
    suspend fun delete(product: ProductEntity)

    @Query("DELETE FROM products WHERE product_id = :productId")
    suspend fun deleteById(productId: String)

    @Query("DELETE FROM products")
    suspend fun deleteAll()

    // ─── Queries ────────────────────────────────────────────────────────────

    @Query("SELECT * FROM products WHERE product_id = :productId")
    suspend fun getById(productId: String): ProductEntity?

    @Query("SELECT * FROM products ORDER BY product_name ASC")
    fun getAll(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE is_addon = 0 ORDER BY product_name ASC")
    fun getMainProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE is_addon = 1 ORDER BY product_name ASC")
    fun getAddons(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE is_synced = 0")
    suspend fun getUnsynced(): List<ProductEntity>

    @Query("UPDATE products SET is_synced = 1, synced_at = :syncedAt WHERE product_id = :productId")
    suspend fun markSynced(productId: String, syncedAt: Long)
}