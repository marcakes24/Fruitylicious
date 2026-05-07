package com.example.fruitylicious.data.local.dao

data class WasteReasonRow(
    val reason: String,
    val count: Int,
    val b1Count: Int = 0,
    val b2Count: Int = 0
)

data class WasteItemRow(
    val ingredientName: String,
    val totalQuantity: Double,
    val unitType: String = "",
    val b1Qty: Double = 0.0,
    val b2Qty: Double = 0.0
)

data class StaffWasteRow(
    val staffName: String,
    val count: Int
)

data class WasteUnitTotal(
    val unitType: String,
    val totalQuantity: Double
)
