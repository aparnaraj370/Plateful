package com.example.plateful.ui.screens.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.plateful.domain.services.AuthenticationService
import com.example.plateful.domain.model.UserEntryType
import com.example.plateful.domain.model.AuthResult
import com.example.plateful.domain.services.GoogleSignInHelper
import com.example.plateful.domain.services.GoogleSignInResult
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.FirebaseException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CustomerLoginUiState(
    val isLoading: Boolean = false,
    val isOtpSent: Boolean = false,
    val verificationId: String = "",
    val error: String? = null,
    val isSuccess: Boolean = false
)

class CustomerLoginViewModel(
    private val authService: AuthenticationService = AuthenticationService()
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(CustomerLoginUiState())
    val uiState: StateFlow<CustomerLoginUiState> = _uiState.asStateFlow()
    
    /**
     * Sign in with email and password
     */
    fun signInWithEmail(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Please fill in all fields")
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val result = authService.signInWithEmail(email, password, UserEntryType.CUSTOMER_ENTRY)
                when (result) {
                    is AuthResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isSuccess = true
                        )
                    }
                    is AuthResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Sign in failed"
                )
            }
        }
    }
    
    /**
     * Send phone verification OTP
     */
    fun sendPhoneOtp(context: Context, phoneNumber: String) {
        if (phoneNumber.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Please enter phone number")
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    // Auto-verification completed
                    verifyPhoneCredential(credential)
                }
                
                override fun onVerificationFailed(e: FirebaseException) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Phone verification failed"
                    )
                }
                
                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isOtpSent = true,
                        verificationId = verificationId
                    )
                }
            }
            
            try {
                authService.sendPhoneVerification(context, phoneNumber, callbacks)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to send OTP"
                )
            }
        }
    }
    
    /**
     * Verify phone OTP
     */
    fun verifyPhoneOtp(otp: String) {
        if (otp.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Please enter OTP")
            return
        }
        
        val verificationId = _uiState.value.verificationId
        if (verificationId.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "Verification ID not found")
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val result = authService.verifyPhoneOTP(verificationId, otp, UserEntryType.CUSTOMER_ENTRY)
                when (result) {
                    is AuthResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isSuccess = true
                        )
                    }
                    is AuthResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "OTP verification failed"
                )
            }
        }
    }
    
    /**
     * Sign in with Google using Credential Manager
     */
    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val googleSignInHelper = GoogleSignInHelper.create(context)
                val googleResult = googleSignInHelper.signIn()
                
                when (googleResult) {
                    is GoogleSignInResult.Success -> {
                        val authResult = authService.signInWithGoogle(
                            googleResult.authCredential, 
                            UserEntryType.CUSTOMER_ENTRY
                        )
                        when (authResult) {
                            is AuthResult.Success -> {
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    isSuccess = true
                                )
                            }
                            is AuthResult.Error -> {
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    error = authResult.message
                                )
                            }
                        }
                    }
                    is GoogleSignInResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = googleResult.message
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Google sign in failed"
                )
            }
        }
    }
    
    /**
     * Send password reset email
     */
    fun sendPasswordResetEmail(email: String) {
        if (email.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Please enter email address")
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val success = authService.sendPasswordResetEmail(email)
                if (success) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Password reset email sent successfully"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to send password reset email"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to send password reset email"
                )
            }
        }
    }
    
    /**
     * Verify phone credential directly
     */
    private fun verifyPhoneCredential(credential: PhoneAuthCredential) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val result = authService.signInWithGoogle(credential, UserEntryType.CUSTOMER_ENTRY)
                when (result) {
                    is AuthResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isSuccess = true
                        )
                    }
                    is AuthResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Phone verification failed"
                )
            }
        }
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    /**
     * Reset success state
     */
    fun resetSuccessState() {
        _uiState.value = _uiState.value.copy(isSuccess = false)
    }
}
