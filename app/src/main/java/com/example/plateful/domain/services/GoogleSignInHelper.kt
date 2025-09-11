package com.example.plateful.domain.services

import android.content.Context
import android.content.IntentSender
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.credentials.*
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.AuthCredential
import kotlinx.coroutines.tasks.await

/**
 * Helper class for managing Google Sign-In using Credential Manager API
 */
class GoogleSignInHelper(
    private val context: Context,
    private val webClientId: String = "161973331481-dvecf0hun3237uft3d8ahm0e3hv1l4eb.apps.googleusercontent.com" // Replace with actual web client ID
) {
    
    private val credentialManager = CredentialManager.create(context)
    
    /**
     * Initiates Google Sign-In flow using Credential Manager
     */
    suspend fun signIn(): GoogleSignInResult {
        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(true)
                .build()
            
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
            
            val result = credentialManager.getCredential(
                request = request,
                context = context as ComponentActivity
            )
            
            handleCredentialResult(result)
        } catch (e: GetCredentialException) {
            Log.e("GoogleSignInHelper", "Get credential failed", e)
            GoogleSignInResult.Error(e.message ?: "Sign in failed")
        } catch (e: Exception) {
            Log.e("GoogleSignInHelper", "Sign in failed", e)
            GoogleSignInResult.Error(e.message ?: "Sign in failed")
        }
    }
    
    /**
     * Handles the credential result and extracts Firebase AuthCredential
     */
    private fun handleCredentialResult(result: GetCredentialResponse): GoogleSignInResult {
        return try {
            when (val credential = result.credential) {
                is CustomCredential -> {
                    if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        val idToken = googleIdTokenCredential.idToken
                        
                        // Create Firebase AuthCredential
                        val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                        
                        GoogleSignInResult.Success(
                            authCredential = firebaseCredential,
                            displayName = googleIdTokenCredential.displayName,
                            email = googleIdTokenCredential.id,
                            photoUrl = googleIdTokenCredential.profilePictureUri?.toString()
                        )
                    } else {
                        GoogleSignInResult.Error("Received invalid credential type")
                    }
                }
                else -> {
                    GoogleSignInResult.Error("Unexpected credential type")
                }
            }
        } catch (e: GoogleIdTokenParsingException) {
            Log.e("GoogleSignInHelper", "Received an invalid google id token response", e)
            GoogleSignInResult.Error("Invalid Google ID token")
        } catch (e: Exception) {
            Log.e("GoogleSignInHelper", "Error handling credential result", e)
            GoogleSignInResult.Error(e.message ?: "Credential processing failed")
        }
    }
    
    /**
     * Signs out the user from Google
     */
    suspend fun signOut(): Boolean {
        return try {
            credentialManager.clearCredentialState(
                ClearCredentialStateRequest()
            )
            true
        } catch (e: Exception) {
            Log.e("GoogleSignInHelper", "Sign out failed", e)
            false
        }
    }
    
    companion object {
        /**
         * Creates a Google Sign-In helper with the provided web client ID
         */
        fun create(context: Context, webClientId: String? = null): GoogleSignInHelper {
            val clientId = webClientId ?: getDefaultWebClientId(context)
            return GoogleSignInHelper(context, clientId)
        }
        
        /**
         * Gets the default web client ID from string resources
         */
        private fun getDefaultWebClientId(context: Context): String {
            // This should be stored in strings.xml or BuildConfig
            // For now, using a placeholder
            return context.getString(
                context.resources.getIdentifier(
                    "default_web_client_id", 
                    "string", 
                    context.packageName
                )
            ).takeIf { it.isNotEmpty() } 
                ?: "YOUR_WEB_CLIENT_ID_HERE"
        }
    }
}

/**
 * Sealed class representing Google Sign-In results
 */
sealed class GoogleSignInResult {
    data class Success(
        val authCredential: AuthCredential,
        val displayName: String?,
        val email: String?,
        val photoUrl: String?
    ) : GoogleSignInResult()
    
    data class Error(val message: String) : GoogleSignInResult()
}

/**
 * Extension function for easier Google Sign-In integration in Composables
 */
suspend fun Context.signInWithGoogle(webClientId: String? = null): GoogleSignInResult {
    return GoogleSignInHelper.create(this, webClientId).signIn()
}
