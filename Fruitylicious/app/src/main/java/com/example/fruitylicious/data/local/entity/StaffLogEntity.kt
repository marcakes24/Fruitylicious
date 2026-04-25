package com.example.fruitylicious.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "staff_logs",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["user_id"],
            childColumns = ["user_id"],
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
        Index(value = ["user_id"]),
        Index(value = ["branch_id"])
    ]
)
data class StaffLogEntity(

    @PrimaryKey
    @ColumnInfo(name = "log_id")
    val logId: String,

    @ColumnInfo(name = "user_id")
    val userId: String,

    @ColumnInfo(name = "branch_id")
    val branchId: String,

    @ColumnInfo(name = "image")
    val image: ByteArray? = null,

    @ColumnInfo(name = "clock_in")
    val clockIn: Long,

    @ColumnInfo(name = "clock_out")
    val clockOut: Long? = null,

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
        other as StaffLogEntity
        if (logId != other.logId) return false
        if (image != null) {
            if (other.image == null) return false
            if (!image.contentEquals(other.image)) return false
        } else if (other.image != null) return false
        return true
    }

    override fun hashCode(): Int {
        var result = logId.hashCode()
        result = 31 * result + (image?.contentHashCode() ?: 0)
        return result
    }
}