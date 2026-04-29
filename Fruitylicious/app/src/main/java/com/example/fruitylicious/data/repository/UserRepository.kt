package com.example.fruitylicious.data.repository

import com.example.fruitylicious.data.local.dao.UserDao
import com.example.fruitylicious.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao
) {

    fun observeUsers(): Flow<List<UserEntity>> {
        return userDao.observeAllUsers()
    }

    fun observeUser(userId: Int): Flow<UserEntity?> {
        return userDao.observeUser(userId)
    }

    fun observeUsersByRole(role: String): Flow<List<UserEntity>> {
        return userDao.observeUsersByRole(role.trim())
    }

    suspend fun getUsers(): List<UserEntity> {
        return userDao.getAllUsers()
    }

    suspend fun getUser(userId: Int): UserEntity? {
        return userDao.getUserById(userId)
    }

    suspend fun getUserByUsername(username: String): UserEntity? {
        return userDao.getUserByUsername(username.trim())
    }

    suspend fun saveUser(
        userId: Int,
        name: String,
        role: String,
        username: String,
        password: String
    ): Result<Unit> {
        val cleanName = name.trim()
        val cleanRole = role.trim().lowercase()
        val cleanUsername = username.trim()

        if (cleanName.isBlank()) {
            return Result.failure(IllegalArgumentException("Name is required."))
        }

        if (cleanRole !in listOf("staff", "admin")) {
            return Result.failure(IllegalArgumentException("Role must be staff or admin."))
        }

        if (cleanUsername.isBlank()) {
            return Result.failure(IllegalArgumentException("Username is required."))
        }

        if (password.isBlank()) {
            return Result.failure(IllegalArgumentException("Password is required."))
        }

        val existingUser = userDao.getUserByUsername(cleanUsername)

        if (existingUser != null && existingUser.userId != userId) {
            return Result.failure(IllegalStateException("Username is already taken."))
        }

        val now = System.currentTimeMillis()

        userDao.upsertUser(
            UserEntity(
                userId = userId,
                name = cleanName,
                role = cleanRole,
                username = cleanUsername,
                password = password,
                lastModified = now,
                isSynced = false,
                syncedAt = null
            )
        )

        return Result.success(Unit)
    }

    suspend fun deleteUser(userId: Int): Result<Unit> {
        if (userId <= 0) {
            return Result.failure(IllegalArgumentException("Invalid user."))
        }

        userDao.deleteUser(userId)
        return Result.success(Unit)
    }

    suspend fun getUnsyncedUsers(): List<UserEntity> {
        return userDao.getUnsyncedUsers()
    }

    suspend fun markSynced(userId: Int, syncedAt: Long) {
        userDao.markSynced(userId, syncedAt)
    }

    suspend fun savePulledUsers(users: List<UserEntity>) {
        val syncedAt = System.currentTimeMillis()

        userDao.upsertUsers(
            users.map {
                it.copy(
                    isSynced = true,
                    syncedAt = it.syncedAt ?: syncedAt
                )
            }
        )
    }
}