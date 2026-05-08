package com.example.fruitylicious.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
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
        Index(value = ["dateTime"]),
        Index(value = ["status"])
    ]
)
data class TransactionEntity(
    @PrimaryKey
    val transactionId: String,
    val userId: Int,
    val branchId: Int,
    val totalAmount: Double,
    @ColumnInfo(name = "transaction_name")
    val transactionName: String?,
    val paymentType: String,
    val dateTime: Long,
    val status: String,
    val lastModified: Long,
    val isSynced: Boolean,
    val syncedAt: Long?
)