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
            parentColumns = ["user_id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.SET_NULL
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
data class TransactionEntity(

    @PrimaryKey
    @ColumnInfo(name = "transaction_id")
    val transactionId: String,

    @ColumnInfo(name = "user_id")
    val userId: String? = null,

    @ColumnInfo(name = "branch_id")
    val branchId: String,

    @ColumnInfo(name = "total_amount")
    val totalAmount: Double,

    @ColumnInfo(name = "payment_type")
    val paymentType: String,

    @ColumnInfo(name = "date_time")
    val dateTime: Long = System.currentTimeMillis(),

    /** "completed" or "void" */
    @ColumnInfo(name = "status")
    val status: String,

    @ColumnInfo(name = "last_modified")
    val lastModified: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "is_synced")
    val isSynced: Boolean = false,

    @ColumnInfo(name = "synced_at")
    val syncedAt: Long? = null
)