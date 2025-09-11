package com.example.plateful.domain.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.example.plateful.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class FoodPackRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // CRUD Operations for Food Packs
    suspend fun createFoodPack(foodPack: FoodPack): String? {
        return withContext(Dispatchers.IO) {
            try {
                val foodPackId = db.collection("foodPacks").document().id
                val foodPackData = foodPack.copy(
                    id = foodPackId,
                    createdAt = Timestamp.now(),
                    updatedAt = Timestamp.now()
                )
                
                db.collection("foodPacks")
                    .document(foodPackId)
                    .set(foodPackData)
                    .await()
                
                Log.d("FoodPackRepository", "Food pack created successfully: $foodPackId")
                foodPackId
            } catch (e: Exception) {
                Log.e("FoodPackRepository", "Error creating food pack", e)
                null
            }
        }
    }
    
    suspend fun getFoodPacksByVendor(vendorId: String): List<FoodPack> {
        return withContext(Dispatchers.IO) {
            try {
                val result = db.collection("foodPacks")
                    .whereEqualTo("vendorId", vendorId)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get()
                    .await()
                
                result.toObjects(FoodPack::class.java)
            } catch (e: Exception) {
                Log.e("FoodPackRepository", "Error getting vendor food packs", e)
                emptyList()
            }
        }
    }
    
    suspend fun getAvailableFoodPacks(): List<FoodPack> {
        return withContext(Dispatchers.IO) {
            try {
                val result = db.collection("foodPacks")
                    .whereEqualTo("status", FoodPackStatus.AVAILABLE.name)
                    .whereGreaterThan("quantity", 0)
                    .whereGreaterThan("expiryTime", Timestamp.now())
                    .orderBy("expiryTime", Query.Direction.ASCENDING)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get()
                    .await()
                
                result.toObjects(FoodPack::class.java)
            } catch (e: Exception) {
                Log.e("FoodPackRepository", "Error getting available food packs", e)
                emptyList()
            }
        }
    }
    
    suspend fun updateFoodPack(foodPack: FoodPack): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val updatedFoodPack = foodPack.copy(updatedAt = Timestamp.now())
                
                db.collection("foodPacks")
                    .document(foodPack.id)
                    .set(updatedFoodPack)
                    .await()
                
                Log.d("FoodPackRepository", "Food pack updated successfully: ${foodPack.id}")
                true
            } catch (e: Exception) {
                Log.e("FoodPackRepository", "Error updating food pack", e)
                false
            }
        }
    }
    
    suspend fun deleteFoodPack(foodPackId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                db.collection("foodPacks")
                    .document(foodPackId)
                    .delete()
                    .await()
                
                Log.d("FoodPackRepository", "Food pack deleted successfully: $foodPackId")
                true
            } catch (e: Exception) {
                Log.e("FoodPackRepository", "Error deleting food pack", e)
                false
            }
        }
    }
    
    // Reservation Management
    suspend fun createReservation(reservation: FoodPackReservation): String? {
        return withContext(Dispatchers.IO) {
            try {
                val reservationId = db.collection("reservations").document().id
                val reservationData = reservation.copy(
                    id = reservationId,
                    reservedAt = Timestamp.now(),
                    pickupCode = generatePickupCode()
                )
                
                // Create reservation
                db.collection("reservations")
                    .document(reservationId)
                    .set(reservationData)
                    .await()
                
                // Update food pack quantity
                updateFoodPackQuantity(reservation.foodPackId, reservation.quantity)
                
                Log.d("FoodPackRepository", "Reservation created successfully: $reservationId")
                reservationId
            } catch (e: Exception) {
                Log.e("FoodPackRepository", "Error creating reservation", e)
                null
            }
        }
    }
    
    suspend fun getReservationsByVendor(vendorId: String): List<FoodPackReservation> {
        return withContext(Dispatchers.IO) {
            try {
                val result = db.collection("reservations")
                    .whereEqualTo("vendorId", vendorId)
                    .orderBy("reservedAt", Query.Direction.DESCENDING)
                    .get()
                    .await()
                
                result.toObjects(FoodPackReservation::class.java)
            } catch (e: Exception) {
                Log.e("FoodPackRepository", "Error getting vendor reservations", e)
                emptyList()
            }
        }
    }
    
    suspend fun updateReservationStatus(
        reservationId: String, 
        status: ReservationStatus,
        cancelReason: String = ""
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val updates = mutableMapOf<String, Any>(
                    "status" to status.name
                )
                
                when (status) {
                    ReservationStatus.COMPLETED -> {
                        updates["completedAt"] = Timestamp.now()
                    }
                    ReservationStatus.CANCELED -> {
                        updates["canceledAt"] = Timestamp.now()
                        if (cancelReason.isNotEmpty()) {
                            updates["cancelReason"] = cancelReason
                        }
                    }
                    else -> {}
                }
                
                db.collection("reservations")
                    .document(reservationId)
                    .update(updates)
                    .await()
                
                Log.d("FoodPackRepository", "Reservation status updated: $reservationId -> $status")
                true
            } catch (e: Exception) {
                Log.e("FoodPackRepository", "Error updating reservation status", e)
                false
            }
        }
    }
    
    // Analytics Functions
    suspend fun getVendorAnalytics(vendorId: String, dateRange: Pair<String, String>): VendorAnalytics {
        return withContext(Dispatchers.IO) {
            try {
                // Get reservations within date range
                val reservations = db.collection("reservations")
                    .whereEqualTo("vendorId", vendorId)
                    .whereGreaterThanOrEqualTo("reservedAt", parseDate(dateRange.first))
                    .whereLessThanOrEqualTo("reservedAt", parseDate(dateRange.second))
                    .get()
                    .await()
                    .toObjects(FoodPackReservation::class.java)
                
                val completedOrders = reservations.filter { it.status == ReservationStatus.COMPLETED }
                
                VendorAnalytics(
                    vendorId = vendorId,
                    date = dateRange.first,
                    totalSales = completedOrders.sumOf { it.totalPrice },
                    totalOrders = completedOrders.size,
                    packsSold = completedOrders.sumOf { it.quantity },
                    foodSavedKg = completedOrders.sumOf { it.quantity * 0.3 }, // Estimate 0.3kg per pack
                    averageRating = calculateAverageRating(vendorId),
                    newCustomers = getNewCustomersCount(vendorId, dateRange),
                    repeatCustomers = getRepeatCustomersCount(vendorId, dateRange)
                )
            } catch (e: Exception) {
                Log.e("FoodPackRepository", "Error getting vendor analytics", e)
                VendorAnalytics(vendorId = vendorId)
            }
        }
    }
    
    // ML Demand Prediction Functions
    suspend fun saveDemandPrediction(prediction: DemandPrediction): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val predictionId = db.collection("demandPredictions").document().id
                val predictionData = prediction.copy(
                    id = predictionId,
                    generatedAt = Timestamp.now()
                )
                
                db.collection("demandPredictions")
                    .document(predictionId)
                    .set(predictionData)
                    .await()
                
                true
            } catch (e: Exception) {
                Log.e("FoodPackRepository", "Error saving demand prediction", e)
                false
            }
        }
    }
    
    suspend fun getDemandPredictions(restaurantId: String): List<DemandPrediction> {
        return withContext(Dispatchers.IO) {
            try {
                val result = db.collection("demandPredictions")
                    .whereEqualTo("restaurantId", restaurantId)
                    .orderBy("predictedDate", Query.Direction.ASCENDING)
                    .get()
                    .await()
                
                result.toObjects(DemandPrediction::class.java)
            } catch (e: Exception) {
                Log.e("FoodPackRepository", "Error getting demand predictions", e)
                emptyList()
            }
        }
    }
    
    // Helper Functions
    private suspend fun updateFoodPackQuantity(foodPackId: String, reservedQuantity: Int) {
        try {
            val foodPackRef = db.collection("foodPacks").document(foodPackId)
            val foodPack = foodPackRef.get().await().toObject(FoodPack::class.java)
            
            if (foodPack != null) {
                val newQuantity = foodPack.quantity - reservedQuantity
                val newStatus = if (newQuantity <= 0) FoodPackStatus.RESERVED else foodPack.status
                
                foodPackRef.update(
                    mapOf(
                        "quantity" to newQuantity,
                        "status" to newStatus.name,
                        "updatedAt" to Timestamp.now()
                    )
                ).await()
            }
        } catch (e: Exception) {
            Log.e("FoodPackRepository", "Error updating food pack quantity", e)
        }
    }
    
    private fun generatePickupCode(): String {
        return (1000..9999).random().toString()
    }
    
    private fun parseDate(dateString: String): Timestamp {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = format.parse(dateString) ?: Date()
        return Timestamp(date)
    }
    
    private suspend fun calculateAverageRating(vendorId: String): Double {
        // Implement rating calculation logic
        return 4.2 // Placeholder
    }
    
    private suspend fun getNewCustomersCount(vendorId: String, dateRange: Pair<String, String>): Int {
        // Implement new customers count logic
        return 5 // Placeholder
    }
    
    private suspend fun getRepeatCustomersCount(vendorId: String, dateRange: Pair<String, String>): Int {
        // Implement repeat customers count logic
        return 12 // Placeholder
    }
}
