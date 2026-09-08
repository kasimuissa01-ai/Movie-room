package com.example.data

import com.example.model.CastMember
import com.example.model.Movie

object SampleMovies {

    val allMovies: List<Movie> = listOf(
        // Inception
        Movie(
            id = "27205",
            title = "Inception",
            description = "A thief who steals corporate secrets through the use of dream-sharing technology is given the inverse task of planting an idea into the mind of a C.E.O., but his tragic past may doom the project and his team to disaster.",
            posterUrl = "https://image.tmdb.org/t/p/w500/oYuLEt3zVCKq57qu2F8dT7NIa6f.jpg",
            backdropUrl = "https://image.tmdb.org/t/p/w1280/8ZTVqvKDQ8emSGUEMjsS4yHAwrp.jpg",
            trailerUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
            year = 2010,
            durationMinutes = 148,
            rating = 8.8f,
            genres = listOf("Action", "Sci-Fi", "Adventure"),
            cast = listOf(
                CastMember("Leonardo DiCaprio", "Dom Cobb", "https://image.tmdb.org/t/p/w185/wo2hJpn04vbtmh0B9utCFdsQhxM.jpg"),
                CastMember("Joseph Gordon-Levitt", "Arthur", "https://image.tmdb.org/t/p/w185/dhv9f3A2nfr1nFmDcl1r4k9Wb9e.jpg"),
                CastMember("Elliot Page", "Ariadne", "https://image.tmdb.org/t/p/w185/tp157eQmgB31nKWFbTsdqj80h9.jpg"),
                CastMember("Tom Hardy", "Eames", "https://image.tmdb.org/t/p/w185/yVGF9FvDxTCunChG29feaqclJWf.jpg"),
                CastMember("Cillian Murphy", "Robert Fischer", "https://image.tmdb.org/t/p/w185/dm6Vv1m6L9rNl6d0F9N35mZ5H1Q.jpg")
            ),
            director = "Christopher Nolan",
            studio = "Warner Bros. Pictures",
            category = "Sci-Fi",
            featured = true,
            trending = true,
            releaseDate = "July 16, 2010",
            quality = "4K Ultra HD",
            contentRating = "PG-13"
        ),

        // Interstellar
        Movie(
            id = "157336",
            title = "Interstellar",
            description = "The adventures of a group of explorers who make use of a newly discovered wormhole to surpass the limitations on human space travel and conquer the vast distances involved in an interstellar voyage.",
            posterUrl = "https://image.tmdb.org/t/p/w500/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg",
            backdropUrl = "https://image.tmdb.org/t/p/w1280/xJHokMbljvjADYdit5fK5VQsXEG.jpg",
            trailerUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
            year = 2014,
            durationMinutes = 169,
            rating = 8.7f,
            genres = listOf("Sci-Fi", "Drama", "Adventure"),
            cast = listOf(
                CastMember("Matthew McConaughey", "Joseph Cooper", "https://image.tmdb.org/t/p/w185/wDeLNxnKk5YxU22v7aI6aIe9fC0.jpg"),
                CastMember("Anne Hathaway", "Dr. Amelia Brand", "https://image.tmdb.org/t/p/w185/tLelKoPNiyJCSEtQT81FGZ6aY1d.jpg"),
                CastMember("Jessica Chastain", "Murphy Cooper", "https://image.tmdb.org/t/p/w185/lodMzLKSbqaO1q8q7eG8fG4mQ9B.jpg"),
                CastMember("Michael Caine", "Professor Brand", "https://image.tmdb.org/t/p/w185/klNxOq3rN0l1Jz0uJk8b9r7F8B.jpg")
            ),
            director = "Christopher Nolan",
            studio = "Paramount Pictures & Syncopy",
            category = "Sci-Fi",
            featured = true,
            trending = true,
            releaseDate = "November 7, 2014",
            quality = "IMAX Enhanced",
            contentRating = "PG-13"
        ),

        // Oppenheimer
        Movie(
            id = "872585",
            title = "Oppenheimer",
            description = "The story of American scientist J. Robert Oppenheimer and his role in the development of the atomic bomb during the Manhattan Project in World War II.",
            posterUrl = "https://image.tmdb.org/t/p/w500/8Gxv8gSFCU0XGDykEGv7zR1n2ua.jpg",
            backdropUrl = "https://image.tmdb.org/t/p/w1280/rLb2cw69rPQUQ9G6qoenvTNQkcp.jpg",
            trailerUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
            year = 2023,
            durationMinutes = 180,
            rating = 8.9f,
            genres = listOf("Drama", "History", "Biography"),
            cast = listOf(
                CastMember("Cillian Murphy", "J. Robert Oppenheimer", "https://image.tmdb.org/t/p/w185/dm6Vv1m6L9rNl6d0F9N35mZ5H1Q.jpg"),
                CastMember("Emily Blunt", "Katherine Oppenheimer", "https://image.tmdb.org/t/p/w185/nPJXaRMVUYSvXx1Nxv0hxMRv4v3.jpg"),
                CastMember("Matt Damon", "Leslie Groves", "https://image.tmdb.org/t/p/w185/elSlNg0VXx0zTzC671gH7gWkZ1F.jpg"),
                CastMember("Robert Downey Jr.", "Lewis Strauss", "https://image.tmdb.org/t/p/w185/5qHNjhtjMD4YWH3fq0Y50o9J9bS.jpg")
            ),
            director = "Christopher Nolan",
            studio = "Universal Pictures & Syncopy",
            category = "Drama",
            featured = true,
            trending = true,
            releaseDate = "July 21, 2023",
            quality = "4K Ultra HD",
            contentRating = "R"
        ),

        // Dune: Part Two
        Movie(
            id = "693134",
            title = "Dune: Part Two",
            description = "Paul Atreides unites with Chani and the Fremen while seeking revenge against the conspirators who destroyed his family. Facing a choice between the love of his life and the fate of the universe.",
            posterUrl = "https://image.tmdb.org/t/p/w500/1pdfLvkbY9ohJlCjQH2CZjjYVvJ.jpg",
            backdropUrl = "https://image.tmdb.org/t/p/w1280/xOMo8BRK7PfcJv9JCnx7s5hj0x2.jpg",
            trailerUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
            year = 2024,
            durationMinutes = 166,
            rating = 8.6f,
            genres = listOf("Sci-Fi", "Adventure", "Action"),
            cast = listOf(
                CastMember("Timothée Chalamet", "Paul Atreides", "https://image.tmdb.org/t/p/w185/BE2sdjpgsa2rNTFa66f7upkaOP.jpg"),
                CastMember("Zendaya", "Chani", "https://image.tmdb.org/t/p/w185/r3A7ev7Qkjom1X48Cfl4zQp6s4.jpg"),
                CastMember("Rebecca Ferguson", "Lady Jessica", "https://image.tmdb.org/t/p/w185/hJSpBdfKq4g4m2zT9XJ09b4S3B.jpg"),
                CastMember("Javier Bardem", "Stilgar", "https://image.tmdb.org/t/p/w185/423hZ09m2n07Jq4zZ1R1P0X9Q6J.jpg")
            ),
            director = "Denis Villeneuve",
            studio = "Warner Bros. & Legendary Pictures",
            category = "Sci-Fi",
            featured = true,
            trending = true,
            releaseDate = "March 1, 2024",
            quality = "IMAX Enhanced",
            contentRating = "PG-13"
        ),

        // The Dark Knight
        Movie(
            id = "155",
            title = "The Dark Knight",
            description = "When the menace known as the Joker wreaks havoc and chaos on the people of Gotham, Batman must accept one of the greatest psychological and physical tests of his ability to fight injustice.",
            posterUrl = "https://image.tmdb.org/t/p/w500/qJ2tW6WMUDux911r6m7haRef0WH.jpg",
            backdropUrl = "https://image.tmdb.org/t/p/w1280/nMKdUUepR0i5zn0y1T4CsSB5chy.jpg",
            trailerUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
            year = 2008,
            durationMinutes = 152,
            rating = 9.0f,
            genres = listOf("Action", "Crime", "Drama"),
            cast = listOf(
                CastMember("Christian Bale", "Bruce Wayne / Batman", "https://image.tmdb.org/t/p/w185/b7fTC9WFkgqGOv771QeuEBuZwR7.jpg"),
                CastMember("Heath Ledger", "Joker", "https://image.tmdb.org/t/p/w185/5Y9HnYYa9jF4D9xQyP7F9vH0k1P.jpg"),
                CastMember("Michael Caine", "Alfred Pennyworth", "https://image.tmdb.org/t/p/w185/klNxOq3rN0l1Jz0uJk8b9r7F8B.jpg"),
                CastMember("Gary Oldman", "James Gordon", "https://image.tmdb.org/t/p/w185/2v9FsV9Z46GaEh0p7P7V9bS0q6.jpg")
            ),
            director = "Christopher Nolan",
            studio = "Warner Bros. Pictures",
            category = "Action",
            featured = false,
            trending = true,
            releaseDate = "July 18, 2008",
            quality = "4K Ultra HD",
            contentRating = "PG-13"
        ),

        // Gladiator II
        Movie(
            id = "558449",
            title = "Gladiator II",
            description = "Years after witnessing the death of the revered hero Maximus at the hands of his uncle, Lucius must enter the Colosseum after his home is conquered by the tyrannical Emperors who now lead Rome with an iron fist.",
            posterUrl = "https://image.tmdb.org/t/p/w500/2cxhvwyEwRlysAmRH4iodkvo0z5.jpg",
            backdropUrl = "https://image.tmdb.org/t/p/w1280/euYIWhGv2Nzxm5ChwmOSIrjrZh.jpg",
            trailerUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
            year = 2024,
            durationMinutes = 148,
            rating = 8.2f,
            genres = listOf("Action", "Adventure", "Drama"),
            cast = listOf(
                CastMember("Paul Mescal", "Lucius", "https://image.tmdb.org/t/p/w185/b5y5J3vF9M6P4M7X6J5L2k5P9k.jpg"),
                CastMember("Pedro Pascal", "Marcus Acacius", "https://image.tmdb.org/t/p/w185/tN12xZ7C3v9Q7bK3V8K7X6k9p6L.jpg"),
                CastMember("Denzel Washington", "Macrinus", "https://image.tmdb.org/t/p/w185/cABo5qZp9aQ8b6P7p8M9aQ0l6X.jpg")
            ),
            director = "Ridley Scott",
            studio = "Paramount Pictures",
            category = "Action",
            featured = false,
            trending = true,
            releaseDate = "November 22, 2024",
            quality = "4K Ultra HD",
            contentRating = "R"
        ),

        // Avengers: Endgame
        Movie(
            id = "299534",
            title = "Avengers: Endgame",
            description = "After the devastating events of Infinity War, the universe is in ruins. With the help of remaining allies, the Avengers assemble once more in order to reverse Thanos' actions and restore balance to the universe.",
            posterUrl = "https://image.tmdb.org/t/p/w500/or06FN3Dka5tukK1e9sl16pB3iy.jpg",
            backdropUrl = "https://image.tmdb.org/t/p/w1280/7RyHsO4yDXtBv1zUU3mTpHeQ0d5.jpg",
            trailerUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
            year = 2019,
            durationMinutes = 181,
            rating = 8.4f,
            genres = listOf("Action", "Sci-Fi", "Adventure"),
            cast = listOf(
                CastMember("Robert Downey Jr.", "Tony Stark / Iron Man", "https://image.tmdb.org/t/p/w185/5qHNjhtjMD4YWH3fq0Y50o9J9bS.jpg"),
                CastMember("Chris Evans", "Steve Rogers / Captain America", "https://image.tmdb.org/t/p/w185/3bOGNsHlrswhyW79uvIHH1V43JI.jpg"),
                CastMember("Chris Hemsworth", "Thor", "https://image.tmdb.org/t/p/w185/jpurJ9jAcLCYjgagqdaY2afRGv1.jpg"),
                CastMember("Scarlett Johansson", "Natasha Romanoff / Black Widow", "https://image.tmdb.org/t/p/w185/6NsMbJXRMTx0b9Ki0Ptvd8xP9R1.jpg")
            ),
            director = "Anthony & Joe Russo",
            studio = "Marvel Studios",
            category = "Action",
            featured = false,
            trending = true,
            releaseDate = "April 26, 2019",
            quality = "IMAX Enhanced",
            contentRating = "PG-13"
        ),

        // Top Gun: Maverick
        Movie(
            id = "361743",
            title = "Top Gun: Maverick",
            description = "After more than thirty years of service as one of the Navy's top aviators, Pete 'Maverick' Mitchell is where he belongs, pushing the envelope as a courageous test pilot and dodging the advancement in rank that would ground him.",
            posterUrl = "https://image.tmdb.org/t/p/w500/62HCnUTziyWcpDaBO2i1DX17ljH.jpg",
            backdropUrl = "https://image.tmdb.org/t/p/w1280/AaV1YIdWKnjAIAOe8UUKBFm327v.jpg",
            trailerUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
            year = 2022,
            durationMinutes = 130,
            rating = 8.3f,
            genres = listOf("Action", "Drama"),
            cast = listOf(
                CastMember("Tom Cruise", "Pete 'Maverick' Mitchell", "https://image.tmdb.org/t/p/w185/8qB9q5BtKxQvG7P9oM2B8J8j3e1.jpg"),
                CastMember("Miles Teller", "Bradley 'Rooster' Bradshaw", "https://image.tmdb.org/t/p/w185/cg3LW0xX2e4e1yT2b7W0Z2q5f8F.jpg"),
                CastMember("Jennifer Connelly", "Penny Benjamin", "https://image.tmdb.org/t/p/w185/eP4aG0u4pB8V3eP1B4m7L8c9r3T.jpg")
            ),
            director = "Joseph Kosinski",
            studio = "Paramount Pictures & Skydance",
            category = "Action",
            featured = false,
            trending = true,
            releaseDate = "May 27, 2022",
            quality = "4K Ultra HD",
            contentRating = "PG-13"
        ),

        // Spider-Man: Into the Spider-Verse
        Movie(
            id = "324857",
            title = "Spider-Man: Into the Spider-Verse",
            description = "Teen Miles Morales becomes the new Spider-Man and joins other Spider-Heroes from parallel dimensions to stop a threat to all reality.",
            posterUrl = "https://image.tmdb.org/t/p/w500/iiZZdoQBEYBv6id8su7ImL0oCbD.jpg",
            backdropUrl = "https://image.tmdb.org/t/p/w1280/7d6EZ0rKnTVz39vCG4PTBp07dm5.jpg",
            trailerUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
            year = 2018,
            durationMinutes = 117,
            rating = 8.4f,
            genres = listOf("Animation", "Action", "Adventure", "Sci-Fi"),
            cast = listOf(
                CastMember("Shameik Moore", "Miles Morales (voice)", "https://image.tmdb.org/t/p/w185/v3G8X2mP9k4a1J8k6a1P8m2N3k.jpg"),
                CastMember("Jake Johnson", "Peter B. Parker (voice)", "https://image.tmdb.org/t/p/w185/4b6uL3kP0l1m9b8Q7a1N2b4V6x.jpg"),
                CastMember("Hailee Steinfeld", "Gwen Stacy (voice)", "https://image.tmdb.org/t/p/w185/3c1gN7a8k2M6L9p1x7j2P8r4a.jpg")
            ),
            director = "Bob Persichetti, Peter Ramsey, Rodney Rothman",
            studio = "Sony Pictures Animation",
            category = "Animation",
            featured = false,
            trending = true,
            releaseDate = "December 14, 2018",
            quality = "4K Ultra HD",
            contentRating = "PG"
        )
    )

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

    val trendingSearches: List<String> = listOf(
        "Inception (ID: 27205)", "27205", "Interstellar", "Oppenheimer", "Dune: Part Two", "Christopher Nolan", "Top Gun", "Avengers"
    )

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
