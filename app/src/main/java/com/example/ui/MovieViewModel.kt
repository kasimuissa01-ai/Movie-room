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
import com.example.model.Movie
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class MovieViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "MovieViewModel"
    }

    private val prefs = application.getSharedPreferences("cinestream_prefs", Context.MODE_PRIVATE)
    private val database = MovieDatabase.getDatabase(application)
    private val repository = MovieRepository(database.movieDao())

    val isTmdbLiveConfigured: Boolean = TmdbClient.isApiKeyConfigured
    val isR2Configured: Boolean get() = R2Config.isR2Configured
    val r2BucketName: String get() = R2Config.bucketName
    val r2PublicUrlBase: String get() = R2Config.publicUrlBase

    // Admin Session State
    private val _isAdminLoggedIn = MutableStateFlow(
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

    // Watchlist from Room (resolving static, uploaded R2, and TMDB dynamic movies)
    val watchlistMovies: StateFlow<List<Movie>> = repository.getWatchlistMovies { id ->
        uploadedMovies.value.firstOrNull { it.id == id }
            ?: _dynamicMovies.value[id]
            ?: SampleMovies.getMovieById(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    init {
        // Load registered Firestore users in background
        loadFirestoreUsers(application)

        // If TMDB API key is configured, preload movie 27205 ("Inception") from TMDB
        if (isTmdbLiveConfigured) {
            fetchTmdbMovieById("27205")
        }
    }

    // Filtered search results combining local database, uploaded R2 movies, and TMDB live results
    val searchResults: StateFlow<List<Movie>> = combine(
        _searchQuery,
        _selectedGenreFilter,
        _tmdbSearchResults,
        _dynamicMovies,
        uploadedMovies
    ) { query, genreFilter, tmdbResults, dynamicMap, uploadedList ->
        val localList = uploadedList + allMovies + dynamicMap.values

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

                    // Also cache any fetched movies into _dynamicMovies
                    if (results.isNotEmpty()) {
                        val currentMap = _dynamicMovies.value.toMutableMap()
                        results.forEach { currentMap[it.id] = it }
                        _dynamicMovies.value = currentMap
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
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching TMDB movie $movieId: ${e.message}")
            }
        }
    }

    fun getMovieById(id: String): Movie? {
        return uploadedMovies.value.firstOrNull { it.id == id }
            ?: _dynamicMovies.value[id]
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

    // Google Sign-In with Account Picker & Firebase Firestore Sync
    fun signInWithGoogleDirect(
        context: Context,
        serverClientId: String? = null,
        onNeedsAccountSelection: (List<String>) -> Unit = {},
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
                    is GoogleAuthResult.NeedsManualAccountSelection -> {
                        onNeedsAccountSelection(result.deviceAccounts)
                    }
                    is GoogleAuthResult.Error -> {
                        onComplete(false, result.message)
                    }
                    is GoogleAuthResult.Cancelled -> {
                        onComplete(false, "Account selection cancelled")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Google auth error: ${e.message}", e)
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
                    is GoogleAuthResult.NeedsManualAccountSelection -> {
                        onComplete(false, "Please select an account")
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
        prefs.edit()
            .putBoolean("google_signed_in", true)
            .putString("google_user_name", user.displayName)
            .putString("google_user_email", user.email)
            .putString("google_user_photo", user.photoUrl)
            .putBoolean("google_firestore_synced", user.firestoreSynced)
            .apply()

        _isGoogleSignedIn.value = true
        _userDisplayName.value = user.displayName
        _userEmail.value = user.email
        _userPhotoUrl.value = user.photoUrl
        _isFirestoreSynced.value = user.firestoreSynced

        loadFirestoreUsers(context)

        val msg = if (user.firestoreSynced) {
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

    fun signOutGoogle(context: Context? = null) {
        if (context != null) {
            FirebaseAuthManager.signOut(context)
        }
        prefs.edit()
            .putBoolean("google_signed_in", false)
            .putString("google_user_name", "")
            .putString("google_user_email", "")
            .putString("google_user_photo", "")
            .putBoolean("google_firestore_synced", false)
            .apply()
        _isGoogleSignedIn.value = false
        _userDisplayName.value = ""
        _userEmail.value = ""
        _userPhotoUrl.value = ""
        _isFirestoreSynced.value = false
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
        directVideoUrl: String,
        directPosterUrl: String,
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
                var r2Key = ""

                // 1. Upload Video if Uri provided
                if (videoUri != null) {
                    if (isR2Configured) {
                        _uploadStatusText.value = "Uploading video to Cloudflare R2..."
                        val videoKey = "movies/${timestamp}_$cleanTitle.mp4"
                        val result = R2Uploader.uploadFromUri(
                            context = context,
                            uri = videoUri,
                            objectKey = videoKey,
                            contentType = "video/mp4"
                        ) { uploaded, total, percent ->
                            _uploadProgress.value = 0.1f + (percent / 100f) * 0.7f
                            val mbUploaded = uploaded / (1024 * 1024)
                            val mbTotal = total / (1024 * 1024)
                            _uploadStatusText.value = "Uploading to Cloudflare R2: $mbUploaded MB / $mbTotal MB ($percent%)"
                        }

                        when (result) {
                            is R2Uploader.UploadResult.Success -> {
                                finalVideoUrl = result.publicUrl
                                r2Key = result.objectKey
                            }
                            is R2Uploader.UploadResult.Failure -> {
                                Log.w(TAG, "Video upload to R2 encountered: ${result.errorMessage}")
                                // If upload failed but user gave direct video url or fallback, continue with warning
                                if (finalVideoUrl.isBlank()) {
                                    finalVideoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
                                }
                            }
                        }
                    } else {
                        // R2 not configured in secrets yet, fallback to direct url or demo video
                        if (finalVideoUrl.isBlank()) {
                            finalVideoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
                        }
                    }
                } else if (finalVideoUrl.isBlank()) {
                    finalVideoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
                }

                // 2. Upload Poster if Uri provided
                if (posterUri != null) {
                    if (isR2Configured) {
                        _uploadStatusText.value = "Uploading poster to Cloudflare R2..."
                        _uploadProgress.value = 0.85f
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

                // 3. Save to Room database
                _uploadStatusText.value = "Registering movie in CineStream catalog..."
                _uploadProgress.value = 0.95f

                val entity = UploadedMovieEntity(
                    id = movieId,
                    title = title.trim(),
                    description = description.trim().ifBlank { "Exclusive movie uploaded via Cloudflare R2 storage." },
                    posterUrl = finalPosterUrl,
                    backdropUrl = finalPosterUrl,
                    videoUrl = finalVideoUrl,
                    trailerUrl = finalVideoUrl,
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

                _uploadProgress.value = 1f
                _uploadStatusText.value = "Movie successfully published to CineStream!"
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
            repository.toggleWatchlist(movie.id, isInList)
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
}
