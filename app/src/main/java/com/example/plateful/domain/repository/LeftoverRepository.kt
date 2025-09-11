package com.example.plateful.domain.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.example.plateful.model.LeftoverEntity
import com.example.plateful.model.LeftoverStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class LeftoverRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun saveLeftover(leftover: LeftoverEntity): String? {
        return withContext(Dispatchers.IO) {
            try {
                val leftoverId = db.collection("leftovers").document().id
                val leftoverData = leftover.copy(
                    id = leftoverId,
                    createdAt = Timestamp.now()
                )
                
                db.collection("leftovers")
                    .document(leftoverId)
                    .set(leftoverData)
                    .await()
                
                Log.d("LeftoverRepository", "Leftover saved successfully: $leftoverId")
                leftoverId
            } catch (e: Exception) {
                Log.e("LeftoverRepository", "Error saving leftover", e)
                null
            }
        }
    }
    
    suspend fun getAvailableLeftovers(): List<LeftoverEntity> {
        return withContext(Dispatchers.IO) {
            try {
                val result = db.collection("leftovers")
                    .whereEqualTo("status", LeftoverStatus.AVAILABLE.name)
                    .whereGreaterThan("pickupEnd", Timestamp.now())
                    .orderBy("pickupEnd", Query.Direction.ASCENDING)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get()
                    .await()
                
                result.toObjects(LeftoverEntity::class.java)
            } catch (e: Exception) {
                Log.e("LeftoverRepository", "Error getting leftovers", e)
                emptyList()
            }
        }
    }
    
    suspend fun getLeftoversByRestaurant(restaurantId: String): List<LeftoverEntity> {
        return withContext(Dispatchers.IO) {
            try {
                val result = db.collection("leftovers")
                    .whereEqualTo("restaurantId", restaurantId)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get()
                    .await()
                
                result.toObjects(LeftoverEntity::class.java)
            } catch (e: Exception) {
                Log.e("LeftoverRepository", "Error getting restaurant leftovers", e)
                emptyList()
            }
        }
    }
    
    suspend fun updateLeftoverStatus(leftoverId: String, status: LeftoverStatus): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                db.collection("leftovers")
                    .document(leftoverId)
                    .update("status", status.name)
                    .await()
                
                Log.d("LeftoverRepository", "Leftover status updated: $leftoverId -> $status")
                true
            } catch (e: Exception) {
                Log.e("LeftoverRepository", "Error updating leftover status", e)
                false
            }
        }
    }
}
