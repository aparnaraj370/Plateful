package com.example.plateful.domain.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.example.plateful.model.Order
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OrderRepository {

    private val db = FirebaseFirestore.getInstance()
    private val ordersCollection = "orders"

    suspend fun createOrder(order: Order): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("OrderRepository", "Creating order: $order")
                db.collection(ordersCollection)
                    .document(order.orderId)
                    .set(order)
                    .await()
                Log.d("OrderRepository", "Order created successfully: ${order.orderId}")
                true
            } catch (e: Exception) {
                Log.e("OrderRepository", "Error creating order", e)
                false
            }
        }
    }

    suspend fun getUserOrders(userId: String): List<Order> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("OrderRepository", "Fetching orders for user: $userId")

                val result = db.collection(ordersCollection)
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()

                Log.d("OrderRepository", "Query executed - found ${result.documents.size} documents")

                val orders = result.documents.mapNotNull { document ->
                    try {
                        val order = document.toObject(Order::class.java)
                        order
                    } catch (e: Exception) {
                        Log.e("OrderRepository", "Error parsing document ${document.id}", e)
                        null
                    }
                }.sortedByDescending { it.orderDate.seconds }

                Log.d("OrderRepository", "Successfully processed ${orders.size} orders")
                return@withContext orders
            } catch (e: Exception) {
                Log.e("OrderRepository", "Error in getUserOrders", e)
                return@withContext emptyList()
            }
        }
    }

    suspend fun updateOrderStatus(orderId: String, newStatus: com.example.plateful.model.OrderStatus): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("OrderRepository", "Updating order status: $orderId to $newStatus")
                db.collection(ordersCollection)
                    .document(orderId)
                    .update("orderStatus", newStatus)
                    .await()
                Log.d("OrderRepository", "Order status updated successfully")
                true
            } catch (e: Exception) {
                Log.e("OrderRepository", "Error updating order status", e)
                false
            }
        }
    }
}
