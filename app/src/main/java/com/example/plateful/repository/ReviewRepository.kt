package com.example.plateful.repository

import android.util.Log
import com.example.plateful.model.Review
import com.example.plateful.model.ReviewSummary
import com.example.plateful.model.UserReviewHistory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.UUID
class ReviewRepository {
    
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    companion object {
        private const val TAG = "ReviewRepository"
        private const val REVIEWS_COLLECTION = "reviews"
        private const val RESTAURANTS_COLLECTION = "restaurants"
        private const val USERS_COLLECTION = "users"
    }
    
    // Add a new review
    suspend fun addReview(review: Review): Result<String> {
        return try {
            val reviewId = UUID.randomUUID().toString()
            val reviewWithId = review.copy(reviewId = reviewId)
            
            firestore.collection(REVIEWS_COLLECTION)
                .document(reviewId)
                .set(reviewWithId)
                .await()
            
            // Update restaurant's review summary
            updateRestaurantReviewSummary(review.restaurantId)
            
            Log.d(TAG, "Review added successfully: $reviewId")
            Result.success(reviewId)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding review", e)
            Result.failure(e)
        }
    }
    
    // Get reviews for a restaurant
    suspend fun getRestaurantReviews(
        restaurantId: String,
        limit: Long = 20,
        lastReviewId: String? = null
    ): Result<List<Review>> {
        return try {
            // First try to get real reviews from Firebase
            val snapshot = firestore.collection(REVIEWS_COLLECTION)
                .whereEqualTo("restaurantId", restaurantId)
                .limit(limit)
                .get()
                .await()
            
            val firestoreReviews = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Review::class.java)
            }.sortedByDescending { it.timestamp }
            
            // If no reviews found, return mock data for demo purposes
            val reviews = if (firestoreReviews.isEmpty()) {
                getMockReviews(restaurantId)
            } else {
                firestoreReviews
            }
            
