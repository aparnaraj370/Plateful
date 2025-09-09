package com.example.plateful.model

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
    val orderDate: Date = Date(),
    val restaurantAddress: String = ""
)

enum class OrderStatus {
    PENDING,
    CONFIRMED,
    READY_FOR_PICKUP,
    COMPLETED,
    CANCELLED
}
