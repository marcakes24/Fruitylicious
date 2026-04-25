package com.example.fruitylicious.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ingredients")
data class IngredientEntity(

    @PrimaryKey
    @ColumnInfo(name = "ingredient_id")
    val ingredientId: String,

    @ColumnInfo(name = "image")
    val image: ByteArray? = null,

    @ColumnInfo(name = "ingredient_name")
    val ingredientName: String,

    @ColumnInfo(name = "unit_type")
    val unitType: String,

    @ColumnInfo(name = "estimated_weight_per_unit")
    val estimatedWeightPerUnit: Double,

    @ColumnInfo(name = "is_packaging")
    val isPackaging: Boolean = false,

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
        other as IngredientEntity
        if (ingredientId != other.ingredientId) return false
        if (image != null) {
            if (other.image == null) return false
            if (!image.contentEquals(other.image)) return false
        } else if (other.image != null) return false
        return true
    }

    override fun hashCode(): Int {
        var result = ingredientId.hashCode()
        result = 31 * result + (image?.contentHashCode() ?: 0)
        return result
    }
}