export interface Env {
  // Bindings
  MOVIE_BUCKET?: R2Bucket;
  APP_CACHE_KV?: KVNamespace;

  // Environment Variables (Public)
  ENVIRONMENT?: string;
  API_VERSION?: string;
  CORS_ORIGIN?: string;

  // Secrets (Injected via Cloudflare Worker Secrets)
  TMDB_API_KEY: string;
  JWT_SECRET: string;
  ADMIN_API_KEY?: string;
  DATABASE_URL?: string;
  DATABASE_SERVICE_KEY?: string;
  R2_ACCESS_KEY?: string;
  R2_SECRET_KEY?: string;
}

export interface UserPayload {
  userId: string;
  email: string;
  role: 'user' | 'admin';
  name?: string;
  iat?: number;
  exp?: number;
}

export interface AuthenticatedRequest extends Request {
  user?: UserPayload;
  rateLimitKey?: string;
}

export interface MovieDto {
  id: string;
  tmdbId?: number;
  title: string;
  overview: string;
  posterUrl: string;
  backdropUrl: string;
  videoUrl: string;
  releaseYear: Int32Array | number | string;
  rating: number;
  durationMinutes: number;
  genres: string[];
  category: string;
  isTrending: boolean;
  isPopular: boolean;
  isFeatured: boolean;
}

export interface WatchlistItem {
  movieId: string;
  addedAt: number;
  movie?: MovieDto;
}

export interface WatchHistoryItem {
  movieId: string;
  watchedDurationSeconds: number;
  totalDurationSeconds: number;
  lastWatchedAt: number;
  completed: boolean;
  movie?: MovieDto;
}
