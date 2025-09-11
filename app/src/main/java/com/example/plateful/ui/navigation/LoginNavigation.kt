package com.example.plateful.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
object NavAppEntrySelectionScreen

@Serializable
object NavCustomerLoginScreen

@Serializable
object NavRestaurantLoginScreen

@Serializable
data class NavCustomerWaitScreen(val userId: String)

@Serializable
data class NavRestaurantWaitScreen(val userId: String)
