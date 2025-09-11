package com.example.plateful.domain.services

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.example.plateful.model.UserEntity
import com.example.plateful.repository.UserDataRepository
import com.example.plateful.domain.model.UserType
import com.example.plateful.domain.model.UserEntryType
import com.example.plateful.domain.model.UserRole
import com.example.plateful.domain.model.SessionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Manages user session state and provides reactive updates
 */
class SessionManager(
    private val context: Context,
    private val userRepository: UserDataRepository = UserDataRepository(),
    private val userRoleService: UserRoleService = UserRoleServiceImpl()
) {
    private val auth = FirebaseAuth.getInstance()
    private val prefs: SharedPreferences = context.getSharedPreferences("session_prefs", Context.MODE_PRIVATE)
    
    private val _sessionState = MutableStateFlow(SessionState())
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()
    
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()
    
    init {
        // Initialize with current auth state
        updateSessionState()
        
        // Listen to auth state changes
        auth.addAuthStateListener { firebaseAuth ->
            updateSessionState()
        }
    }
    
    /**
     * Updates the session state based on current Firebase auth
     */
    private fun updateSessionState() {
        val currentUser = auth.currentUser
        _isLoggedIn.value = currentUser != null
        
        if (currentUser != null) {
            // Load user data asynchronously
            GlobalScope.launch {
                try {
                    val userEntity = userRepository.getUserData(currentUser.uid)
                    val userRole = userRoleService.getUserRole(currentUser.uid)
                    
                    _sessionState.value = SessionState(
                        isAuthenticated = true,
                        firebaseUser = currentUser,
                        userEntity = userEntity,
                        userRole = userRole,
                        isLoading = false
                    )
                } catch (e: Exception) {
                    _sessionState.value = SessionState(
                        isAuthenticated = true,
                        firebaseUser = currentUser,
                        userEntity = null,
                        userRole = null,
                        isLoading = false,
                        error = e.message
                    )
                }
            }
        } else {
            _sessionState.value = SessionState(isAuthenticated = false)
            clearSessionData()
        }
    }
    
    /**
     * Saves user data to session after successful authentication
     */
    suspend fun saveUserSession(firebaseUser: FirebaseUser, userEntity: UserEntity) {
        withContext(Dispatchers.IO) {
            // Save to SharedPreferences for quick access
            prefs.edit().apply {
                putString("user_id", firebaseUser.uid)
                putString("user_email", firebaseUser.email)
                putString("user_name", firebaseUser.displayName)
                putString("user_type", userEntity.userType.name)
                putString("entry_type", userEntity.entryType.name)
                putString("restaurant_id", userEntity.restaurantId)
                putBoolean("is_profile_complete", userEntity.isProfileComplete)
                apply()
            }
            
            // Update state
            val userRole = userRoleService.getUserRole(firebaseUser.uid)
            _sessionState.value = SessionState(
                isAuthenticated = true,
                firebaseUser = firebaseUser,
                userEntity = userEntity,
                userRole = userRole,
                isLoading = false
            )
        }
    }
    
    /**
     * Updates user role after restaurant onboarding
     */
    suspend fun updateUserRole(restaurantId: String) {
        withContext(Dispatchers.IO) {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                // Update user to restaurant owner
                val success = userRepository.setUserAsRestaurantOwner(currentUser.uid, restaurantId)
                if (success) {
                    // Update SharedPreferences
                    prefs.edit().apply {
                        putString("user_type", "RESTAURANT_OWNER")
                        putString("restaurant_id", restaurantId)
                        putBoolean("is_profile_complete", true)
                        apply()
                    }
                    
                    // Refresh session state
                    updateSessionState()
                }
            }
        }
    }
    
    /**
     * Gets current user type from session
     */
    fun getCurrentUserType(): UserType? {
        return when (prefs.getString("user_type", null)) {
            "CUSTOMER" -> UserType.CUSTOMER
            "RESTAURANT_OWNER" -> UserType.RESTAURANT_OWNER
            else -> null
        }
    }
    
    /**
     * Gets current entry type from session
     */
    fun getCurrentEntryType(): UserEntryType? {
        return when (prefs.getString("entry_type", null)) {
            "CUSTOMER_ENTRY" -> UserEntryType.CUSTOMER_ENTRY
            "RESTAURANT_ENTRY" -> UserEntryType.RESTAURANT_ENTRY
            else -> null
        }
    }
    
    /**
     * Gets restaurant ID if user is restaurant owner
     */
    fun getRestaurantId(): String? {
        return prefs.getString("restaurant_id", null)
    }
    
    /**
     * Checks if user profile is complete
     */
    fun isProfileComplete(): Boolean {
        return prefs.getBoolean("is_profile_complete", false)
    }
    
    /**
     * Signs out user and clears session
     */
    fun signOut() {
        auth.signOut()
        clearSessionData()
    }
    
    /**
     * Clears all session data
     */
    private fun clearSessionData() {
        prefs.edit().clear().apply()
        _sessionState.value = SessionState(isAuthenticated = false)
    }
    
    /**
     * Determines next screen based on session state
     */
    fun getNextScreen(): String {
        val state = _sessionState.value
        
        return when {
            !state.isAuthenticated -> "app_entry_selection"
            state.userEntity == null -> "personal_details"
            !state.userEntity.isProfileComplete -> "personal_details"
            state.userRole?.userType == UserType.RESTAURANT_OWNER -> "NavOwnerDashboard"
            state.userRole?.entryType == UserEntryType.RESTAURANT_ENTRY -> "restaurant_onboarding"
            else -> "main_screen"
        }
    }
    
    /**
     * Validates if user can access restaurant features
     */
    fun canAccessRestaurantFeatures(): Boolean {
        val userType = getCurrentUserType()
        val restaurantId = getRestaurantId()
        return userType == UserType.RESTAURANT_OWNER && !restaurantId.isNullOrEmpty()
    }
    
    /**
     * Validates if user can access customer features
     */
    fun canAccessCustomerFeatures(): Boolean {
        val userType = getCurrentUserType()
        val entryType = getCurrentEntryType()
        return userType == UserType.CUSTOMER || entryType == UserEntryType.CUSTOMER_ENTRY
    }
}

/**
 * Represents the current session state
 */
data class SessionState(
    val isAuthenticated: Boolean = false,
    val firebaseUser: FirebaseUser? = null,
    val userEntity: UserEntity? = null,
    val userRole: UserRole? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)
