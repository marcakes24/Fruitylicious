package com.example.fruitylicious.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "products",
    indices = [
        Index(value = ["productName"]),
        Index(value = ["isAddon"])
    ]
)
data class ProductEntity(
    @PrimaryKey
    val productId: Int,
    val image: String?,
    val productName: String,
    val isAddon: Boolean,
    val price: Double,
    val lastModified: Long,
    val isSynced: Boolean,
    val syncedAt: Long?,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
)