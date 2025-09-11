package com.example.plateful.model

import com.google.firebase.Timestamp

enum class LeftoverStatus {
    AVAILABLE,
    RESERVED,
    SOLD_OUT,
    EXPIRED
}

data class LeftoverEntity(
    val id: String = "",
    val restaurantId: String = "",
    val ownerId: String = "",
    val restaurantName: String = "",
    val name: String = "",
    val description: String = "",
    val category: String = "",
    val quantity: Int = 0,
    val price: Int = 0,
    val isFree: Boolean = false,
    val pickupStart: Timestamp = Timestamp.now(),
    val pickupEnd: Timestamp = Timestamp.now(),
    val createdAt: Timestamp = Timestamp.now(),
    val status: LeftoverStatus = LeftoverStatus.AVAILABLE,
    val images: List<String> = emptyList()
)
