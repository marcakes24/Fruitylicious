package com.example.fruitylicious.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class ReceiptSummary(
    val transactionId: String = "",
    val paymentType: String = "Cash",
    val totalAmount: Double = 0.0,
    val completedAt: Long = 0L
)

@Singleton
class CartRepository @Inject constructor() {

    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private val _lastReceiptSummary = MutableStateFlow<ReceiptSummary?>(null)
    val lastReceiptSummary: StateFlow<ReceiptSummary?> = _lastReceiptSummary.asStateFlow()

    fun addProduct(
        productId: Int,
        productName: String,
        unitPrice: Double
    ) {
        val currentCart = _cartItems.value
        val existingItem = currentCart.firstOrNull { it.productId == productId }

        val updatedCart = if (existingItem == null) {
            currentCart + CartItem(
                productId = productId,
                productName = productName,
                quantity = 1,
                unitPrice = unitPrice,
                subtotal = unitPrice
            )
        } else {
            currentCart.map { item ->
                if (item.productId == productId) {
                    val newQuantity = item.quantity + 1
                    item.copy(
                        quantity = newQuantity,
                        subtotal = newQuantity * item.unitPrice
                    )
                } else {
                    item
                }
            }
        }

        _cartItems.value = updatedCart
    }

    fun removeProduct(productId: Int) {
        val updatedCart = _cartItems.value.mapNotNull { item ->
            if (item.productId == productId) {
                val newQuantity = item.quantity - 1

                if (newQuantity <= 0) {
                    null
                } else {
                    item.copy(
                        quantity = newQuantity,
                        subtotal = newQuantity * item.unitPrice
                    )
                }
            } else {
                item
            }
        }

        _cartItems.value = updatedCart
    }

    fun clearCart() {
        _cartItems.value = emptyList()
    }

    fun getTotalAmount(): Double {
        return _cartItems.value.sumOf { it.subtotal }
    }

    fun saveReceiptSummary(
        transactionId: String,
        paymentType: String,
        totalAmount: Double,
        completedAt: Long
    ) {
        _lastReceiptSummary.value = ReceiptSummary(
            transactionId = transactionId,
            paymentType = paymentType,
            totalAmount = totalAmount,
            completedAt = completedAt
        )
    }
}