package com.example.fruitylicious.data.remote.api

import com.example.fruitylicious.data.remote.dto.AuditLogReportDto
import com.example.fruitylicious.data.remote.dto.InventoryReportDto
import com.example.fruitylicious.data.remote.dto.RestockReportDto
import com.example.fruitylicious.data.remote.dto.SalesReportDto
import com.example.fruitylicious.data.remote.dto.StaffLogReportDto
import com.example.fruitylicious.data.remote.dto.TransactionReportDto
import com.example.fruitylicious.data.remote.dto.WasteReportDto
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

}