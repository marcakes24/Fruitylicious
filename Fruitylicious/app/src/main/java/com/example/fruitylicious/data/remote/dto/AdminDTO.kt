package com.example.fruitylicious.data.remote.dto

data class AdminProductDto(
    val productId: Int,
    val image: String?,
    val productName: String,
    val isAddon: Boolean,
    val price: Double,
    val lastModified: Long,
    val isSynced: Boolean,
    val syncedAt: Long?
)

data class AdminIngredientDto(
    val ingredientId: Int,
    val image: String?,
    val ingredientName: String,
    val unitType: String,
    val isPackaging: Boolean,
    val lowStockThreshold: Double,
    val lastModified: Long,
    val isSynced: Boolean,
    val syncedAt: Long?
)

data class AdminRecipeDto(
    val recipeId: Int,
    val productId: Int,
    val ingredientId: Int,
    val quantityRequired: Double,
    val lastModified: Long,
    val isSynced: Boolean,
    val syncedAt: Long?
)

data class AdminUserDto(
    val userId: Int,
    val name: String,
    val role: String,
    val username: String,
    val password: String,
    val lastModified: Long,
    val isSynced: Boolean,
    val syncedAt: Long?
)