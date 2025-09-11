package com.example.plateful.domain.services

import com.example.plateful.domain.model.UserRole
import com.example.plateful.domain.model.UserEntryType
import com.example.plateful.domain.model.UserType
import com.example.plateful.repository.UserDataRepository
import com.google.firebase.firestore.FirebaseFirestore

class UserRoleServiceImpl(
    private val userRepository: UserDataRepository = UserDataRepository(FirebaseFirestore.getInstance()),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : UserRoleService {

    private var currentUserRole: UserRole? = null

    override suspend fun getUserRole(userId: String, entryType: UserEntryType?): UserRole {
        val user = userRepository.getUserData(userId)
        val hasRestaurant = hasRestaurantProfile(userId)
        
        return when {
            user != null && hasRestaurant -> {
                val restaurantId = getRestaurantIdForOwner(userId) ?: ""
                UserRole(
                    userType = UserType.RESTAURANT_OWNER,
                    entryType = entryType ?: UserEntryType.RESTAURANT_ENTRY,
                    restaurantId = restaurantId,
                    isProfileComplete = user.isProfileComplete
                )
            }
            user != null -> {
                UserRole(
                    userType = UserType.CUSTOMER,
                    entryType = entryType ?: UserEntryType.CUSTOMER_ENTRY,
                    restaurantId = "",
                    isProfileComplete = user.isProfileComplete
                )
            }
            else -> {
                UserRole(
                    userType = UserType.CUSTOMER,
                    entryType = entryType ?: UserEntryType.CUSTOMER_ENTRY,
                    restaurantId = "",
                    isProfileComplete = false
                )
            }
        }.also {
            currentUserRole = it
        }
    }

    override suspend fun hasRestaurantProfile(userId: String): Boolean {
        return try {
            val querySnapshot = firestore.collection("restaurants")
                .whereEqualTo("ownerId", userId)
                .get()
                .result
            !querySnapshot.isEmpty
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getRestaurantIdForOwner(userId: String): String? {
        return try {
            val querySnapshot = firestore.collection("restaurants")
                .whereEqualTo("ownerId", userId)
                .get()
                .result
            if (!querySnapshot.isEmpty) {
                querySnapshot.documents.firstOrNull()?.id
            } else null
        } catch (e: Exception) {
            null
        }
    }

    override fun getNextRoute(userRole: UserRole): String {
        return when {
            !userRole.isProfileComplete -> "NavPersonalDetails"
            userRole.userType == UserType.RESTAURANT_OWNER && userRole.restaurantId.isNotEmpty() -> "NavRestaurantDashboardScreen"
            userRole.userType == UserType.RESTAURANT_OWNER -> "NavListingRestaurant"
            else -> "NavMainScreen"
        }
    }

    override fun canAccessRestaurantFeatures(userRole: UserRole): Boolean {
        return userRole.userType == UserType.RESTAURANT_OWNER
    }

    override fun canAccessCustomerFeatures(userRole: UserRole): Boolean {
        return userRole.userType == UserType.CUSTOMER
    }

    override fun getCurrentUserRole(): UserRole? {
        return currentUserRole
    }
}
