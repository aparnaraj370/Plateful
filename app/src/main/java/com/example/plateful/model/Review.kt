package com.example.plateful.model

import com.google.firebase.Timestamp
import kotlinx.serialization.Serializable

@Serializable
data class Review(
    val reviewId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userProfileImage: String? = null,
    val restaurantId: String = "",
    val restaurantName: String = "",
    val menuItemId: String? = null, // Optional - for item-specific reviews
    val menuItemName: String? = null,
    val rating: Float = 0f, // 1.0 to 5.0
    val reviewText: String = "",
    val reviewImages: List<String> = emptyList(), // URLs to review images
    val timestamp: Long = System.currentTimeMillis(),
    val isVerifiedPurchase: Boolean = false, // Whether user actually ordered this item
    val helpfulCount: Int = 0, // Number of users who found this review helpful
    val reportCount: Int = 0, // Number of reports (for moderation)
    val response: RestaurantResponse? = null // Restaurant's response to the review
)

@Serializable
data class RestaurantResponse(
    val responseText: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val restaurantManagerName: String = ""
)

@Serializable
data class ReviewSummary(
    val averageRating: Float = 0f,
    val totalReviews: Int = 0,
    val ratingDistribution: Map<Int, Int> = mapOf(
        1 to 0, 2 to 0, 3 to 0, 4 to 0, 5 to 0
    ),
    val recentReviews: List<Review> = emptyList()
)

@Serializable
data class UserReviewHistory(
    val userId: String = "",
    val reviews: List<Review> = emptyList(),
    val totalReviews: Int = 0,
    val averageRatingGiven: Float = 0f
)

// State classes for UI
data class ReviewState(
    val reviews: List<Review> = emptyList(),
    val reviewSummary: ReviewSummary = ReviewSummary(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasMoreReviews: Boolean = true
)

data class AddReviewState(
    val rating: Float = 0f,
    val reviewText: String = "",
    val selectedImages: List<String> = emptyList(),
    val isSubmitting: Boolean = false,
    val isSubmitted: Boolean = false,
    val error: String? = null
)

// Filter and sort options
enum class ReviewSortOption(val displayName: String) {
    MOST_RECENT("Most Recent"),
    OLDEST_FIRST("Oldest First"),
    HIGHEST_RATING("Highest Rating"),
    LOWEST_RATING("Lowest Rating"),
    MOST_HELPFUL("Most Helpful")
}

enum class ReviewFilterOption(val displayName: String) {
    ALL("All Reviews"),
    FIVE_STAR("5 Stars"),
    FOUR_STAR("4 Stars"),
    THREE_STAR("3 Stars"),
    TWO_STAR("2 Stars"),
    ONE_STAR("1 Star"),
    WITH_PHOTOS("With Photos"),
    VERIFIED_PURCHASES("Verified Purchases")
}
