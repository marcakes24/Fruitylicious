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
        return try {
            val response = reportApi.getWasteReport(
                branchId = branchId,
                from = from,
                to = to
            )

            if (response.isSuccessful) {
                val body = response.body()

                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(
                        IllegalStateException("Empty waste report response.")
                    )
                }
            } else {
                Result.failure(
                    IllegalStateException(
                        "Waste report request failed: ${response.code()} ${response.message()}"
                    )
                )
            }
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    suspend fun getRestockReport(
        branchId: Int,
        from: Long,
        to: Long
    ): Result<RestockReportDto> {
        return try {
            val response = reportApi.getRestockReport(
                branchId = branchId,
                from = from,
                to = to
            )

            if (response.isSuccessful) {
                val body = response.body()

                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(
                        IllegalStateException("Empty restock report response.")
                    )
                }
            } else {
                Result.failure(
                    IllegalStateException(
                        "Restock report request failed: ${response.code()} ${response.message()}"
                    )
                )
            }
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    suspend fun getInventoryReport(
        branchId: Int
    ): Result<InventoryReportDto> {
        return try {
            val response = reportApi.getInventoryReport(
                branchId = branchId
            )

            if (response.isSuccessful) {
                val body = response.body()

                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(
                        IllegalStateException("Empty inventory response.")
                    )
                }
            } else {
                Result.failure(
                    IllegalStateException(
                        "Inventory request failed: ${response.code()} ${response.message()}"
                    )
                )
            }
        } catch (exception: Exception) {
            Result.failure(
                exception
            )
        }
    }

    suspend fun getTransactionReport(branchId: Int, from: Long, to: Long): Result<TransactionReportDto> {
        return safeApiCall { reportApi.getTransactionReport(branchId, from, to) }
    }

    suspend fun getCombinedTransactionReport(
        from: Long,
        to: Long
    ): Result<TransactionReportDto> {
        return try {
            val response = reportApi.getCombinedTransactionReport(
                from = from,
                to = to
            )

            if (response.isSuccessful) {
                val body = response.body()

                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(IllegalStateException("Empty combined transaction response."))
                }
            } else {
                Result.failure(
                    IllegalStateException("Combined transaction request failed: ${response.code()} ${response.message()}")
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getStaffLogsReport(
        branchId: Int,
        from: Long,
        to: Long
    ): Result<StaffLogReportDto> {
        return try {
            val response = reportApi.getStaffLogsReport(
                branchId = branchId,
                from = from,
                to = to
            )

            if (response.isSuccessful) {
                val body = response.body()

                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(IllegalStateException("Empty staff logs response."))
                }
            } else {
                Result.failure(
                    IllegalStateException("Staff logs request failed: ${response.code()} ${response.message()}")
                )
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Result.failure(e)
        }
    }

    suspend fun getAuditLogsReport(
        branchId: Int,
        from: Long,
        to: Long
    ): Result<AuditLogReportDto> {
        return try {
            val response = reportApi.getAuditLogsReport(
                branchId = branchId,
                from = from,
                to = to
            )

            if (response.isSuccessful) {
                val body = response.body()

                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(IllegalStateException("Empty audit logs response."))
                }
            } else {
                Result.failure(
                    IllegalStateException("Audit logs request failed: ${response.code()} ${response.message()}")
                )
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Result.failure(e)
        }
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