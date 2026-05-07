package com.example.fruitylicious.data.remote.api

import com.example.fruitylicious.data.remote.dto.AuditLogReportDto
import com.example.fruitylicious.data.remote.dto.AuditLogReportItemDto
import com.example.fruitylicious.data.remote.dto.InventoryAdjustmentReportItemDto
import com.example.fruitylicious.data.remote.dto.InventoryAdjustmentSummaryDto
import com.example.fruitylicious.data.remote.dto.InventoryReportDto
import com.example.fruitylicious.data.remote.dto.PageResponseDto
import com.example.fruitylicious.data.remote.dto.RestockReportDto
import com.example.fruitylicious.data.remote.dto.RestockReportItemDto
import com.example.fruitylicious.data.remote.dto.RestockSummaryDto
import com.example.fruitylicious.data.remote.dto.SalesReportDto
import com.example.fruitylicious.data.remote.dto.SalesReportItemDto
import com.example.fruitylicious.data.remote.dto.SalesSummaryDto
import com.example.fruitylicious.data.remote.dto.StaffLogReportDto
import com.example.fruitylicious.data.remote.dto.StaffLogReportItemDto
import com.example.fruitylicious.data.remote.dto.TransactionReportDto
import com.example.fruitylicious.data.remote.dto.TransactionReportItemDto
import com.example.fruitylicious.data.remote.dto.WasteReportDto
import com.example.fruitylicious.data.remote.dto.WasteReportItemDto
import com.example.fruitylicious.data.remote.dto.WasteSummaryDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ReportApi {

    @GET("api/reports/sales")
    suspend fun getSalesReport(
        @Query("branchId") branchId: Int,
        @Query("from") from: Long,
        @Query("to") to: Long
    ): Response<SalesReportDto>

    @GET("api/reports/sales/combined")
    suspend fun getCombinedSalesReport(
        @Query("from") from: Long,
        @Query("to") to: Long
    ): Response<SalesReportDto>

    @GET("api/reports/waste")
    suspend fun getWasteReport(
        @Query("branchId") branchId: Int,
        @Query("from") from: Long,
        @Query("to") to: Long
    ): Response<WasteReportDto>

    @GET("api/reports/restock")
    suspend fun getRestockReport(
        @Query("branchId") branchId: Int,
        @Query("from") from: Long,
        @Query("to") to: Long
    ): Response<RestockReportDto>

    @GET("api/reports/inventory")
    suspend fun getInventoryReport(
        @Query("branchId") branchId: Int
    ): Response<InventoryReportDto>

    @GET("api/reports/transactions")
    suspend fun getTransactionReport(
        @Query("branchId") branchId: Int,
        @Query("from") from: Long,
        @Query("to") to: Long
    ): Response<TransactionReportDto>

    @GET("api/reports/transactions/combined")
    suspend fun getCombinedTransactionReport(
        @Query("from") from: Long,
        @Query("to") to: Long
    ): Response<TransactionReportDto>

    @GET("api/reports/staff-logs")
    suspend fun getStaffLogsReport(
        @Query("branchId") branchId: Int,
        @Query("from") from: Long,
        @Query("to") to: Long
    ): Response<StaffLogReportDto>

    @GET("api/reports/audit-logs")
    suspend fun getAuditLogsReport(
        @Query("branchId") branchId: Int,
        @Query("from") from: Long,
        @Query("to") to: Long
    ): Response<AuditLogReportDto>

    // New optimized endpoints
    @GET("api/reports/sales/summary")
    suspend fun getSalesSummary(
        @Query("branchId") branchId: Int?,
        @Query("from") from: Long,
        @Query("to") to: Long
    ): Response<SalesSummaryDto>

    @GET("api/reports/sales/items")
    suspend fun getSalesItemsPage(
        @Query("branchId") branchId: Int?,
        @Query("from") from: Long,
        @Query("to") to: Long,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): Response<PageResponseDto<SalesReportItemDto>>

    @GET("api/reports/sales/top-items")
    suspend fun getTopSellingItems(
        @Query("branchId") branchId: Int?,
        @Query("from") from: Long,
        @Query("to") to: Long,
        @Query("limit") limit: Int = 5
    ): Response<List<SalesReportItemDto>>

    @GET("api/reports/waste/summary")
    suspend fun getWasteSummary(
        @Query("branchId") branchId: Int?,
        @Query("from") from: Long,
        @Query("to") to: Long
    ): Response<WasteSummaryDto>

    @GET("api/reports/waste/page")
    suspend fun getWastePage(
        @Query("branchId") branchId: Int?,
        @Query("from") from: Long,
        @Query("to") to: Long,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): Response<PageResponseDto<WasteReportItemDto>>

    @GET("api/reports/restock/summary")
    suspend fun getRestockSummary(
        @Query("branchId") branchId: Int?,
        @Query("from") from: Long,
        @Query("to") to: Long
    ): Response<RestockSummaryDto>

    @GET("api/reports/restock/page")
    suspend fun getRestockPage(
        @Query("branchId") branchId: Int?,
        @Query("from") from: Long,
        @Query("to") to: Long,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): Response<PageResponseDto<RestockReportItemDto>>

    @GET("api/reports/inventory-adjustments/summary")
    suspend fun getInventoryAdjustmentSummary(
        @Query("branchId") branchId: Int?,
        @Query("from") from: Long,
        @Query("to") to: Long
    ): Response<InventoryAdjustmentSummaryDto>

    @GET("api/reports/inventory-adjustments/page")
    suspend fun getInventoryAdjustmentPage(
        @Query("branchId") branchId: Int?,
        @Query("from") from: Long,
        @Query("to") to: Long,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): Response<PageResponseDto<InventoryAdjustmentReportItemDto>>

    @GET("api/reports/transactions/page")
    suspend fun getTransactionPage(
        @Query("branchId") branchId: Int?,
        @Query("from") from: Long,
        @Query("to") to: Long,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): Response<PageResponseDto<TransactionReportItemDto>>

    @GET("api/reports/staff-logs/page")
    suspend fun getStaffLogsPage(
        @Query("branchId") branchId: Int?,
        @Query("from") from: Long,
        @Query("to") to: Long,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): Response<PageResponseDto<StaffLogReportItemDto>>

    @GET("api/reports/audit-logs/page")
    suspend fun getAuditLogsPage(
        @Query("branchId") branchId: Int?,
        @Query("from") from: Long,
        @Query("to") to: Long,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): Response<PageResponseDto<AuditLogReportItemDto>>

}