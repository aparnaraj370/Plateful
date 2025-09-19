package com.example.plateful.presentation.vendor

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.example.plateful.domain.repository.FoodPackRepository
import com.example.plateful.domain.services.UserRoleService
import com.example.plateful.domain.services.UserRoleServiceImpl
import com.example.plateful.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date

data class FoodPackManagementUiState(
    val isLoading: Boolean = false,
    val foodPacks: List<FoodPack> = emptyList(),
    val reservations: List<FoodPackReservation> = emptyList(),
    val analytics: VendorAnalytics = VendorAnalytics(),
    val demandPredictions: List<DemandPrediction> = emptyList(),
    val errorMessage: String = "",
    val isError: Boolean = false
)

class FoodPackManagementViewModel : ViewModel() {
    
    private val repository = FoodPackRepository()
    private val userRoleService = UserRoleServiceImpl()
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    
    private val _uiState = MutableStateFlow(FoodPackManagementUiState())
    val uiState: StateFlow<FoodPackManagementUiState> = _uiState.asStateFlow()
    
    var selectedFoodPack by mutableStateOf<FoodPack?>(null)
        private set
        
    var isEditMode by mutableStateOf(false)
        private set
    
    init {
        loadVendorData()
    }
    
    private fun loadVendorData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                val currentUserId = auth.currentUser?.uid ?: return@launch
                
                // Load food packs
                val foodPacks = repository.getFoodPacksByVendor(currentUserId)
                
                // Load reservations
                val reservations = repository.getReservationsByVendor(currentUserId)
                
                // Load analytics (last 7 days)
                val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(Date())
                val weekAgo = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(
                    Date(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000)
                )
                val analytics = repository.getVendorAnalytics(currentUserId, Pair(weekAgo, today))
                
