package com.example.fruitylicious.data.local.dao

data class RestockFrequencyRow(
    val ingredientName: String,
    val restockCount: Int,
    val avgUnits: Double
)