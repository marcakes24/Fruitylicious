package com.example.fruitylicious.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "product_variants",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["productId"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["productId"]),
        Index(value = ["productId", "sizeName"], unique = true)
    ]
)
data class ProductVariantEntity(
    @PrimaryKey
    val variantId: Int,
    val productId: Int,
    val sizeName: String,
    val price: Double,
    val lastModified: Long,
    val isSynced: Boolean,
    val syncedAt: Long?,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
)