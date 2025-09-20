package com.example.plateful.model

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
// import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.plateful.domain.repository.FoodPackRepository
import com.example.plateful.presentation.login.Screens.allCategories
import com.example.plateful.presentation.login.Screens.cards
import com.example.plateful.presentation.login.Screens.Card
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import com.example.plateful.domain.events.FoodPackEvents

data class OffersUiState(
    val isLoading: Boolean = false,
    val offers: List<Card> = emptyList(),
    val error: String? = null,
    val lastUpdated: Long = System.currentTimeMillis() // Force recomposition
)

/**
 * ViewModel for Top Offers Home Section.
 * Manages category filtering, persistent selection, and filtered offer exposure for the UI.
 */
class TopOffersViewModel : ViewModel(), LifecycleObserver {
    
    private val foodPackRepository = FoodPackRepository()
    private val db = FirebaseFirestore.getInstance()
    
    private val _uiState = MutableStateFlow(OffersUiState())
    val uiState: StateFlow<OffersUiState> = _uiState.asStateFlow()
    
    val categories: List<String> = listOf("All") + allCategories
    
    // Keep static offers as fallback
    private val staticOffers: List<Card> = cards
    
    var selectedCategories by mutableStateOf(setOf("All"))
        private set
    
    // Real-time listener for Firestore changes
    private var foodPacksListener: ListenerRegistration? = null
    
    // Auto-refresh job
    private var autoRefreshJob: Job? = null
    
    // Last refresh timestamp
    private var lastRefreshTime: Long = 0
    
    // Combined offers (real + static)
    val allOffers: List<Card>
        get() {
            val realOffers = _uiState.value.offers
            val combined = realOffers + staticOffers
            Log.d("TopOffersViewModel", "🔄 allOffers: ${realOffers.size} real + ${staticOffers.size} static = ${combined.size} total")
            return combined
        }

    val filteredOffers: List<Card>
        get() {
            val all = allOffers
            val filtered = if (selectedCategories.contains("All")) {
                Log.d("TopOffersViewModel", "🏷️ filteredOffers: 'All' selected, returning ${all.size} offers")
                all
            } else {
                val result = all.filter { offer ->
                    offer.categories.any { cat -> selectedCategories.contains(cat) }
                }
                Log.d("TopOffersViewModel", "🏷️ filteredOffers: Categories ${selectedCategories} selected, ${result.size} offers match")
                result
            }
            return filtered
        }
    
    init {
        startRealTimeUpdates()
        startAutoRefresh()
        setupEventListeners()
    }
    
    private fun loadFoodPacks() {
        viewModelScope.launch {
            Log.d("TopOffersViewModel", "=== STARTING FOOD PACK LOADING ===")
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                Log.d("TopOffersViewModel", "📦 Loading food packs from Firebase...")
                val foodPacks = foodPackRepository.getAvailableFoodPacks()
                Log.d("TopOffersViewModel", "✅ Loaded ${foodPacks.size} food packs from Firebase")
                
                if (foodPacks.isEmpty()) {
                    Log.w("TopOffersViewModel", "⚠️ No food packs found in Firebase")
                } else {
                    Log.d("TopOffersViewModel", "📋 Food pack details:")
                    foodPacks.forEachIndexed { index, pack ->
                        Log.d("TopOffersViewModel", "  $index: '${pack.name}' by '${pack.restaurantName}' - ₹${pack.discountedPrice}")
                    }
                }
                
                Log.d("TopOffersViewModel", "🔄 Converting ${foodPacks.size} food packs to UI cards...")
                val cards = foodPacks.map { foodPack -> convertFoodPackToCard(foodPack) }
                
                Log.d("TopOffersViewModel", "✅ Successfully converted to ${cards.size} UI cards")
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    offers = cards,
                    error = null,
                    lastUpdated = System.currentTimeMillis()
                )
                
                Log.d("TopOffersViewModel", "🎉 Food pack loading completed! UI state updated.")
                
            } catch (e: Exception) {
                Log.e("TopOffersViewModel", "❌ ERROR loading food packs: ${e.message}", e)
                Log.e("TopOffersViewModel", "❌ Exception type: ${e.javaClass.simpleName}")
                Log.e("TopOffersViewModel", "❌ Stack trace: ${e.stackTrace.joinToString("\n")}")
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message,
                    offers = emptyList(), // Will fall back to static offers
                    lastUpdated = System.currentTimeMillis()
                )
                
