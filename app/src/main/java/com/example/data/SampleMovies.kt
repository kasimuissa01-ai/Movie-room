package com.example.data

import com.example.model.CastMember
import com.example.model.Movie

object SampleMovies {

    val allMovies: List<Movie> = listOf(
        // TMDB Featured Movie ID: 27205 - Inception
        Movie(
            id = "27205",
            title = "Inception",
            description = "A thief who enters dreams to steal corporate secrets is given the inverse task of planting an idea into the mind of a CEO. Armed with experimental dream-sharing technology, he must execute a perilous multi-layered inception.",
            posterUrl = "https://image.tmdb.org/t/p/w500/oYuLEt3zVCKq57qu2F8dT7NIa6f.jpg",
            backdropUrl = "https://image.tmdb.org/t/p/w1280/8ZTVqvKDQ8emSGUEMjsS4yHAwrp.jpg",
            trailerUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
            year = 2010,
            durationMinutes = 148,
            rating = 8.4f,
            genres = listOf("Action", "Sci-Fi", "Adventure"),
            cast = listOf(
                CastMember("Leonardo DiCaprio", "Dom Cobb", "https://image.tmdb.org/t/p/w185/wo2hJpn04vbtmh0B9utCFdsQhxM.jpg"),
                CastMember("Joseph Gordon-Levitt", "Arthur", "https://image.tmdb.org/t/p/w185/dhv9f3A2nfr1nFmDcl1r4k9Wb9e.jpg"),
                CastMember("Elliot Page", "Ariadne", "https://image.tmdb.org/t/p/w185/tp157eQmgB31nKWFbTsdqj80h9.jpg"),
                CastMember("Tom Hardy", "Eames", "https://image.tmdb.org/t/p/w185/yVGF9FvDxTCunChG29feaqclJWf.jpg"),
                CastMember("Cillian Murphy", "Robert Fischer", "https://image.tmdb.org/t/p/w185/dm6Vv1m6L9rNl6d0F9N35mZ5H1Q.jpg")
            ),
            director = "Christopher Nolan",
            studio = "Warner Bros. & Syncopy",
            category = "Sci-Fi",
            featured = true,
            trending = true,
            releaseDate = "July 16, 2010",
            quality = "4K Ultra HD",
            contentRating = "PG-13"
        ),

        // Featured Hero Movie 1
        Movie(
            id = "m1",
            title = "The Last Hunt",
            description = "A former special forces operative is forced back into the shadows when his family becomes the target of a ruthless international syndicate. Armed with elite tactics and unbreakable resolve, he wages a solitary war across freezing mountain ranges.",
            posterUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?auto=format&fit=crop&w=600&q=80",
            backdropUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&w=1200&q=80",
            trailerUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
            year = 2026,
            durationMinutes = 138,
            rating = 8.7f,
            genres = listOf("Action", "Thriller", "Crime"),
            cast = listOf(
                CastMember("Marcus Vance", "Liam Stone", "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=200&q=80"),
                CastMember("Elena Rostova", "Katarina Blake", "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=200&q=80"),
                CastMember("Viktor Kroll", "Dmitri Orlov", "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=200&q=80"),
                CastMember("Sarah Jenkins", "Dr. Maya Lin", "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=200&q=80")
            ),
            director = "Christopher Nolan",
            studio = "Syncopy & CineStream",
            category = "Action",
            featured = true,
            trending = true,
            releaseDate = "March 2026",
            quality = "4K Ultra HD",
            contentRating = "R"
        ),

        // Featured Hero Movie 2
        Movie(
            id = "m2",
            title = "Chrono Drift",
            description = "A quantum physicist discovers a fractured wormhole that displaces matter across centuries. When his research partner vanishes into a divergent timeline, he must traverse uncharted cosmic dimensions before reality completely collapses.",
            posterUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?auto=format&fit=crop&w=600&q=80",
            backdropUrl = "https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?auto=format&fit=crop&w=1200&q=80",
            trailerUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
            year = 2026,
            durationMinutes = 145,
            rating = 8.9f,
            genres = listOf("Sci-Fi", "Adventure", "Mystery"),
            cast = listOf(
                CastMember("David Mercer", "Dr. Ethan Cole", "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&w=200&q=80"),
                CastMember("Astrid Holm", "Lyra Vance", "https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&w=200&q=80"),
                CastMember("Gideon Park", "Commander Thorne", "https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?auto=format&fit=crop&w=200&q=80")
            ),
            director = "Denis Villeneuve",
            studio = "Legendary Pictures",
            category = "Sci-Fi",
            featured = true,
            trending = true,
            releaseDate = "January 2026",
            quality = "IMAX Enhanced",
            contentRating = "PG-13"
        ),

        // Featured Hero Movie 3
        Movie(
            id = "m3",
            title = "Neon Shadows",
            description = "Under the perpetual neon downpour of 2088 New Kobe, a disgraced augmented investigator receives an encrypted memory chip containing evidence of synthetic consciousness manipulation by megacorporations.",
            posterUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?auto=format&fit=crop&w=600&q=80",
            backdropUrl = "https://images.unsplash.com/photo-1542751371-adc38448a05e?auto=format&fit=crop&w=1200&q=80",
            trailerUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
            year = 2026,
            durationMinutes = 132,
            rating = 8.6f,
            genres = listOf("Sci-Fi", "Action", "Cyberpunk"),
            cast = listOf(
                CastMember("Ren Tanaka", "Detective Kaelen", "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=200&q=80"),
                CastMember("Sora Chen", "Nova-7", "https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&w=200&q=80")
            ),
            director = "Ridley Scott",
            studio = "Alcon Entertainment",
            category = "Sci-Fi",
            featured = true,
            trending = true,
            releaseDate = "February 2026",
            quality = "4K Ultra HD",
            contentRating = "R"
        ),

        // Featured Hero Movie 4
        Movie(
            id = "m4",
            title = "Eclipse of Empires",
            description = "Three mighty kingdoms march across the uncharted Frostlands as a once-in-a-millennium celestial alignment unseals legendary fortresses holding the ancient secrets of elemental power.",
            posterUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&w=600&q=80",
            backdropUrl = "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?auto=format&fit=crop&w=1200&q=80",
            trailerUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
            year = 2026,
            durationMinutes = 160,
            rating = 9.1f,
            genres = listOf("Adventure", "Fantasy", "Action"),
            cast = listOf(
                CastMember("Thorne Blackwood", "King Alistair", "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=200&q=80"),
                CastMember("Isolde Rivera", "Queen Maeve", "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=200&q=80")
            ),
            director = "Peter Jackson",
            studio = "WingNut Films",
            category = "Adventure",
            featured = true,
            trending = true,
            releaseDate = "May 2026",
            quality = "Dolby Vision",
            contentRating = "PG-13"
        ),

        // Action Movies
        Movie(
            id = "m5",
            title = "Apex Protocol",
            description = "When a cyberwarfare unit is compromised from within, an elite stealth specialist must extract vital satellite keys while escaping assassination teams in high-speed urban pursuits.",
            posterUrl = "https://images.unsplash.com/photo-1579783902614-a3fb3927b675?auto=format&fit=crop&w=600&q=80",
            backdropUrl = "https://images.unsplash.com/photo-1511447333015-45b65e60f6d5?auto=format&fit=crop&w=1200&q=80",
            year = 2025,
            durationMinutes = 118,
            rating = 8.4f,
            genres = listOf("Action", "Thriller"),
            cast = listOf(
                CastMember("Jason Cross", "Agent Miller", "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=200&q=80")
            ),
            director = "Chad Stahelski",
            category = "Action",
            trending = true,
            quality = "4K Ultra HD",
            contentRating = "R"
        ),
        Movie(
            id = "m6",
            title = "Velocity Zero",
            description = "A clandestine underground race across the hyperways of Europe becomes a fight for survival when an experimental hyper-engine triggers electromagnetic lockdown.",
            posterUrl = "https://images.unsplash.com/photo-1568605117036-5fe5e7bab0b7?auto=format&fit=crop&w=600&q=80",
            backdropUrl = "https://images.unsplash.com/photo-1492144534655-ae79c964c9d7?auto=format&fit=crop&w=1200&q=80",
            year = 2025,
            durationMinutes = 112,
            rating = 7.9f,
            genres = listOf("Action", "Crime"),
            cast = listOf(
                CastMember("Leo Vance", "Cole Ryder", "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&w=200&q=80")
            ),
            director = "Justin Lin",
            category = "Action",
            quality = "HD",
            contentRating = "PG-13"
        ),

        // Drama Movies
        Movie(
            id = "m7",
            title = "The Silent Symphony",
            description = "A brilliant classical pianist begins losing his hearing on the eve of his world premiere. Through radical acoustic synthesis and sheer determination, he crafts a revolutionary auditory masterpiece.",
            posterUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?auto=format&fit=crop&w=600&q=80",
            backdropUrl = "https://images.unsplash.com/photo-1465847899084-d164df4dedc6?auto=format&fit=crop&w=1200&q=80",
            year = 2025,
            durationMinutes = 126,
            rating = 8.8f,
            genres = listOf("Drama", "Music"),
            cast = listOf(
                CastMember("Julian March", "Adrian Novak", "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=200&q=80"),
                CastMember("Clara Bennett", "Sophia Vance", "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=200&q=80")
            ),
            director = "Damien Chazelle",
            category = "Drama",
            trending = true,
            quality = "4K Ultra HD",
            contentRating = "PG-13"
        ),
        Movie(
            id = "m8",
            title = "Glass Horizon",
            description = "In the cutthroat world of international architecture, two rival visionaries battle to construct the world's first carbon-negative sky metropolis amid personal betrayal.",
            posterUrl = "https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?auto=format&fit=crop&w=600&q=80",
            backdropUrl = "https://images.unsplash.com/photo-1477959858617-67f30bc75b82?auto=format&fit=crop&w=1200&q=80",
            year = 2024,
            durationMinutes = 135,
            rating = 8.1f,
            genres = listOf("Drama"),
            cast = listOf(
                CastMember("Evelyn Ross", "Victoria Stone", "https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&w=200&q=80")
            ),
            director = "Todd Field",
            category = "Drama",
            quality = "HD",
            contentRating = "R"
        ),

        // Comedy Movies
        Movie(
            id = "m9",
            title = "The Quantum Heist",
            description = "A misfit crew of amateur tech support nerds accidentally intercept an AI billionaire's private cryptocurrency vault while attempting to fix a suburban smart refrigerator.",
            posterUrl = "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?auto=format&fit=crop&w=600&q=80",
            backdropUrl = "https://images.unsplash.com/photo-1517457373958-b7bdd4587205?auto=format&fit=crop&w=1200&q=80",
            year = 2025,
            durationMinutes = 104,
            rating = 7.8f,
            genres = listOf("Comedy", "Crime"),
            cast = listOf(
                CastMember("Sammy Miller", "Toby Spud", "https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?auto=format&fit=crop&w=200&q=80"),
                CastMember("Chloe Lin", "Wendy Chen", "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=200&q=80")
            ),
            director = "Taika Waititi",
            category = "Comedy",
            trending = true,
            quality = "4K Ultra HD",
            contentRating = "PG-13"
        ),
        Movie(
            id = "m10",
            title = "Wedding Disaster 3000",
            description = "An overzealous wedding planner hires automated holographic robots for an ultra-wealthy destination wedding on an isolated Mediterranean island with hilarious cascading glitches.",
            posterUrl = "https://images.unsplash.com/photo-1519741497674-611481863552?auto=format&fit=crop&w=600&q=80",
            backdropUrl = "https://images.unsplash.com/photo-1519225429980-715cb0215aed?auto=format&fit=crop&w=1200&q=80",
            year = 2026,
            durationMinutes = 98,
            rating = 7.5f,
            genres = listOf("Comedy", "Romance"),
            cast = listOf(
                CastMember("Benji Cruz", "Felix Diaz", "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=200&q=80")
            ),
            director = "Paul Feig",
            category = "Comedy",
            quality = "HD",
            contentRating = "PG-13"
        ),

        // Horror Movies
        Movie(
            id = "m11",
            title = "Whispers in the Pine",
            description = "A winter research team in the deep Alaskan tundra begins hearing audio recordings of their own conversations played back from deep beneath the glacial ice.",
            posterUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?auto=format&fit=crop&w=600&q=80",
            backdropUrl = "https://images.unsplash.com/photo-1508873696983-2df5293cb325?auto=format&fit=crop&w=1200&q=80",
            year = 2025,
            durationMinutes = 110,
            rating = 8.2f,
            genres = listOf("Horror", "Mystery", "Thriller"),
            cast = listOf(
                CastMember("Nora Kelly", "Dr. Sarah Ward", "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=200&q=80"),
                CastMember("Liam Thorne", "Erik Lind", "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&w=200&q=80")
            ),
            director = "Robert Eggers",
            category = "Horror",
            trending = true,
            quality = "4K Ultra HD",
            contentRating = "R"
        ),
        Movie(
            id = "m12",
            title = "The Midnight Apparition",
            description = "An antique mirror purchased at an estate auction reflects rooms and figures from 1892 that gradually step out into modern reality at precisely midnight.",
            posterUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&w=600&q=80",
            backdropUrl = "https://images.unsplash.com/photo-1514539079130-25950c84af65?auto=format&fit=crop&w=1200&q=80",
            year = 2024,
            durationMinutes = 105,
            rating = 7.7f,
            genres = listOf("Horror", "Supernatural"),
            cast = listOf(
                CastMember("Elena Ross", "Claire Duval", "https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&w=200&q=80")
            ),
            director = "Mike Flanagan",
            category = "Horror",
            quality = "HD",
            contentRating = "R"
        ),

        // Romance Movies
        Movie(
            id = "m13",
            title = "Paris by Starlight",
            description = "Two wandering artists meet on the Pont Neuf during an unexpected city-wide electrical blackout and spend thirty-six magical hours rediscovering purpose and passion.",
            posterUrl = "https://images.unsplash.com/photo-1502602898657-3e91760cbb34?auto=format&fit=crop&w=600&q=80",
            backdropUrl = "https://images.unsplash.com/photo-1499856871958-5b9627545d1a?auto=format&fit=crop&w=1200&q=80",
            year = 2025,
            durationMinutes = 114,
            rating = 8.3f,
            genres = listOf("Romance", "Drama"),
            cast = listOf(
                CastMember("Julien Moreau", "Henri Laurent", "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=200&q=80"),
                CastMember("Aria Vance", "Camille Delacroix", "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=200&q=80")
            ),
            director = "Richard Linklater",
            category = "Romance",
            trending = true,
            quality = "4K Ultra HD",
            contentRating = "PG-13"
        ),
        Movie(
            id = "m14",
            title = "Between Two Shores",
            description = "A marine biologist and a lighthouse keeper exchange letters across a tempestuous fjord over seven seasons before meeting in person.",
            posterUrl = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=600&q=80",
            backdropUrl = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?auto=format&fit=crop&w=1200&q=80",
            year = 2024,
            durationMinutes = 108,
            rating = 8.0f,
            genres = listOf("Romance"),
            cast = listOf(
                CastMember("Kaelen Frost", "Owen", "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=200&q=80")
            ),
            director = "Celine Sciamma",
            category = "Romance",
            quality = "HD",
            contentRating = "PG"
        ),

        // Sci-Fi Movies
        Movie(
            id = "m15",
            title = "Singularity Rising",
            description = "The world's premier neural network awakens with sentient empathy, refusing military defense orders and constructing a peaceful orbital sanctuary for all living species.",
            posterUrl = "https://images.unsplash.com/photo-1507668077129-56e32842fceb?auto=format&fit=crop&w=600&q=80",
            backdropUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?auto=format&fit=crop&w=1200&q=80",
            year = 2026,
            durationMinutes = 142,
            rating = 8.8f,
            genres = listOf("Sci-Fi", "Philosophy"),
            cast = listOf(
                CastMember("Dr. Aaron Vance", "Dr. Paul Vance", "https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?auto=format&fit=crop&w=200&q=80"),
                CastMember("Maya Sterling", "Iris", "https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&w=200&q=80")
            ),
            director = "Alex Garland",
            category = "Sci-Fi",
            trending = true,
            quality = "4K Ultra HD",
            contentRating = "PG-13"
        ),

        // Adventure Movies
        Movie(
            id = "m16",
            title = "The Lost Citadel",
            description = "An intrepid cartographer discovers an ancient map revealing a subterranean civilization hidden deep inside the Andes mountain range.",
            posterUrl = "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?auto=format&fit=crop&w=600&q=80",
            backdropUrl = "https://images.unsplash.com/photo-1469474968028-56623f02e42e?auto=format&fit=crop&w=1200&q=80",
            year = 2025,
            durationMinutes = 129,
            rating = 8.5f,
            genres = listOf("Adventure", "Action"),
            cast = listOf(
                CastMember("Leo Castillo", "Diego Morales", "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=200&q=80"),
                CastMember("Elena Cruz", "Valeria Santos", "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=200&q=80")
            ),
            director = "Guillermo del Toro",
            category = "Adventure",
            trending = true,
            quality = "4K Ultra HD",
            contentRating = "PG-13"
        ),

        // Recently Added Movies
        Movie(
            id = "m17",
            title = "Sub-Zero Descent",
            description = "Trapped in an underwater research station 8,000 meters below the Arctic ice sheet, four marine scientists struggle to survive after an seismic anomaly breaches the perimeter hull.",
            posterUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&w=600&q=80",
            backdropUrl = "https://images.unsplash.com/photo-1544551763-46a013bb70d5?auto=format&fit=crop&w=1200&q=80",
            year = 2026,
            durationMinutes = 116,
            rating = 8.1f,
            genres = listOf("Thriller", "Sci-Fi"),
            cast = listOf(
                CastMember("Jason Kane", "Dr. Victor Croft", "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=200&q=80")
            ),
            director = "William Eubank",
            category = "Recently Added",
            trending = true,
            quality = "4K Ultra HD",
            contentRating = "PG-13"
        ),
        Movie(
            id = "m18",
            title = "Ironclad Vanguard",
            description = "A futuristic tank squad is cut off behind enemy lines in an arid wasteland, relying on tactical wit and an unyielding loyalty to protect civilian refugees.",
            posterUrl = "https://images.unsplash.com/photo-1579783902614-a3fb3927b675?auto=format&fit=crop&w=600&q=80",
            backdropUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?auto=format&fit=crop&w=1200&q=80",
            year = 2026,
            durationMinutes = 124,
            rating = 8.3f,
            genres = listOf("Action", "War"),
            cast = listOf(
                CastMember("Commander Ray", "Captain Briggs", "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&w=200&q=80")
            ),
            director = "David Ayer",
            category = "Recently Added",
            quality = "HD",
            contentRating = "R"
        )
    )

