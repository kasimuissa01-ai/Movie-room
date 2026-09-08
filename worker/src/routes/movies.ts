import { Env, MovieDto } from '../types';
import { TmdbService } from '../services/tmdb';
import { FirestoreService } from '../services/firestore';
import { createJsonResponse, createErrorResponse, CACHE_PUBLIC_METADATA, CACHE_SEARCH } from '../middleware/cache';
import { checkRateLimit, getClientIp } from '../middleware/rateLimiter';

// High-speed in-memory & default fallback catalog
const FALLBACK_MOVIES: MovieDto[] = [
  {
    id: "27205",
    tmdbId: 27205,
    title: "Inception",
    overview: "A thief who steals corporate secrets through the use of dream-sharing technology is given the inverse task of planting an idea into the mind of a C.E.O., but his tragic past may doom the project and his team to disaster.",
    posterUrl: "https://image.tmdb.org/t/p/w780/oYuLEt3zVCKq57qu2F8dT7NIa6f.jpg",
    backdropUrl: "https://image.tmdb.org/t/p/w1280/8ZTVqvKDQ8emSGUEMjsS4yHAwrp.jpg",
    videoUrl: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
    releaseYear: "2010",
    rating: 8.8,
    durationMinutes: 148,
    genres: ["Action", "Sci-Fi", "Adventure"],
    category: "Sci-Fi",
    isTrending: true,
    isPopular: true,
    isFeatured: true
  },
  {
    id: "157336",
    tmdbId: 157336,
    title: "Interstellar",
    overview: "When Earth becomes uninhabitable in the future, a farmer and ex-NASA pilot, Joseph Cooper, is tasked to pilot a spacecraft, along with a team of researchers, to find a new planet for humans.",
    posterUrl: "https://image.tmdb.org/t/p/w780/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg",
    backdropUrl: "https://image.tmdb.org/t/p/w1280/xJHokMbljvjADYdit5fK5VQsXEG.jpg",
    videoUrl: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
    releaseYear: "2014",
    rating: 8.7,
    durationMinutes: 169,
    genres: ["Sci-Fi", "Drama", "Adventure"],
    category: "Sci-Fi",
    isTrending: true,
    isPopular: true,
    isFeatured: true
  },
  {
    id: "872585",
    tmdbId: 872585,
    title: "Oppenheimer",
    overview: "The story of American scientist J. Robert Oppenheimer and his role in the development of the atomic bomb during the Manhattan Project in World War II.",
    posterUrl: "https://image.tmdb.org/t/p/w780/8Gxv8gSFCU0XGDykEGv7zR1n2ua.jpg",
    backdropUrl: "https://image.tmdb.org/t/p/w1280/rLb2cw69rPQUQ9G6qoenvTNQkcp.jpg",
    videoUrl: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
    releaseYear: "2023",
    rating: 8.9,
    durationMinutes: 180,
    genres: ["Drama", "History", "Biography"],
    category: "Drama",
    isTrending: true,
    isPopular: true,
    isFeatured: true
  },
  {
    id: "693134",
    tmdbId: 693134,
    title: "Dune: Part Two",
    overview: "Paul Atreides unites with Chani and the Fremen while seeking revenge against the conspirators who destroyed his family.",
    posterUrl: "https://image.tmdb.org/t/p/w780/1pdfLvkbY9ohJlCjQH2CZjjYVvJ.jpg",
    backdropUrl: "https://image.tmdb.org/t/p/w1280/xOMo8BRK7PfcJv9JCnx7s5hj0x2.jpg",
    videoUrl: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
    releaseYear: "2024",
    rating: 8.6,
    durationMinutes: 166,
    genres: ["Sci-Fi", "Adventure", "Action"],
    category: "Sci-Fi",
    isTrending: true,
    isPopular: true,
    isFeatured: true
  },
  {
    id: "155",
    tmdbId: 155,
    title: "The Dark Knight",
    overview: "When the menace known as the Joker wreaks havoc and chaos on the people of Gotham, Batman must accept one of the greatest psychological and physical tests of his ability to fight injustice.",
    posterUrl: "https://image.tmdb.org/t/p/w780/qJ2tW6WMUDux911r6m7haRef0WH.jpg",
    backdropUrl: "https://image.tmdb.org/t/p/w1280/nMKdUUepR0i5zn0y1T4CsSB5chy.jpg",
    videoUrl: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
    releaseYear: "2008",
    rating: 9.0,
    durationMinutes: 152,
    genres: ["Action", "Crime", "Drama"],
    category: "Action",
    isTrending: true,
    isPopular: true,
    isFeatured: false
  },
  {
    id: "558449",
    tmdbId: 558449,
    title: "Gladiator II",
    overview: "Years after witnessing the death of Maximus, Lucius must enter the Colosseum after his home is conquered by tyrannical Emperors.",
    posterUrl: "https://image.tmdb.org/t/p/w780/2cxhvwyEwRlysAmRH4iodkvo0z5.jpg",
    backdropUrl: "https://image.tmdb.org/t/p/w1280/euYIWhGv2Nzxm5ChwmOSIrjrZh.jpg",
    videoUrl: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
    releaseYear: "2024",
    rating: 8.2,
    durationMinutes: 148,
    genres: ["Action", "Adventure", "Drama"],
    category: "Action",
    isTrending: true,
    isPopular: true,
    isFeatured: false
  },
  {
    id: "299534",
    tmdbId: 299534,
    title: "Avengers: Endgame",
    overview: "After the devastating events of Infinity War, the universe is in ruins. With the help of remaining allies, the Avengers assemble once more in order to reverse Thanos' actions and restore balance to the universe.",
    posterUrl: "https://image.tmdb.org/t/p/w780/or06FN3Dka5tukK1e9sl16pB3iy.jpg",
    backdropUrl: "https://image.tmdb.org/t/p/w1280/7RyHsO4yDXtBv1zUU3mTpHeQ0d5.jpg",
    videoUrl: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
    releaseYear: "2019",
    rating: 8.4,
    durationMinutes: 181,
    genres: ["Action", "Sci-Fi", "Adventure"],
    category: "Action",
    isTrending: true,
    isPopular: true,
    isFeatured: false
  },
  {
    id: "361743",
    tmdbId: 361743,
    title: "Top Gun: Maverick",
    overview: "After more than thirty years of service as one of the Navy's top aviators, Pete 'Maverick' Mitchell is where he belongs, pushing the envelope as a courageous test pilot.",
    posterUrl: "https://image.tmdb.org/t/p/w780/62HCnUTziyWcpDaBO2i1DX17ljH.jpg",
    backdropUrl: "https://image.tmdb.org/t/p/w1280/AaV1YIdWKnjAIAOe8UUKBFm327v.jpg",
    videoUrl: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
    releaseYear: "2022",
    rating: 8.3,
    durationMinutes: 130,
    genres: ["Action", "Drama"],
    category: "Action",
    isTrending: true,
    isPopular: true,
    isFeatured: false
  },
  {
    id: "324857",
    tmdbId: 324857,
    title: "Spider-Man: Into the Spider-Verse",
    overview: "Teen Miles Morales becomes the new Spider-Man and joins other Spider-Heroes from parallel dimensions to stop a threat to all reality.",
    posterUrl: "https://image.tmdb.org/t/p/w780/iiZZdoQBEYBv6id8su7ImL0oCbD.jpg",
    backdropUrl: "https://image.tmdb.org/t/p/w1280/7d6EZ0rKnTVz39vCG4PTBp07dm5.jpg",
    videoUrl: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
    releaseYear: "2018",
    rating: 8.4,
    durationMinutes: 117,
    genres: ["Animation", "Action", "Adventure", "Sci-Fi"],
    category: "Animation",
    isTrending: true,
    isPopular: true,
    isFeatured: false
  }
];

