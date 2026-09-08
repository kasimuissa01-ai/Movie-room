package com.example.data

import com.example.model.CastMember
import com.example.model.Movie

object SampleMovies {

    val allMovies: List<Movie> = emptyList()

    val featuredMovies: List<Movie> = allMovies.filter { it.featured }
    val trendingMovies: List<Movie> = allMovies.filter { it.trending }

    val categories: List<Pair<String, String>> = listOf(
        "trending" to "Trending Now in Movie Room",
        "Action" to "Action & Adrenaline",
        "Sci-Fi" to "Sci-Fi & Cosmic Horizons",
        "Drama" to "Award-Winning Drama",
        "Animation" to "Animation & Fantasy"
    )

    val popularGenres: List<String> = listOf(
        "Action", "Sci-Fi", "Drama", "Adventure", "Crime", "Thriller", "Animation", "Biography"
    )

    val trendingSearches: List<String> = emptyList()

    fun getMovieById(id: String): Movie? {
        val clean = id.trim()
        val numeric = clean.removePrefix("tmdb_")
        return allMovies.firstOrNull { it.id == clean || it.id == numeric || "tmdb_${it.id}" == clean }
    }

    fun getRelatedMovies(movie: Movie, limit: Int = 6): List<Movie> {
        return getSimilarMovies(movie, limit)
    }

    fun getRecommended(movieId: String, limit: Int = 6): List<Movie> {
        val current = getMovieById(movieId)
        return if (current != null) {
            getSimilarMovies(current, limit)
        } else {
            trendingMovies.take(limit)
        }
    }

    fun getMoviesForCategory(categoryKey: String): List<Movie> {
        return when (categoryKey.lowercase()) {
            "trending" -> trendingMovies
            "featured" -> featuredMovies
            else -> allMovies.filter { movie ->
                movie.category.equals(categoryKey, ignoreCase = true) ||
                        movie.genres.any { it.equals(categoryKey, ignoreCase = true) }
            }
        }
    }

    fun getSimilarMovies(movie: Movie, limit: Int = 6): List<Movie> {
        return allMovies
            .filter { it.id != movie.id }
            .filter { other ->
                other.category.equals(movie.category, ignoreCase = true) ||
                        other.genres.any { genre -> movie.genres.contains(genre) }
            }
            .take(limit)
    }
}
