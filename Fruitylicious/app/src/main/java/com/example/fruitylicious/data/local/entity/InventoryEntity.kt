package com.example.fruitylicious.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "inventory",
    primaryKeys = ["ingredientId", "branchId"],
    foreignKeys = [
        ForeignKey(
            entity = IngredientEntity::class,
            parentColumns = ["ingredientId"],
            childColumns = ["ingredientId"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = BranchEntity::class,
            parentColumns = ["branchId"],
            childColumns = ["branchId"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["ingredientId"]),
        Index(value = ["branchId"])
    ]
)
data class InventoryEntity(
    val ingredientId: Int,
    val branchId: Int,
    val currentStock: Double,
    val lastModified: Long,
    val isSynced: Boolean,
    val syncedAt: Long?
)