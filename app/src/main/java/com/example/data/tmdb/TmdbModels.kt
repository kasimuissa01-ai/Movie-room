package com.example.data.tmdb

import com.example.model.CastMember
import com.example.model.Movie
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TmdbMovieDetailResponse(
    val id: Int,
    val title: String,
    val overview: String? = null,
    @field:Json(name = "poster_path") val posterPath: String? = null,
    @field:Json(name = "backdrop_path") val backdropPath: String? = null,
    @field:Json(name = "vote_average") val voteAverage: Float? = null,
    @field:Json(name = "vote_count") val voteCount: Int? = null,
    @field:Json(name = "release_date") val releaseDate: String? = null,
    val runtime: Int? = null,
    val genres: List<TmdbGenre>? = null,
    val credits: TmdbCreditsResponse? = null,
    val videos: TmdbVideosResponse? = null,
    val tagline: String? = null,
    @field:Json(name = "production_companies") val productionCompanies: List<TmdbCompany>? = null
)

@JsonClass(generateAdapter = true)
data class TmdbGenre(
    val id: Int,
    val name: String
)

@JsonClass(generateAdapter = true)
data class TmdbCreditsResponse(
    val cast: List<TmdbCastMember>? = null,
    val crew: List<TmdbCrewMember>? = null
)

@JsonClass(generateAdapter = true)
data class TmdbCastMember(
    val id: Int,
    val name: String,
    val character: String? = null,
    @field:Json(name = "profile_path") val profilePath: String? = null
)

@JsonClass(generateAdapter = true)
data class TmdbCrewMember(
    val name: String,
    val job: String? = null
)

@JsonClass(generateAdapter = true)
data class TmdbVideosResponse(
    val results: List<TmdbVideoItem>? = null
)

@JsonClass(generateAdapter = true)
data class TmdbVideoItem(
    val key: String? = null,
    val site: String? = null,
    val type: String? = null
)

@JsonClass(generateAdapter = true)
data class TmdbCompany(
    val name: String
)

@JsonClass(generateAdapter = true)
data class TmdbSearchResponse(
    val page: Int? = null,
    val results: List<TmdbMovieSummary>? = null,
    @field:Json(name = "total_results") val totalResults: Int? = null
)

@JsonClass(generateAdapter = true)
data class TmdbMovieSummary(
    val id: Int,
    val title: String,
    val overview: String? = null,
    @field:Json(name = "poster_path") val posterPath: String? = null,
    @field:Json(name = "backdrop_path") val backdropPath: String? = null,
    @field:Json(name = "vote_average") val voteAverage: Float? = null,
    @field:Json(name = "release_date") val releaseDate: String? = null,
    @field:Json(name = "genre_ids") val genreIds: List<Int>? = null
)

fun TmdbMovieDetailResponse.toDomainMovie(): Movie {
    val imageBase = "https://image.tmdb.org/t/p/"
    val poster = if (!posterPath.isNullOrEmpty()) {
        "${imageBase}w500$posterPath"
    } else {
        "https://images.unsplash.com/photo-1534447677768-be436bb09401?auto=format&fit=crop&w=600&q=80"
    }

    val backdrop = if (!backdropPath.isNullOrEmpty()) {
        "${imageBase}w1280$backdropPath"
    } else {
        poster
    }

    val yearInt = releaseDate?.take(4)?.toIntOrNull() ?: 2026
    val directorName = credits?.crew?.find { it.job.equals("Director", ignoreCase = true) }?.name
        ?: "Visionary Director"
    val studioName = productionCompanies?.firstOrNull()?.name ?: "Hollywood Studios"

    val domainCast = credits?.cast?.take(8)?.map { c ->
        CastMember(
            name = c.name,
            character = c.character ?: "Starring",
            avatarUrl = if (!c.profilePath.isNullOrEmpty()) {
                "${imageBase}w185${c.profilePath}"
            } else {
                "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=200&q=80"
            }
        )
    } ?: emptyList()

    val genreList = genres?.map { it.name }?.ifEmpty { listOf("Cinema") } ?: listOf("Cinema")

    val youtubeTrailer = videos?.results?.find {
        it.site.equals("YouTube", ignoreCase = true) &&
        (it.type.equals("Trailer", ignoreCase = true) || it.type.equals("Teaser", ignoreCase = true))
    }?.key

    return Movie(
        id = id.toString(),
        title = title,
        description = overview?.ifEmpty { tagline } ?: "An epic cinematic journey.",
        posterUrl = poster,
        backdropUrl = backdrop,
        trailerUrl = if (youtubeTrailer != null) "https://www.youtube.com/watch?v=$youtubeTrailer" else "",
        videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
        year = yearInt,
        durationMinutes = runtime ?: 120,
        rating = voteAverage ?: 8.0f,
        genres = genreList,
        cast = domainCast,
        director = directorName,
        studio = studioName,
        category = genreList.firstOrNull() ?: "Cinema",
        featured = true,
        trending = (voteAverage ?: 0f) >= 7.5f,
        releaseDate = releaseDate ?: "$yearInt",
        quality = "4K Ultra HD",
        contentRating = "PG-13"
    )
}

fun TmdbMovieSummary.toDomainMovie(): Movie {
    val imageBase = "https://image.tmdb.org/t/p/"
    val poster = if (!posterPath.isNullOrEmpty()) {
        "${imageBase}w500$posterPath"
    } else {
        "https://images.unsplash.com/photo-1534447677768-be436bb09401?auto=format&fit=crop&w=600&q=80"
    }

    val backdrop = if (!backdropPath.isNullOrEmpty()) {
        "${imageBase}w1280$backdropPath"
    } else {
        poster
    }

    val yearInt = releaseDate?.take(4)?.toIntOrNull() ?: 2026

    return Movie(
        id = id.toString(),
        title = title,
        description = overview ?: "A remarkable cinematic release.",
        posterUrl = poster,
        backdropUrl = backdrop,
        year = yearInt,
        durationMinutes = 125,
        rating = voteAverage ?: 7.5f,
        genres = listOf("Movie"),
        cast = emptyList(),
        director = "Cinema Master",
        studio = "Universal Releases",
        category = "Movies",
        featured = false,
        trending = true,
        releaseDate = releaseDate ?: "$yearInt",
        quality = "4K Ultra HD",
        contentRating = "PG-13"
    )
}
