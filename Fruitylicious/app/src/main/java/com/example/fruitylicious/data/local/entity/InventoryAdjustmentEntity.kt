package com.example.fruitylicious.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "inventory_adjustments",
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
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["ingredientId"]),
        Index(value = ["branchId"]),
        Index(value = ["userId"]),
        Index(value = ["dateTime"])
    ]
)
data class InventoryAdjustmentEntity(
    @PrimaryKey
    val adjustmentId: String,
    val ingredientId: Int,
    val branchId: Int,
    val userId: Int,
    val adjustmentAmount: Double,
    val reason: String,
    val dateTime: Long,
    val lastModified: Long,
    val isSynced: Boolean,
    val syncedAt: Long?
)