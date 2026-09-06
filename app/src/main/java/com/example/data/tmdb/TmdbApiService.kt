package com.example.data.tmdb

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApiService {

    @GET("movie/{movie_id}")
    suspend fun getMovieDetails(
        @Path("movie_id") movieId: String,
        @Query("api_key") apiKey: String? = null,
        @Query("append_to_response") appendToResponse: String = "credits,videos"
    ): TmdbMovieDetailResponse

    @GET("search/movie")
    suspend fun searchMovies(
        @Query("query") query: String,
        @Query("api_key") apiKey: String? = null,
        @Query("page") page: Int = 1
    ): TmdbSearchResponse

    @GET("trending/movie/day")
    suspend fun getTrendingMovies(
        @Query("api_key") apiKey: String? = null,
        @Query("page") page: Int = 1
    ): TmdbSearchResponse
}
