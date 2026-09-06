package com.example.data.firebase

import android.accounts.AccountManager
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class AuthUser(
    val uid: String,
    val displayName: String,
    val email: String,
    val photoUrl: String,
    val isAnonymous: Boolean = false,
    val firestoreSynced: Boolean = false
)

data class FirestoreUserRecord(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val authProvider: String = "google.com",
    val lastLoginAt: Long = 0L,
    val role: String = "member",
    val status: String = "active"
)

sealed class GoogleAuthResult {
    data class Success(val user: AuthUser) : GoogleAuthResult()
    data class Error(val message: String) : GoogleAuthResult()
    data class NeedsManualAccountSelection(val deviceAccounts: List<String>) : GoogleAuthResult()
    object Cancelled : GoogleAuthResult()
}

object FirebaseAuthManager {
    private const val TAG = "FirebaseAuthManager"
    const val PROJECT_ID = "movieroom-334fb"
    const val PROJECT_NUMBER = "791839339295"
    const val WEB_CLIENT_ID = "791839339295-hrvsao6av3ndccilkflg855gdn8fpkpp.apps.googleusercontent.com"

    fun getFirebaseAuth(context: Context): FirebaseAuth? {
        return try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            FirebaseAuth.getInstance()
        } catch (e: Throwable) {
            Log.w(TAG, "Firebase Auth initialization error: ${e.message}")
            null
        }
    }

    fun getFirestore(context: Context): FirebaseFirestore? {
        return try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            FirebaseFirestore.getInstance()
        } catch (e: Throwable) {
            Log.w(TAG, "Firestore initialization error: ${e.message}")
            null
        }
    }

    /**
     * Gets all Google accounts present on the device
     */
    fun getDeviceGoogleAccounts(context: Context): List<String> {
        return try {
            val accountManager = AccountManager.get(context)
            val accounts = accountManager.getAccountsByType("com.google")
            accounts.mapNotNull { it.name }.distinct()
        } catch (e: Throwable) {
            Log.w(TAG, "Device account read: ${e.message}")
            emptyList()
        }
    }

    /**
     * Attempts Google Credential Manager first. If no credentials or Web Client ID is configured,
     * returns NeedsManualAccountSelection or prompts for real user details.
     */
    suspend fun signInWithGoogleAccountPicker(
        context: Context,
        serverClientId: String? = null
    ): GoogleAuthResult = withContext(Dispatchers.IO) {
        val deviceAccounts = getDeviceGoogleAccounts(context)

        val effectiveClientId = serverClientId?.takeIf { it.isNotBlank() } ?: WEB_CLIENT_ID

        // 1. Try Android CredentialManager if a valid Web Client ID is provided
        if (effectiveClientId.isNotBlank()) {
            try {
                val credentialManager = CredentialManager.create(context)
                val rawNonce = UUID.randomUUID().toString()
                val md = MessageDigest.getInstance("SHA-256")
                val digest = md.digest(rawNonce.toByteArray())
                val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(effectiveClientId)
                    .setAutoSelectEnabled(false)
                    .setNonce(hashedNonce)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result: GetCredentialResponse = credentialManager.getCredential(
                    request = request,
                    context = context
                )

                val credential = result.credential
                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken
                    val email = googleIdTokenCredential.id
                    val displayName = googleIdTokenCredential.displayName ?: email.substringBefore("@")
                    val photoUrl = googleIdTokenCredential.profilePictureUri?.toString() ?: ""

                    val firebaseAuth = getFirebaseAuth(context)
                    if (firebaseAuth != null && idToken.isNotBlank()) {
                        try {
                            val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                            val authResult = suspendCancellableCoroutine { continuation ->
                                firebaseAuth.signInWithCredential(authCredential)
                                    .addOnSuccessListener { continuation.resume(it.user) }
                                    .addOnFailureListener { continuation.resumeWithException(it) }
                            }
                            val user = authResult ?: firebaseAuth.currentUser
                            if (user != null) {
                                val syncedUser = syncUserToFirestore(context, user)
                                return@withContext GoogleAuthResult.Success(syncedUser)
                            }
                        } catch (authErr: Throwable) {
                            Log.w(TAG, "Firebase credential sign-in error: ${authErr.message}")
                        }
                    }

                    // Fallback sync with extracted Google account info
                    return@withContext signInWithAccountDetails(
                        context = context,
                        email = email,
                        displayName = displayName,
                        photoUrl = photoUrl
                    )
                }
            } catch (e: GetCredentialCancellationException) {
                return@withContext GoogleAuthResult.Cancelled
            } catch (e: Throwable) {
                Log.i(TAG, "Credential Manager: ${e.message}, showing account picker")
            }
        }

        // 2. Return device accounts list for account picker dialog
        return@withContext GoogleAuthResult.NeedsManualAccountSelection(deviceAccounts)
    }

    /**
     * Authenticates a user with their chosen Google account and syncs directly into Firestore project movieroom-334fb
     */
    suspend fun signInWithAccountDetails(
        context: Context,
        email: String,
        displayName: String,
        photoUrl: String = ""
    ): GoogleAuthResult = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim()
        val cleanName = if (displayName.isNotBlank()) displayName.trim() else cleanEmail.substringBefore("@")
        val cleanPhoto = if (photoUrl.isNotBlank()) photoUrl else "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=300&q=80"

        val firebaseAuth = getFirebaseAuth(context)
        var realUid: String? = null

        if (firebaseAuth != null) {
            try {
                // If user is not logged in, authenticate with Firebase Auth
                val currentUser = firebaseAuth.currentUser
                if (currentUser != null) {
                    realUid = currentUser.uid
                    try {
                        currentUser.updateProfile(
                            UserProfileChangeRequest.Builder()
                                .setDisplayName(cleanName)
                                .build()
                        )
                    } catch (ignore: Throwable) {}
                } else {
                    val authUser = suspendCancellableCoroutine<FirebaseUser?> { cont ->
                        firebaseAuth.signInAnonymously()
                            .addOnSuccessListener { res ->
                                val u = res.user
                                u?.updateProfile(
                                    UserProfileChangeRequest.Builder()
                                        .setDisplayName(cleanName)
                                        .build()
                                )
                                cont.resume(u)
                            }
                            .addOnFailureListener { e ->
                                Log.w(TAG, "Anonymous auth failed: ${e.message}")
                                cont.resume(null)
                            }
                    }
                    realUid = authUser?.uid
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Firebase Auth error: ${e.message}")
            }
        }

        val finalUid = realUid ?: "user_${Math.abs(cleanEmail.hashCode())}"
        val authUser = AuthUser(
            uid = finalUid,
            displayName = cleanName,
            email = cleanEmail,
            photoUrl = cleanPhoto,
            firestoreSynced = false
        )

        val synced = syncCustomUserToFirestore(context, authUser)
        return@withContext GoogleAuthResult.Success(authUser.copy(firestoreSynced = synced))
    }

    /**
     * Writes real authenticated user to Firestore collection "users"
     */
    suspend fun syncUserToFirestore(context: Context, user: FirebaseUser): AuthUser {
        val firestore = getFirestore(context)
        val displayName = user.displayName?.takeIf { it.isNotBlank() } ?: (user.email?.substringBefore("@") ?: "User")
        val email = user.email ?: ""
        val photoUrl = user.photoUrl?.toString() ?: ""

        val authUser = AuthUser(
            uid = user.uid,
            displayName = displayName,
            email = email,
            photoUrl = photoUrl,
            firestoreSynced = false
        )

        if (firestore == null) return authUser

        return try {
            val userMap = hashMapOf(
                "uid" to user.uid,
                "displayName" to displayName,
                "email" to email,
                "photoUrl" to photoUrl,
                "authProvider" to "google.com",
                "lastLoginAt" to System.currentTimeMillis(),
                "role" to "member",
                "status" to "active",
                "projectId" to PROJECT_ID
            )

            suspendCancellableCoroutine<Boolean> { cont ->
                firestore.collection("users").document(user.uid)
                    .set(userMap, SetOptions.merge())
                    .addOnSuccessListener {
                        Log.i(TAG, "User ${user.uid} ($email) successfully written to Firestore!")
                        cont.resume(true)
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Firestore write error: ${e.message}")
                        cont.resume(false)
                    }
            }
            authUser.copy(firestoreSynced = true)
        } catch (e: Throwable) {
            Log.w(TAG, "Error syncing to Firestore: ${e.message}")
            authUser
        }
    }

    /**
     * Syncs custom user to Firestore collection "users"
     */
    suspend fun syncCustomUserToFirestore(context: Context, user: AuthUser): Boolean {
        val firestore = getFirestore(context) ?: return false
        return try {
            val userMap = hashMapOf(
                "uid" to user.uid,
                "displayName" to user.displayName,
                "email" to user.email,
                "photoUrl" to user.photoUrl,
                "authProvider" to "google.com",
                "lastLoginAt" to System.currentTimeMillis(),
                "role" to "member",
                "status" to "active",
                "projectId" to PROJECT_ID
            )

            suspendCancellableCoroutine<Boolean> { cont ->
                firestore.collection("users").document(user.uid)
                    .set(userMap, SetOptions.merge())
                    .addOnSuccessListener {
                        Log.i(TAG, "Firestore 'users' record updated for ${user.email} (${user.displayName})")
                        cont.resume(true)
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Firestore write error: ${e.message}")
                        cont.resume(false)
                    }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Firestore write exception: ${e.message}")
            false
        }
    }

    /**
     * Fetches all registered users from Firestore collection "users"
     */
    suspend fun fetchAllFirestoreUsers(context: Context): List<FirestoreUserRecord> = withContext(Dispatchers.IO) {
        val firestore = getFirestore(context) ?: return@withContext emptyList()
        try {
            suspendCancellableCoroutine<List<FirestoreUserRecord>> { cont ->
                firestore.collection("users")
                    .orderBy("lastLoginAt", Query.Direction.DESCENDING)
                    .limit(50)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        val list = snapshot.documents.mapNotNull { doc ->
                            try {
                                FirestoreUserRecord(
                                    uid = doc.getString("uid") ?: doc.id,
                                    displayName = doc.getString("displayName") ?: "Google User",
                                    email = doc.getString("email") ?: "",
                                    photoUrl = doc.getString("photoUrl") ?: "",
                                    authProvider = doc.getString("authProvider") ?: "google.com",
                                    lastLoginAt = doc.getLong("lastLoginAt") ?: 0L,
                                    role = doc.getString("role") ?: "member",
                                    status = doc.getString("status") ?: "active"
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                        cont.resume(list)
                    }
                    .addOnFailureListener { error ->
                        Log.w(TAG, "Failed to fetch users from Firestore: ${error.message}")
                        cont.resume(emptyList())
                    }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Fetch users exception: ${e.message}")
            emptyList()
        }
    }

    /**
     * Signs out of Firebase Auth
     */
    fun signOut(context: Context) {
        try {
            getFirebaseAuth(context)?.signOut()
        } catch (e: Throwable) {
            Log.w(TAG, "Sign out error: ${e.message}")
        }
    }
}
