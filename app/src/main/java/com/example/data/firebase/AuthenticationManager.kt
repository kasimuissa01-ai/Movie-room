package com.example.data.firebase

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialCustomException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.UUID

/**
 * Result state for Google Authentication
 */
sealed class AuthResult {
    data class Success(
        val idToken: String,
        val email: String,
        val displayName: String,
        val profilePictureUri: String?
    ) : AuthResult()

    data class Error(val message: String, val cause: Throwable? = null) : AuthResult()
    object Cancelled : AuthResult()
}

/**
 * AuthenticationManager handles Google Sign-In using Android CredentialManager and GoogleIdOption.
 */
class AuthenticationManager(
    private val context: Context,
    private val serverClientId: String = DEFAULT_WEB_CLIENT_ID
) {
    companion object {
        private const val TAG = "AuthenticationManager"
        const val DEFAULT_WEB_CLIENT_ID = "791839339295-hrvsao6av3ndccilkflg855gdn8fpkpp.apps.googleusercontent.com"
    }

    private val credentialManager: CredentialManager = CredentialManager.create(context)

    /**
     * Traverses context hierarchy to obtain an Activity for UI bottom sheet presentation.
     */
    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

    /**
     * Generates a SHA-256 hashed nonce for OAuth token request integrity.
     */
    private fun generateHashedNonce(): String {
        val rawNonce = UUID.randomUUID().toString()
        val bytes = MessageDigest.getInstance("SHA-256").digest(rawNonce.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Builds the Google ID Option and GetCredentialRequest.
     */
    private fun buildGoogleIdOption(
        filterByAuthorizedAccounts: Boolean = false,
        autoSelectEnabled: Boolean = false
    ): GetGoogleIdOption {
        return GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(filterByAuthorizedAccounts)
            .setServerClientId(serverClientId)
            .setAutoSelectEnabled(autoSelectEnabled)
            .setNonce(generateHashedNonce())
            .build()
    }

    /**
     * Initiates Google Sign-In with CredentialManager.
     * Correctly handles account selection, cancellations, and the 'NoCredentialException' error.
     */
    suspend fun signInWithGoogle(): AuthResult = withContext(Dispatchers.Main) {
        val activity = context.findActivity() ?: context

        try {
            // First attempt with standard request (displays account chooser for all Google accounts on device)
            val googleIdOption = buildGoogleIdOption(
                filterByAuthorizedAccounts = false,
                autoSelectEnabled = false
            )

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val response: GetCredentialResponse = credentialManager.getCredential(
                request = request,
                context = activity
            )

            handleCredentialResponse(response)
        } catch (e: GetCredentialCancellationException) {
            Log.d(TAG, "Google sign-in was cancelled by user: ${e.message}")
            AuthResult.Cancelled
        } catch (e: NoCredentialException) {
            Log.w(TAG, "No credential available. Trying fallback without nonce filter: ${e.message}")
            // Fallback attempt with minimal options
            tryFallbackSignIn(activity)
        } catch (e: GetCredentialCustomException) {
            Log.e(TAG, "Custom credential exception: ${e.type} - ${e.message}", e)
            AuthResult.Error("Google Sign-In failed: ${e.message ?: "Authentication error"}", e)
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Credential Manager error: ${e.message}", e)
            AuthResult.Error("Google Sign-In error: ${e.message ?: "Could not sign in"}", e)
        } catch (e: Throwable) {
            Log.e(TAG, "Unexpected error during Google sign-in: ${e.message}", e)
            AuthResult.Error(e.localizedMessage ?: "Unexpected error during Google sign in", e)
        }
    }

    /**
     * Fallback signIn attempt if first pass returned NoCredentialException.
     */
    private suspend fun tryFallbackSignIn(activityContext: Context): AuthResult {
        return try {
            val fallbackOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(serverClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(fallbackOption)
                .build()

            val response: GetCredentialResponse = credentialManager.getCredential(
                request = request,
                context = activityContext
            )

            handleCredentialResponse(response)
        } catch (e: GetCredentialCancellationException) {
            AuthResult.Cancelled
        } catch (e: NoCredentialException) {
            Log.w(TAG, "No Google accounts available on this device: ${e.message}")
            AuthResult.Error("No Google account selected or available on this device", e)
        } catch (e: Throwable) {
            Log.e(TAG, "Fallback signIn failed: ${e.message}", e)
            AuthResult.Error(e.localizedMessage ?: "Google Sign-In failed", e)
        }
    }

    /**
     * Parses the credential response into an AuthResult.
     */
    private fun handleCredentialResponse(response: GetCredentialResponse): AuthResult {
        val credential = response.credential

        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            return try {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                val email = googleIdTokenCredential.id
                val displayName = googleIdTokenCredential.displayName ?: email.substringBefore("@")
                val photoUrl = googleIdTokenCredential.profilePictureUri?.toString()

                if (idToken.isNotBlank()) {
                    AuthResult.Success(
                        idToken = idToken,
                        email = email,
                        displayName = displayName,
                        profilePictureUri = photoUrl
                    )
                } else {
                    AuthResult.Error("Received empty Google ID Token")
                }
            } catch (e: GoogleIdTokenParsingException) {
                Log.e(TAG, "Failed to parse Google ID Token: ${e.message}", e)
                AuthResult.Error("Failed to parse Google account credentials: ${e.message}", e)
            }
        }

        return AuthResult.Error("Unsupported credential type: ${credential.javaClass.simpleName}")
    }
}
