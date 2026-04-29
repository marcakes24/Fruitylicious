package com.example.fruitylicious.data.repository

import com.example.fruitylicious.data.local.dao.UserDao
import com.example.fruitylicious.data.local.entity.UserEntity
import com.example.fruitylicious.data.remote.api.AuthApi
import com.example.fruitylicious.data.remote.dto.LoginRequestDto
import com.example.fruitylicious.data.remote.dto.LoginResponseDto
import com.example.fruitylicious.util.BranchConfig
import com.example.fruitylicious.util.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

sealed class LoginResult {
    data class Success(val response: LoginResponseDto) : LoginResult()
    data class OfflineSuccess(val user: UserEntity) : LoginResult()
    data class Error(val message: String) : LoginResult()
}

@Singleton
class AuthRepository @Inject constructor(
    private val authApi: AuthApi,
    private val userDao: UserDao,
    private val sessionManager: SessionManager,
    private val branchConfig: BranchConfig
) {

    suspend fun login(username: String, password: String): LoginResult = withContext(Dispatchers.IO) {
        val trimmedUsername = username.trim()

        if (trimmedUsername.isBlank() || password.isBlank()) {
            return@withContext LoginResult.Error("Username and password are required.")
        }

        try {
            val response = authApi.login(
                LoginRequestDto(
                    username = trimmedUsername,
                    password = password,
                    branchId = branchConfig.branchId
                )
            )

            if (response.isSuccessful) {
                val body = response.body()
                    ?: return@withContext LoginResult.Error("Empty login response from server.")

                sessionManager.saveSession(
                    token = body.token,
                    userId = body.userId,
                    userName = body.name,
                    username = body.username,
                    role = body.role,
                    branchId = branchConfig.branchId,
                    loginTime = System.currentTimeMillis()
                )

                val now = System.currentTimeMillis()
                userDao.upsertUser(
                    UserEntity(
                        userId = body.userId,
                        name = body.name,
                        role = body.role,
                        username = body.username,
                        password = password,
                        lastModified = now,
                        isSynced = true,
                        syncedAt = now
                    )
                )

                LoginResult.Success(body)
            } else {
                loginOffline(trimmedUsername, password)
            }
        } catch (exception: Exception) {
            loginOffline(trimmedUsername, password)
        }
    }

    private suspend fun loginOffline(username: String, password: String): LoginResult {
        val user = userDao.getUserByUsername(username)

        return if (user != null && user.password == password) {
            sessionManager.saveSession(
                token = "",
                userId = user.userId,
                userName = user.name,
                username = user.username,
                role = user.role,
                branchId = branchConfig.branchId,
                loginTime = System.currentTimeMillis()
            )
            LoginResult.OfflineSuccess(user)
        } else {
            LoginResult.Error("Invalid username or password.")
        }
    }

    fun isLoggedIn(): Boolean {
        return sessionManager.isLoggedIn() && !sessionManager.isSessionExpired()
    }

    fun currentRole(): String? {
        return sessionManager.getRole()
    }

    fun currentUserId(): Int {
        return sessionManager.getUserId()
    }

    fun currentBranchId(): Int {
        return sessionManager.getBranchId()
    }

    fun logout() {
        sessionManager.clearSession()
    }
}