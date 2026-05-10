package com.example.fruitylicious.data.repository

import com.example.fruitylicious.data.remote.api.ReportApi
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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportRepository @Inject constructor(
    private val reportApi: ReportApi
) {

    suspend fun getSalesReport(branchId: Int, from: Long, to: Long): Result<SalesReportDto> {
        return safeApiCall { reportApi.getSalesReport(branchId, from, to) }
    }

    suspend fun getCombinedSalesReport(from: Long, to: Long): Result<SalesReportDto> {
        return safeApiCall { reportApi.getCombinedSalesReport(from, to) }
    }

    suspend fun getWasteReport(
        branchId: Int,
        from: Long,
        to: Long
    ): Result<WasteReportDto> {
        return safeApiCall { reportApi.getWasteReport(branchId, from, to) }
    }

    suspend fun getRestockReport(
        branchId: Int,
        from: Long,
        to: Long
    ): Result<RestockReportDto> {
        return safeApiCall { reportApi.getRestockReport(branchId, from, to) }
    }

    suspend fun getInventoryReport(
        branchId: Int
    ): Result<InventoryReportDto> {
        return safeApiCall { reportApi.getInventoryReport(branchId) }
    }

    suspend fun getTransactionReport(branchId: Int, from: Long, to: Long): Result<TransactionReportDto> {
        return safeApiCall { reportApi.getTransactionReport(branchId, from, to) }
    }

    suspend fun getCombinedTransactionReport(
        from: Long,
        to: Long
    ): Result<TransactionReportDto> {
        return safeApiCall { reportApi.getCombinedTransactionReport(from, to) }
    }

    suspend fun getStaffLogsReport(
        branchId: Int,
        from: Long,
        to: Long
    ): Result<StaffLogReportDto> {
        return safeApiCall { reportApi.getStaffLogsReport(branchId, from, to) }
    }

    suspend fun getAuditLogsReport(
        branchId: Int,
        from: Long,
        to: Long
    ): Result<AuditLogReportDto> {
        return safeApiCall { reportApi.getAuditLogsReport(branchId, from, to) }
    }

    // Optimized repo methods
    suspend fun getSalesSummary(branchId: Int?, from: Long, to: Long): Result<SalesSummaryDto> {
        return safeApiCall { reportApi.getSalesSummary(branchId, from, to) }
    }

    suspend fun getSalesItemsPage(branchId: Int?, from: Long, to: Long, page: Int, size: Int): Result<PageResponseDto<SalesReportItemDto>> {
        return safeApiCall { reportApi.getSalesItemsPage(branchId, from, to, page, size) }
    }

    suspend fun getTopSellingItems(branchId: Int?, from: Long, to: Long, limit: Int = 5): Result<List<SalesReportItemDto>> {
        return safeApiCall { reportApi.getTopSellingItems(branchId, from, to, limit) }
    }

    suspend fun getWasteSummary(branchId: Int?, from: Long, to: Long): Result<WasteSummaryDto> {
        return safeApiCall { reportApi.getWasteSummary(branchId, from, to) }
    }

    suspend fun getWastePage(branchId: Int?, from: Long, to: Long, page: Int, size: Int): Result<PageResponseDto<WasteReportItemDto>> {
        return safeApiCall { reportApi.getWastePage(branchId, from, to, page, size) }
    }

    suspend fun getRestockSummary(branchId: Int?, from: Long, to: Long): Result<RestockSummaryDto> {
        return safeApiCall { reportApi.getRestockSummary(branchId, from, to) }
    }

    suspend fun getRestockPage(branchId: Int?, from: Long, to: Long, page: Int, size: Int): Result<PageResponseDto<RestockReportItemDto>> {
        return safeApiCall { reportApi.getRestockPage(branchId, from, to, page, size) }
    }

    suspend fun getInventoryAdjustmentSummary(branchId: Int?, from: Long, to: Long): Result<InventoryAdjustmentSummaryDto> {
        return safeApiCall { reportApi.getInventoryAdjustmentSummary(branchId, from, to) }
    }

    suspend fun getInventoryAdjustmentPage(branchId: Int?, from: Long, to: Long, page: Int, size: Int): Result<PageResponseDto<InventoryAdjustmentReportItemDto>> {
        return safeApiCall { reportApi.getInventoryAdjustmentPage(branchId, from, to, page, size) }
    }

    suspend fun getTransactionPage(branchId: Int?, from: Long, to: Long, page: Int, size: Int): Result<PageResponseDto<TransactionReportItemDto>> {
        return safeApiCall { reportApi.getTransactionPage(branchId, from, to, page, size) }
    }

    suspend fun getStaffLogsPage(branchId: Int?, from: Long, to: Long, page: Int, size: Int): Result<PageResponseDto<StaffLogReportItemDto>> {
        return safeApiCall { reportApi.getStaffLogsPage(branchId, from, to, page, size) }
    }

    suspend fun getAuditLogsPage(branchId: Int?, from: Long, to: Long, page: Int, size: Int): Result<PageResponseDto<AuditLogReportItemDto>> {
        return safeApiCall { reportApi.getAuditLogsPage(branchId, from, to, page, size) }
    }

    private suspend fun <T> safeApiCall(call: suspend () -> retrofit2.Response<T>): Result<T> {
        return try {
            val response = call()

            if (response.isSuccessful) {
                val body = response.body()
                    ?: return Result.failure(IllegalStateException("Empty server response."))
                Result.success(body)
            } else {
                Result.failure(IllegalStateException("Server error ${response.code()}: ${response.message()}"))
            }
        } catch (exception: Exception) {
            if (exception is kotlinx.coroutines.CancellationException) throw exception
            Result.failure(exception)
        }
    }


}