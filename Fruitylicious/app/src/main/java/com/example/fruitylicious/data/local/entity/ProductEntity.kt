package com.example.fruitylicious.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(

    @PrimaryKey
    @ColumnInfo(name = "product_id")
    val productId: String,

    @ColumnInfo(name = "image")
    val image: ByteArray? = null,

    @ColumnInfo(name = "product_name")
    val productName: String,

    @ColumnInfo(name = "is_addon")
    val isAddon: Boolean = false,

    @ColumnInfo(name = "price")
    val price: Double,

    @ColumnInfo(name = "last_modified")
    val lastModified: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "is_synced")
    val isSynced: Boolean = false,

    @ColumnInfo(name = "synced_at")
    val syncedAt: Long? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ProductEntity
        if (productId != other.productId) return false
        if (image != null) {
            if (other.image == null) return false
            if (!image.contentEquals(other.image)) return false
        } else if (other.image != null) return false
        return true
    }

    override fun hashCode(): Int {
        var result = productId.hashCode()
        result = 31 * result + (image?.contentHashCode() ?: 0)
        return result
    }
}