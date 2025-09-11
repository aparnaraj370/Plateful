package com.example.plateful.model

import com.google.firebase.Timestamp
import java.util.Date

enum class FoodPackStatus {
    AVAILABLE,
    RESERVED,
    COMPLETED,
    EXPIRED,
    CANCELED
}

enum class CuisineType {
    NORTH_INDIAN,
    SOUTH_INDIAN,
    CHINESE,
    ITALIAN,
    CONTINENTAL,
    FAST_FOOD,
    DESSERTS,
    BEVERAGES,
    VEGAN,
    VEGETARIAN,
    NON_VEGETARIAN
}

data class FoodPack(
    val id: String = "",
    val restaurantId: String = "",
    val restaurantName: String = "",
    val vendorId: String = "", // Restaurant owner/vendor ID
    val name: String = "", // Food pack name
    val description: String = "",
    val originalPrice: Double = 0.0,
    val discountedPrice: Double = 0.0, // Discounted price for leftover
    val quantity: Int = 0, // Available quantity
    val totalQuantity: Int = 0, // Total quantity initially added
    val expiryTime: Timestamp = Timestamp.now(), // When food expires
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    val cuisineType: CuisineType = CuisineType.VEGETARIAN,
    val isVegetarian: Boolean = true,
    val isVegan: Boolean = false,
    val status: FoodPackStatus = FoodPackStatus.AVAILABLE,
    val images: List<String> = emptyList(), // Image URLs
    val nutritionInfo: String = "", // Optional nutrition information
    val allergyInfo: String = "", // Allergy information
    val packagingType: String = "", // Type of packaging
    val pickupInstructions: String = "", // Special pickup instructions
    val rating: Double = 0.0, // Average rating for this food pack
    val reviewCount: Int = 0, // Number of reviews
    val location: RestaurantLocation = RestaurantLocation()
)

data class RestaurantLocation(
    val address: String = "",
    val city: String = "",
    val state: String = "",
    val postalCode: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

// Reservation model for tracking customer orders
data class FoodPackReservation(
    val id: String = "",
    val foodPackId: String = "",
    val restaurantId: String = "",
    val vendorId: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val quantity: Int = 0,
    val totalPrice: Double = 0.0,
    val status: ReservationStatus = ReservationStatus.CONFIRMED,
    val reservedAt: Timestamp = Timestamp.now(),
    val pickupTime: Timestamp = Timestamp.now(),
    val completedAt: Timestamp? = null,
    val canceledAt: Timestamp? = null,
    val cancelReason: String = "",
    val pickupCode: String = "", // Unique code for pickup verification
    val specialInstructions: String = "",
    val paymentStatus: PaymentStatus = PaymentStatus.PENDING
)

enum class ReservationStatus {
    CONFIRMED,
    READY_FOR_PICKUP,
    COMPLETED,
    CANCELED,
    NO_SHOW
}

enum class PaymentStatus {
    PENDING,
    PAID,
    REFUNDED,
    FAILED
}

// Analytics data models
data class VendorAnalytics(
    val vendorId: String = "",
    val restaurantId: String = "",
    val date: String = "", // YYYY-MM-DD format
    val totalSales: Double = 0.0,
    val totalOrders: Int = 0,
    val foodSavedKg: Double = 0.0, // Estimated food saved from wastage
    val packsSold: Int = 0,
    val averageRating: Double = 0.0,
    val newCustomers: Int = 0,
    val repeatCustomers: Int = 0,
    val popularItems: List<String> = emptyList(),
    val peakHours: List<String> = emptyList()
)

// Demand prediction model
data class DemandPrediction(
    val id: String = "",
    val restaurantId: String = "",
    val foodPackName: String = "",
    val cuisineType: CuisineType = CuisineType.VEGETARIAN,
    val predictedDate: String = "", // YYYY-MM-DD
    val predictedDemand: Int = 0, // Predicted quantity demand
    val confidence: Double = 0.0, // Confidence level (0-1)
    val factors: List<String> = emptyList(), // Factors affecting prediction
    val generatedAt: Timestamp = Timestamp.now()
)
