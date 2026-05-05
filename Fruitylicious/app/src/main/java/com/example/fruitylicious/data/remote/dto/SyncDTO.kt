package com.example.fruitylicious.data.remote.dto

import com.example.fruitylicious.data.local.entity.AuditLogEntity
import com.example.fruitylicious.data.local.entity.BranchEntity
import com.example.fruitylicious.data.local.entity.IngredientEntity
import com.example.fruitylicious.data.local.entity.InventoryAdjustmentEntity
import com.example.fruitylicious.data.local.entity.InventoryEntity
import com.example.fruitylicious.data.local.entity.ProductEntity
import com.example.fruitylicious.data.local.entity.ProductRecipeEntity
import com.example.fruitylicious.data.local.entity.ProductVariantEntity
import com.example.fruitylicious.data.local.entity.RestockLogEntity
import com.example.fruitylicious.data.local.entity.StaffLogEntity
import com.example.fruitylicious.data.local.entity.TransactionEntity
import com.example.fruitylicious.data.local.entity.TransactionItemAddonEntity
import com.example.fruitylicious.data.local.entity.TransactionItemEntity
import com.example.fruitylicious.data.local.entity.UserEntity
import com.example.fruitylicious.data.local.entity.WasteLogEntity

data class PushRequestDto(
    val branches: List<BranchEntity> = emptyList(),
    val users: List<UserEntity> = emptyList(),
    val products: List<ProductEntity> = emptyList(),
    val ingredients: List<IngredientEntity> = emptyList(),
    val productVariants: List<ProductVariantEntity> = emptyList(),
    val productRecipes: List<ProductRecipeEntity> = emptyList(),
    val inventory: List<InventoryEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val transactionItems: List<TransactionItemEntity> = emptyList(),
    val transactionItemAddons: List<TransactionItemAddonEntity> = emptyList(),
    val restockLogs: List<RestockLogEntity> = emptyList(),
    val inventoryAdjustments: List<InventoryAdjustmentEntity> = emptyList(),
    val wasteLogs: List<WasteLogEntity> = emptyList(),
    val staffLogs: List<StaffLogEntity> = emptyList(),
    val auditLogs: List<AuditLogEntity> = emptyList()
)

data class SyncRecordResultDto(
    val recordId: String = "",
    val success: Boolean = false,
    val error: String? = null
)

data class PushResponseDto(
    val branches: List<SyncRecordResultDto> = emptyList(),
    val users: List<SyncRecordResultDto> = emptyList(),
    val products: List<SyncRecordResultDto> = emptyList(),
    val ingredients: List<SyncRecordResultDto> = emptyList(),
    val productVariants: List<SyncRecordResultDto> = emptyList(),
    val productRecipes: List<SyncRecordResultDto> = emptyList(),
    val inventory: List<SyncRecordResultDto> = emptyList(),
    val transactions: List<SyncRecordResultDto> = emptyList(),
    val transactionItems: List<SyncRecordResultDto> = emptyList(),
    val transactionItemAddons: List<SyncRecordResultDto> = emptyList(),
    val restockLogs: List<SyncRecordResultDto> = emptyList(),
    val inventoryAdjustments: List<SyncRecordResultDto> = emptyList(),
    val wasteLogs: List<SyncRecordResultDto> = emptyList(),
    val staffLogs: List<SyncRecordResultDto> = emptyList(),
    val auditLogs: List<SyncRecordResultDto> = emptyList()
)

data class PullResponseDto(
    val since: Long = 0L,
    val serverTime: Long = 0L,
    val branches: List<BranchEntity> = emptyList(),
    val users: List<UserEntity> = emptyList(),
    val products: List<ProductEntity> = emptyList(),
    val ingredients: List<IngredientEntity> = emptyList(),
    val productVariants: List<ProductVariantEntity> = emptyList(),
    val productRecipes: List<ProductRecipeEntity> = emptyList(),
    val inventory: List<InventoryEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val transactionItems: List<TransactionItemEntity> = emptyList(),
    val transactionItemAddons: List<TransactionItemAddonEntity> = emptyList(),
    val restockLogs: List<RestockLogEntity> = emptyList(),
    val inventoryAdjustments: List<InventoryAdjustmentEntity> = emptyList(),
    val wasteLogs: List<WasteLogEntity> = emptyList(),
    val staffLogs: List<StaffLogEntity> = emptyList(),
    val auditLogs: List<AuditLogEntity> = emptyList()
)

data class HasUpdatesResponseDto(
    val hasUpdates: Boolean,
    val changedCount: Int = 0,
    val serverTime: Long
)