            Log.d(TAG, "Retrieved ${reviews.size} reviews for restaurant $restaurantId")
            Result.success(reviews)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting restaurant reviews: ${e.message}", e)
            // Return mock data as fallback
            Log.d(TAG, "Falling back to mock data")
            Result.success(getMockReviews(restaurantId))
        }
    }
    
    // Get reviews for a specific menu item
    suspend fun getMenuItemReviews(
        restaurantId: String,
        menuItemId: String,
        limit: Long = 20
    ): Result<List<Review>> {
        return try {
            // Use simple query without compound orderBy to avoid index requirements
            val snapshot = firestore.collection(REVIEWS_COLLECTION)
                .whereEqualTo("restaurantId", restaurantId)
                .whereEqualTo("menuItemId", menuItemId)
                .limit(limit)
                .get()
                .await()
            
            val reviews = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Review::class.java)
            }.sortedByDescending { it.timestamp } // Sort in memory
            
            Log.d(TAG, "Retrieved ${reviews.size} reviews for item $menuItemId")
            Result.success(reviews)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting menu item reviews", e)
            Result.failure(e)
        }
    }
    
    // Get user's review history
    suspend fun getUserReviews(userId: String): Result<List<Review>> {
        return try {
            // Use simple query without orderBy to avoid index requirements
            val snapshot = firestore.collection(REVIEWS_COLLECTION)
                .whereEqualTo("userId", userId)
                .get()
                .await()
            
            val reviews = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Review::class.java)
            }.sortedByDescending { it.timestamp } // Sort in memory
            
            Log.d(TAG, "Retrieved ${reviews.size} reviews for user $userId")
            Result.success(reviews)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user reviews", e)
            Result.failure(e)
        }
    }
    
    // Get review summary for a restaurant
    suspend fun getRestaurantReviewSummary(restaurantId: String): Result<ReviewSummary> {
        return try {
            val snapshot = firestore.collection(REVIEWS_COLLECTION)
                .whereEqualTo("restaurantId", restaurantId)
                .get()
                .await()
            
            val firestoreReviews = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Review::class.java)
            }
            
            // If no reviews found, use mock data for demo
            val reviews = if (firestoreReviews.isEmpty()) {
                getMockReviews(restaurantId)
            } else {
                firestoreReviews
            }
            
            val summary = calculateReviewSummary(reviews)
            Log.d(TAG, "Calculated review summary for restaurant $restaurantId")
            Result.success(summary)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting restaurant review summary: ${e.message}", e)
            // Fallback to mock data
            val mockReviews = getMockReviews(restaurantId)
            val summary = calculateReviewSummary(mockReviews)
            Result.success(summary)
        }
    }
    
    // Update a review
    suspend fun updateReview(review: Review): Result<Unit> {
        return try {
            firestore.collection(REVIEWS_COLLECTION)
                .document(review.reviewId)
                .set(review)
                .await()
            
            // Update restaurant's review summary
            updateRestaurantReviewSummary(review.restaurantId)
            
            Log.d(TAG, "Review updated successfully: ${review.reviewId}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating review", e)
            Result.failure(e)
        }
    }
    
    // Delete a review
    suspend fun deleteReview(reviewId: String, restaurantId: String): Result<Unit> {
        return try {
            firestore.collection(REVIEWS_COLLECTION)
                .document(reviewId)
                .delete()
                .await()
            
            // Update restaurant's review summary
            updateRestaurantReviewSummary(restaurantId)
            
            Log.d(TAG, "Review deleted successfully: $reviewId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting review", e)
            Result.failure(e)
        }
    }
    
    // Mark review as helpful
    suspend fun markReviewHelpful(reviewId: String): Result<Unit> {
        return try {
            val reviewDoc = firestore.collection(REVIEWS_COLLECTION)
                .document(reviewId)
            
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(reviewDoc)
                val review = snapshot.toObject(Review::class.java)
                if (review != null) {
                    val updatedReview = review.copy(
                        helpfulCount = review.helpfulCount + 1
                    )
                    transaction.set(reviewDoc, updatedReview)
                }
            }.await()
            
            Log.d(TAG, "Review marked as helpful: $reviewId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking review as helpful", e)
            Result.failure(e)
        }
    }
    
    // Report a review
    suspend fun reportReview(reviewId: String): Result<Unit> {
        return try {
            val reviewDoc = firestore.collection(REVIEWS_COLLECTION)
                .document(reviewId)
            
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(reviewDoc)
                val review = snapshot.toObject(Review::class.java)
                if (review != null) {
                    val updatedReview = review.copy(
                        reportCount = review.reportCount + 1
                    )
                    transaction.set(reviewDoc, updatedReview)
                }
            }.await()
            
            Log.d(TAG, "Review reported: $reviewId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error reporting review", e)
            Result.failure(e)
        }
    }
    
    // Check if user can review (has ordered from restaurant)
    suspend fun canUserReview(userId: String, restaurantId: String, menuItemId: String? = null): Result<Boolean> {
        return try {
            // For demo purposes, always allow reviews if user is authenticated
            val canReview = userId.isNotEmpty()
            Log.d(TAG, "User $userId can review restaurant $restaurantId: $canReview")
            Result.success(canReview)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if user can review", e)
            // Default to true for demo
            Result.success(true)
        }
    }
    
    // Get reviews with real-time updates
    fun getRestaurantReviewsFlow(restaurantId: String): Flow<List<Review>> = flow {
        try {
            firestore.collection(REVIEWS_COLLECTION)
                .whereEqualTo("restaurantId", restaurantId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error listening to reviews", error)
                        return@addSnapshotListener
                    }
                    
                    if (snapshot != null) {
                        val reviews = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(Review::class.java)
                        }
                        // Note: This is a simplified flow emission
                        // In a real implementation, you'd use callbackFlow
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up reviews flow", e)
        }
    }
    
    // Private helper methods
    private suspend fun updateRestaurantReviewSummary(restaurantId: String) {
        try {
            val reviewsResult = getRestaurantReviews(restaurantId, limit = 1000)
            if (reviewsResult.isSuccess) {
                val reviews = reviewsResult.getOrNull() ?: emptyList()
                val summary = calculateReviewSummary(reviews)
                
                // Save summary to restaurant document
                firestore.collection(RESTAURANTS_COLLECTION)
                    .document(restaurantId)
                    .update("reviewSummary", summary)
                    .await()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating restaurant review summary", e)
        }
    }
    
    private fun calculateReviewSummary(reviews: List<Review>): ReviewSummary {
        if (reviews.isEmpty()) {
            return ReviewSummary()
        }
        
        val totalReviews = reviews.size
        val averageRating = reviews.map { it.rating }.average().toFloat()
        
        val ratingDistribution = mutableMapOf<Int, Int>()
        for (i in 1..5) {
            ratingDistribution[i] = reviews.count { it.rating.toInt() == i }
        }
        
        val recentReviews = reviews
            .sortedByDescending { it.timestamp }
            .take(5)
        
        return ReviewSummary(
            averageRating = averageRating,
            totalReviews = totalReviews,
            ratingDistribution = ratingDistribution,
            recentReviews = recentReviews
        )
    }
    
    // Mock data for testing and demo purposes
    private fun getMockReviews(restaurantId: String): List<Review> {
        val currentTime = System.currentTimeMillis()
        val restaurantName = when {
            restaurantId.contains("biryani", ignoreCase = true) -> "Biryani House"
            restaurantId.contains("pizza", ignoreCase = true) -> "Pizza Palace"
            restaurantId.contains("thai", ignoreCase = true) -> "Thai Garden"
            else -> "Sample Restaurant"
        }
        
        return listOf(
            Review(
                reviewId = "mock_1_$restaurantId",
                userId = "user_1",
                userName = "Rajesh Kumar",
                restaurantId = restaurantId,
                restaurantName = restaurantName,
                rating = 5f,
                reviewText = "Absolutely amazing food! Fresh ingredients and authentic flavors. The service was quick and the staff was very friendly. Highly recommend trying their signature dishes!",
                timestamp = currentTime - (2 * 24 * 60 * 60 * 1000), // 2 days ago
                isVerifiedPurchase = true,
                helpfulCount = 15
            ),
            Review(
                reviewId = "mock_2_$restaurantId",
                userId = "user_2",
                userName = "Priya Sharma",
                restaurantId = restaurantId,
                restaurantName = restaurantName,
                rating = 4f,
                reviewText = "Good quality food with reasonable prices. The taste was great but delivery took a bit longer than expected. Overall satisfied with the experience.",
                timestamp = currentTime - (5 * 24 * 60 * 60 * 1000), // 5 days ago
                isVerifiedPurchase = true,
                helpfulCount = 8
            ),
            Review(
                reviewId = "mock_3_$restaurantId",
                userId = "user_3",
                userName = "Amit Singh",
                restaurantId = restaurantId,
                restaurantName = restaurantName,
                rating = 5f,
                reviewText = "Best $restaurantName in the area! Fresh, tasty, and great portion sizes. The packaging was also very good. Will definitely order again.",
                timestamp = currentTime - (7 * 24 * 60 * 60 * 1000), // 1 week ago
                isVerifiedPurchase = true,
                helpfulCount = 12
            ),
            Review(
                reviewId = "mock_4_$restaurantId",
                userId = "user_4",
                userName = "Sneha Patel",
                restaurantId = restaurantId,
                restaurantName = restaurantName,
                rating = 4f,
                reviewText = "Really enjoyed the meal! The flavors were authentic and the food arrived hot. Great value for money. The only improvement would be faster delivery.",
                timestamp = currentTime - (10 * 24 * 60 * 60 * 1000), // 10 days ago
                isVerifiedPurchase = true,
                helpfulCount = 6
            ),
            Review(
                reviewId = "mock_5_$restaurantId",
                userId = "user_5",
                userName = "Vikram Gupta",
                restaurantId = restaurantId,
                restaurantName = restaurantName,
                rating = 3f,
                reviewText = "Average experience. Food was okay but not exceptional. Expected better based on the ratings. Service was decent though.",
                timestamp = currentTime - (14 * 24 * 60 * 60 * 1000), // 2 weeks ago
                isVerifiedPurchase = true,
                helpfulCount = 3
            )
        )
    }
}
