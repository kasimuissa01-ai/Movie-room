/**
 * CineStream API Client Service
 *
 * Connects exclusively to our Cloudflare Worker backend.
 * Contains ZERO third-party API keys or Cloudflare R2 credentials.
 */

export interface Movie {
  id: string;
  tmdbId?: number;
  title: string;
  overview: string;
  posterUrl: string;
  backdropUrl: string;
  videoUrl: string;
  releaseYear: string | number;
  rating: number;
  durationMinutes: number;
  genres: string[];
  category: string;
  isTrending: boolean;
  isPopular: boolean;
  isFeatured: boolean;
}

export interface ApiResponse<T> {
  success: boolean;
  count?: number;
  data?: T;
  error?: string;
  message?: string;
}

class CineStreamApiClient {
  private baseUrl: string;
  private authToken: string | null = null;

  constructor(baseUrl?: string) {
    this.baseUrl = (baseUrl || 'https://movie-api.grapherkidd0.workers.dev').replace(/\/+$/, '');
  }

  setBaseUrl(url: string) {
    this.baseUrl = url.replace(/\/+$/, '');
  }

  setAuthToken(token: string | null) {
    this.authToken = token;
  }

  private async request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
    const url = `${this.baseUrl}${endpoint.startsWith('/') ? endpoint : `/${endpoint}`}`;
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      'Accept': 'application/json',
      ...(options.headers as Record<string, string> || {})
    };

    if (this.authToken) {
      headers['Authorization'] = `Bearer ${this.authToken}`;
    }

    const response = await fetch(url, {
      ...options,
      headers
    });

    if (!response.ok) {
      let errorMessage = 'An error occurred while connecting to the server.';
      try {
        const errorJson = await response.json();
        if (errorJson.error) errorMessage = errorJson.error;
      } catch (e) {
        // Fallback
      }
      throw new Error(errorMessage);
    }

    return await response.json() as T;
  }

  // --- Public Catalog ---
  async getTrendingMovies(): Promise<Movie[]> {
    const res = await this.request<ApiResponse<Movie[]>>('/api/movies/trending');
    return res.data || [];
  }

  async getPopularMovies(): Promise<Movie[]> {
    const res = await this.request<ApiResponse<Movie[]>>('/api/movies/popular');
    return res.data || [];
  }

  async getNowPlayingMovies(): Promise<Movie[]> {
    const res = await this.request<ApiResponse<Movie[]>>('/api/movies/now-playing');
    return res.data || [];
  }

  async getUpcomingMovies(): Promise<Movie[]> {
    const res = await this.request<ApiResponse<Movie[]>>('/api/movies/upcoming');
    return res.data || [];
  }

  async getActionMovies(): Promise<Movie[]> {
    const res = await this.request<ApiResponse<Movie[]>>('/api/movies/action');
    return res.data || [];
  }

  async getMoviesByCategory(category: string): Promise<Movie[]> {
    const normalized = category.toLowerCase().replace(/[^a-z0-9]/g, '');
    const res = await this.request<ApiResponse<Movie[]>>(`/api/movies/${normalized}`);
    return res.data || [];
  }

  async searchMovies(query: string): Promise<Movie[]> {
    const encoded = encodeURIComponent(query.trim());
    const res = await this.request<ApiResponse<Movie[]>>(`/api/movies/search?query=${encoded}`);
    return res.data || [];
  }

  async getMovie(id: string): Promise<Movie | null> {
    const cleanId = encodeURIComponent(id);
    const res = await this.request<ApiResponse<Movie>>(`/api/movies/movie/${cleanId}`);
    return res.data || null;
  }

  async getRecommendations(movieId: string): Promise<Movie[]> {
    const cleanId = encodeURIComponent(movieId);
    const res = await this.request<ApiResponse<Movie[]>>(`/api/movies/movie/${cleanId}/recommendations`);
    return res.data || [];
  }

  // --- Authentication ---
  async login(email?: string, name?: string): Promise<{ token: string; user: any }> {
    const res = await this.request<{ success: boolean; token: string; user: any }>('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, name })
    });
    this.setAuthToken(res.token);
    return res;
  }

  async adminLogin(passcode: string): Promise<{ token: string; user: any }> {
    const res = await this.request<{ success: boolean; token: string; user: any }>('/api/auth/admin-login', {
      method: 'POST',
      body: JSON.stringify({ passcode })
    });
    this.setAuthToken(res.token);
    return res;
  }

  // --- User Authenticated Endpoints ---
  async getProfile(): Promise<any> {
    const res = await this.request<ApiResponse<any>>('/api/user/profile');
    return res.data;
  }

  async getWatchlist(): Promise<{ movieId: string; addedAt: number }[]> {
    const res = await this.request<ApiResponse<{ movieId: string; addedAt: number }[]>>('/api/user/watchlist');
    return res.data || [];
  }

  async addToWatchlist(movieId: string): Promise<void> {
    await this.request('/api/user/watchlist', {
      method: 'POST',
      body: JSON.stringify({ movieId })
    });
  }

  async removeFromWatchlist(movieId: string): Promise<void> {
    await this.request(`/api/user/watchlist/${movieId}`, {
      method: 'DELETE'
    });
  }

  async getHistory(): Promise<any[]> {
    const res = await this.request<ApiResponse<any[]>>('/api/user/history');
    return res.data || [];
  }

  async updateHistory(movieId: string, watchedSeconds: number, totalSeconds: number): Promise<void> {
    await this.request('/api/user/history', {
      method: 'POST',
      body: JSON.stringify({
        movieId,
        watchedDurationSeconds: watchedSeconds,
        totalDurationSeconds: totalSeconds
      })
    });
  }

  // --- Admin Endpoints ---
  async createMovie(movie: Partial<Movie>): Promise<Movie> {
    const res = await this.request<ApiResponse<Movie>>('/api/admin/movies', {
      method: 'POST',
      body: JSON.stringify(movie)
    });
    return res.data!;
  }

  async deleteMovie(id: string): Promise<void> {
    await this.request(`/api/admin/movies/${id}`, {
      method: 'DELETE'
    });
  }

  async uploadMedia(filename: string, fileBytes: Blob | ArrayBuffer, contentType: string): Promise<string> {
    const url = `${this.baseUrl}/api/admin/r2/upload?filename=${encodeURIComponent(filename)}`;
    const headers: Record<string, string> = {
      'Content-Type': contentType,
      'Authorization': `Bearer ${this.authToken || ''}`
    };

    const response = await fetch(url, {
      method: 'POST',
      headers,
      body: fileBytes
    });

    if (!response.ok) {
      throw new Error('Failed to upload file to Cloudflare R2 backend.');
    }

    const data: any = await response.json();
    return data.url;
  }
}

export const api = new CineStreamApiClient();
export default api;
