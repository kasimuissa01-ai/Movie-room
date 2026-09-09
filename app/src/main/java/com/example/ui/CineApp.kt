package com.example.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.SampleMovies
import com.example.model.Movie
import com.example.ui.components.AdminEditMovieDialog
import com.example.ui.components.AdminLoginDialog
import com.example.ui.components.AdminReleasePublisherDialog
import com.example.ui.components.AdminUploadMovieDialog
import com.example.ui.components.AppUpdateDialog
import com.example.ui.components.BackgroundUpdateBanner
import com.example.ui.components.DownloadQualityDialog
import com.example.ui.navigation.CineBottomBar
import com.example.ui.navigation.CineNavTab
import com.example.ui.screens.CategoryDetailScreen
import com.example.ui.screens.DownloadsScreen
import com.example.ui.screens.GoogleAuthScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MovieDetailsScreen
import com.example.ui.screens.MyListScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.VideoPlayerScreen
import com.example.ui.theme.CineBlack
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun CineApp(
    viewModel: MovieViewModel = viewModel(),
    notificationPayload: com.example.NotificationPayload? = null,
    onClearNotificationPayload: () -> Unit = {}
) {
    val navController = rememberNavController()
    val onboardingCompleted by viewModel.onboardingCompleted.collectAsState()
    val watchlistMovies by viewModel.watchlistMovies.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedGenreFilter by viewModel.selectedGenreFilter.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val searchErrorMessage by viewModel.searchErrorMessage.collectAsState()
    val homeFeedState by viewModel.homeFeedState.collectAsState()
    val movieDetailsMap by viewModel.movieDetailsMap.collectAsState()

    val isAdminLoggedIn by viewModel.isAdminLoggedIn.collectAsState()
    val uploadedMovies by viewModel.uploadedMovies.collectAsState()

    val isGoogleSignedIn by viewModel.isGoogleSignedIn.collectAsState()
    val isFirestoreSynced by viewModel.isFirestoreSynced.collectAsState()
    val userDisplayName by viewModel.userDisplayName.collectAsState()
    val userEmail by viewModel.userEmail.collectAsState()
    val userPhotoUrl by viewModel.userPhotoUrl.collectAsState()
    val firestoreUsers by viewModel.firestoreUsers.collectAsState()
    val isLoadingFirestoreUsers by viewModel.isLoadingFirestoreUsers.collectAsState()

    // Offline Downloads State
    val downloadedMovies by viewModel.downloadedMovies.collectAsState()
    val downloadProgressMap by viewModel.downloadProgressMap.collectAsState()
    val pendingDownloadMovie by viewModel.pendingDownloadMovie.collectAsState()

    // Network & Room Offline Cache State
    val isOnline by viewModel.isOnline.collectAsState()
    val cachedMoviesCount by viewModel.cachedMoviesCount.collectAsState()
    val cachedMovies by viewModel.cachedMovies.collectAsState()

    // OTA Auto-Update State
    val appUpdateInfo by viewModel.appUpdateInfo.collectAsState()
    val updateDownloadProgress by viewModel.updateDownloadProgress.collectAsState()
    val showUpdateDialog by viewModel.showUpdateDialog.collectAsState()
    val showAdminReleasePublisher by viewModel.showAdminReleasePublisher.collectAsState()
    val isPublishingRelease by viewModel.isPublishingRelease.collectAsState()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showAdminLoginDialog by remember { mutableStateOf(false) }
    var showAdminUploadDialog by remember { mutableStateOf(false) }
    var movieToEdit by remember { mutableStateOf<Movie?>(null) }

    var currentTab by remember { mutableStateOf(CineNavTab.HOME) }

    // Respond to Push Notification Deep Links (tap on notification or notification actions)
    LaunchedEffect(notificationPayload) {
        val payload = notificationPayload ?: return@LaunchedEffect
        val movieId = payload.movieId
        val action = payload.action

        if (!movieId.isNullOrBlank()) {
            when (action) {
                com.example.notification.MovieNotificationHelper.ACTION_PLAY -> {
                    navController.navigate("player/$movieId/false")
                }
                else -> {
                    navController.navigate("details/$movieId")
                }
            }
        } else if (action == com.example.notification.MovieNotificationHelper.ACTION_EXPLORE) {
            currentTab = CineNavTab.HOME
        }
        onClearNotificationPayload()
    }

    val startDestination = rememberSaveable {
        when {
            !viewModel.onboardingCompleted.value -> "onboarding"
            !viewModel.isGoogleSignedIn.value -> "auth"
            else -> "main"
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { fadeIn(animationSpec = tween(300)) },
        exitTransition = { fadeOut(animationSpec = tween(300)) }
    ) {
        // Onboarding Screen
        composable("onboarding") {
            OnboardingScreen(
                onFinished = {
                    viewModel.completeOnboarding()
                    navController.navigate("auth") {
                        popUpTo("onboarding") { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        // Google Authentication Welcome Screen
        composable("auth") {
            GoogleAuthScreen(
                viewModel = viewModel,
                onAuthSuccess = {
                    navController.navigate("main") {
                        popUpTo("auth") { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onSkipGuest = {
                    navController.navigate("main") {
                        popUpTo("auth") { inclusive = true }
                        launchSingleTop = true
                    }
                },
                canDismiss = navController.previousBackStackEntry != null
            )
        }

        // Main App with Bottom Navigation
        composable("main") {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    CineBottomBar(
                        currentTab = currentTab,
                        onTabSelected = { tab -> currentTab = tab },
                        watchListCount = watchlistMovies.size,
                        downloadCount = downloadedMovies.size
                    )
                },
                containerColor = CineBlack,
                contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = innerPadding.calculateBottomPadding())
                ) {
                    when (currentTab) {
                        CineNavTab.HOME -> {
                            HomeScreen(
                                homeFeedState = homeFeedState,
                                onRefresh = { viewModel.loadHomeFeeds() },
                                onMovieClick = { movie ->
                                    navController.navigate("details/${movie.id}")
                                },
                                onWatchClick = { movie ->
                                    navController.navigate("player/${movie.id}/false")
                                },
                                onDownloadClick = { movie ->
                                    viewModel.startMovieDownload(movie, context)
                                    coroutineScope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = "Saving \"${movie.title}\" to private in-app storage (not in phone gallery).",
                                            actionLabel = "View",
                                            duration = SnackbarDuration.Short
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            currentTab = CineNavTab.DOWNLOADS
                                        }
                                    }
                                },
                                downloadedMovieIds = downloadedMovies.filter { it.downloadStatus == "COMPLETED" }.map { it.movieId }.toSet(),
                                downloadProgressMap = downloadProgressMap,
                                onSeeAllClick = { categoryKey, categoryTitle ->
                                    val encodedKey = URLEncoder.encode(categoryKey, StandardCharsets.UTF_8.toString())
                                    val encodedTitle = URLEncoder.encode(categoryTitle, StandardCharsets.UTF_8.toString())
                                    navController.navigate("category/$encodedKey/$encodedTitle")
                                },
                                onSearchClick = {
                                    navController.navigate("search")
                                },
                                onProfileClick = {
                                    currentTab = CineNavTab.PROFILE
                                },
                                isAdminLoggedIn = isAdminLoggedIn,
                                uploadedMovies = uploadedMovies,
                                onUploadClick = {
                                    showAdminUploadDialog = true
                                }
                            )
                        }

                        CineNavTab.DOWNLOADS -> {
                            DownloadsScreen(
                                downloadedMovies = downloadedMovies,
                                downloadProgressMap = downloadProgressMap,
                                onWatchMovie = { movie ->
                                    navController.navigate("player/${movie.id}/false")
                                },
                                onDeleteDownload = { movieId ->
                                    viewModel.deleteDownloadedMovie(movieId, context)
                                },
                                onCancelDownload = { movieId ->
                                    viewModel.cancelMovieDownload(movieId, context)
                                },
                                onExploreMoviesClick = {
                                    currentTab = CineNavTab.HOME
                                },
                                onSearchClick = {
                                    navController.navigate("search")
                                }
                            )
                        }

                        CineNavTab.MY_LIST -> {
                            MyListScreen(
                                movies = watchlistMovies,
                                onMovieClick = { movie ->
                                    navController.navigate("details/${movie.id}")
                                },
                                onDiscoverClick = {
                                    currentTab = CineNavTab.HOME
                                },
                                isOnline = isOnline,
                                cachedMoviesCount = cachedMoviesCount
                            )
                        }

                        CineNavTab.PROFILE -> {
                            ProfileScreen(
                                onReplayOnboarding = {
                                    viewModel.resetOnboarding()
                                    navController.navigate("onboarding")
                                },
                                isTmdbLive = true,
                                isAdminLoggedIn = isAdminLoggedIn,
                                uploadedMoviesCount = uploadedMovies.size,
                                onAdminLoginClick = {
                                    showAdminLoginDialog = true
                                },
                                onAdminLogoutClick = {
                                    viewModel.logoutAdmin()
                                },
                                onUploadMovieClick = {
                                    showAdminUploadDialog = true
                                },
                                onOpenAdminReleasePublisher = {
                                    viewModel.openAdminReleasePublisher()
                                },
                                onCheckForUpdates = {
                                    viewModel.checkForAppUpdates(context, isManualCheck = true) { isNewAvailable, message ->
                                        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                },
                                currentVersionName = viewModel.currentVersionName,
                                currentVersionCode = viewModel.currentVersionCode,
                                isGoogleSignedIn = isGoogleSignedIn,
                                isFirestoreSynced = isFirestoreSynced,
                                userDisplayName = userDisplayName,
                                userEmail = userEmail,
                                userPhotoUrl = userPhotoUrl,
                                firestoreUsers = firestoreUsers,
                                isLoadingFirestoreUsers = isLoadingFirestoreUsers,
                                onRefreshFirestoreUsers = {
                                    viewModel.loadFirestoreUsers(context)
                                },
                                onGoogleAuthClick = {
                                    navController.navigate("auth")
                                },
                                onSignOutGoogleClick = {
                                    viewModel.signOutGoogle()
                                }
                            )
                        }
                    }

                    // Admin Authentication Dialog
                    if (showAdminLoginDialog) {
                        AdminLoginDialog(
                            onDismiss = { showAdminLoginDialog = false },
                            onLogin = { passcode ->
                                viewModel.loginAdmin(passcode)
                            }
                        )
                    }

                    // Download Resolution & Data Saver Quality Dialog
                    if (pendingDownloadMovie != null) {
                        DownloadQualityDialog(
                            movie = pendingDownloadMovie!!,
                            onStartDownload = { qualityOption ->
                                viewModel.confirmDownloadWithQuality(
                                    movie = pendingDownloadMovie!!,
                                    qualityOption = qualityOption,
                                    context = context
                                )
                                coroutineScope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "Started download for \"${pendingDownloadMovie!!.title}\" (${qualityOption.label} • ~${qualityOption.estimatedSizeMb} MB)",
                                        actionLabel = "View",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        currentTab = CineNavTab.DOWNLOADS
                                    }
                                }
                            },
                            onDismiss = {
                                viewModel.dismissDownloadQualityDialog()
                            }
                        )
                    }

                    // Admin Cloudflare R2 Upload Movie Dialog
                    if (showAdminUploadDialog) {
                        AdminUploadMovieDialog(
                            viewModel = viewModel,
                            onDismiss = { showAdminUploadDialog = false }
                        )
                    }

                    // Admin Edit Movie Dialog
                    if (movieToEdit != null && isAdminLoggedIn) {
                        AdminEditMovieDialog(
                            movie = movieToEdit!!,
                            viewModel = viewModel,
                            onDismiss = { movieToEdit = null }
                        )
                    }

                    // Floating top banner when update is minimized or ready
                    if (!showUpdateDialog && appUpdateInfo != null) {
                        BackgroundUpdateBanner(
                            updateInfo = appUpdateInfo!!,
                            downloadProgress = updateDownloadProgress,
                            onExpand = {
                                viewModel.expandUpdateDialog()
                            },
                            onInstall = {
                                viewModel.installDownloadedApk(context)
                            },
                            onDismiss = {
                                viewModel.dismissUpdateDialog()
                            },
                            modifier = Modifier.align(Alignment.TopCenter)
                        )
                    }

                    // Direct OTA App Update Dialog
                    if (showUpdateDialog && appUpdateInfo != null) {
                        AppUpdateDialog(
                            updateInfo = appUpdateInfo!!,
                            downloadProgress = updateDownloadProgress,
                            onStartUpdate = {
                                viewModel.startAppUpdateDownload(context)
                            },
                            onInstallApk = {
                                viewModel.installDownloadedApk(context)
                            },
                            onMinimize = {
                                viewModel.minimizeUpdateDialog()
                            },
                            onDismiss = {
                                viewModel.dismissUpdateDialog()
                            }
                        )
                    }

                    // Admin Release Publisher Dialog (grapherkidd0@gmail.com)
                    if (showAdminReleasePublisher && isAdminLoggedIn) {
                        AdminReleasePublisherDialog(
                            currentUpdateInfo = appUpdateInfo,
                            currentVersionCode = viewModel.currentVersionCode,
                            currentVersionName = viewModel.currentVersionName,
                            isPublishing = isPublishingRelease,
                            onPublish = { code, name, apkUrl, notes, size, isForce ->
                                viewModel.publishNewRelease(
                                    context = context,
                                    versionCode = code,
                                    versionName = name,
                                    apkUrl = apkUrl,
                                    notes = notes,
                                    size = size,
                                    isForce = isForce
                                ) { success, msg ->
                                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                                }
                            },
                            onDismiss = {
                                viewModel.dismissAdminReleasePublisher()
                            }
                        )
                    }
                }
            }
        }

        // Search Screen Route (Accessible from header / home search button)
        composable(
            route = "search",
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(300)
                )
            }
        ) {
            SearchScreen(
                query = searchQuery,
                onQueryChange = { viewModel.updateSearchQuery(it) },
                selectedGenre = selectedGenreFilter,
                onSelectGenre = { viewModel.selectGenreFilter(it) },
                searchResults = searchResults,
                recentSearches = recentSearches,
                onSelectRecentSearch = { term -> viewModel.addRecentSearch(term) },
                onRemoveRecentSearch = { term -> viewModel.removeRecentSearch(term) },
                onClearRecentSearches = { viewModel.clearRecentSearches() },
                onMovieClick = { movie ->
                    navController.navigate("details/${movie.id}")
                },
                isSearching = isSearching,
                searchErrorMessage = searchErrorMessage,
                onRetrySearch = { viewModel.updateSearchQuery(searchQuery) }
            )
        }

        // Movie Details Screen
        composable(
            route = "details/{movieId}",
            arguments = listOf(navArgument("movieId") { type = NavType.StringType }),
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(350)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(300)
                )
            }
        ) { backStackEntry ->
            val movieId = backStackEntry.arguments?.getString("movieId") ?: ""
            
            LaunchedEffect(movieId) {
                if (movieId.isNotBlank()) {
                    viewModel.loadMovieDetails(movieId)
                }
            }

            val detailState = movieDetailsMap[movieId]
            val movie = detailState?.movie ?: viewModel.getMovieById(movieId)
            
            val isInWatchlist = movie?.let { viewModel.isMovieInWatchlist(it.id) } ?: false
            val downloadedEntity = downloadedMovies.firstOrNull { it.movieId == movieId && it.downloadStatus == "COMPLETED" }
            val isDownloaded = downloadedEntity != null
            val isRoomCached = (cachedMovies.any { it.id == movieId } || isInWatchlist)
            val downloadProgress = downloadProgressMap[movieId]

            MovieDetailsScreen(
                movie = movie,
                isInWatchlist = isInWatchlist,
                isDownloaded = isDownloaded,
                downloadProgress = downloadProgress,
                isOnline = isOnline,
                isRoomCached = isRoomCached,
                isLoading = detailState?.isLoading ?: (movie == null),
                isError = detailState?.isError ?: false,
                errorMessage = detailState?.errorMessage ?: "",
                recommendations = detailState?.recommendations ?: emptyList(),
                isLoadingRecommendations = detailState?.isLoadingRecommendations ?: false,
                onRetry = { viewModel.loadMovieDetails(movieId) },
                onToggleWatchlist = { movie?.let { viewModel.toggleWatchlist(it) } },
                onDownloadClick = { movieToDownload ->
                    viewModel.startMovieDownload(movieToDownload, context)
                },
                onDeleteDownloadClick = { movieIdToDelete ->
                    viewModel.deleteDownloadedMovie(movieIdToDelete, context)
                },
                onWatchClick = { m ->
                    navController.navigate("player/${m.id}/false")
                },
                onTrailerClick = { m ->
                    navController.navigate("player/${m.id}/true")
                },
                onRelatedMovieClick = { rec ->
                    navController.navigate("details/${rec.id}")
                },
                onBackClick = { navController.popBackStack() },
                isAdminLoggedIn = isAdminLoggedIn,
                onEditMovieClick = { movieToEdit = it }
            )
        }

        // Video Player Screen
        composable(
            route = "player/{movieId}/{isTrailer}",
            arguments = listOf(
                navArgument("movieId") { type = NavType.StringType },
                navArgument("isTrailer") { type = NavType.BoolType; defaultValue = false }
            ),
            enterTransition = { fadeIn(animationSpec = tween(400)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) }
        ) { backStackEntry ->
            val movieId = backStackEntry.arguments?.getString("movieId") ?: ""
            val isTrailer = backStackEntry.arguments?.getBoolean("isTrailer") ?: false
            val baseMovie = viewModel.getMovieById(movieId)
            val downloadedEntity = downloadedMovies.firstOrNull { it.movieId == movieId && it.downloadStatus == "COMPLETED" }

            // If movie is downloaded locally and user wants to watch the movie, play the local offline video file!
            val movieToPlay = if (!isTrailer && downloadedEntity != null) {
                downloadedEntity.toMovie()
            } else {
                baseMovie
            }

            if (movieToPlay != null) {
                VideoPlayerScreen(
                    movie = movieToPlay,
                    isTrailer = isTrailer,
                    onBackClick = { navController.popBackStack() },
                    onSaveProgress = { position, total ->
                        viewModel.recordWatchProgress(movieToPlay.id, position, total)
                    }
                )
            }
        }

        // Category Detail Screen ("See All")
        composable(
            route = "category/{categoryKey}/{categoryTitle}",
            arguments = listOf(
                navArgument("categoryKey") { type = NavType.StringType },
                navArgument("categoryTitle") { type = NavType.StringType }
            ),
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(350)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(300)
                )
            }
        ) { backStackEntry ->
            val categoryKey = URLDecoder.decode(
                backStackEntry.arguments?.getString("categoryKey") ?: "",
                StandardCharsets.UTF_8.toString()
            )
            val categoryTitle = URLDecoder.decode(
                backStackEntry.arguments?.getString("categoryTitle") ?: "",
                StandardCharsets.UTF_8.toString()
            )
            val movies = when (categoryKey) {
                "storage", "uploaded" -> uploadedMovies
                else -> {
                    uploadedMovies.filter { movie ->
                        movie.genres.any { it.equals(categoryKey, ignoreCase = true) } ||
                                movie.category.equals(categoryKey, ignoreCase = true)
                    }
                }
            }
            CategoryDetailScreen(
                categoryTitle = categoryTitle,
                movies = movies,
                onMovieClick = { movie ->
                    navController.navigate("details/${movie.id}")
                },
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
