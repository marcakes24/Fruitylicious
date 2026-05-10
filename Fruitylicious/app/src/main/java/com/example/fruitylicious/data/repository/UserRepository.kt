package com.example.fruitylicious.data.repository

import com.example.fruitylicious.data.local.dao.UserDao
import com.example.fruitylicious.data.local.entity.UserEntity
import com.example.fruitylicious.sync.AutoSyncManager
import com.example.fruitylicious.util.PasswordHasher
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao,
    private val autoSyncManager: AutoSyncManager,
    private val passwordHasher: PasswordHasher
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
        return userDao.getUserByUsername(username.trim().lowercase())
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
        val cleanUsername = username.trim().lowercase()

        if (cleanName.isBlank()) {
            return Result.failure(IllegalArgumentException("Name is required."))
        }

        if (cleanRole !in listOf("staff", "admin")) {
            return Result.failure(IllegalArgumentException("Role must be staff or owner."))
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

        val hashedPassword = if (passwordHasher.isHashed(password)) {
            password
        } else {
            passwordHasher.hashPassword(password)
        }
        val now = System.currentTimeMillis()

        userDao.upsertUser(
            UserEntity(
                userId = userId,
                name = cleanName,
                role = cleanRole,
                username = cleanUsername,
                password = hashedPassword,
                lastModified = now,
                isSynced = false,
                syncedAt = null
            )
        )

        autoSyncManager.requestSync("user_changed")
        return Result.success(Unit)
    }

    suspend fun deleteUser(userId: Int): Result<Unit> {
        if (userId <= 0) {
            return Result.failure(IllegalArgumentException("Invalid user."))
        }

        val now = System.currentTimeMillis()
        userDao.softDeleteUser(userId, now)
        autoSyncManager.requestSync("user_deleted")
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