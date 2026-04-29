package com.example.fruitylicious.data.remote.dto

data class LoginRequestDto(
    val username: String,
    val password: String,
    val branchId: Int
)

data class LoginResponseDto(
    val token: String,
    val userId: Int,
    val name: String,
    val role: String,
    val username: String,
    val expiresAt: Long
)