package com.example.fruitylicious.domain.usecase.auth

import com.example.fruitylicious.data.repository.AuthRepository
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {

    operator fun invoke() {
        authRepository.logout()
    }
}