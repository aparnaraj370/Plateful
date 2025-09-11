package com.example.plateful.domain.model

import com.google.firebase.auth.FirebaseUser
import com.example.plateful.model.UserEntity

data class SessionState(
    val isAuthenticated: Boolean = false,
    val firebaseUser: FirebaseUser? = null,
    val userEntity: UserEntity? = null,
    val userRole: UserRole? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)
