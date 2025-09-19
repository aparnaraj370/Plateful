package com.example.plateful.model

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.plateful.domain.repository.FoodPackRepository
import com.example.plateful.presentation.login.Screens.allCategories
import com.example.plateful.presentation.login.Screens.cards
import com.example.plateful.presentation.login.Screens.Card
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OffersUiState(
    val isLoading: Boolean = false,
    val offers: List<Card> = emptyList(),
    val error: String? = null
)

/**
 * ViewModel for Top Offers Home Section.
 * Manages category filtering, persistent selection, and filtered offer exposure for the UI.
 */
class TopOffersViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val foodPackRepository = FoodPackRepository()
    
    private val _uiState = MutableStateFlow(OffersUiState())
    val uiState: StateFlow<OffersUiState> = _uiState.asStateFlow()
    
    val categories: List<String> = listOf("All") + allCategories
    
    // Keep static offers as fallback
    private val staticOffers: List<Card> = cards
    
    var selectedCategories by mutableStateOf(
        savedStateHandle.get<Set<String>>("selectedCategories") ?: setOf("All")
    )
        private set
    
    // Combined offers (real + static)
    val allOffers: List<Card>
        get() = _uiState.value.offers + staticOffers

    val filteredOffers: List<Card>
        get() = if (selectedCategories.contains("All")) allOffers
        else allOffers.filter { offer ->
            offer.categories.any { cat -> selectedCategories.contains(cat) }
        }
    
    init {
        loadFoodPacks()
    }
    
    private fun loadFoodPacks() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                Log.d("TopOffersViewModel", "Loading food packs from Firebase")
                val foodPacks = foodPackRepository.getAvailableFoodPacks()
                Log.d("TopOffersViewModel", "Loaded ${foodPacks.size} food packs")
                
                val cards = foodPacks.map { foodPack ->
                    // Convert FoodPack to Card format
                    Card(
                        Menu = foodPack.name,
                        RestroName = foodPack.restaurantName,
                        Offer = "₹${foodPack.discountedPrice.toInt()} (${((1 - foodPack.discountedPrice/foodPack.originalPrice) * 100).toInt()}% Off)",
                        Icon = Icons.Default.ArrowForward,
                        color = getRandomGradient(),
                        categories = mapCuisineTypeToCategories(foodPack.cuisineType),
                        price = foodPack.originalPrice.toFloat(),
                        discountedPrice = foodPack.discountedPrice.toFloat(),
                        distance = 2.0f, // TODO: Calculate based on user location
                        rating = foodPack.rating.toFloat(),
                        isVegan = foodPack.isVegan,
                        location = foodPack.location.city.ifEmpty { "Restaurant" },
                        pickupTime = "${(foodPack.expiryTime.seconds - System.currentTimeMillis()/1000)/3600} hours left"
                    )
                }
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    offers = cards,
                    error = null
                )
                
            } catch (e: Exception) {
                Log.e("TopOffersViewModel", "Error loading food packs", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message,
                    offers = emptyList() // Will fall back to static offers
                )
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
    
    fun refreshOffers() {
        loadFoodPacks()
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
        savedStateHandle["selectedCategories"] = selectedCategories
    }
}
