package com.example.fruitylicious.data.local.dao

data class SalesBreakdownRow(
    val productName: String,
    val qty: Int,
    val totalAmount: Double,
    val b1Qty: Int = 0,
    val b2Qty: Int = 0,
    val b1Amount: Double = 0.0,
    val b2Amount: Double = 0.0
)

data class TopSellingItemRow(
    val productName: String,
    val totalQty: Int
)

data class TopAddonRow(
    val addonName: String,
    val totalQty: Int
)

data class TopComboRow(
    val comboName: String,
    val count: Int
)

data class StaffSalesRow(
    val staffName: String,
    val transactionCount: Int,
    val totalSales: Double
)
