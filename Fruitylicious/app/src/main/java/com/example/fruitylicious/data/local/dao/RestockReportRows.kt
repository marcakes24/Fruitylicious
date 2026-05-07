package com.example.fruitylicious.data.local.dao

data class RestockFrequencyRow(
    val ingredientName: String,
    val restockCount: Int,
    val avgUnits: Double
)

data class RestockIngredientRow(
    val ingredientName: String,
    val totalQuantity: Double,
    val count: Int,
    val unitType: String = ""
)

data class StaffRestockRow(
    val staffName: String,
    val count: Int
)

data class RestockUnitTotal(
    val unitType: String,
    val totalQuantity: Double
)
