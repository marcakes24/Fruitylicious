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
            Result.failure(e)
        }
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