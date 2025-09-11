package com.example.plateful.domain.model

data class UserRole(
    val userType: UserType,
    val entryType: UserEntryType,
    val restaurantId: String = "",
    val isProfileComplete: Boolean = false
)
