package com.example.plateful.domain.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.plateful.model.RestaurantEntity
import com.example.plateful.domain.model.UserType
import com.example.plateful.repository.UserDataRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class RestaurantDataRepository {

    private val db = FirebaseFirestore.getInstance()
    private val userRepository = UserDataRepository()

    suspend fun getRestaurantData(restaurantId: String): RestaurantEntity? {
        return withContext(Dispatchers.IO) {
            try {
                db.collection("restaurants")
                    .document(restaurantId)
                    .get()
                    .await()?.toObject(RestaurantEntity::class.java)
            } catch (e: Exception) {
                Log.e("RestaurantDataRepository", "Error getting restaurant data", e)
                null
            }
        }
    }

    suspend fun setRestaurantData(restaurant: RestaurantEntity): Boolean {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val ownerId = currentUser?.uid ?: return false
        
        val restaurantSave = restaurant.copy(
            restaurantId = ownerId,
            ownerId = ownerId
        )
        
        return withContext(Dispatchers.IO) {
            try {
                // Save restaurant data
                db.collection("restaurants")
                    .document(restaurantSave.restaurantId)
                    .set(restaurantSave)
                    .await()
                
                // Update user type to RESTAURANT_OWNER
                val updateSuccess = userRepository.setUserAsRestaurantOwner(ownerId, restaurantSave.restaurantId)
                
                if (updateSuccess) {
                    Log.d("RestaurantDataRepository", "Restaurant created and user updated to owner")
                    true
                } else {
                    Log.w("RestaurantDataRepository", "Restaurant created but failed to update user type")
                    true // Still return true as restaurant was created
                }
            } catch (e: Exception) {
                Log.e("RestaurantDataRepository", "Error setting restaurant data", e)
                false
            }
        }
    }
}