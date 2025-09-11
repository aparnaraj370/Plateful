package com.example.plateful.domain.services

import com.example.plateful.domain.model.UserRole
import com.example.plateful.domain.model.UserEntryType
import com.example.plateful.domain.model.UserType

interface UserRoleService {
    suspend fun getUserRole(userId: String, entryType: UserEntryType? = null): UserRole
    suspend fun hasRestaurantProfile(userId: String): Boolean
    suspend fun getRestaurantIdForOwner(userId: String): String?
    fun getNextRoute(userRole: UserRole): String
    fun canAccessRestaurantFeatures(userRole: UserRole): Boolean
    fun canAccessCustomerFeatures(userRole: UserRole): Boolean
    fun getCurrentUserRole(): UserRole?
}
