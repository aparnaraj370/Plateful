package com.example.plateful.domain.model

import com.google.firebase.auth.FirebaseUser
import com.example.plateful.model.UserEntity

sealed class AuthResult {
    data class Success(
        val firebaseUser: FirebaseUser,
        val userEntity: UserEntity
    ) : AuthResult()
    
    data class Error(
        val message: String
    ) : AuthResult()
}
