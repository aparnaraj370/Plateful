package com.example.plateful.presentation.reviews

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.plateful.model.AddReviewState
import com.example.plateful.model.Review
import com.example.plateful.model.ReviewState
import com.example.plateful.model.ReviewSummary
import com.example.plateful.model.ReviewSortOption
import com.example.plateful.model.ReviewFilterOption
import com.example.plateful.repository.ReviewRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReviewViewModel(
    private val reviewRepository: ReviewRepository = ReviewRepository()
) : ViewModel() {
    
    private val auth = FirebaseAuth.getInstance()
    
    companion object {
        private const val TAG = "ReviewViewModel"
    }
    
    // Review list state
    private val _reviewState = MutableStateFlow(ReviewState())
    val reviewState: StateFlow<ReviewState> = _reviewState.asStateFlow()
    
    // Add review state
    private val _addReviewState = MutableStateFlow(AddReviewState())
    val addReviewState: StateFlow<AddReviewState> = _addReviewState.asStateFlow()
    
    // Current filters and sorting
    private val _currentSortOption = MutableStateFlow(ReviewSortOption.MOST_RECENT)
    val currentSortOption: StateFlow<ReviewSortOption> = _currentSortOption.asStateFlow()
    
    private val _currentFilterOption = MutableStateFlow(ReviewFilterOption.ALL)
    val currentFilterOption: StateFlow<ReviewFilterOption> = _currentFilterOption.asStateFlow()
    
    // User's ability to review
    private val _canUserReview = MutableStateFlow(false)
    val canUserReview: StateFlow<Boolean> = _canUserReview.asStateFlow()
    
    // Current restaurant/item being reviewed
    private var currentRestaurantId: String = ""
    private var currentMenuItemId: String? = null
    
    // All reviews cache (for filtering/sorting without re-fetching)
    private var allReviews: List<Review> = emptyList()
    
    // Load restaurant reviews
    fun loadRestaurantReviews(restaurantId: String) {
        currentRestaurantId = restaurantId
        viewModelScope.launch {
            _reviewState.value = _reviewState.value.copy(isLoading = true, error = null)
            
            try {
                val result = reviewRepository.getRestaurantReviews(restaurantId)
                if (result.isSuccess) {
                    val reviews = result.getOrNull() ?: emptyList()
                    allReviews = reviews
                    applyFiltersAndSort()
                    
                    // Load review summary
                    loadReviewSummary(restaurantId)
                    
                    // Check if current user can review
                    checkUserCanReview(restaurantId)
                } else {
                    _reviewState.value = _reviewState.value.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Failed to load reviews"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading restaurant reviews", e)
                _reviewState.value = _reviewState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }
    
    // Load menu item reviews
    fun loadMenuItemReviews(restaurantId: String, menuItemId: String) {
        currentRestaurantId = restaurantId
        currentMenuItemId = menuItemId
        
        viewModelScope.launch {
            _reviewState.value = _reviewState.value.copy(isLoading = true, error = null)
            
            try {
                val result = reviewRepository.getMenuItemReviews(restaurantId, menuItemId)
                if (result.isSuccess) {
                    val reviews = result.getOrNull() ?: emptyList()
                    allReviews = reviews
                    applyFiltersAndSort()
                    
                    // Check if current user can review
                    checkUserCanReview(restaurantId, menuItemId)
                } else {
                    _reviewState.value = _reviewState.value.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Failed to load reviews"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading menu item reviews", e)
                _reviewState.value = _reviewState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }
    
    // Load review summary
    private fun loadReviewSummary(restaurantId: String) {
        viewModelScope.launch {
            try {
                val result = reviewRepository.getRestaurantReviewSummary(restaurantId)
                if (result.isSuccess) {
                    val summary = result.getOrNull() ?: ReviewSummary()
                    _reviewState.value = _reviewState.value.copy(reviewSummary = summary)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading review summary", e)
            }
        }
    }
    
    // Check if user can review
    private fun checkUserCanReview(restaurantId: String, menuItemId: String? = null) {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                val result = reviewRepository.canUserReview(userId, restaurantId, menuItemId)
                if (result.isSuccess) {
                    _canUserReview.value = result.getOrNull() ?: false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking if user can review", e)
            }
        }
    }
    
    // Add a new review
    fun addReview(
        restaurantId: String,
        restaurantName: String,
        menuItemId: String? = null,
        menuItemName: String? = null
    ) {
        viewModelScope.launch {
            _addReviewState.value = _addReviewState.value.copy(isSubmitting = true, error = null)
            
            try {
                val userId = auth.currentUser?.uid ?: throw Exception("User not logged in")
                val userName = auth.currentUser?.displayName ?: "Anonymous User"
                
                val review = Review(
                    userId = userId,
                    userName = userName,
                    restaurantId = restaurantId,
                    restaurantName = restaurantName,
                    menuItemId = menuItemId,
                    menuItemName = menuItemName,
                    rating = _addReviewState.value.rating,
                    reviewText = _addReviewState.value.reviewText,
                    reviewImages = _addReviewState.value.selectedImages,
                    isVerifiedPurchase = true, // Assume verified since we check canUserReview
                    timestamp = System.currentTimeMillis()
                )
                
                val result = reviewRepository.addReview(review)
                if (result.isSuccess) {
                    _addReviewState.value = _addReviewState.value.copy(
                        isSubmitting = false,
                        isSubmitted = true
                    )
                    
                    // Refresh reviews
                    if (menuItemId != null) {
                        loadMenuItemReviews(restaurantId, menuItemId)
                    } else {
                        loadRestaurantReviews(restaurantId)
                    }
                } else {
                    _addReviewState.value = _addReviewState.value.copy(
                        isSubmitting = false,
                        error = result.exceptionOrNull()?.message ?: "Failed to add review"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error adding review", e)
                _addReviewState.value = _addReviewState.value.copy(
                    isSubmitting = false,
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }
    
    // Update review rating
    fun updateReviewRating(rating: Float) {
        _addReviewState.value = _addReviewState.value.copy(rating = rating)
    }
    
    // Update review text
    fun updateReviewText(text: String) {
        _addReviewState.value = _addReviewState.value.copy(reviewText = text)
    }
    
    // Add review image
    fun addReviewImage(imageUrl: String) {
        val currentImages = _addReviewState.value.selectedImages.toMutableList()
        if (currentImages.size < 5) { // Limit to 5 images
            currentImages.add(imageUrl)
            _addReviewState.value = _addReviewState.value.copy(selectedImages = currentImages)
        }
    }
    
    // Remove review image
    fun removeReviewImage(imageUrl: String) {
        val currentImages = _addReviewState.value.selectedImages.toMutableList()
        currentImages.remove(imageUrl)
        _addReviewState.value = _addReviewState.value.copy(selectedImages = currentImages)
    }
    
    // Reset add review state
    fun resetAddReviewState() {
        _addReviewState.value = AddReviewState()
    }
    
    // Mark review as helpful
    fun markReviewHelpful(reviewId: String) {
        viewModelScope.launch {
            try {
                val result = reviewRepository.markReviewHelpful(reviewId)
                if (result.isSuccess) {
                    // Update the review in our current state
                    val updatedReviews = _reviewState.value.reviews.map { review ->
                        if (review.reviewId == reviewId) {
                            review.copy(helpfulCount = review.helpfulCount + 1)
                        } else {
                            review
                        }
                    }
                    _reviewState.value = _reviewState.value.copy(reviews = updatedReviews)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error marking review as helpful", e)
            }
        }
    }
    
    // Report review
    fun reportReview(reviewId: String) {
        viewModelScope.launch {
            try {
                val result = reviewRepository.reportReview(reviewId)
                if (result.isSuccess) {
                    Log.d(TAG, "Review reported successfully")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reporting review", e)
            }
        }
    }
    
    // Set sort option
    fun setSortOption(sortOption: ReviewSortOption) {
        _currentSortOption.value = sortOption
        applyFiltersAndSort()
    }
    
    // Set filter option
    fun setFilterOption(filterOption: ReviewFilterOption) {
        _currentFilterOption.value = filterOption
        applyFiltersAndSort()
    }
    
    // Apply filters and sorting
    private fun applyFiltersAndSort() {
        viewModelScope.launch {
            try {
                var filteredReviews = allReviews
                
                // Apply filters
                when (_currentFilterOption.value) {
                    ReviewFilterOption.FIVE_STAR -> filteredReviews = filteredReviews.filter { it.rating >= 4.5f }
                    ReviewFilterOption.FOUR_STAR -> filteredReviews = filteredReviews.filter { it.rating >= 3.5f && it.rating < 4.5f }
                    ReviewFilterOption.THREE_STAR -> filteredReviews = filteredReviews.filter { it.rating >= 2.5f && it.rating < 3.5f }
                    ReviewFilterOption.TWO_STAR -> filteredReviews = filteredReviews.filter { it.rating >= 1.5f && it.rating < 2.5f }
                    ReviewFilterOption.ONE_STAR -> filteredReviews = filteredReviews.filter { it.rating < 1.5f }
                    ReviewFilterOption.WITH_PHOTOS -> filteredReviews = filteredReviews.filter { it.reviewImages.isNotEmpty() }
                    ReviewFilterOption.VERIFIED_PURCHASES -> filteredReviews = filteredReviews.filter { it.isVerifiedPurchase }
                    ReviewFilterOption.ALL -> {} // No filtering
                }
                
                // Apply sorting
                val sortedReviews = when (_currentSortOption.value) {
                    ReviewSortOption.MOST_RECENT -> filteredReviews.sortedByDescending { it.timestamp }
                    ReviewSortOption.OLDEST_FIRST -> filteredReviews.sortedBy { it.timestamp }
                    ReviewSortOption.HIGHEST_RATING -> filteredReviews.sortedByDescending { it.rating }
                    ReviewSortOption.LOWEST_RATING -> filteredReviews.sortedBy { it.rating }
                    ReviewSortOption.MOST_HELPFUL -> filteredReviews.sortedByDescending { it.helpfulCount }
                }
                
                _reviewState.value = _reviewState.value.copy(
                    reviews = sortedReviews,
                    isLoading = false
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error applying filters and sort", e)
                _reviewState.value = _reviewState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Error filtering reviews"
                )
            }
        }
    }
    
    // Load more reviews (pagination)
    fun loadMoreReviews() {
        if (_reviewState.value.isLoading || !_reviewState.value.hasMoreReviews) return
        
        viewModelScope.launch {
            try {
                val lastReviewId = _reviewState.value.reviews.lastOrNull()?.reviewId
                val result = reviewRepository.getRestaurantReviews(
                    currentRestaurantId, 
                    lastReviewId = lastReviewId
                )
                
                if (result.isSuccess) {
                    val newReviews = result.getOrNull() ?: emptyList()
                    if (newReviews.isNotEmpty()) {
                        allReviews = allReviews + newReviews
                        applyFiltersAndSort()
                    } else {
                        _reviewState.value = _reviewState.value.copy(hasMoreReviews = false)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading more reviews", e)
            }
        }
    }
    
    // Clear error
    fun clearError() {
        _reviewState.value = _reviewState.value.copy(error = null)
        _addReviewState.value = _addReviewState.value.copy(error = null)
    }
}
