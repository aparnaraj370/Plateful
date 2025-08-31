package com.example.plateful.model

import android.net.Uri
import com.example.plateful.presentation.restaurantonboarding.AddressEntity
import com.example.plateful.presentation.restaurantonboarding.DayOfWeek
import com.example.plateful.presentation.restaurantonboarding.RestaurantTimeSlot

data class RestaurantEntity(
    val restaurantId: String = "",
    val restaurantName: String = "",
    val restaurantAddress: AddressEntity = AddressEntity(),
    val phoneNumber: String = "",
    val alternatePhoneNumber: String = "",
    val email: String = "",
    val ownerMobileNumber: String = "",
    val ownerName: String = "",
    val ownerEmail: String = "",
    val isOwnerEmailSameAsRestaurantEmail: Boolean = false,
    val isOwnerMobileSameAsRestaurantMobile: Boolean = false,
    val selectedRestaurantType: List<RestaurantType> = emptyList(),
    val workingDaysList: List<DayOfWeek> = emptyList(),
    val timeSlots: List<RestaurantTimeSlot> = listOf(RestaurantTimeSlot()),
    val restaurantImages: List<String> = emptyList()
)
