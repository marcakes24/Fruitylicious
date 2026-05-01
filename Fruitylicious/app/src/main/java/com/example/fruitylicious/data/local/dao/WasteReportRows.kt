package com.example.fruitylicious.data.local.dao

data class WasteReasonRow(
    val reason: String,
    val count: Int
)

data class WasteItemRow(
    val ingredientName: String,
    val totalQuantity: Double
)