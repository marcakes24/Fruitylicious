package com.example.fruitylicious.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "restock_logs",
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
data class RestockLogEntity(
    @PrimaryKey
    val restockId: String,
    val ingredientId: Int,
    val branchId: Int,
    val userId: Int,
    val quantityAdded: Double,
    val supplier: String,
    val dateTime: Long,
    val lastModified: Long,
    val isSynced: Boolean,
    val syncedAt: Long?
)