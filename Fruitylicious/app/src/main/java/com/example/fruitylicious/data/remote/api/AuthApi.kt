package com.example.fruitylicious.data.remote.api

import com.example.fruitylicious.data.remote.dto.LoginRequestDto
import com.example.fruitylicious.data.remote.dto.LoginResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("/api/auth/login")
    suspend fun login(
        @Body request: LoginRequestDto
    ): Response<LoginResponseDto>
}