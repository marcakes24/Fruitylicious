package com.example.fruitylicious.data.remote.api

import com.example.fruitylicious.data.remote.dto.PullResponseDto
import com.example.fruitylicious.data.remote.dto.PushRequestDto
import com.example.fruitylicious.data.remote.dto.PushResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface SyncApi {

    @POST("api/sync/push")
    suspend fun push(
        @Body request: PushRequestDto
    ): Response<PushResponseDto>

    @GET("api/sync/pull")
    suspend fun pull(
        @Query("since") since: Long
    ): Response<PullResponseDto>
}