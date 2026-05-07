package com.example.fruitylicious.data.remote.dto

import com.google.gson.annotations.SerializedName

data class SalesReportDto(
    val branchId: Int?,
    val branchName: String?,
    val from: Long,
    val to: Long,
    val totalSales: Double,
    val totalTransactions: Int,
    val averageTransactionValue: Double,
    val previousSales: Double,
    val cashTotal: Double,
    val gcashTotal: Double,
    val items: List<SalesReportItemDto>
)

data class SalesReportItemDto(
    val productId: Int,
    val productName: String,
    val quantitySold: Int,
    val grossSales: Double,
    val b1Qty: Int = 0,
    val b2Qty: Int = 0,
    val b1Amount: Double = 0.0,
    val b2Amount: Double = 0.0
)

data class InventoryReportDto(
    val branchId: Int,
    val branchName: String,
    val generatedAt: Long,
    val items: List<InventoryReportItemDto>
)

data class InventoryReportItemDto(
    val ingredientId: Int,
    val ingredientName: String,
    val unitType: String,
    val currentStock: Double,
    val lowStockThreshold: Double,
    val isLowStock: Boolean
)

data class WasteReportDto(
    val branchId: Int?,
    val branchName: String?,
    val from: Long,
    val to: Long,
    val totalWasteQuantity: Double,
    val items: List<WasteReportItemDto>
)

data class WasteReportItemDto(
    @SerializedName("wasteId") val wasteId: String,
    @SerializedName("ingredientId") val ingredientId: Int,
    @SerializedName("ingredientName") val ingredientName: String,
    @SerializedName("quantity") val quantity: Double,
    @SerializedName("unitType") val unitType: String,
    @SerializedName("reason") val reason: String,
    @SerializedName("userId") val userId: Int,
    @SerializedName("userName") val userName: String,
    @SerializedName("branchId") val branchId: Int? = null,
    @SerializedName("dateTime") val dateTime: Long,
    @SerializedName("image") val image: String? = null
)

data class RestockReportDto(
    val branchId: Int?,
    val branchName: String?,
    val from: Long,
    val to: Long,
    val totalRestockQuantity: Double,
    val items: List<RestockReportItemDto>
)

data class RestockReportItemDto(
    val restockId: String,
    val ingredientId: Int,
    val ingredientName: String,
    val quantityAdded: Double,
    val unitType: String,
    val supplier: String,
    val userId: Int,
    val userName: String,
    val dateTime: Long
)

data class TransactionReportDto(
    val branchId: Int?,
    val branchName: String?,
    val from: Long,
    val to: Long,
    val transactions: List<TransactionReportItemDto>
)

data class TransactionReportItemDto(
    val transactionId: String,
    val userId: Int,
    val userName: String,
    val branchId: Int,
    val totalAmount: Double,
    val paymentType: String,
    val dateTime: Long,
    val status: String,
    val items: List<TransactionLineReportDto>
)

data class TransactionLineReportDto(
    val productId: Int,
    val productName: String,
    val quantity: Int,
    val subtotal: Double,
    val sizeName: String? = null,
    val addons: List<String> = emptyList()
)

data class StaffLogReportDto(
    val branchId: Int?,
    val branchName: String?,
    val from: Long,
    val to: Long,
    val logs: List<StaffLogReportItemDto>
)

data class StaffLogReportItemDto(
    val logId: String,
    val userId: Int,
    val userName: String,
    val clockIn: Long,
    val clockOut: Long?,
    val image: String?,
    val branchId: Int? = null,
    val branchName: String? = null
)

data class AuditLogReportDto(
    val branchId: Int?,
    val branchName: String?,
    val from: Long,
    val to: Long,
    val logs: List<AuditLogReportItemDto>
)

data class AuditLogReportItemDto(
    val logId: String,
    val userId: Int,
    val userName: String,
    val action: String,
    val tableAffected: String,
    val timestamp: Long
)

data class SalesSummaryDto(
    val totalSales: Double,
    val totalTransactions: Int,
    val averageTransactionValue: Double,
    val previousSales: Double,
    val cashTotal: Double,
    val gcashTotal: Double
)

data class WasteSummaryDto(
    val totalWasteQuantity: Double,
    val totalWasteCost: Double = 0.0,
    val topWastedIngredient: String? = null
)

data class RestockSummaryDto(
    val totalRestockQuantity: Double,
    val totalRestockCost: Double = 0.0
)

data class InventoryAdjustmentSummaryDto(
    @SerializedName("totalAdjustmentEntries") val totalAdjustments: Int = 0,
    @SerializedName("totalAdjustmentAmount") val netAdjustmentQuantity: Double = 0.0
)

data class InventoryAdjustmentReportItemDto(
    val adjustmentId: String,
    val ingredientId: Int,
    val ingredientName: String,
    @SerializedName("adjustmentAmount") val adjustmentAmount: Double = 0.0,
    @SerializedName("unitType") val unitType: String? = null,
    val reason: String,
    val userId: Int,
    val userName: String,
    val dateTime: Long
)

data class PageResponseDto<T>(
    val items: List<T> = emptyList(),
    val page: Int = 0,
    val size: Int = 50,
    val totalItems: Long = 0L,
    val totalPages: Int = 0,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false
)