export async function handleTrending(request: Request, env: Env): Promise<Response> {
  const firestore = new FirestoreService(env);
  const firestoreMovies = await firestore.getPublishedMovies();

  const tmdb = new TmdbService(env);
  const tmdbMovies = await tmdb.getTrending();
  const baseMovies = tmdbMovies.length > 0 ? tmdbMovies : FALLBACK_MOVIES;

  const result = [...firestoreMovies, ...baseMovies];
  return createJsonResponse({ success: true, count: result.length, data: result }, 200, CACHE_PUBLIC_METADATA);
}

export async function handlePopular(request: Request, env: Env): Promise<Response> {
  const firestore = new FirestoreService(env);
  const firestoreMovies = await firestore.getPublishedMovies();

  const tmdb = new TmdbService(env);
  const tmdbMovies = await tmdb.getPopular();
  const baseMovies = tmdbMovies.length > 0 ? tmdbMovies : FALLBACK_MOVIES;

  const result = [...firestoreMovies, ...baseMovies];
  return createJsonResponse({ success: true, count: result.length, data: result }, 200, CACHE_PUBLIC_METADATA);
}

export async function handleNowPlaying(request: Request, env: Env): Promise<Response> {
  const firestore = new FirestoreService(env);
  const firestoreMovies = await firestore.getPublishedMovies();

  const tmdb = new TmdbService(env);
  const tmdbMovies = await tmdb.getNowPlaying();
  const baseMovies = tmdbMovies.length > 0 ? tmdbMovies : FALLBACK_MOVIES;

  const result = [...firestoreMovies, ...baseMovies];
  return createJsonResponse({ success: true, count: result.length, data: result }, 200, CACHE_PUBLIC_METADATA);
}

