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
                Log.d("FoodPackRepository", "=== CREATING FOOD PACK ===")
                Log.d("FoodPackRepository", "🍴 Food pack name: '${foodPack.name}'")
                Log.d("FoodPackRepository", "🏢 Restaurant: '${foodPack.restaurantName}'")
                Log.d("FoodPackRepository", "👤 Vendor ID: '${foodPack.vendorId}'")
                Log.d("FoodPackRepository", "💰 Price: ₹${foodPack.discountedPrice}")
                Log.d("FoodPackRepository", "💰 Original Price: ₹${foodPack.originalPrice}")
                Log.d("FoodPackRepository", "🔢 Quantity: ${foodPack.quantity}")
                Log.d("FoodPackRepository", "⏰ Expiry: ${foodPack.expiryTime}")
                
                val foodPackId = db.collection("foodPacks").document().id
                Log.d("FoodPackRepository", "🏷️ Generated ID: $foodPackId")
                
                val foodPackData = mapOf(
                    "id" to foodPackId,
                    "restaurantId" to foodPack.restaurantId,
                    "restaurantName" to foodPack.restaurantName,
                    "vendorId" to foodPack.vendorId,
                    "name" to foodPack.name,
                    "description" to foodPack.description,
                    "originalPrice" to foodPack.originalPrice,
                    "discountedPrice" to foodPack.discountedPrice,
                    "quantity" to foodPack.quantity,
                    "totalQuantity" to foodPack.totalQuantity,
                    "expiryTime" to foodPack.expiryTime,
                    "createdAt" to Timestamp.now(),
                    "updatedAt" to Timestamp.now(),
                    "cuisineType" to foodPack.cuisineType.name,
                    "isVegetarian" to foodPack.isVegetarian,
                    "isVegan" to foodPack.isVegan,
                    "status" to foodPack.status.name,
                    "images" to foodPack.images,
                    "nutritionInfo" to foodPack.nutritionInfo,
                    "allergyInfo" to foodPack.allergyInfo,
                    "packagingType" to foodPack.packagingType,
                    "pickupInstructions" to foodPack.pickupInstructions,
                    "rating" to foodPack.rating,
                    "reviewCount" to foodPack.reviewCount,
                    "location" to mapOf(
                        "address" to foodPack.location.address,
                        "city" to foodPack.location.city,
                        "state" to foodPack.location.state,
                        "postalCode" to foodPack.location.postalCode,
                        "latitude" to foodPack.location.latitude,
                        "longitude" to foodPack.location.longitude
                    )
                )
                
                Log.d("FoodPackRepository", "📊 Data map created with ${foodPackData.size} fields")
                Log.d("FoodPackRepository", "📝 Data preview: {")
                foodPackData.forEach { (key, value) ->
                    if (key != "location") {
                        Log.d("FoodPackRepository", "  $key: $value")
                    }
                }
                Log.d("FoodPackRepository", "}")
                
                Log.d("FoodPackRepository", "📡 Starting Firestore write operation...")
                val documentRef = db.collection("foodPacks").document(foodPackId)
                Log.d("FoodPackRepository", "📋 Document reference created: ${documentRef.path}")
                
                // Perform the actual write
                documentRef.set(foodPackData).await()
                Log.d("FoodPackRepository", "✅ Firestore set() operation completed successfully!")
                
                // Verify the write by reading back
                Log.d("FoodPackRepository", "🔍 Verifying write by reading back...")
                val verificationDoc = documentRef.get().await()
                if (verificationDoc.exists()) {
                    val retrievedData = verificationDoc.data
                    Log.d("FoodPackRepository", "✅ Verification successful - document exists")
                    Log.d("FoodPackRepository", "📄 Retrieved document has ${retrievedData?.size} fields")
                    Log.d("FoodPackRepository", "🏷️ Retrieved name: ${retrievedData?.get("name")}")
                } else {
                    Log.w("FoodPackRepository", "⚠️ Verification failed - document does not exist after write!")
                }
                
                Log.d("FoodPackRepository", "🎉 Food pack created successfully: $foodPackId")
                foodPackId
            } catch (e: Exception) {
                Log.e("FoodPackRepository", "❌ Error creating food pack: ${e.message}", e)
                Log.e("FoodPackRepository", "❌ Exception class: ${e.javaClass.simpleName}")
                if (e.cause != null) {
                    Log.e("FoodPackRepository", "❌ Root cause: ${e.cause?.message}")
                }
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
                
                result.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    mapDocumentToFoodPack(data)
                }
            } catch (e: Exception) {
                Log.e("FoodPackRepository", "Error getting vendor food packs", e)
                emptyList()
            }
        }
    }
    
    suspend fun getAvailableFoodPacks(): List<FoodPack> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("FoodPackRepository", "=== FETCHING AVAILABLE FOOD PACKS ===")
                Log.d("FoodPackRepository", "🔍 Querying foodPacks collection...")
                Log.d("FoodPackRepository", "  - Status: ${FoodPackStatus.AVAILABLE.name}")
                Log.d("FoodPackRepository", "  - Quantity > 0")
                Log.d("FoodPackRepository", "  - ExpiryTime > ${Timestamp.now()}")
                
                // Simplified query to avoid composite index issues
                val result = db.collection("foodPacks")
                    .whereEqualTo("status", FoodPackStatus.AVAILABLE.name)
                    .whereGreaterThan("quantity", 0)
                    .get()
                    .await()
                
                Log.d("FoodPackRepository", "✅ Query executed successfully")
                Log.d("FoodPackRepository", "📄 Found ${result.documents.size} documents")
                
                val allFoodPacks = result.documents.mapNotNull { doc ->
                    Log.d("FoodPackRepository", "🔄 Processing document: ${doc.id}")
                    val data = doc.data
                    if (data == null) {
                        Log.w("FoodPackRepository", "⚠️ Document ${doc.id} has null data")
                        return@mapNotNull null
                    }
                    
                    try {
                        val foodPack = mapDocumentToFoodPack(data)
                        if (foodPack != null) {
                            Log.d("FoodPackRepository", "✅ Successfully mapped: ${foodPack.name}")
                        } else {
                            Log.w("FoodPackRepository", "⚠️ Failed to map document ${doc.id}")
                        }
                        foodPack
                    } catch (e: Exception) {
                        Log.e("FoodPackRepository", "Error mapping document ${doc.id}", e)
                        null
                    }
                }
                
                // Filter by expiry time in code (to avoid composite index requirement)
                val currentTime = Timestamp.now()
                val availableFoodPacks = allFoodPacks.filter { foodPack ->
                    foodPack.expiryTime.seconds > currentTime.seconds
                }
                
                Log.d("FoodPackRepository", "🎉 Final result: ${availableFoodPacks.size} available food packs")
                availableFoodPacks.forEach { pack ->
                    Log.d("FoodPackRepository", "  - ${pack.name} by ${pack.restaurantName} (₹${pack.discountedPrice})")
                }
                
                availableFoodPacks
            } catch (e: Exception) {
                Log.e("FoodPackRepository", "❌ Error getting available food packs: ${e.message}", e)
                Log.e("FoodPackRepository", "❌ Exception type: ${e.javaClass.simpleName}")
                emptyList()
            }
        }
    }
    
    suspend fun updateFoodPack(foodPack: FoodPack): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val foodPackData = mapOf(
                    "id" to foodPack.id,
                    "restaurantId" to foodPack.restaurantId,
                    "restaurantName" to foodPack.restaurantName,
                    "vendorId" to foodPack.vendorId,
                    "name" to foodPack.name,
                    "description" to foodPack.description,
                    "originalPrice" to foodPack.originalPrice,
                    "discountedPrice" to foodPack.discountedPrice,
                    "quantity" to foodPack.quantity,
                    "totalQuantity" to foodPack.totalQuantity,
                    "expiryTime" to foodPack.expiryTime,
                    "createdAt" to foodPack.createdAt,
                    "updatedAt" to Timestamp.now(),
                    "cuisineType" to foodPack.cuisineType.name,
                    "isVegetarian" to foodPack.isVegetarian,
                    "isVegan" to foodPack.isVegan,
                    "status" to foodPack.status.name,
                    "images" to foodPack.images,
                    "nutritionInfo" to foodPack.nutritionInfo,
                    "allergyInfo" to foodPack.allergyInfo,
                    "packagingType" to foodPack.packagingType,
                    "pickupInstructions" to foodPack.pickupInstructions,
                    "rating" to foodPack.rating,
                    "reviewCount" to foodPack.reviewCount,
                    "location" to mapOf(
                        "address" to foodPack.location.address,
                        "city" to foodPack.location.city,
                        "state" to foodPack.location.state,
                        "postalCode" to foodPack.location.postalCode,
                        "latitude" to foodPack.location.latitude,
                        "longitude" to foodPack.location.longitude
                    )
                )
                
                db.collection("foodPacks")
                    .document(foodPack.id)
                    .set(foodPackData)
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
    fun mapDocumentToFoodPack(data: Map<String, Any>): FoodPack? {
        return try {
            Log.d("FoodPackRepository", "📝 Mapping document data to FoodPack...")
            
            // Log all available fields
            Log.d("FoodPackRepository", "Available fields: ${data.keys.joinToString(", ")}")
            
            val id = data["id"] as? String
            val restaurantId = data["restaurantId"] as? String
            val restaurantName = data["restaurantName"] as? String ?: ""
            val vendorId = data["vendorId"] as? String
            val name = data["name"] as? String
            val description = data["description"] as? String ?: ""
            val originalPrice = (data["originalPrice"] as? Number)?.toDouble() ?: 0.0
            val discountedPrice = (data["discountedPrice"] as? Number)?.toDouble() ?: 0.0
            val quantity = (data["quantity"] as? Long)?.toInt() ?: 0
            val totalQuantity = (data["totalQuantity"] as? Long)?.toInt() ?: 0
            val expiryTime = data["expiryTime"] as? Timestamp
            val statusString = data["status"] as? String ?: FoodPackStatus.AVAILABLE.name
            val createdAtTimestamp = data["createdAt"] as? Timestamp ?: Timestamp.now()
            val updatedAtTimestamp = data["updatedAt"] as? Timestamp ?: Timestamp.now()
            
            // Log extracted values
            Log.d("FoodPackRepository", "Extracted values:")
            Log.d("FoodPackRepository", "  - id: '$id'")
            Log.d("FoodPackRepository", "  - restaurantId: '$restaurantId'")
            Log.d("FoodPackRepository", "  - vendorId: '$vendorId'")
            Log.d("FoodPackRepository", "  - restaurantName: '$restaurantName'")
            Log.d("FoodPackRepository", "  - name: '$name'")
            Log.d("FoodPackRepository", "  - originalPrice: $originalPrice")
            Log.d("FoodPackRepository", "  - discountedPrice: $discountedPrice")
            Log.d("FoodPackRepository", "  - quantity: $quantity")
            Log.d("FoodPackRepository", "  - status: '$statusString'")
            Log.d("FoodPackRepository", "  - expiryTime: $expiryTime")
            
            // Check required fields
            if (id == null) {
                Log.w("FoodPackRepository", "❌ Missing required field: id")
                return null
            }
            if (restaurantId == null) {
                Log.w("FoodPackRepository", "❌ Missing required field: restaurantId")
                return null
            }
            if (vendorId == null) {
                Log.w("FoodPackRepository", "❌ Missing required field: vendorId")
                return null
            }
            if (name == null) {
                Log.w("FoodPackRepository", "❌ Missing required field: name")
                return null
            }
            if (expiryTime == null) {
                Log.w("FoodPackRepository", "❌ Missing required field: expiryTime")
                return null
            }
            
            val status = try {
                FoodPackStatus.valueOf(statusString)
            } catch (e: Exception) {
                Log.w("FoodPackRepository", "Invalid status '$statusString', using AVAILABLE")
                FoodPackStatus.AVAILABLE
            }
            
            val cuisineType = try {
                CuisineType.valueOf(data["cuisineType"] as? String ?: "VEGETARIAN")
            } catch (e: Exception) {
                Log.w("FoodPackRepository", "Invalid cuisineType, using VEGETARIAN")
                CuisineType.VEGETARIAN
            }
            
            val foodPack = FoodPack(
                id = id,
                restaurantId = restaurantId,
                restaurantName = restaurantName,
                vendorId = vendorId,
                name = name,
                description = description,
                originalPrice = originalPrice,
                discountedPrice = discountedPrice,
                quantity = quantity,
                totalQuantity = totalQuantity,
                expiryTime = expiryTime,
                createdAt = createdAtTimestamp,
                updatedAt = updatedAtTimestamp,
                cuisineType = cuisineType,
                isVegetarian = data["isVegetarian"] as? Boolean ?: false,
                isVegan = data["isVegan"] as? Boolean ?: false,
                status = status,
                images = (data["images"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                nutritionInfo = data["nutritionInfo"] as? String ?: "",
                allergyInfo = data["allergyInfo"] as? String ?: "",
                packagingType = data["packagingType"] as? String ?: "",
                pickupInstructions = data["pickupInstructions"] as? String ?: "",
                rating = (data["rating"] as? Number)?.toDouble() ?: 0.0,
                reviewCount = (data["reviewCount"] as? Long)?.toInt() ?: 0,
                location = run {
                    val locationData = data["location"] as? Map<String, Any> ?: return@run RestaurantLocation()
                    RestaurantLocation(
                        address = locationData["address"] as? String ?: "",
                        city = locationData["city"] as? String ?: "",
                        state = locationData["state"] as? String ?: "",
                        postalCode = locationData["postalCode"] as? String ?: "",
                        latitude = (locationData["latitude"] as? Number)?.toDouble() ?: 0.0,
                        longitude = (locationData["longitude"] as? Number)?.toDouble() ?: 0.0
                    )
                }
            )
            
            Log.d("FoodPackRepository", "✅ Successfully created FoodPack object for '${foodPack.name}'")
            foodPack
        } catch (e: Exception) {
            Log.e("FoodPackRepository", "❌ Error mapping document to FoodPack: ${e.message}", e)
            Log.e("FoodPackRepository", "❌ Document data: $data")
            null
        }
    }
    
    private suspend fun updateFoodPackQuantity(foodPackId: String, reservedQuantity: Int) {
        try {
            val foodPackRef = db.collection("foodPacks").document(foodPackId)
            val doc = foodPackRef.get().await()
            val data = doc.data
            
            if (data != null) {
                val currentQuantity = (data["quantity"] as? Long)?.toInt() ?: 0
                val currentStatus = try { 
                    FoodPackStatus.valueOf(data["status"] as String) 
                } catch (e: Exception) { 
                    FoodPackStatus.AVAILABLE 
                }
                
                val newQuantity = currentQuantity - reservedQuantity
                val newStatus = if (newQuantity <= 0) FoodPackStatus.RESERVED else currentStatus
                
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
