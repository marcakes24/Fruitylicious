package com.example.fruitylicious.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.fruitylicious.data.local.entity.ProductVariantEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductVariantDao {

    @Query(
        """
        SELECT * FROM product_variants
        ORDER BY productId ASC, price ASC
        """
    )
    fun observeAllVariants(): Flow<List<ProductVariantEntity>>

    @Query(
        """
        SELECT * FROM product_variants
        WHERE productId = :productId
        ORDER BY price ASC
        """
    )
    fun observeVariantsForProduct(productId: Int): Flow<List<ProductVariantEntity>>

    @Query(
        """
        SELECT * FROM product_variants
        WHERE productId = :productId
        ORDER BY price ASC
        """
    )
    suspend fun getVariantsForProduct(productId: Int): List<ProductVariantEntity>

    @Query("SELECT * FROM product_variants WHERE variantId = :variantId")
    suspend fun getVariantById(variantId: Int): ProductVariantEntity?

    @Query("SELECT * FROM product_variants WHERE isSynced = 0")
    suspend fun getUnsyncedVariants(): List<ProductVariantEntity>

    @Upsert
    suspend fun upsertVariant(variant: ProductVariantEntity)

    @Upsert
    suspend fun upsertVariants(variants: List<ProductVariantEntity>)

    @Query(
        """
        UPDATE product_variants
        SET isSynced = 1, syncedAt = :syncedAt
        WHERE variantId = :variantId
        """
    )
    suspend fun markSynced(
        variantId: Int,
        syncedAt: Long
    )

    @Query("DELETE FROM product_variants WHERE variantId = :variantId")
    suspend fun deleteVariant(variantId: Int)

    @Query("DELETE FROM product_variants WHERE productId = :productId")
    suspend fun deleteVariantsForProduct(productId: Int)
}