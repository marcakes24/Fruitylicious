package com.example.fruitylicious.data.local.dao

data class SalesBreakdownRow(
    val productName: String,
    val qty: Int,
    val totalAmount: Double
)

data class TopSellingItemRow(
    val productName: String,
    val totalQty: Int
)