package com.example.fruitylicious.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "restock_logs",
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
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["user_id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["ingredient_id"]),
        Index(value = ["branch_id"]),
        Index(value = ["user_id"])
    ]
)
data class RestockLogEntity(

    @PrimaryKey
    @ColumnInfo(name = "restock_id")
    val restockId: String,

    @ColumnInfo(name = "ingredient_id")
    val ingredientId: String,

    @ColumnInfo(name = "branch_id")
    val branchId: String,

    @ColumnInfo(name = "quantity_added")
    val quantityAdded: Double,

    @ColumnInfo(name = "supplier")
    val supplier: String? = null,

    @ColumnInfo(name = "date_time")
    val dateTime: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "user_id")
    val userId: String? = null,

    @ColumnInfo(name = "last_modified")
    val lastModified: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "is_synced")
    val isSynced: Boolean = false,

    @ColumnInfo(name = "synced_at")
    val syncedAt: Long? = null
)