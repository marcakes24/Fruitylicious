package com.example.fruitylicious.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "branches")
data class BranchEntity(

    @PrimaryKey
    @ColumnInfo(name = "branch_id")
    val branchId: String,

    @ColumnInfo(name = "branch_name")
    val branchName: String,

    @ColumnInfo(name = "address")
    val address: String,

    @ColumnInfo(name = "contact_number")
    val contactNumber: String,

    @ColumnInfo(name = "last_modified")
    val lastModified: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "is_synced")
    val isSynced: Boolean = false,

    @ColumnInfo(name = "synced_at")
    val syncedAt: Long? = null
)