package com.example.plateful.model

import com.example.plateful.domain.model.UserType
import com.example.plateful.domain.model.UserEntryType

data class UserEntity(
    var uid: String = "",
    var email: String = "",
    var name: String = "",
    var phoneNumber: String = "",
    var gender: String = "",
    var isEmailVerified: Boolean = false,
    var isPhoneVerified: Boolean = false,
    var isProfileComplete: Boolean = false,
    var profileUrl : String = "",
    var houseNumber: String = "",
    var street: String = "",
    var city: String = "",
    var state: String = "",
    var pincode: String = "",
    var locality: String = "",
    var userType: UserType = UserType.CUSTOMER,
    var entryType: UserEntryType = UserEntryType.CUSTOMER_ENTRY,
    var restaurantId: String = "",
    var createdAt: Long = 0L,
    var updatedAt: Long = 0L
)
