package com.example.fruitylicious.data.local.dao

import androidx.room.Embedded
import androidx.room.Relation
import com.example.fruitylicious.data.local.entity.TransactionEntity
import com.example.fruitylicious.data.local.entity.TransactionItemAddonEntity
import com.example.fruitylicious.data.local.entity.TransactionItemEntity

data class TransactionItemWithAddons(
    @Embedded val item: TransactionItemEntity,
    @Relation(
        parentColumn = "transactionItemId",
        entityColumn = "transactionItemId"
    )
    val addons: List<TransactionItemAddonEntity>
)

data class TransactionWithItems(
    @Embedded val transaction: TransactionEntity,
    @Relation(
        entity = TransactionItemEntity::class,
        parentColumn = "transactionId",
        entityColumn = "transactionId"
    )
    val items: List<TransactionItemWithAddons>
)
