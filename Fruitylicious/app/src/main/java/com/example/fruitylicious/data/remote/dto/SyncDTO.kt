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
    val branchId: Int,
    val pushedAt: Long,
    val branches: List<BranchEntity>,
    val users: List<UserEntity>,
    val products: List<ProductEntity>,
    val ingredients: List<IngredientEntity>,
    val recipes: List<ProductRecipeEntity>,
    val inventory: List<InventoryEntity>,
    val restockLogs: List<RestockLogEntity>,
    val inventoryAdjustments: List<InventoryAdjustmentEntity>,
    val wasteLogs: List<WasteLogEntity>,
    val transactions: List<TransactionEntity>,
    val transactionItems: List<TransactionItemEntity>,
    val auditLogs: List<AuditLogEntity>,
    val staffLogs: List<StaffLogEntity>
)

data class PushResponseDto(
    val success: Boolean,
    val syncedAt: Long,
    val results: List<PushResultDto>,
    val message: String?
)

data class PushResultDto(
    val tableName: String,
    val recordId: String,
    val success: Boolean,
    val message: String?
)

data class PullResponseDto(
    val success: Boolean,
    val pulledAt: Long,
    val branches: List<BranchEntity>,
    val users: List<UserEntity>,
    val products: List<ProductEntity>,
    val ingredients: List<IngredientEntity>,
    val recipes: List<ProductRecipeEntity>,
    val inventory: List<InventoryEntity>,
    val message: String?
)