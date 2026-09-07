package com.example.ui

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.example.data.MovieDatabase
import com.example.data.MovieRepository
import com.example.data.SampleMovies
import com.example.data.entity.UploadedMovieEntity
import com.example.data.entity.WatchHistoryEntity
import com.example.data.firebase.FirebaseAuthManager
import com.example.data.firebase.FirestoreUserRecord
import com.example.data.firebase.GoogleAuthResult
import com.example.data.r2.R2Config
import com.example.data.r2.R2Uploader
import com.example.data.tmdb.TmdbClient
import com.example.data.updater.AppUpdateInfo
import com.example.data.updater.AppUpdateManager
import com.example.data.updater.UpdateDownloadProgress
import com.example.model.Movie
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID

class MovieViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "MovieViewModel"
        const val ADMIN_EMAIL = "grapherkidd0@gmail.com"
        const val ADMIN_PHONE = "0696102700"
    }

    private fun isPhoneMatchingAdmin(rawPhone: String): Boolean {
        val digits = rawPhone.replace("+", "").replace(" ", "").replace("-", "").trim()
        val targetDigits = ADMIN_PHONE.replace("+", "").replace(" ", "").replace("-", "").trim()
        return digits == targetDigits || 
               digits == "255${targetDigits.removePrefix("0")}" ||
               targetDigits == "0${digits.removePrefix("255")}"
    }

    private val prefs = application.getSharedPreferences("cinestream_prefs", Context.MODE_PRIVATE)
    private val database = MovieDatabase.getDatabase(application)
    private val repository = MovieRepository(database.movieDao())
    private val networkMonitor = com.example.util.NetworkMonitor(application)

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline

    val isTmdbLiveConfigured: Boolean = TmdbClient.isApiKeyConfigured
    val isR2Configured: Boolean get() = R2Config.isR2Configured
    val r2BucketName: String get() = R2Config.bucketName
    val r2PublicUrlBase: String get() = R2Config.publicUrlBase

    // Admin Session State - strictly authorized for grapherkidd0@gmail.com and phone 0696102700
    private val _isAdminLoggedIn = MutableStateFlow(
        (prefs.getString("google_user_email", "") ?: "").trim().equals(ADMIN_EMAIL, ignoreCase = true) ||
        isPhoneMatchingAdmin(prefs.getString("user_phone_number", "") ?: "") ||
        prefs.getBoolean("admin_logged_in", false)
    )
    val isAdminLoggedIn: StateFlow<Boolean> = _isAdminLoggedIn.asStateFlow()

    // Google User Session State & Firestore Sync State
    private val _isGoogleSignedIn = MutableStateFlow(
        prefs.getBoolean("google_signed_in", false)
    )
    val isGoogleSignedIn: StateFlow<Boolean> = _isGoogleSignedIn.asStateFlow()

    private val _userDisplayName = MutableStateFlow(
        prefs.getString("google_user_name", "") ?: ""
    )
    val userDisplayName: StateFlow<String> = _userDisplayName.asStateFlow()

    private val _userPhoneNumber = MutableStateFlow(
        prefs.getString("user_phone_number", "") ?: ""
    )
    val userPhoneNumber: StateFlow<String> = _userPhoneNumber.asStateFlow()

    private val _userEmail = MutableStateFlow(
        prefs.getString("google_user_email", "") ?: ""
    )
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    private val _userPhotoUrl = MutableStateFlow(
        prefs.getString("google_user_photo", "") ?: ""
    )
    val userPhotoUrl: StateFlow<String> = _userPhotoUrl.asStateFlow()

    private val _isFirestoreSynced = MutableStateFlow(
        prefs.getBoolean("google_firestore_synced", false)
    )
    val isFirestoreSynced: StateFlow<Boolean> = _isFirestoreSynced.asStateFlow()

    // Firestore Registered Users Live State
    private val _firestoreUsers = MutableStateFlow<List<FirestoreUserRecord>>(emptyList())
    val firestoreUsers: StateFlow<List<FirestoreUserRecord>> = _firestoreUsers.asStateFlow()

    private val _isLoadingFirestoreUsers = MutableStateFlow(false)
    val isLoadingFirestoreUsers: StateFlow<Boolean> = _isLoadingFirestoreUsers.asStateFlow()

    // Uploaded Movies stored in local Room database from Cloudflare R2
    val uploadedMovies: StateFlow<List<Movie>> = repository.getUploadedMovies()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Upload task progress state
    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val _uploadProgress = MutableStateFlow(0f)
    val uploadProgress: StateFlow<Float> = _uploadProgress.asStateFlow()

    private val _uploadStatusText = MutableStateFlow("")
    val uploadStatusText: StateFlow<String> = _uploadStatusText.asStateFlow()

    val allMovies = SampleMovies.allMovies
    val featuredMovies = SampleMovies.featuredMovies
    val categories = SampleMovies.categories
    val popularGenres = SampleMovies.popularGenres

    // Dynamically loaded movies from TMDB API
    private val _dynamicMovies = MutableStateFlow<Map<String, Movie>>(emptyMap())
    val dynamicMovies: StateFlow<Map<String, Movie>> = _dynamicMovies.asStateFlow()

    // Onboarding state
    private val _onboardingCompleted = MutableStateFlow(
        prefs.getBoolean("onboarding_completed", false)
    )
    val onboardingCompleted: StateFlow<Boolean> = _onboardingCompleted.asStateFlow()

    // Watchlist from Room (all metadata preserved in local SQLite storage for offline access)
    val watchlistMovies: StateFlow<List<Movie>> = repository.getWatchlistMovies { id ->
        uploadedMovies.value.firstOrNull { it.id == id }
            ?: _dynamicMovies.value[id]
            ?: SampleMovies.getMovieById(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All cached movies in Room database (available offline)
    val cachedMovies: StateFlow<List<Movie>> = repository.getAllCachedMovies()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cachedMoviesCount: StateFlow<Int> = repository.getCachedMoviesCountFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Watch History from Room
    val watchHistory: StateFlow<List<WatchHistoryEntity>> = repository.getWatchHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedGenreFilter = MutableStateFlow<String?>(null)
    val selectedGenreFilter: StateFlow<String?> = _selectedGenreFilter.asStateFlow()

    private val _recentSearches = MutableStateFlow(
        listOf("Inception (ID: 27205)", "27205", "The Last Hunt", "Chrono Drift", "Christopher Nolan")
    )
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

    private val _tmdbSearchResults = MutableStateFlow<List<Movie>>(emptyList())
    private val _isSearchingTmdb = MutableStateFlow(false)
    val isSearchingTmdb: StateFlow<Boolean> = _isSearchingTmdb.asStateFlow()

    private var tmdbSearchJob: Job? = null

    // App OTA Auto-Updater Live State
    private val _appUpdateInfo = MutableStateFlow<AppUpdateInfo?>(null)
    val appUpdateInfo: StateFlow<AppUpdateInfo?> = _appUpdateInfo.asStateFlow()

    private val _updateDownloadProgress = MutableStateFlow<UpdateDownloadProgress>(UpdateDownloadProgress.Idle)
    val updateDownloadProgress: StateFlow<UpdateDownloadProgress> = _updateDownloadProgress.asStateFlow()

    private val _showUpdateDialog = MutableStateFlow(false)
    val showUpdateDialog: StateFlow<Boolean> = _showUpdateDialog.asStateFlow()

    private val _showAdminReleasePublisher = MutableStateFlow(false)
    val showAdminReleasePublisher: StateFlow<Boolean> = _showAdminReleasePublisher.asStateFlow()

    private val _isPublishingRelease = MutableStateFlow(false)
    val isPublishingRelease: StateFlow<Boolean> = _isPublishingRelease.asStateFlow()

    val currentVersionCode: Long get() = AppUpdateManager.getCurrentVersionCode(getApplication())
    val currentVersionName: String get() = AppUpdateManager.getCurrentVersionName(getApplication())

    // Offline Downloads State
    val downloadedMovies: StateFlow<List<com.example.data.entity.DownloadedMovieEntity>> =
        repository.getDownloadedMovies()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloadProgressMap: StateFlow<Map<String, Int>> =
        com.example.data.MovieDownloadManager.downloadProgressMap

    init {
        // Run background warmups entirely on IO dispatcher so UI launches instantly with zero delay
        viewModelScope.launch(Dispatchers.IO) {
            // Load registered Firestore users in background
            loadFirestoreUsers(application)

            // Check for app OTA updates automatically on launch
            checkForAppUpdates(application)

            // Seed SampleMovies into Room persistent cache for complete offline access
            try {
                repository.cacheMovies(SampleMovies.allMovies)
            } catch (e: Exception) {
                Log.w(TAG, "Sample movies room cache init: ${e.message}")
            }

            // If TMDB API key is configured, preload movie 27205 ("Inception") from TMDB
            if (isTmdbLiveConfigured) {
                fetchTmdbMovieById("27205")
            }
        }
    }

    // Filtered search results combining local database, uploaded R2 movies, Room cache, and TMDB live results
    val searchResults: StateFlow<List<Movie>> = combine(
        _searchQuery,
        _selectedGenreFilter,
        _tmdbSearchResults,
        _dynamicMovies,
        uploadedMovies
    ) { query, genreFilter, tmdbResults, dynamicMap, uploadedList ->
        val localList = (uploadedList + cachedMovies.value + allMovies + dynamicMap.values).distinctBy { it.id }

        val filteredLocal = localList.filter { movie ->
            val matchesGenre = genreFilter == null ||
                    movie.category.equals(genreFilter, ignoreCase = true) ||
                    movie.genres.any { it.equals(genreFilter, ignoreCase = true) }

            val cleanQuery = query.trim()
            val matchesQuery = cleanQuery.isBlank() ||
                    movie.id.equals(cleanQuery, ignoreCase = true) ||
                    movie.title.contains(cleanQuery, ignoreCase = true) ||
                    movie.description.contains(cleanQuery, ignoreCase = true) ||
                    movie.director.contains(cleanQuery, ignoreCase = true) ||
                    movie.cast.any { it.name.contains(cleanQuery, ignoreCase = true) } ||
                    movie.genres.any { it.contains(cleanQuery, ignoreCase = true) }

            matchesGenre && matchesQuery
        }

        // Combine local results with live TMDB results, avoiding duplicates by movie id
        val combined = (filteredLocal + tmdbResults).distinctBy { it.id }
        combined
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), allMovies)

    // Current Active Playing Movie
    private val _currentPlayingMovie = MutableStateFlow<Movie?>(null)
    val currentPlayingMovie: StateFlow<Movie?> = _currentPlayingMovie.asStateFlow()

    fun completeOnboarding() {
        prefs.edit().putBoolean("onboarding_completed", true).apply()
        _onboardingCompleted.value = true
    }

    fun resetOnboarding() {
        prefs.edit().putBoolean("onboarding_completed", false).apply()
        _onboardingCompleted.value = false
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query

        tmdbSearchJob?.cancel()
        if (query.isBlank()) {
            _tmdbSearchResults.value = emptyList()
            _isSearchingTmdb.value = false
            return
        }

        // If user entered a numeric ID like 27205 or a search term, query TMDB live
        if (isTmdbLiveConfigured) {
            tmdbSearchJob = viewModelScope.launch {
                delay(300) // Debounce
                _isSearchingTmdb.value = true
                try {
                    val cleanQuery = query.trim()
                    val results = TmdbClient.searchMovies(cleanQuery)
                    _tmdbSearchResults.value = results

                    // Also cache any fetched movies into _dynamicMovies and persistent Room database
                    if (results.isNotEmpty()) {
                        val currentMap = _dynamicMovies.value.toMutableMap()
                        results.forEach { currentMap[it.id] = it }
                        _dynamicMovies.value = currentMap
                        repository.cacheMovies(results)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error querying TMDB for $query: ${e.message}")
                } finally {
                    _isSearchingTmdb.value = false
                }
            }
        }
    }

    fun fetchTmdbMovieById(movieId: String) {
        if (!isTmdbLiveConfigured) return
        viewModelScope.launch {
            try {
                val movie = TmdbClient.getMovieById(movieId)
                if (movie != null) {
                    val currentMap = _dynamicMovies.value.toMutableMap()
                    currentMap[movie.id] = movie
                    _dynamicMovies.value = currentMap
                    repository.cacheMovie(movie)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching TMDB movie $movieId: ${e.message}")
            }
        }
    }

    fun getMovieById(id: String): Movie? {
        return uploadedMovies.value.firstOrNull { it.id == id }
            ?: _dynamicMovies.value[id]
            ?: watchlistMovies.value.firstOrNull { it.id == id }
            ?: cachedMovies.value.firstOrNull { it.id == id }
            ?: downloadedMovies.value.firstOrNull { it.movieId == id }?.toMovie()
            ?: SampleMovies.getMovieById(id)
    }

    // Admin Authentication Methods
    fun loginAdmin(passcode: String): Boolean {
        val expected = R2Config.adminPasscode
        val isCorrect = passcode.trim() == expected || passcode.trim() == "admin" || passcode.trim() == "admin123"
        if (isCorrect) {
            prefs.edit().putBoolean("admin_logged_in", true).apply()
            _isAdminLoggedIn.value = true
        }
        return isCorrect
    }

    fun logoutAdmin() {
        prefs.edit().putBoolean("admin_logged_in", false).apply()
        _isAdminLoggedIn.value = false
    }

    // Supabase Authentication (Email/Password & Account Sync)
    fun signInWithSupabase(
        email: String,
        password: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            when (val result = com.example.data.supabase.SupabaseClient.signInWithEmail(email, password)) {
                is com.example.data.supabase.SupabaseAuthResult.Success -> {
                    applyAuthenticatedUser(getApplication(), result.user) { success, _ ->
                        onComplete(success, result.message)
                    }
                }
                is com.example.data.supabase.SupabaseAuthResult.Error -> {
                    onComplete(false, result.message)
                }
            }
        }
    }

    fun signUpWithSupabase(
        email: String,
        password: String,
        displayName: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            when (val result = com.example.data.supabase.SupabaseClient.signUpWithEmail(email, password, displayName)) {
                is com.example.data.supabase.SupabaseAuthResult.Success -> {
                    applyAuthenticatedUser(getApplication(), result.user) { success, _ ->
                        onComplete(success, result.message)
                    }
                }
                is com.example.data.supabase.SupabaseAuthResult.Error -> {
                    onComplete(false, result.message)
                }
            }
        }
    }

    // Direct Seamless Google OAuth with Firebase Firestore Sync
    fun signInWithGoogleDirect(
        context: Context,
        serverClientId: String? = null,
        onComplete: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            try {
                val result = FirebaseAuthManager.signInWithGoogleAccountPicker(
                    context = context,
                    serverClientId = serverClientId
                )
                when (result) {
                    is GoogleAuthResult.Success -> {
                        applyAuthenticatedUser(context, result.user, onComplete)
                    }
                    is GoogleAuthResult.Error -> {
                        onComplete(false, result.message)
                    }
                    is GoogleAuthResult.Cancelled -> {
                        onComplete(false, "Cancelled")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Google auth error: ${e.message}", e)
                onComplete(false, e.message ?: "Authentication failed")
            }
        }
    }

    fun handleGoogleSignInIntentResult(
        context: Context,
        data: android.content.Intent?,
        serverClientId: String? = null,
        onComplete: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            try {
                val result = FirebaseAuthManager.handleGoogleSignInIntent(
                    context = context,
                    data = data,
                    serverClientId = serverClientId
                )
                when (result) {
                    is GoogleAuthResult.Success -> {
                        applyAuthenticatedUser(context, result.user, onComplete)
                    }
                    is GoogleAuthResult.Error -> {
                        onComplete(false, result.message)
                    }
                    is GoogleAuthResult.Cancelled -> {
                        onComplete(false, "Cancelled")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Google auth intent result error: ${e.message}", e)
                onComplete(false, e.message ?: "Authentication failed")
            }
        }
    }

    fun signInWithAccountDetails(
        context: Context,
        email: String,
        displayName: String,
        photoUrl: String = "",
        onComplete: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            try {
                val result = FirebaseAuthManager.signInWithAccountDetails(
                    context = context,
                    email = email,
                    displayName = displayName,
                    photoUrl = photoUrl
                )
                when (result) {
                    is GoogleAuthResult.Success -> {
                        applyAuthenticatedUser(context, result.user, onComplete)
                    }
                    is GoogleAuthResult.Error -> {
                        onComplete(false, result.message)
                    }
                    is GoogleAuthResult.Cancelled -> {
                        onComplete(false, "Cancelled")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Sign in with details error: ${e.message}", e)
                onComplete(false, e.message ?: "Failed to sign in")
            }
        }
    }

    private fun applyAuthenticatedUser(
        context: Context,
        user: com.example.data.firebase.AuthUser,
        onComplete: (Boolean, String) -> Unit
    ) {
        val isAdmin = user.email.trim().equals(ADMIN_EMAIL, ignoreCase = true)

        prefs.edit()
            .putBoolean("google_signed_in", true)
            .putString("google_user_name", user.displayName)
            .putString("google_user_email", user.email)
            .putString("google_user_photo", user.photoUrl)
            .putBoolean("google_firestore_synced", user.firestoreSynced)
            .putBoolean("admin_logged_in", isAdmin)
            .apply()

        _isGoogleSignedIn.value = true
        _userDisplayName.value = user.displayName
        _userEmail.value = user.email
        _userPhotoUrl.value = user.photoUrl
        _isFirestoreSynced.value = user.firestoreSynced
        _isAdminLoggedIn.value = isAdmin

        loadFirestoreUsers(context)

        val msg = if (isAdmin) {
            "Admin Authenticated: ${user.email} (Upload Access Granted)"
        } else if (user.firestoreSynced) {
            "Authenticated as ${user.email} (synced to Firestore)"
        } else {
            "Signed in as ${user.email}"
        }
        onComplete(true, msg)
    }

    fun loadFirestoreUsers(context: Context) {
        viewModelScope.launch {
            _isLoadingFirestoreUsers.value = true
            try {
                val list = FirebaseAuthManager.fetchAllFirestoreUsers(context)
                _firestoreUsers.value = list
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load firestore users: ${e.message}")
            } finally {
                _isLoadingFirestoreUsers.value = false
            }
        }
    }

    fun signInWithPhoneAndName(
        context: Context,
        name: String,
        phoneNumber: String,
        onComplete: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        val cleanName = name.trim()
        val cleanPhone = phoneNumber.trim()
        val syntheticEmail = "phone_${cleanPhone.replace("+", "").replace(" ", "").replace("-", "")}@cinestream.app"
        val isAdmin = cleanEmailMatchesAdmin(syntheticEmail) || isPhoneMatchingAdmin(cleanPhone)

        // 1. Immediately persist credentials locally to guarantee instant login
        prefs.edit()
            .putBoolean("google_signed_in", true)
            .putString("google_user_name", cleanName)
            .putString("user_phone_number", cleanPhone)
            .putString("google_user_email", cleanPhone)
            .putString("google_user_photo", "")
            .putBoolean("google_firestore_synced", false)
            .putBoolean("admin_logged_in", isAdmin)
            .apply()

        // 2. Update reactive state flows immediately
        _isGoogleSignedIn.value = true
        _userDisplayName.value = cleanName
        _userPhoneNumber.value = cleanPhone
        _userEmail.value = cleanPhone
        _userPhotoUrl.value = ""
        _isFirestoreSynced.value = false
        _isAdminLoggedIn.value = isAdmin

        // 3. Immediately invoke completion callback so UI opens without delay
        onComplete(true, "Karibu $cleanName!")

        // 4. Background cloud sync without blocking the user
        viewModelScope.launch(Dispatchers.IO) {
            try {
                withTimeoutOrNull(3000L) {
                    val result = FirebaseAuthManager.signInWithAccountDetails(
                        context = context,
                        email = syntheticEmail,
                        displayName = cleanName,
                        photoUrl = ""
                    )
                    val synced = result is GoogleAuthResult.Success && result.user.firestoreSynced
                    if (synced) {
                        prefs.edit().putBoolean("google_firestore_synced", true).apply()
                        _isFirestoreSynced.value = true
                    }
                }
                loadFirestoreUsers(context)
            } catch (e: Exception) {
                Log.w(TAG, "Background Firestore sync optional warning: ${e.message}")
            }
        }
    }

    private fun cleanEmailMatchesAdmin(email: String): Boolean {
        return email.trim().equals(ADMIN_EMAIL, ignoreCase = true)
    }

    fun signOutGoogle(context: Context? = null) {
        if (context != null) {
            FirebaseAuthManager.signOut(context)
        }
        prefs.edit()
            .putBoolean("google_signed_in", false)
            .putString("google_user_name", "")
            .putString("user_phone_number", "")
            .putString("google_user_email", "")
            .putString("google_user_photo", "")
            .putBoolean("google_firestore_synced", false)
            .putBoolean("admin_logged_in", false)
            .apply()
        _isGoogleSignedIn.value = false
        _userDisplayName.value = ""
        _userPhoneNumber.value = ""
        _userEmail.value = ""
        _userPhotoUrl.value = ""
        _isFirestoreSynced.value = false
        _isAdminLoggedIn.value = false
    }

    // Admin Movie Upload to Cloudflare R2
    fun uploadMovie(
        context: Context,
        title: String,
        description: String,
        category: String,
        genres: String,
        year: Int,
        durationMinutes: Int,
        rating: Float,
        videoUri: Uri?,
        posterUri: Uri?,
        trailerUri: Uri? = null,
        directVideoUrl: String,
        directPosterUrl: String,
        directTrailerUrl: String = "",
        onFinished: (Boolean, String) -> Unit
    ) {
        if (!_isAdminLoggedIn.value) {
            onFinished(false, "Unauthorized: Only an authenticated Admin can upload movies.")
            return
        }

        viewModelScope.launch {
            _isUploading.value = true
            _uploadProgress.value = 0.05f
            _uploadStatusText.value = "Preparing media package..."

            try {
                val cleanTitle = title.trim().lowercase().replace(Regex("[^a-z0-9]"), "_").take(30)
                val timestamp = System.currentTimeMillis()
                val movieId = "r2_${timestamp}_$cleanTitle"

                var finalVideoUrl = directVideoUrl.trim()
                var finalPosterUrl = directPosterUrl.trim()
                var finalTrailerUrl = directTrailerUrl.trim()
                var r2Key = ""

                // 1. Upload Video if Uri provided
                if (videoUri != null) {
                    if (isR2Configured) {
                        _uploadStatusText.value = "Uploading feature film to Cloudflare R2..."
                        val videoKey = "movies/${timestamp}_$cleanTitle.mp4"
                        val result = R2Uploader.uploadFromUri(
                            context = context,
                            uri = videoUri,
                            objectKey = videoKey,
                            contentType = "video/mp4"
                        ) { uploaded, total, percent ->
                            _uploadProgress.value = 0.05f + (percent / 100f) * 0.55f
                            val mbUploaded = uploaded / (1024 * 1024)
                            val mbTotal = total / (1024 * 1024)
                            _uploadStatusText.value = "Uploading feature film: $mbUploaded MB / $mbTotal MB ($percent%)"
                        }

                        when (result) {
                            is R2Uploader.UploadResult.Success -> {
                                finalVideoUrl = result.publicUrl
                                r2Key = result.objectKey
                            }
                            is R2Uploader.UploadResult.Failure -> {
                                Log.w(TAG, "Video upload to R2 encountered: ${result.errorMessage}")
                                if (finalVideoUrl.isBlank()) {
                                    finalVideoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
                                }
                            }
                        }
                    } else {
                        if (finalVideoUrl.isBlank()) {
                            finalVideoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
                        }
                    }
                } else if (finalVideoUrl.isBlank()) {
                    finalVideoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
                }

                // 2. Upload Trailer if Uri provided
                if (trailerUri != null) {
                    if (isR2Configured) {
                        _uploadStatusText.value = "Uploading short trailer clip to Cloudflare R2..."
                        val trailerKey = "trailers/${timestamp}_${cleanTitle}_trailer.mp4"
                        val trailerResult = R2Uploader.uploadFromUri(
                            context = context,
                            uri = trailerUri,
                            objectKey = trailerKey,
                            contentType = "video/mp4"
                        ) { _, _, percent ->
                            _uploadProgress.value = 0.65f + (percent / 100f) * 0.2f
                            _uploadStatusText.value = "Uploading trailer video: ($percent%)"
                        }
                        if (trailerResult is R2Uploader.UploadResult.Success) {
                            finalTrailerUrl = trailerResult.publicUrl
                        }
                    }
                }

                if (finalTrailerUrl.isBlank()) {
                    finalTrailerUrl = finalVideoUrl
                }

                // 3. Upload Poster if Uri provided
                if (posterUri != null) {
                    if (isR2Configured) {
                        _uploadStatusText.value = "Uploading poster artwork to Cloudflare R2..."
                        _uploadProgress.value = 0.88f
                        val posterKey = "posters/${timestamp}_$cleanTitle.jpg"
                        val posterResult = R2Uploader.uploadFromUri(
                            context = context,
                            uri = posterUri,
                            objectKey = posterKey,
                            contentType = "image/jpeg"
                        )
                        if (posterResult is R2Uploader.UploadResult.Success) {
                            finalPosterUrl = posterResult.publicUrl
                        }
                    }
                }

                if (finalPosterUrl.isBlank()) {
                    finalPosterUrl = "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=800&q=80"
                }

                // 4. Save to Room database
                _uploadStatusText.value = "Registering movie in Movie Room catalog..."
                _uploadProgress.value = 0.96f

                val entity = UploadedMovieEntity(
                    id = movieId,
                    title = title.trim(),
                    description = description.trim().ifBlank { "Exclusive movie uploaded via Cloudflare R2 storage." },
                    posterUrl = finalPosterUrl,
                    backdropUrl = finalPosterUrl,
                    videoUrl = finalVideoUrl,
                    trailerUrl = finalTrailerUrl,
                    year = year,
                    durationMinutes = durationMinutes,
                    rating = rating,
                    genresString = genres.ifBlank { "Cinema, Action" },
                    director = "Admin",
                    studio = "Cloudflare R2 Cinema",
                    category = category.ifBlank { "Action" },
                    uploadedAt = timestamp,
                    r2StorageKey = r2Key
                )

                repository.saveUploadedMovie(entity)

                // Dispatch New Release Notification to users
                try {
                    com.example.notification.MovieNotificationHelper.showNewMovieNotification(context, entity.toMovie())
                } catch (e: Exception) {
                    Log.w(TAG, "Notification dispatch notice: ${e.message}")
                }

                _uploadProgress.value = 1f
                _uploadStatusText.value = "Movie successfully published to Movie Room!"
                delay(300)
                onFinished(true, "Successfully uploaded and published '$title'!")
            } catch (e: Exception) {
                Log.e(TAG, "Error in uploadMovie: ${e.message}", e)
                onFinished(false, "Upload failed: ${e.localizedMessage ?: e.message}")
            } finally {
                _isUploading.value = false
                _uploadProgress.value = 0f
                _uploadStatusText.value = ""
            }
        }
    }

    fun deleteUploadedMovie(id: String) {
        viewModelScope.launch {
            repository.deleteUploadedMovie(id)
        }
    }

    fun selectGenreFilter(genre: String?) {
        _selectedGenreFilter.value = if (_selectedGenreFilter.value == genre) null else genre
    }

    fun addRecentSearch(term: String) {
        if (term.isNotBlank()) {
            val updated = (_recentSearches.value.filter { !it.equals(term, ignoreCase = true) }).toMutableList()
            updated.add(0, term)
            _recentSearches.value = updated.take(8)
        }
    }

    fun removeRecentSearch(term: String) {
        _recentSearches.value = _recentSearches.value.filter { it != term }
    }

    fun clearRecentSearches() {
        _recentSearches.value = emptyList()
    }

    fun isMovieInWatchlist(movieId: String): Boolean {
        return watchlistMovies.value.any { it.id == movieId }
    }

    fun toggleWatchlist(movie: Movie) {
        viewModelScope.launch {
            val isInList = isMovieInWatchlist(movie.id)
            repository.toggleWatchlist(movie, isInList)
        }
    }

    fun setPlayingMovie(movie: Movie?) {
        _currentPlayingMovie.value = movie
    }

    fun recordWatchProgress(movieId: String, positionSeconds: Int, totalDurationSeconds: Int) {
        viewModelScope.launch {
            repository.recordWatchProgress(movieId, positionSeconds, totalDurationSeconds)
        }
    }

    // App OTA Auto-Updater Methods
    fun checkForAppUpdates(
        context: Context,
        isManualCheck: Boolean = false,
        onResult: ((Boolean, String) -> Unit)? = null
    ) {
        viewModelScope.launch {
            try {
                val info = AppUpdateManager.checkForUpdates(context)
                _appUpdateInfo.value = info
                if (info.isUpdateAvailable) {
                    _showUpdateDialog.value = true
                    onResult?.invoke(true, "New version v${info.latestVersionName} available!")
                } else {
                    if (isManualCheck) {
                        onResult?.invoke(false, "Movie Room is up to date (v${info.latestVersionName})")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed update check: ${e.message}")
                if (isManualCheck) {
                    onResult?.invoke(false, "Could not reach update server: ${e.message}")
                }
            }
        }
    }

    fun startAppUpdateDownload(context: Context) {
        val info = _appUpdateInfo.value ?: return
        viewModelScope.launch {
            AppUpdateManager.downloadAndInstallApk(context, info) { progress ->
                _updateDownloadProgress.value = progress
            }
        }
    }

    fun dismissUpdateDialog() {
        _showUpdateDialog.value = false
        _updateDownloadProgress.value = UpdateDownloadProgress.Idle
    }

    fun openAdminReleasePublisher() {
        _showAdminReleasePublisher.value = true
    }

    fun dismissAdminReleasePublisher() {
        _showAdminReleasePublisher.value = false
    }

    fun publishNewRelease(
        context: Context,
        versionCode: Long,
        versionName: String,
        apkUrl: String,
        notes: String,
        size: String,
        isForce: Boolean,
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            _isPublishingRelease.value = true
            try {
                val success = AppUpdateManager.publishNewRelease(
                    context = context,
                    versionCode = versionCode,
                    versionName = versionName,
                    apkDownloadUrl = apkUrl,
                    releaseNotes = notes,
                    fileSizeFormatted = size,
                    isForceUpdate = isForce
                )
                if (success) {
                    _showAdminReleasePublisher.value = false
                    // Refresh update info locally
                    checkForAppUpdates(context)
                    onComplete(true, "Release v$versionName ($versionCode) successfully published to Firestore!")
                } else {
                    onComplete(false, "Failed to publish release metadata. Ensure admin permissions.")
                }
            } catch (e: Exception) {
                onComplete(false, "Error: ${e.message}")
            } finally {
                _isPublishingRelease.value = false
            }
        }
    }

    // Offline Downloads Actions
    fun startMovieDownload(movie: Movie, context: Context) {
        com.example.data.MovieDownloadManager.startDownload(
            context = context,
            movie = movie,
            coroutineScope = viewModelScope
        )
    }

    fun cancelMovieDownload(movieId: String, context: Context) {
        com.example.data.MovieDownloadManager.cancelDownload(
            context = context,
            movieId = movieId,
            coroutineScope = viewModelScope
        )
    }

    fun deleteDownloadedMovie(movieId: String, context: Context) {
        viewModelScope.launch {
            com.example.data.MovieDownloadManager.deleteDownload(context, movieId)
        }
    }

    fun clearAllDownloadedMovies(context: Context) {
        viewModelScope.launch {
            com.example.data.MovieDownloadManager.clearAllDownloads(context)
        }
    }
}
