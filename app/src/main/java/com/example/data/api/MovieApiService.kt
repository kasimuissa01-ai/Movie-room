package com.example.data.api

import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface MovieApiService {

    // --- Public Movie Endpoints ---
    @GET("api/movies/trending")
    suspend fun getTrendingMovies(): ApiResponse<List<MovieDto>>

    @GET("api/movies/popular")
    suspend fun getPopularMovies(): ApiResponse<List<MovieDto>>

    @GET("api/movies/now-playing")
    suspend fun getNowPlayingMovies(): ApiResponse<List<MovieDto>>

    @GET("api/movies/upcoming")
    suspend fun getUpcomingMovies(): ApiResponse<List<MovieDto>>

    @GET("api/movies/action")
    suspend fun getActionMovies(): ApiResponse<List<MovieDto>>

    @GET("api/movies/{category}")
    suspend fun getMoviesByCategory(
        @Path("category") category: String
    ): ApiResponse<List<MovieDto>>

    @GET("api/movies/search")
    suspend fun searchMovies(
        @Query("query") query: String
    ): ApiResponse<List<MovieDto>>

    @GET("api/movies/movie/{id}")
    suspend fun getMovieById(
        @Path("id") id: String
    ): ApiResponse<MovieDto>

    @GET("api/movies/{id}")
    suspend fun getMovieByIdAlt(
        @Path("id") id: String
    ): ApiResponse<MovieDto>

    @GET("api/movies/movie/{id}/recommendations")
    suspend fun getRecommendations(
        @Path("id") id: String
    ): ApiResponse<List<MovieDto>>

    @GET("api/movies/{id}/recommendations")
    suspend fun getRecommendationsAlt(
        @Path("id") id: String
    ): ApiResponse<List<MovieDto>>

    // --- Authentication ---
    @POST("api/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): AuthResponse

    @POST("api/auth/admin-login")
    suspend fun adminLogin(
        @Body request: AdminLoginRequest
    ): AuthResponse

    // --- Authenticated User Endpoints ---
    @GET("api/user/profile")
    suspend fun getUserProfile(
        @Header("Authorization") authHeader: String
    ): ApiResponse<UserDto>

    @GET("api/user/watchlist")
    suspend fun getWatchlist(
        @Header("Authorization") authHeader: String
    ): ApiResponse<List<Map<String, Any>>>

    @POST("api/user/watchlist")
    suspend fun addToWatchlist(
        @Header("Authorization") authHeader: String,
        @Body request: WatchlistActionRequest
    ): ApiResponse<Map<String, Any>>

    @DELETE("api/user/watchlist/{movieId}")
    suspend fun removeFromWatchlist(
        @Header("Authorization") authHeader: String,
        @Path("movieId") movieId: String
    ): ApiResponse<Map<String, Any>>

    @GET("api/user/history")
    suspend fun getWatchHistory(
        @Header("Authorization") authHeader: String
    ): ApiResponse<List<Map<String, Any>>>

    @POST("api/user/history")
    suspend fun updateWatchHistory(
        @Header("Authorization") authHeader: String,
        @Body request: WatchHistoryRequest
    ): ApiResponse<Map<String, Any>>

    // --- Admin Endpoints ---
    @POST("api/admin/movies")
    suspend fun adminCreateMovie(
        @Header("Authorization") authHeader: String,
        @Body movie: Map<String, Any>
    ): ApiResponse<MovieDto>

    @POST("api/admin/movies/{id}/publish")
    suspend fun adminPublishMovie(
        @Header("Authorization") authHeader: String,
        @Path("id") id: String
    ): ApiResponse<MovieDto>

    @DELETE("api/admin/movies/{id}")
    suspend fun adminDeleteMovie(
        @Header("Authorization") authHeader: String,
        @Path("id") id: String
    ): ApiResponse<Map<String, Any>>

    @POST("api/admin/r2/upload")
    suspend fun adminUploadR2(
        @Header("Authorization") authHeader: String,
        @Query("filename") filename: String,
        @Query("folder") folder: String,
        @Header("Content-Type") contentType: String,
        @Body fileBody: RequestBody
    ): R2UploadResponse

    @POST("api/admin/uploads/initiate")
    suspend fun adminInitiateUpload(
        @Header("Authorization") authHeader: String,
        @Body request: InitiateUploadRequest
    ): InitiateUploadResponse

    @POST("api/admin/uploads/sign-part")
    suspend fun adminSignPart(
        @Header("Authorization") authHeader: String,
        @Body request: SignPartRequest
    ): SignPartResponse

    @POST("api/admin/uploads/complete")
    suspend fun adminCompleteUpload(
        @Header("Authorization") authHeader: String,
        @Body request: CompleteUploadRequest
    ): CompleteUploadResponse
}

data class InitiateUploadRequest(
    val filename: String,
    val folder: String,
    val fileSize: Long,
    val contentType: String,
    val mode: String? = null
)

data class InitiateUploadResponse(
    val success: Boolean,
    val mode: String? = null,
    val key: String? = null,
    val uploadUrl: String? = null,
    val uploadId: String? = null,
    val partSize: Long = 16777216L,
    val totalParts: Int = 1,
    val publicUrl: String? = null,
    val message: String? = null,
    val error: String? = null
)

data class SignPartRequest(
    val key: String,
    val uploadId: String,
    val partNumber: Int
)

data class SignPartResponse(
    val success: Boolean = true,
    val key: String? = null,
    val uploadId: String? = null,
    val partNumber: Int = 1,
    val uploadUrl: String? = null,
    val error: String? = null
)

data class CompletePartDto(
    val partNumber: Int,
    val etag: String
)

data class CompleteUploadRequest(
    val key: String,
    val uploadId: String? = null,
    val parts: List<CompletePartDto>? = null,
    val movieId: String? = null,
    val isTrailer: Boolean? = null,
    val movieData: Map<String, Any?>? = null
)

data class CompleteUploadResponse(
    val success: Boolean,
    val key: String? = null,
    val url: String? = null,
    val message: String? = null,
    val error: String? = null
)
