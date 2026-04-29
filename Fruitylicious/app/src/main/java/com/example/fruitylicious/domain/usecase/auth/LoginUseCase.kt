package com.example.fruitylicious.domain.usecase.auth

import com.example.fruitylicious.data.repository.AuthRepository
import com.example.fruitylicious.data.repository.LoginResult
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {

    suspend operator fun invoke(
        username: String,
        password: String
    ): LoginResult {
        return authRepository.login(username, password)
    }
}