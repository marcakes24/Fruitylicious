package com.example.fruitylicious.data.remote.dto

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
    val grossSales: Double
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
    val wasteId: String,
    val ingredientId: Int,
    val ingredientName: String,
    val quantity: Double,
    val unitType: String,
    val reason: String,
    val userId: Int,
    val userName: String,
    val dateTime: Long,
    val image: String? = null
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