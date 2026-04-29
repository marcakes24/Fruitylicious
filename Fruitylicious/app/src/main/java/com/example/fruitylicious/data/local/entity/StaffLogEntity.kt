package com.example.fruitylicious.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "staff_logs",
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
        Index(value = ["clockIn"]),
        Index(value = ["clockOut"])
    ]
)
data class StaffLogEntity(
    @PrimaryKey
    val logId: String,
    val userId: Int,
    val branchId: Int,
    val image: String?,
    val clockIn: Long,
    val clockOut: Long?,
    val lastModified: Long,
    val isSynced: Boolean,
    val syncedAt: Long?
)