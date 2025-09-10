package com.example.plateful.model

import com.example.plateful.presentation.login.Screens.Card

// State data class for UI consumption
data class CartState(
    val restaurantCarts: List<RestaurantCart> = emptyList(),
    val subtotal: Double = 0.0,
    val deliveryFee: Double = 40.0,
    val taxes: Double = 0.0,
    val discount: Double = 0.0
) {
    val total: Double
        get() = subtotal + deliveryFee + taxes - discount
}

data class CartItem(
    val id: String,
    val foodCard: Card,
    var quantity: Int = 1,
    val specialInstructions: String = ""
) {
    val itemTotal: Float
        get() = foodCard.discountedPrice * quantity
        
    val originalTotal: Float
        get() = foodCard.price * quantity
        
    val savings: Float
        get() = originalTotal - itemTotal
}

data class RestaurantCart(
    val restaurantId: String,
    val restaurantName: String,
    val items: MutableList<CartItem> = mutableListOf()
) {
    val subtotal: Float
        get() = items.sumOf { it.itemTotal.toDouble() }.toFloat()
        
    val originalTotal: Float
        get() = items.sumOf { it.originalTotal.toDouble() }.toFloat()
        
    val totalSavings: Float
        get() = originalTotal - subtotal
        
    val totalItems: Int
        get() = items.sumOf { it.quantity }
}

data class Cart(
    val userId: String,
    val restaurants: MutableMap<String, RestaurantCart> = mutableMapOf()
) {
    val totalItems: Int
        get() = restaurants.values.sumOf { it.totalItems }
        
    val grandTotal: Float
        get() = restaurants.values.sumOf { it.subtotal.toDouble() }.toFloat()
        
    val totalSavings: Float
        get() = restaurants.values.sumOf { it.totalSavings.toDouble() }.toFloat()
        
    fun addItem(foodCard: Card, quantity: Int = 1, specialInstructions: String = ""): Boolean {
        val restaurantId = foodCard.RestroName.replace(" ", "_").lowercase()
        
        // Get or create restaurant cart
        val restaurantCart = restaurants.getOrPut(restaurantId) {
            RestaurantCart(
                restaurantId = restaurantId,
                restaurantName = foodCard.RestroName
            )
        }
        
        // Check if item already exists
        val existingItem = restaurantCart.items.find { it.foodCard.Menu == foodCard.Menu }
        
        if (existingItem != null) {
            // Update quantity
            existingItem.quantity += quantity
            return true
        } else {
            // Add new item
            val cartItem = CartItem(
                id = "${restaurantId}_${foodCard.Menu}".replace(" ", "_").lowercase(),
                foodCard = foodCard,
                quantity = quantity,
                specialInstructions = specialInstructions
            )
            restaurantCart.items.add(cartItem)
            return true
        }
    }
    
    fun removeItem(itemId: String): Boolean {
        restaurants.values.forEach { restaurantCart ->
            val itemToRemove = restaurantCart.items.find { it.id == itemId }
            if (itemToRemove != null) {
                restaurantCart.items.remove(itemToRemove)
                
                // Remove restaurant cart if empty
                if (restaurantCart.items.isEmpty()) {
                    restaurants.remove(restaurantCart.restaurantId)
                }
                return true
            }
        }
        return false
    }
    
    fun updateQuantity(itemId: String, newQuantity: Int): Boolean {
        if (newQuantity <= 0) return removeItem(itemId)
        
        restaurants.values.forEach { restaurantCart ->
            val item = restaurantCart.items.find { it.id == itemId }
            if (item != null) {
                item.quantity = newQuantity
                return true
            }
        }
        return false
    }
    
    fun clearCart() {
        restaurants.clear()
    }
    
    fun clearRestaurant(restaurantId: String): Boolean {
        return restaurants.remove(restaurantId) != null
    }
    
    fun isEmpty(): Boolean = restaurants.isEmpty()
}
