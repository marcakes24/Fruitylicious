package com.example.fruitylicious.data.local.entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "audit_logs",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
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
        Index(value = ["userId"]),
        Index(value = ["branchId"]),
        Index(value = ["action"]),
        Index(value = ["tableAffected"]),
        Index(value = ["timestamp"])
    ]
)
data class AuditLogEntity(
    @PrimaryKey
    val logId: String,
    val userId: Int,
    val branchId: Int,
    val action: String,
    val tableAffected: String,
    val timestamp: Long,
    val lastModified: Long,
    val isSynced: Boolean,
    val syncedAt: Long?
)
