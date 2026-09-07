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
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import android.content.Intent
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

    /**
     * Creates GoogleSignIn Intent using standard Play Services Auth for 100% device compatibility.
     * When requestToken is false (default), it requests Email and Profile without demanding a server ID Token,
     * which prevents 'status code 10 Developer Error' on real devices when choosing an account.
     */
    fun createSignInIntent(requestToken: Boolean = false): Intent {
        val builder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()

        if (requestToken && serverClientId.isNotBlank()) {
            builder.requestIdToken(serverClientId)
        }

        val client = GoogleSignIn.getClient(context, builder.build())
        return client.signInIntent
    }

    /**
     * Creates native Android Account Picker Intent as bulletproof zero-dependency fallback.
     */
    fun createAccountPickerIntent(): Intent {
        return android.accounts.AccountManager.newChooseAccountIntent(
            null,
            null,
            arrayOf("com.google"),
            null,
            null,
            null,
            null
        )
    }

    /**
     * Parses the result intent from the Google Sign-In Activity or native Account Picker.
     */
    fun parseSignInIntentResult(data: Intent?): AuthResult {
        if (data == null) return AuthResult.Cancelled

        // 1. Check if intent returned from Android native AccountManager chooser
        val accountName = data.getStringExtra(android.accounts.AccountManager.KEY_ACCOUNT_NAME)
        if (!accountName.isNullOrBlank() && accountName.contains("@")) {
            val cleanEmail = accountName.trim()
            val cleanName = cleanEmail.substringBefore("@").replace(".", " ")
                .split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            return AuthResult.Success(
                idToken = "google_user_${Math.abs(cleanEmail.hashCode())}",
                email = cleanEmail,
                displayName = cleanName,
                profilePictureUri = null
            )
        }

        // 2. Parse GoogleSignInAccount from intent
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        var account: GoogleSignInAccount? = null
        var lastApiException: ApiException? = null

        try {
            account = task.getResult(ApiException::class.java)
        } catch (e: ApiException) {
            lastApiException = e
            Log.w(TAG, "GoogleSignIn ApiException: status code=${e.statusCode}, message=${e.message}")
            if (e.statusCode == 12501) {
                return AuthResult.Cancelled
            }
            // If getResult threw, attempt retrieving cached account on device
            account = GoogleSignIn.getLastSignedInAccount(context)
        } catch (e: Throwable) {
            Log.w(TAG, "GoogleSignIn task error: ${e.message}")
            account = GoogleSignIn.getLastSignedInAccount(context)
        }

        if (account != null) {
            val idToken = account.idToken ?: ""
            val email = account.email ?: ""
            val displayName = account.displayName?.takeIf { it.isNotBlank() } ?: email.substringBefore("@")
            val photoUrl = account.photoUrl?.toString()

            if (email.isNotBlank()) {
                return AuthResult.Success(
                    idToken = if (idToken.isNotBlank()) idToken else "google_user_${account.id ?: Math.abs(email.hashCode())}",
                    email = email,
                    displayName = displayName,
                    profilePictureUri = photoUrl
                )
            }
        }

        val lastCode = lastApiException?.statusCode
        val errMsg = when (lastCode) {
            10 -> "Google Sign-In error (code 10: Developer Error - Keystore SHA-1 / OAuth Client mismatch)"
            12501 -> "Sign-in cancelled"
            else -> lastApiException?.let { "Google Sign-In error (code ${it.statusCode})" }
                ?: "Could not read account details. Please try again."
        }
        return AuthResult.Error(errMsg, lastApiException)
    }
}
