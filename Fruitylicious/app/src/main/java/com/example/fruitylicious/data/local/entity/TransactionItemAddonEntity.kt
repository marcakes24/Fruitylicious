package com.example.fruitylicious.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transaction_item_addons",
    foreignKeys = [
        ForeignKey(
            entity = TransactionItemEntity::class,
            parentColumns = ["transactionItemId"],
            childColumns = ["transactionItemId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["productId"],
            childColumns = ["addonProductId"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["transactionItemId"]),
        Index(value = ["addonProductId"])
    ]
)
data class TransactionItemAddonEntity(
    @PrimaryKey
    val transactionItemAddonId: String,
    val transactionItemId: String,
    val addonProductId: Int,
    val quantity: Int,
    val subtotal: Double,
    val lastModified: Long,
    val isSynced: Boolean,
    val syncedAt: Long?
)