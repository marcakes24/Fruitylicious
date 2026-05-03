package com.example.fruitylicious.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.fruitylicious.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Query("SELECT * FROM users WHERE isDeleted = 0 ORDER BY name ASC")
    fun observeAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE isDeleted = 0 ORDER BY name ASC")
    suspend fun getAllUsers(): List<UserEntity>

    @Query("SELECT * FROM users WHERE userId = :userId AND isDeleted = 0 LIMIT 1")
    fun observeUser(userId: Int): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE userId = :userId AND isDeleted = 0 LIMIT 1")
    suspend fun getUserById(userId: Int): UserEntity?

    @Query("SELECT * FROM users WHERE username = :username AND isDeleted = 0 LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE role = :role AND isDeleted = 0 ORDER BY name ASC")
    fun observeUsersByRole(role: String): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE isSynced = 0")
    suspend fun getUnsyncedUsers(): List<UserEntity>

    @Upsert
    suspend fun upsertUser(user: UserEntity)

    @Upsert
    suspend fun upsertUsers(users: List<UserEntity>)

    @Query("UPDATE users SET isSynced = 1, syncedAt = :syncedAt WHERE userId = :userId")
    suspend fun markSynced(userId: Int, syncedAt: Long)

    @Query("SELECT * FROM users WHERE isDeleted = 0 ORDER BY name ASC")
    fun observeUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE userId = :userId AND isDeleted = 0 LIMIT 1")
    fun observeUserById(userId: Int): Flow<UserEntity?>

    @Query(
        """
    SELECT * FROM users
    WHERE username = :username
    AND password = :password
    AND isDeleted = 0
    LIMIT 1
    """
    )
    suspend fun login(username: String, password: String): UserEntity?

    @Query(
        """
    UPDATE users
    SET isDeleted = 1,
        deletedAt = :now,
        lastModified = :now,
        isSynced = 0,
        syncedAt = NULL
    WHERE userId = :userId
    """
    )
    suspend fun softDeleteUser(
        userId: Int,
        now: Long
    )
}