package com.example.plateful.presentation.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.plateful.model.Cart
import com.example.plateful.model.CartItem
import com.example.plateful.model.CartState
import com.example.plateful.model.RestaurantCart
import com.example.plateful.presentation.login.Screens.Card
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted

class CartViewModel : ViewModel() {
    
    private val _cart = MutableStateFlow<Cart?>(null)
    val cart: StateFlow<Cart?> = _cart.asStateFlow()
    
    // Expose cart as CartState for UI consumption
    val cartState: StateFlow<CartState> = _cart.map { cart ->
        if (cart == null) {
            CartState()
        } else {
            val subtotal = cart.grandTotal.toDouble()
            val taxes = subtotal * 0.08 // 8% tax
            CartState(
                restaurantCarts = cart.restaurants.values.toList(),
                subtotal = subtotal,
                taxes = taxes
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CartState()
    )
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _showAddToCartSnackbar = MutableStateFlow(false)
    val showAddToCartSnackbar: StateFlow<Boolean> = _showAddToCartSnackbar.asStateFlow()
    
    init {
        initializeCart()
    }
    
    private fun initializeCart() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
        _cart.value = Cart(userId = userId)
    }
    
    fun addToCart(
        foodCard: Card, 
        quantity: Int = 1, 
        specialInstructions: String = ""
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                val currentCart = _cart.value ?: return@launch
                val success = currentCart.addItem(foodCard, quantity, specialInstructions)
                
                if (success) {
                    _cart.value = currentCart.copy() // Trigger recomposition
                    _showAddToCartSnackbar.value = true
                }
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun removeFromCart(itemId: String) {
        viewModelScope.launch {
            val currentCart = _cart.value ?: return@launch
            val success = currentCart.removeItem(itemId)
            
            if (success) {
                _cart.value = currentCart.copy() // Trigger recomposition
            }
        }
    }
    
    // Overloaded method for restaurant and menu item ID
    fun removeFromCart(restaurantId: String, menuItemName: String) {
        val itemId = "${restaurantId}_${menuItemName}".replace(" ", "_").lowercase()
        removeFromCart(itemId)
    }
    
    fun updateQuantity(itemId: String, newQuantity: Int) {
        viewModelScope.launch {
            val currentCart = _cart.value ?: return@launch
            val success = currentCart.updateQuantity(itemId, newQuantity)
            
            if (success) {
                _cart.value = currentCart.copy() // Trigger recomposition
            }
        }
    }
    
    // Overloaded method for restaurant and menu item ID  
    fun updateItemQuantity(restaurantId: String, menuItemName: String, newQuantity: Int) {
        val itemId = "${restaurantId}_${menuItemName}".replace(" ", "_").lowercase()
        updateQuantity(itemId, newQuantity)
    }
    
    fun clearCart() {
        viewModelScope.launch {
            val currentCart = _cart.value ?: return@launch
            currentCart.clearCart()
            _cart.value = currentCart.copy() // Trigger recomposition
        }
    }
    
    fun clearRestaurant(restaurantId: String) {
        viewModelScope.launch {
            val currentCart = _cart.value ?: return@launch
            val success = currentCart.clearRestaurant(restaurantId)
            
            if (success) {
                _cart.value = currentCart.copy() // Trigger recomposition
            }
        }
    }
    
    fun getCartItemCount(): Int {
        return _cart.value?.totalItems ?: 0
    }
    
    fun getCartTotal(): Float {
        return _cart.value?.grandTotal ?: 0f
    }
    
    fun dismissAddToCartSnackbar() {
        _showAddToCartSnackbar.value = false
    }
    
    // Helper function to check if a specific item is in cart
    fun isItemInCart(foodCard: Card): Boolean {
        val currentCart = _cart.value ?: return false
        return currentCart.restaurants.values.any { restaurantCart ->
            restaurantCart.items.any { it.foodCard.Menu == foodCard.Menu }
        }
    }
    
    // Get quantity of specific item in cart
    fun getItemQuantityInCart(foodCard: Card): Int {
        val currentCart = _cart.value ?: return 0
        currentCart.restaurants.values.forEach { restaurantCart ->
            val item = restaurantCart.items.find { it.foodCard.Menu == foodCard.Menu }
            if (item != null) {
                return item.quantity
            }
        }
        return 0
    }
    
    // Function to create orders from cart (for checkout)
    fun getCheckoutData(): List<CheckoutRestaurant> {
        val currentCart = _cart.value ?: return emptyList()
        return currentCart.restaurants.values.map { restaurantCart ->
            CheckoutRestaurant(
                restaurantId = restaurantCart.restaurantId,
                restaurantName = restaurantCart.restaurantName,
                items = restaurantCart.items.toList(),
                subtotal = restaurantCart.subtotal,
                totalSavings = restaurantCart.totalSavings
            )
        }
    }
}

data class CheckoutRestaurant(
    val restaurantId: String,
    val restaurantName: String,
    val items: List<CartItem>,
    val subtotal: Float,
    val totalSavings: Float
)
