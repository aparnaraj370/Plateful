package com.example.plateful.model

import com.google.firebase.Timestamp
import java.util.Date

data class Order(
    val orderId: String = "",
    val userId: String = "",
    val restaurantId: String = "",
    val restaurantName: String = "",
    val itemName: String = "",
    val itemDescription: String = "",
    val price: String = "",
    val pickupTime: String = "",
    val orderStatus: OrderStatus = OrderStatus.PENDING,
    val orderDate: Timestamp = Timestamp.now(),
    val restaurantAddress: String = ""
) {
    // Helper property to get Date for compatibility
    fun getOrderDateAsDate(): Date = orderDate.toDate()
}

enum class OrderStatus {
    PENDING,
    CONFIRMED,
    READY_FOR_PICKUP,
    COMPLETED,
    CANCELLED
}
