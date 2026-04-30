package com.example.fruitylicious.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "product_recipes",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["productId"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ProductVariantEntity::class,
            parentColumns = ["variantId"],
            childColumns = ["variantId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = IngredientEntity::class,
            parentColumns = ["ingredientId"],
            childColumns = ["ingredientId"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["productId"]),
        Index(value = ["variantId"]),
        Index(value = ["ingredientId"])
    ]
)
data class ProductRecipeEntity(
    @PrimaryKey
    val recipeId: Int,
    val productId: Int,
    val variantId: Int?,
    val ingredientId: Int,
    val quantityRequired: Double,
    val lastModified: Long,
    val isSynced: Boolean,
    val syncedAt: Long?
)