                // Load demand predictions
                val restaurantId = userRoleService.getRestaurantIdForOwner(currentUserId)
                val demandPredictions = if (restaurantId != null) {
                    repository.getDemandPredictions(restaurantId)
                } else emptyList()
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    foodPacks = foodPacks,
                    reservations = reservations,
                    analytics = analytics,
                    demandPredictions = demandPredictions,
                    isError = false,
                    errorMessage = ""
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isError = true,
                    errorMessage = e.message ?: "Failed to load vendor data"
                )
            }
        }
    }
    
    // Food Pack Management Functions
    fun createFoodPack(
        name: String,
        description: String,
        originalPrice: Double,
        discountedPrice: Double,
        quantity: Int,
        expiryHours: Int,
        cuisineType: CuisineType,
        isVegetarian: Boolean,
        isVegan: Boolean,
        images: List<String> = emptyList(),
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                Log.d("FoodPackViewModel", "Starting food pack creation")
                val currentUserId = auth.currentUser?.uid
                Log.d("FoodPackViewModel", "Current user ID: $currentUserId")
                
                if (currentUserId == null) {
                    Log.e("FoodPackViewModel", "No authenticated user found")
                    onResult(false, "User not authenticated")
                    return@launch
                }
                
                Log.d("FoodPackViewModel", "Fetching restaurant ID for owner: $currentUserId")
                val restaurantId = userRoleService.getRestaurantIdForOwner(currentUserId)
                Log.d("FoodPackViewModel", "Restaurant ID: $restaurantId")
                
                if (restaurantId == null) {
                    Log.e("FoodPackViewModel", "Restaurant not found for user: $currentUserId")
                    onResult(false, "Restaurant not found. Please complete restaurant setup first.")
                    return@launch
                }
                
                // Fetch restaurant data
                Log.d("FoodPackViewModel", "Fetching restaurant data for ID: $restaurantId")
                val restaurantData = getRestaurantData(restaurantId)
                val restaurantName = restaurantData?.restaurantName ?: "Unknown Restaurant"
                Log.d("FoodPackViewModel", "Restaurant name: $restaurantName")
                
                Log.d("FoodPackViewModel", "Creating food pack object")
                val expiryTime = Timestamp(Date(System.currentTimeMillis() + expiryHours * 60 * 60 * 1000))
                
                val foodPack = FoodPack(
                    restaurantId = restaurantId,
                    restaurantName = restaurantName,
                    vendorId = currentUserId,
                    name = name,
                    description = description,
                    originalPrice = originalPrice,
                    discountedPrice = discountedPrice,
                    quantity = quantity,
                    totalQuantity = quantity,
                    expiryTime = expiryTime,
                    cuisineType = cuisineType,
                    isVegetarian = isVegetarian,
                    isVegan = isVegan,
                    images = images,
                    status = FoodPackStatus.AVAILABLE,
                    location = restaurantData?.restaurantAddress?.let {
                        RestaurantLocation(
                            address = it.street,
                            city = it.city,
                            state = it.state,
                            postalCode = it.postalCode
                        )
                    } ?: RestaurantLocation()
                )
                
                Log.d("FoodPackViewModel", "Food pack object: $foodPack")
                Log.d("FoodPackViewModel", "Calling repository.createFoodPack")
                val result = repository.createFoodPack(foodPack)
                Log.d("FoodPackViewModel", "Repository result: $result")
                
                if (result != null) {
                    Log.d("FoodPackViewModel", "Food pack created successfully, refreshing data")
                    loadVendorData() // Refresh data
                    onResult(true, "Food pack created successfully")
                } else {
                    Log.e("FoodPackViewModel", "Failed to create food pack - repository returned null")
                    onResult(false, "Failed to create food pack")
                }
                
            } catch (e: Exception) {
                Log.e("FoodPackViewModel", "Exception during food pack creation", e)
                onResult(false, e.message ?: "Failed to create food pack")
            }
        }
    }
    
    fun updateFoodPack(
        foodPack: FoodPack,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val success = repository.updateFoodPack(foodPack)
                if (success) {
                    loadVendorData() // Refresh data
                    selectedFoodPack = null
                    isEditMode = false
                    onResult(true, "Food pack updated successfully")
                } else {
                    onResult(false, "Failed to update food pack")
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "Failed to update food pack")
            }
        }
    }
    
    fun deleteFoodPack(
        foodPackId: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val success = repository.deleteFoodPack(foodPackId)
                if (success) {
                    loadVendorData() // Refresh data
                    onResult(true, "Food pack deleted successfully")
                } else {
                    onResult(false, "Failed to delete food pack")
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "Failed to delete food pack")
            }
        }
    }
    
    // Reservation Management Functions
    fun updateReservationStatus(
        reservationId: String,
        status: ReservationStatus,
        cancelReason: String = "",
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val success = repository.updateReservationStatus(reservationId, status, cancelReason)
                if (success) {
                    loadVendorData() // Refresh data
                    onResult(true, "Reservation status updated successfully")
                } else {
                    onResult(false, "Failed to update reservation status")
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "Failed to update reservation status")
            }
        }
    }
    
    // UI State Management Functions
    fun selectFoodPack(foodPack: FoodPack) {
        selectedFoodPack = foodPack
        isEditMode = true
    }
    
    fun clearSelection() {
        selectedFoodPack = null
        isEditMode = false
    }
    
    fun refreshData() {
        loadVendorData()
    }
    
    // Analytics Functions
    fun getFilteredReservations(status: ReservationStatus? = null): List<FoodPackReservation> {
        return if (status != null) {
            _uiState.value.reservations.filter { it.status == status }
        } else {
            _uiState.value.reservations
        }
    }
    
    fun getTodayReservations(): List<FoodPackReservation> {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(Date())
        return _uiState.value.reservations.filter {
            val reservationDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                .format(it.reservedAt.toDate())
            reservationDate == today
        }
    }
    
    fun getActiveFoodPacks(): List<FoodPack> {
        return _uiState.value.foodPacks.filter {
            it.status == FoodPackStatus.AVAILABLE && 
            it.quantity > 0 && 
            it.expiryTime.toDate().after(Date())
        }
    }
    
    fun getExpiringSoonFoodPacks(): List<FoodPack> {
        val twoHoursFromNow = Date(System.currentTimeMillis() + 2 * 60 * 60 * 1000)
        return _uiState.value.foodPacks.filter {
            it.status == FoodPackStatus.AVAILABLE && 
            it.quantity > 0 && 
            it.expiryTime.toDate().before(twoHoursFromNow)
        }
    }
    
    // Helper method to get restaurant data
    private suspend fun getRestaurantData(restaurantId: String): com.example.plateful.model.RestaurantEntity? {
        return try {
            val document = firestore.collection("restaurants")
                .document(restaurantId)
                .get()
                .await()
            document.toObject(com.example.plateful.model.RestaurantEntity::class.java)
        } catch (e: Exception) {
            Log.e("FoodPackViewModel", "Error fetching restaurant data", e)
            null
        }
    }
    
    // Helper method to get restaurant name
    private suspend fun getRestaurantName(restaurantId: String): String? {
        return try {
            val document = firestore.collection("restaurants")
                .document(restaurantId)
                .get()
                .await()
            // Try multiple field names for restaurant name
            document.getString("restaurantName") 
                ?: document.getString("name")
                ?: "Unknown Restaurant"
        } catch (e: Exception) {
            Log.e("FoodPackViewModel", "Error fetching restaurant name", e)
            null
        }
    }
}
