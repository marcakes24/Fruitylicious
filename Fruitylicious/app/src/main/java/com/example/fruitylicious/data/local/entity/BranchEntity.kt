package com.example.fruitylicious.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "branches")
data class BranchEntity(
    @PrimaryKey
    val branchId: Int,
    val branchName: String,
    val address: String,
    val contactNumber: String,
    val lastModified: Long,
    val isSynced: Boolean,
    val syncedAt: Long?
)