package com.example.fruitylicious.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.fruitylicious.data.local.entity.ProductVariantEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductVariantDao {

    @Query("SELECT * FROM product_variants WHERE isDeleted = 0 ORDER BY productId ASC, sizeName ASC")
    fun observeAllVariants(): Flow<List<ProductVariantEntity>>

    @Query(
        """
        SELECT * FROM product_variants
        WHERE productId = :productId
        AND isDeleted = 0
        ORDER BY price ASC
        """
    )
    fun observeVariantsForProduct(productId: Int): Flow<List<ProductVariantEntity>>

    @Query(
        """
        SELECT * FROM product_variants
        WHERE productId = :productId
        AND isDeleted = 0
        ORDER BY price ASC
        """
    )
    suspend fun getVariantsForProduct(productId: Int): List<ProductVariantEntity>

    @Query("SELECT * FROM product_variants WHERE variantId = :variantId AND isDeleted = 0")
    suspend fun getVariantById(variantId: Int): ProductVariantEntity?

    @Query("SELECT * FROM product_variants WHERE isSynced = 0")
    suspend fun getUnsyncedVariants(): List<ProductVariantEntity>

    @Upsert
    suspend fun upsertVariant(variant: ProductVariantEntity)

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

    @Upsert
    suspend fun upsertVariants(variants: List<ProductVariantEntity>)

    @Query(
        """
    SELECT * FROM product_variants
    WHERE productId = :productId
    AND isDeleted = 0
    ORDER BY price ASC
    """
    )
    fun observeVariantsByProduct(productId: Int): Flow<List<ProductVariantEntity>>

    @Query(
        """
    UPDATE product_variants
    SET isDeleted = 1,
        deletedAt = :now,
        lastModified = :now,
        isSynced = 0,
        syncedAt = NULL
    WHERE variantId = :variantId
    """
    )
    suspend fun softDeleteVariant(
        variantId: Int,
        now: Long
    )

    @Query(
        """
    UPDATE product_variants
    SET isDeleted = 1,
        deletedAt = :now,
        lastModified = :now,
        isSynced = 0,
        syncedAt = NULL
    WHERE productId = :productId
    """
    )
    suspend fun softDeleteVariantsByProduct(
        productId: Int,
        now: Long
    )
    

}