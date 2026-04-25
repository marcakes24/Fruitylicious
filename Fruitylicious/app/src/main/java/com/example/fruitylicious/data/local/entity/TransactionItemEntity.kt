package com.example.fruitylicious.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transaction_items",
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["transaction_id"],
            childColumns = ["transaction_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["product_id"],
            childColumns = ["product_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["transaction_id"]),
        Index(value = ["product_id"])
    ]
)
data class TransactionItemEntity(

    @PrimaryKey
    @ColumnInfo(name = "transaction_item_id")
    val transactionItemId: String,

    @ColumnInfo(name = "transaction_id")
    val transactionId: String,

    @ColumnInfo(name = "product_id")
    val productId: String,

    @ColumnInfo(name = "quantity")
    val quantity: Int,

    @ColumnInfo(name = "subtotal")
    val subtotal: Double,

    @ColumnInfo(name = "last_modified")
    val lastModified: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "is_synced")
    val isSynced: Boolean = false,

    @ColumnInfo(name = "synced_at")
    val syncedAt: Long? = null
)