export async function handleUpcoming(request: Request, env: Env): Promise<Response> {
  const firestore = new FirestoreService(env);
  const firestoreMovies = await firestore.getPublishedMovies();

  const tmdb = new TmdbService(env);
  const tmdbMovies = await tmdb.getUpcoming();
  const baseMovies = tmdbMovies.length > 0 ? tmdbMovies : FALLBACK_MOVIES;

  const result = [...firestoreMovies, ...baseMovies];
  return createJsonResponse({ success: true, count: result.length, data: result }, 200, CACHE_PUBLIC_METADATA);
}

export async function handleCategory(request: Request, env: Env, categoryName: string, genreId: number): Promise<Response> {
  const firestore = new FirestoreService(env);
  const firestoreMovies = await firestore.getPublishedMovies();
  const matchingFirestore = firestoreMovies.filter(m => m.category.toLowerCase() === categoryName.toLowerCase() || m.genres.some(g => g.toLowerCase() === categoryName.toLowerCase()));

  const tmdb = new TmdbService(env);
  const tmdbMovies = await tmdb.getByCategory(genreId, categoryName);
  const baseMovies = tmdbMovies.length > 0 ? tmdbMovies : FALLBACK_MOVIES.filter(m => m.genres.includes(categoryName));

  const result = [...matchingFirestore, ...baseMovies];
  return createJsonResponse({ success: true, category: categoryName, count: result.length, data: result }, 200, CACHE_PUBLIC_METADATA);
}

export async function handleSearch(request: Request, env: Env): Promise<Response> {
  const ip = getClientIp(request);
  const rate = await checkRateLimit(`search:${ip}`, { maxRequests: 30, windowSeconds: 60 }, env);
  if (!rate.allowed) {
    return createErrorResponse('Too many search requests. Please slow down.', 429);
  }

  const url = new URL(request.url);
  const query = url.searchParams.get('query') || url.searchParams.get('q') || '';
  if (!query.trim()) {
    return createJsonResponse({ success: true, count: 0, data: [] });
  }

  const firestore = new FirestoreService(env);
  const firestoreMovies = await firestore.getPublishedMovies();
  const matchingFirestore = firestoreMovies.filter(m => m.title.toLowerCase().includes(query.toLowerCase()) || m.overview.toLowerCase().includes(query.toLowerCase()));

  const tmdb = new TmdbService(env);
  const tmdbMovies = await tmdb.searchMovies(query);
  const baseMovies = tmdbMovies.length > 0 ? tmdbMovies : FALLBACK_MOVIES.filter(m => m.title.toLowerCase().includes(query.toLowerCase()));

  const result = [...matchingFirestore, ...baseMovies];
  return createJsonResponse({ success: true, query, count: result.length, data: result }, 200, CACHE_SEARCH);
}

export async function handleGetMovieById(request: Request, env: Env, id: string): Promise<Response> {
  const firestore = new FirestoreService(env);
  const firestoreMovie = await firestore.getMovieById(id);
  if (firestoreMovie) {
    return createJsonResponse({ success: true, data: firestoreMovie }, 200, CACHE_PUBLIC_METADATA);
  }

  const cleanId = id.replace(/^tmdb_/, '');
  const tmdbId = parseInt(cleanId, 10);
  if (!isNaN(tmdbId)) {
    const tmdb = new TmdbService(env);
    const movie = await tmdb.getMovieDetails(tmdbId);
    if (movie) {
      return createJsonResponse({ success: true, data: movie }, 200, CACHE_PUBLIC_METADATA);
    }
  }

  const found = FALLBACK_MOVIES.find(m => m.id === id || m.tmdbId?.toString() === cleanId);
  if (found) {
    return createJsonResponse({ success: true, data: found }, 200, CACHE_PUBLIC_METADATA);
  }

  return createErrorResponse('Movie not found', 404);
}

export async function handleGetRecommendations(request: Request, env: Env, id: string): Promise<Response> {
  const firestore = new FirestoreService(env);
  const firestoreMovies = await firestore.getPublishedMovies();

  const tmdbId = id.startsWith('tmdb_') ? parseInt(id.replace('tmdb_', ''), 10) : parseInt(id, 10);
  if (!isNaN(tmdbId)) {
    const tmdb = new TmdbService(env);
    const recs = await tmdb.getRecommendations(tmdbId);
    if (recs.length > 0) {
      const result = [...firestoreMovies.filter(m => m.id !== id), ...recs];
      return createJsonResponse({ success: true, count: result.length, data: result }, 200, CACHE_PUBLIC_METADATA);
    }
  }
  const result = [...firestoreMovies, ...FALLBACK_MOVIES];
  return createJsonResponse({ success: true, count: result.length, data: result }, 200, CACHE_PUBLIC_METADATA);
}
