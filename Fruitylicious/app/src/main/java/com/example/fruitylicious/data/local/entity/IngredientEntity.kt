package com.example.fruitylicious.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ingredients",
    indices = [
        Index(value = ["ingredientName"]),
        Index(value = ["isPackaging"])
    ]
)
data class IngredientEntity(
    @PrimaryKey
    val ingredientId: Int,
    val image: String?,
    val ingredientName: String,
    val unitType: String,
    val isPackaging: Boolean,
    val lowStockThreshold: Double,
    val lastModified: Long,
    val isSynced: Boolean,
    val syncedAt: Long?,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
)