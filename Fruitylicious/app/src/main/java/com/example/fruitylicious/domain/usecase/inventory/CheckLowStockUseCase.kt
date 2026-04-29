package com.example.fruitylicious.domain.usecase.inventory

import com.example.fruitylicious.data.local.entity.InventoryEntity
import com.example.fruitylicious.data.repository.InventoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CheckLowStockUseCase @Inject constructor(
    private val inventoryRepository: InventoryRepository
) {

    operator fun invoke(branchId: Int): Flow<List<InventoryEntity>> {
        return inventoryRepository.observeLowStockItems(branchId)
    }

    suspend fun once(branchId: Int): List<InventoryEntity> {
        return inventoryRepository.getLowStockItems(branchId)
    }
}