package com.example.fruitylicious.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
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

    fun addCustomItem(item: CartItem) {
        _cartItems.value = _cartItems.value + item
    }

    fun updateQuantity(cartLineId: String, delta: Int) {
        _cartItems.value = _cartItems.value.mapNotNull { item ->
            if (item.cartLineId == cartLineId) {
                val newQuantity = item.quantity + delta

                if (newQuantity <= 0) {
                    null
                } else {
                    item.copy(
                        quantity = newQuantity,
                        subtotal = computeSubtotal(
                            unitPrice = item.unitPrice,
                            quantity = newQuantity,
                            addons = item.addons
                        )
                    )
                }
            } else {
                item
            }
        }
    }

    fun removeLine(cartLineId: String) {
        _cartItems.value = _cartItems.value.filterNot {
            it.cartLineId == cartLineId
        }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
    }

    fun updateFullItem(updatedItem: CartItem) {
        _cartItems.value = _cartItems.value.map { item ->
            if (item.cartLineId == updatedItem.cartLineId) {
                updatedItem
            } else {
                item
            }
        }
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

    private fun computeSubtotal(
        unitPrice: Double,
        quantity: Int,
        addons: List<CartAddon>
    ): Double {
        val addonTotalPerItem = addons.sumOf { it.subtotal }
        return (unitPrice + addonTotalPerItem) * quantity
    }
}