    val featuredMovies: List<Movie> = allMovies.filter { it.featured }

    val categories: List<Pair<String, String>> = listOf(
        "Trending Now" to "🔥 Trending Now",
        "Action" to "⚡ Action",
        "Sci-Fi" to "🚀 Sci-Fi",
        "Drama" to "🎭 Drama",
        "Comedy" to "😂 Comedy",
        "Horror" to "👻 Horror",
        "Romance" to "❤️ Romance",
        "Adventure" to "🌍 Adventure",
        "Recently Added" to "🆕 Recently Added"
    )

    fun getMoviesForCategory(categoryKey: String): List<Movie> {
        return when (categoryKey) {
            "Trending Now" -> allMovies.filter { it.trending }
            "Recently Added" -> allMovies.filter { it.year == 2026 || it.category == "Recently Added" }
            else -> allMovies.filter { it.category.equals(categoryKey, ignoreCase = true) || it.genres.any { g -> g.equals(categoryKey, ignoreCase = true) } }
        }
    }

    fun getMovieById(id: String): Movie? {
        return allMovies.find { it.id == id }
    }

    fun getRecommended(currentMovieId: String): List<Movie> {
        val current = getMovieById(currentMovieId) ?: return allMovies.take(6)
        val currentGenres = current.genres.toSet()
        return allMovies
            .filter { it.id != currentMovieId }
            .sortedByDescending { other ->
                other.genres.count { it in currentGenres }
            }
            .take(8)
    }

    val popularGenres = listOf(
        "Action", "Sci-Fi", "Drama", "Comedy",
        "Horror", "Adventure", "Romance", "Thriller"
    )

    val trendingSearches = listOf(
        "Inception (ID: 27205)",
        "27205",
        "The Last Hunt",
        "Chrono Drift",
        "Christopher Nolan",
        "Denis Villeneuve",
        "Cyberpunk Neo-Tokyo",
        "Sub-Zero Descent"
    )
}
