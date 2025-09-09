package com.example.plateful.presentation.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.plateful.domain.repository.OrderRepository
import com.example.plateful.model.Order
import com.example.plateful.model.OrderStatus
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import android.util.Log

class OrderViewModel : ViewModel() {

    private val orderRepository = OrderRepository()
    
    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _orderCreationState = MutableStateFlow<OrderCreationState>(OrderCreationState.Idle)
    val orderCreationState: StateFlow<OrderCreationState> = _orderCreationState.asStateFlow()

    fun createOrder(
        userId: String,
        restaurantId: String,
        restaurantName: String,
        itemName: String,
        itemDescription: String,
        price: String,
        pickupTime: String,
        restaurantAddress: String
    ) {
        viewModelScope.launch {
            try {
                _orderCreationState.value = OrderCreationState.Loading
                Log.d("OrderViewModel", "Creating order for user: $userId")
                
                val order = Order(
                    orderId = UUID.randomUUID().toString(),
                    userId = userId,
                    restaurantId = restaurantId,
                    restaurantName = restaurantName,
                    itemName = itemName,
                    itemDescription = itemDescription,
                    price = price,
                    pickupTime = pickupTime,
                    orderStatus = OrderStatus.PENDING,
                    restaurantAddress = restaurantAddress
                )
                
                val success = orderRepository.createOrder(order)
                if (success) {
                    _orderCreationState.value = OrderCreationState.Success
                    Log.d("OrderViewModel", "Order created successfully")
                    // Refresh user orders
                    loadUserOrders(userId)
                } else {
                    _orderCreationState.value = OrderCreationState.Error("Failed to create order")
                    Log.e("OrderViewModel", "Failed to create order")
                }
            } catch (e: Exception) {
                _orderCreationState.value = OrderCreationState.Error(e.message ?: "Unknown error")
                Log.e("OrderViewModel", "Exception creating order", e)
            }
        }
    }

    fun loadUserOrders(userId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                Log.d("OrderViewModel", "Loading orders for user: $userId")
                
                val userOrders = orderRepository.getUserOrders(userId)
                _orders.value = userOrders
                Log.d("OrderViewModel", "Loaded ${userOrders.size} orders")
            } catch (e: Exception) {
                Log.e("OrderViewModel", "Exception loading user orders", e)
                _orders.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun resetOrderCreationState() {
        _orderCreationState.value = OrderCreationState.Idle
    }
}

sealed class OrderCreationState {
    object Idle : OrderCreationState()
    object Loading : OrderCreationState()
    object Success : OrderCreationState()
    data class Error(val message: String) : OrderCreationState()
}
