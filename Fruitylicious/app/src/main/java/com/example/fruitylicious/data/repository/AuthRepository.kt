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

    suspend fun login(
        username: String,
        password: String
    ): LoginResult = withContext(Dispatchers.IO) {
        val trimmedUsername = username.trim()

        if (trimmedUsername.isBlank() || password.isBlank()) {
            return@withContext LoginResult.Error("Username and password are required.")
        }

        val localLoginResult = loginOfflineIfValid(
            username = trimmedUsername,
            password = password
        )

        if (localLoginResult != null) {
            return@withContext localLoginResult
        }

        return@withContext loginOnline(
            username = trimmedUsername,
            password = password
        )
    }

    private suspend fun loginOfflineIfValid(
        username: String,
        password: String
    ): LoginResult? {
        val user = userDao.getUserByUsername(username)

        if (user == null || user.password != password) {
            return null
        }

        sessionManager.saveSession(
            token = "",
            userId = user.userId,
            userName = user.name,
            username = user.username,
            role = user.role,
            branchId = branchConfig.branchId,
            loginTime = System.currentTimeMillis()
        )

        return LoginResult.OfflineSuccess(user)
    }

    private suspend fun loginOnline(
        username: String,
        password: String
    ): LoginResult {
        return try {
            val response = authApi.login(
                LoginRequestDto(
                    username = username,
                    password = password,
                    branchId = branchConfig.branchId
                )
            )

            if (!response.isSuccessful) {
                return LoginResult.Error("Invalid username or password.")
            }

            val body = response.body()
                ?: return LoginResult.Error("Empty login response from server.")

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
        } catch (exception: Exception) {
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