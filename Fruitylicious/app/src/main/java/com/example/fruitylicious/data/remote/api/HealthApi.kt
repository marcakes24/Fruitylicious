package com.example.fruitylicious.data.remote.api

import retrofit2.Response
import retrofit2.http.GET

data class HealthResponseDto(
    val status: String,
    val serverTime: Long
)

interface HealthApi {
    @GET("api/health")
    suspend fun health(): Response<HealthResponseDto>
}
