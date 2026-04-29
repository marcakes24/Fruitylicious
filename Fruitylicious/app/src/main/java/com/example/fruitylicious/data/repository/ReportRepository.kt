package com.example.fruitylicious.data.repository

import com.example.fruitylicious.data.remote.api.ReportApi
import com.example.fruitylicious.data.remote.dto.AuditLogReportDto
import com.example.fruitylicious.data.remote.dto.InventoryReportDto
import com.example.fruitylicious.data.remote.dto.RestockReportDto
import com.example.fruitylicious.data.remote.dto.SalesReportDto
import com.example.fruitylicious.data.remote.dto.StaffLogReportDto
import com.example.fruitylicious.data.remote.dto.TransactionReportDto
import com.example.fruitylicious.data.remote.dto.WasteReportDto
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

    suspend fun getWasteReport(branchId: Int, from: Long, to: Long): Result<WasteReportDto> {
        return safeApiCall { reportApi.getWasteReport(branchId, from, to) }
    }

    suspend fun getRestockReport(branchId: Int, from: Long, to: Long): Result<RestockReportDto> {
        return safeApiCall { reportApi.getRestockReport(branchId, from, to) }
    }

    suspend fun getInventoryReport(branchId: Int): Result<InventoryReportDto> {
        return safeApiCall { reportApi.getInventoryReport(branchId) }
    }

    suspend fun getTransactionReport(branchId: Int, from: Long, to: Long): Result<TransactionReportDto> {
        return safeApiCall { reportApi.getTransactionReport(branchId, from, to) }
    }

    suspend fun getStaffLogsReport(branchId: Int, from: Long, to: Long): Result<StaffLogReportDto> {
        return safeApiCall { reportApi.getStaffLogsReport(branchId, from, to) }
    }

    suspend fun getAuditLogsReport(branchId: Int, from: Long, to: Long): Result<AuditLogReportDto> {
        return safeApiCall { reportApi.getAuditLogsReport(branchId, from, to) }
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
            Result.failure(exception)
        }
    }
}