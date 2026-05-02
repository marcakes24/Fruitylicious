package com.example.fruitylicious.domain.usecase.admin

import com.example.fruitylicious.data.local.entity.ProductEntity
import com.example.fruitylicious.data.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ManageProductUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {

    fun observeProducts(): Flow<List<ProductEntity>> {
        return productRepository.observeProducts()
    }

    fun observeProduct(productId: Int): Flow<ProductEntity?> {
        return productRepository.observeProduct(productId)
    }

    suspend fun getProduct(productId: Int): ProductEntity? {
        return productRepository.getProduct(productId)
    }

    suspend fun saveProduct(
        productId: Int,
        image: String?,
        productName: String,
        isAddon: Boolean,
        price: Double
    ): Result<Unit> {
        return productRepository.saveProduct(
            productId = productId,
            image = image,
            productName = productName,
            isAddon = isAddon,
            price = price
        )
    }

    suspend fun deleteProduct(productId: Int): Result<Unit> {
        return productRepository.deleteProduct(productId)
    }

    suspend fun updateProductImage(
        productId: Int,
        imagePath: String?,
        lastModified: Long
    ) {
        productRepository.updateProductImage(productId, imagePath, lastModified)
    }
}