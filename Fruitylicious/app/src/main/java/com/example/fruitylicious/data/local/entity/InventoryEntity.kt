package com.example.fruitylicious.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "inventory",
    primaryKeys = ["ingredient_id", "branch_id"],
    foreignKeys = [
        ForeignKey(
            entity = IngredientEntity::class,
            parentColumns = ["ingredient_id"],
            childColumns = ["ingredient_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = BranchEntity::class,
            parentColumns = ["branch_id"],
            childColumns = ["branch_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["ingredient_id"]),
        Index(value = ["branch_id"])
    ]
)
data class InventoryEntity(

    @ColumnInfo(name = "ingredient_id")
    val ingredientId: String,

    @ColumnInfo(name = "branch_id")
    val branchId: String,

    @ColumnInfo(name = "current_stock")
    val currentStock: Double,

    @ColumnInfo(name = "last_modified")
    val lastModified: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "is_synced")
    val isSynced: Boolean = false,

    @ColumnInfo(name = "synced_at")
    val syncedAt: Long? = null
)