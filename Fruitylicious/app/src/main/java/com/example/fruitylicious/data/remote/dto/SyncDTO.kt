package com.example.fruitylicious.data.remote.dto

import com.example.fruitylicious.data.local.entity.AuditLogEntity
import com.example.fruitylicious.data.local.entity.BranchEntity
import com.example.fruitylicious.data.local.entity.IngredientEntity
import com.example.fruitylicious.data.local.entity.InventoryAdjustmentEntity
import com.example.fruitylicious.data.local.entity.InventoryEntity
import com.example.fruitylicious.data.local.entity.ProductEntity
import com.example.fruitylicious.data.local.entity.ProductRecipeEntity
import com.example.fruitylicious.data.local.entity.RestockLogEntity
import com.example.fruitylicious.data.local.entity.StaffLogEntity
import com.example.fruitylicious.data.local.entity.TransactionEntity
import com.example.fruitylicious.data.local.entity.TransactionItemEntity
import com.example.fruitylicious.data.local.entity.UserEntity
import com.example.fruitylicious.data.local.entity.WasteLogEntity


data class PushRequestDto(
    val inventory: List<InventoryEntity> = emptyList(),
    val restockLogs: List<RestockLogEntity> = emptyList(),
    val inventoryAdjustments: List<InventoryAdjustmentEntity> = emptyList(),
    val wasteLogs: List<WasteLogEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val transactionItems: List<TransactionItemEntity> = emptyList(),
    val auditLogs: List<AuditLogEntity> = emptyList(),
    val staffLogs: List<StaffLogEntity> = emptyList()
)

data class SyncRecordResultDto(
    val id: Long = 0L,
    val success: Boolean = false,
    val error: String? = null
)


data class PushResponseDto(
    val inventory: List<SyncRecordResultDto>? = emptyList(),
    val restockLogs: List<SyncRecordResultDto>? = emptyList(),
    val inventoryAdjustments: List<SyncRecordResultDto>? = emptyList(),
    val wasteLogs: List<SyncRecordResultDto>? = emptyList(),
    val transactions: List<SyncRecordResultDto>? = emptyList(),
    val transactionItems: List<SyncRecordResultDto>? = emptyList(),
    val auditLogs: List<SyncRecordResultDto>? = emptyList(),
    val staffLogs: List<SyncRecordResultDto>? = emptyList()
)


data class PushResultDto(
    val tableName: String = "",
    val recordId: String = "",
    val success: Boolean = true,
    val message: String? = null
)

data class PullResponseDto(
    val since: String? = null,
    val branches: List<BranchEntity>? = emptyList(),
    val users: List<UserEntity>? = emptyList(),
    val products: List<ProductEntity>? = emptyList(),
    val ingredients: List<IngredientEntity>? = emptyList(),
    val recipes: List<ProductRecipeEntity>? = emptyList()
)