                Log.w("TopOffersViewModel", "🔄 Will fall back to static offers for display")
            }
        }
    }
    
    private fun mapCuisineTypeToCategories(cuisineType: CuisineType): List<String> {
        return when (cuisineType) {
            CuisineType.NORTH_INDIAN, CuisineType.SOUTH_INDIAN -> listOf("Main Course")
            CuisineType.CHINESE, CuisineType.ITALIAN, CuisineType.CONTINENTAL -> listOf("Main Course")
            CuisineType.FAST_FOOD -> listOf("Snack")
            CuisineType.DESSERTS -> listOf("Dessert")
            CuisineType.BEVERAGES -> listOf("Beverage")
            CuisineType.VEGAN, CuisineType.VEGETARIAN -> listOf("Main Course")
            CuisineType.NON_VEGETARIAN -> listOf("Main Course")
        }
    }
    
    private fun getRandomGradient(): androidx.compose.ui.graphics.Brush {
        val gradients = listOf(
            com.example.plateful.presentation.login.Screens.getGradiet(
                com.example.plateful.ui.theme.GreenStart, 
                com.example.plateful.ui.theme.GreenEnd
            ),
            com.example.plateful.presentation.login.Screens.getGradiet(
                com.example.plateful.ui.theme.BlueStart, 
                com.example.plateful.ui.theme.BlueEnd
            ),
            com.example.plateful.presentation.login.Screens.getGradiet(
                com.example.plateful.ui.theme.OrangeStart, 
                com.example.plateful.ui.theme.OrangeEnd
            ),
            com.example.plateful.presentation.login.Screens.getGradiet(
                com.example.plateful.ui.theme.PurpleStart, 
                com.example.plateful.ui.theme.PurpleEnd
            )
        )
        return gradients.random()
    }
    
    /**
     * Start real-time listener for Firestore food packs collection
     */
    private fun startRealTimeUpdates() {
        Log.d("TopOffersViewModel", "🔄 Setting up real-time Firestore listener...")
        
        stopRealTimeUpdates() // Stop any existing listener
        
        // Simplified query to catch ALL documents first, then filter in code
        foodPacksListener = db.collection("foodPacks")
            .addSnapshotListener { snapshot, error ->
                Log.d("TopOffersViewModel", "📡 REAL-TIME LISTENER TRIGGERED")
                
                if (error != null) {
                    Log.e("TopOffersViewModel", "❌ Real-time listener error: ${error.message}", error)
                    Log.e("TopOffersViewModel", "❌ Error code: ${error.code}")
                    Log.e("TopOffersViewModel", "❌ Error details: $error")
                    // Fall back to manual loading if listener fails
                    loadFoodPacks()
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    Log.d("TopOffersViewModel", "🔥 Real-time update received!")
                    Log.d("TopOffersViewModel", "📄 Total documents: ${snapshot.size()}")
                    Log.d("TopOffersViewModel", "🔄 Document changes: ${snapshot.documentChanges.size}")
                    
                    // Log document changes
                    snapshot.documentChanges.forEach { change ->
                        Log.d("TopOffersViewModel", "🔎 Change type: ${change.type}")
                        Log.d("TopOffersViewModel", "🔎 Document ID: ${change.document.id}")
                        Log.d("TopOffersViewModel", "🔎 Document data: ${change.document.data}")
                    }
                    
                    viewModelScope.launch {
                        Log.d("TopOffersViewModel", "🚀 Processing snapshot in coroutine...")
                        val foodPacks = snapshot.documents.mapNotNull { doc ->
                            Log.d("TopOffersViewModel", "📝 Processing document: ${doc.id}")
                            val data = doc.data
                            if (data != null) {
                                Log.d("TopOffersViewModel", "📀 Document data available, mapping to FoodPack...")
                                Log.d("TopOffersViewModel", "📊 Document fields: ${data.keys.joinToString(", ")}")
                                try {
                                    val foodPack = foodPackRepository.mapDocumentToFoodPack(data)
                                    if (foodPack != null) {
                                        Log.d("TopOffersViewModel", "✅ Successfully mapped: ${foodPack.name}")
                                    } else {
                                        Log.w("TopOffersViewModel", "❌ Mapping returned null for document ${doc.id}")
                                        Log.w("TopOffersViewModel", "❌ Document data was: $data")
                                    }
                                    foodPack
                                } catch (e: Exception) {
                                    Log.e("TopOffersViewModel", "❌ Exception mapping document ${doc.id}: ${e.message}", e)
                                    Log.e("TopOffersViewModel", "❌ Document data was: $data")
                                    null
                                }
                            } else {
                                Log.w("TopOffersViewModel", "⚠️ Document ${doc.id} has null data")
                                null
                            }
                        }
                        Log.d("TopOffersViewModel", "🎆 About to process ${foodPacks.size} food packs")
                        processFoodPacksSnapshot(foodPacks)
                    }
                } else {
                    Log.w("TopOffersViewModel", "⚠️ Received null snapshot from real-time listener")
                }
            }
        
        Log.d("TopOffersViewModel", "✅ Real-time listener started")
    }
    
    /**
     * Stop real-time listener
     */
    private fun stopRealTimeUpdates() {
        foodPacksListener?.remove()
        foodPacksListener = null
        Log.d("TopOffersViewModel", "🛑 Real-time listener stopped")
    }
    
    /**
     * Start automatic refresh every 5 minutes as backup
     */
    private fun startAutoRefresh() {
        autoRefreshJob?.cancel() // Cancel existing job
        
        autoRefreshJob = viewModelScope.launch {
            while (true) {
                delay(300_000) // Wait 5 minutes
                Log.d("TopOffersViewModel", "⏰ Auto-refresh triggered")
                loadFoodPacks()
            }
        }
        
        Log.d("TopOffersViewModel", "✅ Auto-refresh started (5-minute intervals)")
    }
    
    /**
     * Process food packs from real-time snapshot
     */
    private suspend fun processFoodPacksSnapshot(foodPacks: List<FoodPack>) {
        try {
            Log.d("TopOffersViewModel", "=== PROCESSING FOOD PACKS SNAPSHOT ===")
            Log.d("TopOffersViewModel", "📦 Input: ${foodPacks.size} food packs from real-time update")
            
            // Log each input food pack
            foodPacks.forEachIndexed { index, pack ->
                Log.d("TopOffersViewModel", "INPUT ${index + 1}: '${pack.name}' by '${pack.restaurantName}' - Status: ${pack.status}")
                Log.d("TopOffersViewModel", "  Quantity: ${pack.quantity}, Expires: ${pack.expiryTime}")
            }
            
            // Apply comprehensive filtering
            val currentTimeSeconds = System.currentTimeMillis() / 1000
            Log.d("TopOffersViewModel", "⏰ Current timestamp: $currentTimeSeconds")
            
            val availableFoodPacks = foodPacks.filter { foodPack ->
                // Check 1: Status should be AVAILABLE
                val isStatusAvailable = foodPack.status == FoodPackStatus.AVAILABLE
                Log.d("TopOffersViewModel", "🟢 '${foodPack.name}' status: ${foodPack.status}, available: $isStatusAvailable")
                
                // Check 2: Quantity should be > 0
                val hasQuantity = foodPack.quantity > 0
                Log.d("TopOffersViewModel", "🔢 '${foodPack.name}' quantity: ${foodPack.quantity}, has quantity: $hasQuantity")
                
                // Check 3: Should not be expired
                val isNotExpired = foodPack.expiryTime.seconds > currentTimeSeconds
                Log.d("TopOffersViewModel", "🕰️ '${foodPack.name}' expires at ${foodPack.expiryTime.seconds}, not expired: $isNotExpired")
                
                // Check 4: Must have essential fields
                val hasEssentialFields = foodPack.name.isNotBlank() && 
                                        foodPack.restaurantName.isNotBlank() && 
                                        foodPack.vendorId.isNotBlank()
                Log.d("TopOffersViewModel", "📋 '${foodPack.name}' has essential fields: $hasEssentialFields")
                
                val isValid = isStatusAvailable && hasQuantity && isNotExpired && hasEssentialFields
                Log.d("TopOffersViewModel", "✅ '${foodPack.name}' final validity: $isValid")
                
                isValid
            }
            
            Log.d("TopOffersViewModel", "✅ FILTERED: ${availableFoodPacks.size} non-expired food packs")
            
            if (availableFoodPacks.isNotEmpty()) {
                Log.d("TopOffersViewModel", "📋 AVAILABLE FOOD PACKS:")
                availableFoodPacks.forEachIndexed { index, pack ->
                    Log.d("TopOffersViewModel", "  ${index + 1}: '${pack.name}' by '${pack.restaurantName}' - ₹${pack.discountedPrice}")
                }
            } else {
                Log.w("TopOffersViewModel", "😭 NO AVAILABLE FOOD PACKS AFTER FILTERING!")
            }
            
            // Convert to UI cards
            Log.d("TopOffersViewModel", "🃏 Converting to UI cards...")
            val cards = availableFoodPacks.map { foodPack -> 
                Log.d("TopOffersViewModel", "🆕 Converting '${foodPack.name}' to Card...")
                val card = convertFoodPackToCard(foodPack)
                Log.d("TopOffersViewModel", "✅ Card created: Menu='${card.Menu}', RestroName='${card.RestroName}', Offer='${card.Offer}'")
                card
            }
            
            Log.d("TopOffersViewModel", "🃏 CARDS CREATED: ${cards.size} UI cards")
            cards.forEachIndexed { index, card ->
                Log.d("TopOffersViewModel", "  CARD ${index + 1}: ${card.Menu} at ${card.RestroName} - ${card.Offer}")
            }
            
            // Get current state before update
            val currentState = _uiState.value
            Log.d("TopOffersViewModel", "📊 BEFORE UPDATE: Current offers count: ${currentState.offers.size}")
            Log.d("TopOffersViewModel", "📊 BEFORE UPDATE: Current loading: ${currentState.isLoading}")
            Log.d("TopOffersViewModel", "📊 BEFORE UPDATE: Current error: ${currentState.error}")
            
            // Update UI state with timestamp to force recomposition
            Log.d("TopOffersViewModel", "🔄 UPDATING UI STATE...")
            val timestamp = System.currentTimeMillis()
            Log.d("TopOffersViewModel", "⏰ Setting lastUpdated timestamp: $timestamp")
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                offers = cards,
                error = null,
                lastUpdated = timestamp
            )
            
            // Verify state after update
            val newState = _uiState.value
            Log.d("TopOffersViewModel", "📊 AFTER UPDATE: New offers count: ${newState.offers.size}")
            Log.d("TopOffersViewModel", "📊 AFTER UPDATE: New loading: ${newState.isLoading}")
            Log.d("TopOffersViewModel", "📊 AFTER UPDATE: New error: ${newState.error}")
            
            lastRefreshTime = System.currentTimeMillis()
            Log.d("TopOffersViewModel", "🎉 REAL-TIME UPDATE PROCESSED SUCCESSFULLY!")
            Log.d("TopOffersViewModel", "=== END PROCESSING SNAPSHOT ===")
            
        } catch (e: Exception) {
            Log.e("TopOffersViewModel", "❌ ERROR processing real-time update: ${e.message}", e)
            Log.e("TopOffersViewModel", "❌ Stack trace: ${e.stackTraceToString()}")
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Failed to update food packs: ${e.message}"
            )
        }
    }
    
    /**
     * Convert FoodPack to Card (extracted from loadFoodPacks for reuse)
     */
    private fun convertFoodPackToCard(foodPack: FoodPack): Card {
        val discountPercentage = if (foodPack.originalPrice > 0) {
            ((1 - foodPack.discountedPrice/foodPack.originalPrice) * 100).toInt()
        } else {
            0
        }
        
        val hoursLeft = try {
            val currentTimeSeconds = System.currentTimeMillis() / 1000
            val expiryTimeSeconds = foodPack.expiryTime.seconds
            val hoursRemaining = (expiryTimeSeconds - currentTimeSeconds) / 3600
            if (hoursRemaining > 0) "${hoursRemaining}h left" else "Expires soon"
        } catch (e: Exception) {
            "Available now"
        }
        
        return Card(
            Menu = foodPack.name,
            RestroName = foodPack.restaurantName,
            Offer = if (discountPercentage > 0) "₹${foodPack.discountedPrice.toInt()} (${discountPercentage}% Off)" else "₹${foodPack.discountedPrice.toInt()}",
            Icon = Icons.AutoMirrored.Filled.ArrowForward,
            color = getRandomGradient(),
            categories = mapCuisineTypeToCategories(foodPack.cuisineType),
            price = foodPack.originalPrice.toFloat(),
            discountedPrice = foodPack.discountedPrice.toFloat(),
            distance = 2.0f, // TODO: Calculate based on user location
            rating = if (foodPack.rating > 0) foodPack.rating.toFloat() else 4.0f,
            isVegan = foodPack.isVegan,
            location = foodPack.location.city.ifEmpty { foodPack.location.address.ifEmpty { "Near You" } },
            pickupTime = hoursLeft
        )
    }
    
    /**
     * Manual refresh method (can be called by UI)
     */
    fun refreshOffers() {
        Log.d("TopOffersViewModel", "🔄 Manual refresh triggered")
        
        // Check if we've refreshed recently to avoid too frequent calls
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastRefreshTime < 2000) { // 2 seconds cooldown
            Log.d("TopOffersViewModel", "⏭️ Skipping manual refresh - too recent (${currentTime - lastRefreshTime}ms ago)")
            return
        }
        
        loadFoodPacks()
    }
    
    /**
     * Force refresh - ignores cooldown (useful after food pack creation)
     */
    fun forceRefresh() {
        Log.d("TopOffersViewModel", "🚀 Force refresh triggered")
        loadFoodPacks()
    }
    
    /**
     * Debug function to check all documents in foodPacks collection
     */
    fun debugCheckAllFoodPacks() {
        viewModelScope.launch {
            try {
                Log.d("TopOffersViewModel", "=== DEBUG: CHECKING ALL FOOD PACKS ===")
                val allDocs = db.collection("foodPacks").get().await()
                Log.d("TopOffersViewModel", "📊 Total documents in foodPacks collection: ${allDocs.size()}")
                
                allDocs.documents.forEachIndexed { index, doc ->
                    Log.d("TopOffersViewModel", "DEBUG DOC ${index + 1}:")
                    Log.d("TopOffersViewModel", "  ID: ${doc.id}")
                    Log.d("TopOffersViewModel", "  Data: ${doc.data}")
                    
                    val data = doc.data
                    if (data != null) {
                        Log.d("TopOffersViewModel", "  name: ${data["name"]}")
                        Log.d("TopOffersViewModel", "  restaurantName: ${data["restaurantName"]}")
                        Log.d("TopOffersViewModel", "  status: ${data["status"]}")
                        Log.d("TopOffersViewModel", "  quantity: ${data["quantity"]}")
                        Log.d("TopOffersViewModel", "  vendorId: ${data["vendorId"]}")
                        Log.d("TopOffersViewModel", "  expiryTime: ${data["expiryTime"]}")
                    }
                }
                Log.d("TopOffersViewModel", "=== END DEBUG CHECK ===")
            } catch (e: Exception) {
                Log.e("TopOffersViewModel", "Debug check failed: ${e.message}", e)
            }
        }
    }
    
    /**
     * Setup event listeners for food pack changes
     */
    private fun setupEventListeners() {
        // Listen for food pack creation events
        viewModelScope.launch {
            FoodPackEvents.foodPackCreated.collect { foodPackId ->
                Log.d("TopOffersViewModel", "🎆 Food pack created event received: $foodPackId")
                Log.d("TopOffersViewModel", "🚀 Triggering immediate force refresh for new food pack")
                // Force immediate refresh
                forceRefresh()
                // Also trigger a secondary refresh after a short delay to ensure real-time listener has processed
                delay(2000) // Wait 2 seconds
                Log.d("TopOffersViewModel", "🔄 Secondary refresh after delay")
                forceRefresh()
            }
        }
        
        // Listen for general food pack changes
        viewModelScope.launch {
            FoodPackEvents.foodPacksChanged.collect {
                Log.d("TopOffersViewModel", "🔄 Food packs changed event received")
                forceRefresh()
            }
        }
        
        Log.d("TopOffersViewModel", "✅ Event listeners setup complete")
    }

    /** Toggle a category chip (multi-select). If 'All' toggled, resets; else toggles just that cat. */
    fun onCategoryChipClick(category: String) {
        selectedCategories = when {
            category == "All" -> setOf("All")
            selectedCategories.contains(category) -> {
                // Remove, fallback to All if selection empty
                val updated = selectedCategories - category
                if (updated.isEmpty()) setOf("All") else updated - "All"
            }

            else -> (selectedCategories - "All") + category
        }
    }
    
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onStart() {
        Log.d("TopOffersViewModel", "🟢 ViewModel onStart - restarting real-time updates")
        startRealTimeUpdates()
    }
    
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onStop() {
        Log.d("TopOffersViewModel", "🔴 ViewModel onStop - stopping real-time updates")
        stopRealTimeUpdates()
    }
    
    override fun onCleared() {
        super.onCleared()
        Log.d("TopOffersViewModel", "🧹 ViewModel cleared - cleaning up resources")
        stopRealTimeUpdates()
        autoRefreshJob?.cancel()
    }
}
