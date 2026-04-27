package com.example.fruitylicious.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "waste_logs",
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
data class WasteLogEntity(

    @PrimaryKey
    @ColumnInfo(name = "waste_id")
    val wasteId: String,

    @ColumnInfo(name = "ingredient_id")
    val ingredientId: String,

    @ColumnInfo(name = "branch_id")
    val branchId: String,

    @ColumnInfo(name = "quantity")
    val quantity: Double,

    @ColumnInfo(name = "image")
    val image: ByteArray? = null,

    @ColumnInfo(name = "reason")
    val reason: String,

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

) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as WasteLogEntity
        if (wasteId != other.wasteId) return false
        if (image != null) {
            if (other.image == null) return false
            if (!image.contentEquals(other.image)) return false
        } else if (other.image != null) return false
        return true
    }

    override fun hashCode(): Int {
        var result = wasteId.hashCode()
        result = 31 * result + (image?.contentHashCode() ?: 0)
        return result
    }
}