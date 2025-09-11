package com.example.plateful.domain.services

import android.content.Context
import android.util.Log
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import com.example.plateful.model.UserEntity
import com.example.plateful.repository.UserDataRepository
import com.example.plateful.domain.model.UserEntryType
import com.example.plateful.domain.model.UserType
import com.example.plateful.domain.model.UserRole
import com.example.plateful.domain.model.AuthResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Handles authentication operations with role-based user management
 */
class AuthenticationService(
    private val userRepository: UserDataRepository = UserDataRepository(),
    private val userRoleService: UserRoleService = UserRoleServiceImpl()
) {
    private val auth = FirebaseAuth.getInstance()
    
    /**
     * Authenticates user with email and password, ensuring user document exists
     */
    suspend fun signInWithEmail(
        email: String, 
        password: String, 
        entryType: UserEntryType
    ): AuthResult {
        return withContext(Dispatchers.IO) {
            try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                val user = result.user ?: throw Exception("Authentication failed")
                
                // Ensure user document exists in database
                val userEntity = userRepository.ensureUserDocument(user)
                    ?: throw Exception("Failed to create user document")
                
                // Update entry type if needed
                updateUserEntryType(user.uid, entryType)
                
                AuthResult.Success(user, userEntity)
            } catch (e: Exception) {
                Log.e("AuthenticationService", "Email sign in failed", e)
                AuthResult.Error(e.message ?: "Authentication failed")
            }
        }
    }
    
    /**
     * Creates new account with email and password
     */
    suspend fun createAccountWithEmail(
        email: String, 
        password: String, 
        name: String,
        entryType: UserEntryType
    ): AuthResult {
        return withContext(Dispatchers.IO) {
            try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val user = result.user ?: throw Exception("Account creation failed")
                
                // Update profile with name
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()
                user.updateProfile(profileUpdates).await()
                
                // Create user document
                val userEntity = UserEntity(
                    uid = user.uid,
                    email = email,
                    name = name,
                    isEmailVerified = user.isEmailVerified,
                    isProfileComplete = false,
                    userType = if (entryType == UserEntryType.RESTAURANT_ENTRY) UserType.RESTAURANT_OWNER else UserType.CUSTOMER,
                    entryType = entryType
                )
                
                val success = userRepository.setUserData(userEntity)
                if (!success) throw Exception("Failed to save user data")
                
                AuthResult.Success(user, userEntity)
            } catch (e: Exception) {
                Log.e("AuthenticationService", "Account creation failed", e)
                AuthResult.Error(e.message ?: "Account creation failed")
            }
        }
    }
    
    /**
     * Initiates phone number verification
     */
    suspend fun sendPhoneVerification(
        context: Context,
        phoneNumber: String,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    ) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, java.util.concurrent.TimeUnit.SECONDS)
            .setActivity(context as androidx.activity.ComponentActivity)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }
    
    /**
     * Verifies phone OTP and signs in user
     */
    suspend fun verifyPhoneOTP(
        verificationId: String, 
        otp: String, 
        entryType: UserEntryType
    ): AuthResult {
        return withContext(Dispatchers.IO) {
            try {
                val credential = PhoneAuthProvider.getCredential(verificationId, otp)
                val result = auth.signInWithCredential(credential).await()
                val user = result.user ?: throw Exception("Phone authentication failed")
                
                // Ensure user document exists
                val userEntity = userRepository.ensureUserDocument(user)
                    ?: throw Exception("Failed to create user document")
                
                // Update entry type
                updateUserEntryType(user.uid, entryType)
                
                AuthResult.Success(user, userEntity)
            } catch (e: Exception) {
                Log.e("AuthenticationService", "Phone verification failed", e)
                AuthResult.Error(e.message ?: "Phone verification failed")
            }
        }
    }
    
    /**
     * Signs in with Google credential
     */
    suspend fun signInWithGoogle(
        credential: AuthCredential, 
        entryType: UserEntryType
    ): AuthResult {
        return withContext(Dispatchers.IO) {
            try {
                val result = auth.signInWithCredential(credential).await()
                val user = result.user ?: throw Exception("Google sign in failed")
                
                // Ensure user document exists
                val userEntity = userRepository.ensureUserDocument(user)
                    ?: throw Exception("Failed to create user document")
                
                // Update entry type
                updateUserEntryType(user.uid, entryType)
                
                AuthResult.Success(user, userEntity)
            } catch (e: Exception) {
                Log.e("AuthenticationService", "Google sign in failed", e)
                AuthResult.Error(e.message ?: "Google sign in failed")
            }
        }
    }
    
    /**
     * Updates user's entry type preference
     */
    private suspend fun updateUserEntryType(uid: String, entryType: UserEntryType) {
        try {
            val updates = mapOf("entryType" to entryType.name)
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .set(updates, com.google.firebase.firestore.SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.w("AuthenticationService", "Failed to update entry type", e)
        }
    }
    
    /**
     * Gets current authenticated user with role information
     */
    suspend fun getCurrentUserWithRole(): Pair<FirebaseUser, UserRole>? {
        return withContext(Dispatchers.IO) {
            try {
                val currentUser = auth.currentUser ?: return@withContext null
                val userRole = userRoleService.getUserRole(currentUser.uid)
                Pair(currentUser, userRole)
            } catch (e: Exception) {
                Log.e("AuthenticationService", "Failed to get current user", e)
                null
            }
        }
    }
    
    /**
     * Signs out current user
     */
    fun signOut() {
        auth.signOut()
    }
    
    /**
     * Sends password reset email
     */
    suspend fun sendPasswordResetEmail(email: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                auth.sendPasswordResetEmail(email).await()
                true
            } catch (e: Exception) {
                Log.e("AuthenticationService", "Password reset failed", e)
                false
            }
        }
    }
}
