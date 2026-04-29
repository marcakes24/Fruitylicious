package com.example.fruitylicious.domain.usecase.admin

import com.example.fruitylicious.data.local.entity.UserEntity
import com.example.fruitylicious.data.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ManageUserUseCase @Inject constructor(
    private val userRepository: UserRepository
) {

    fun observeUsers(): Flow<List<UserEntity>> {
        return userRepository.observeUsers()
    }

    fun observeUser(userId: Int): Flow<UserEntity?> {
        return userRepository.observeUser(userId)
    }

    fun observeUsersByRole(role: String): Flow<List<UserEntity>> {
        return userRepository.observeUsersByRole(role)
    }

    suspend fun getUser(userId: Int): UserEntity? {
        return userRepository.getUser(userId)
    }

    suspend fun saveUser(
        userId: Int,
        name: String,
        role: String,
        username: String,
        password: String
    ): Result<Unit> {
        return userRepository.saveUser(
            userId = userId,
            name = name,
            role = role,
            username = username,
            password = password
        )
    }

    suspend fun deleteUser(userId: Int): Result<Unit> {
        return userRepository.deleteUser(userId)
    }
}