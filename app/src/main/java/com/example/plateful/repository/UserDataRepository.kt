package com.example.plateful.repository

import com.example.plateful.model.UserEntity
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserDataRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    suspend fun getUserData(uid: String): UserEntity? {
        return try {
            val document = firestore.collection("users").document(uid).get().await()
            document.toObject(UserEntity::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun setUserData(user: UserEntity): Boolean {
        return try {
            firestore.collection("users").document(user.uid).set(user).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun ensureUserDocument(firebaseUser: FirebaseUser): UserEntity? {
        val existing = getUserData(firebaseUser.uid)
        if (existing != null) return existing

        val newUser = UserEntity(
            uid = firebaseUser.uid,
            email = firebaseUser.email ?: "",
            name = firebaseUser.displayName ?: "",
            phoneNumber = firebaseUser.phoneNumber ?: "",
            isEmailVerified = firebaseUser.isEmailVerified,
            profileUrl = firebaseUser.photoUrl?.toString() ?: "",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        return if (setUserData(newUser)) newUser else null
    }

    suspend fun setUserAsRestaurantOwner(uid: String, restaurantId: String): Boolean {
        return try {
            val updates = mapOf(
                "userType" to "RESTAURANT_OWNER",
                "restaurantId" to restaurantId,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore.collection("users").document(uid).update(updates).await()
            true
        } catch (e: Exception) {
            false
        }
    }
}
