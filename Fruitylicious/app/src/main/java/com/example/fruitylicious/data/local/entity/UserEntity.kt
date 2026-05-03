package com.example.fruitylicious.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [
        Index(value = ["username"], unique = true),
        Index(value = ["role"])
    ]
)
data class UserEntity(
    @PrimaryKey
    val userId: Int,
    val name: String,
    val role: String,
    val username: String,
    val password: String,
    val lastModified: Long,
    val isSynced: Boolean,
    val syncedAt: Long?,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
)