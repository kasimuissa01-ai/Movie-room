import { Env, MovieDto } from '../types';

const TMDB_BASE_URL = 'https://api.themoviedb.org/3';
const IMAGE_BASE_URL = 'https://image.tmdb.org/t/p/w780';
const BACKDROP_BASE_URL = 'https://image.tmdb.org/t/p/w1280';

const GENRE_MAP: Record<number, string> = {
  28: 'Action',
  12: 'Adventure',
  16: 'Animation',
  35: 'Comedy',
  80: 'Crime',
  99: 'Documentary',
  18: 'Drama',
  10751: 'Family',
  14: 'Fantasy',
  36: 'History',
  27: 'Horror',
  10402: 'Music',
  9648: 'Mystery',
  10749: 'Romance',
  878: 'Sci-Fi',
  10770: 'TV Movie',
  53: 'Thriller',
  10752: 'War',
  37: 'Western'
};

export class TmdbService {
  private apiKey: string;

  constructor(env: Env) {
    this.apiKey = env.TMDB_API_KEY || '';
  }

  private getAuthHeaders(): HeadersInit {
    if (this.apiKey.startsWith('ey')) {
      return {
        'Authorization': `Bearer ${this.apiKey}`,
        'Accept': 'application/json'
      };
    }
    return {
      'Accept': 'application/json'
    };
  }

  private buildUrl(path: string, params: Record<string, string> = {}): string {
    const url = new URL(`${TMDB_BASE_URL}${path}`);
    if (!this.apiKey.startsWith('ey') && this.apiKey) {
      url.searchParams.set('api_key', this.apiKey);
    }
    for (const [k, v] of Object.entries(params)) {
      url.searchParams.set(k, v);
    }
    return url.toString();
  }

  private mapTmdbMovie(item: any, categoryOverride?: string): MovieDto {
    const genres = (item.genre_ids || item.genres || []).map((g: any) => {
      if (typeof g === 'number') return GENRE_MAP[g] || 'Movie';
      return g.name || 'Movie';
    });

    const releaseYear = item.release_date ? item.release_date.split('-')[0] : '2025';
    const primaryGenre = genres.length > 0 ? genres[0] : (categoryOverride || 'Trending');

    return {
      id: `tmdb_${item.id}`,
      tmdbId: item.id,
      title: item.title || item.name || 'Untitled Movie',
      overview: item.overview || 'No description available.',
      posterUrl: item.poster_path ? `${IMAGE_BASE_URL}${item.poster_path}` : 'https://images.unsplash.com/photo-1489599849927-2ee91cede3ba',
      backdropUrl: item.backdrop_path ? `${BACKDROP_BASE_URL}${item.backdrop_path}` : (item.poster_path ? `${IMAGE_BASE_URL}${item.poster_path}` : ''),
      videoUrl: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4',
      releaseYear: releaseYear,
      rating: item.vote_average ? Math.round(item.vote_average * 10) / 10 : 7.5,
      durationMinutes: item.runtime || 120,
      genres: genres.length > 0 ? genres : [primaryGenre],
      category: categoryOverride || primaryGenre,
      isTrending: true,
      isPopular: (item.popularity || 0) > 50,
      isFeatured: (item.vote_average || 0) > 7.5
    };
  }

  async getTrending(): Promise<MovieDto[]> {
    if (!this.apiKey || this.apiKey.startsWith('YOUR_')) return [];
    try {
      const res = await fetch(this.buildUrl('/trending/movie/day'), {
        headers: this.getAuthHeaders()
      });
      if (!res.ok) return [];
      const data: any = await res.json();
      return (data.results || []).map((m: any) => this.mapTmdbMovie(m, 'Trending'));
    } catch (e) {
      console.error('TMDB trending error:', e);
      return [];
    }
  }

  async getPopular(): Promise<MovieDto[]> {
    if (!this.apiKey || this.apiKey.startsWith('YOUR_')) return [];
    try {
      const res = await fetch(this.buildUrl('/movie/popular'), {
        headers: this.getAuthHeaders()
      });
      if (!res.ok) return [];
      const data: any = await res.json();
      return (data.results || []).map((m: any) => this.mapTmdbMovie(m, 'Popular'));
    } catch (e) {
      console.error('TMDB popular error:', e);
      return [];
    }
  }

  async getNowPlaying(): Promise<MovieDto[]> {
    if (!this.apiKey || this.apiKey.startsWith('YOUR_')) return [];
    try {
      const res = await fetch(this.buildUrl('/movie/now_playing'), {
        headers: this.getAuthHeaders()
      });
      if (!res.ok) return [];
      const data: any = await res.json();
      return (data.results || []).map((m: any) => this.mapTmdbMovie(m, 'Now Playing'));
    } catch (e) {
      console.error('TMDB now playing error:', e);
      return [];
    }
  }

  async getUpcoming(): Promise<MovieDto[]> {
    if (!this.apiKey || this.apiKey.startsWith('YOUR_')) return [];
    try {
      const res = await fetch(this.buildUrl('/movie/upcoming'), {
        headers: this.getAuthHeaders()
      });
      if (!res.ok) return [];
      const data: any = await res.json();
      return (data.results || []).map((m: any) => this.mapTmdbMovie(m, 'Upcoming'));
    } catch (e) {
      console.error('TMDB upcoming error:', e);
      return [];
    }
  }

  async getByCategory(genreId: number, categoryName: string): Promise<MovieDto[]> {
    if (!this.apiKey || this.apiKey.startsWith('YOUR_')) return [];
    try {
      const res = await fetch(this.buildUrl('/discover/movie', {
        with_genres: genreId.toString(),
        sort_by: 'popularity.desc'
      }), {
        headers: this.getAuthHeaders()
      });
      if (!res.ok) return [];
      const data: any = await res.json();
      return (data.results || []).map((m: any) => this.mapTmdbMovie(m, categoryName));
    } catch (e) {
      console.error('TMDB category error:', e);
      return [];
    }
  }

  async searchMovies(query: string): Promise<MovieDto[]> {
    if (!this.apiKey || this.apiKey.startsWith('YOUR_') || !query.trim()) return [];
    try {
      const res = await fetch(this.buildUrl('/search/movie', {
        query: query.trim(),
        include_adult: 'false'
      }), {
        headers: this.getAuthHeaders()
      });
      if (!res.ok) return [];
      const data: any = await res.json();
      return (data.results || []).map((m: any) => this.mapTmdbMovie(m));
    } catch (e) {
      console.error('TMDB search error:', e);
      return [];
    }
  }

  async getMovieDetails(tmdbId: number): Promise<MovieDto | null> {
    if (!this.apiKey || this.apiKey.startsWith('YOUR_')) return null;
    try {
      const res = await fetch(this.buildUrl(`/movie/${tmdbId}`), {
        headers: this.getAuthHeaders()
      });
      if (!res.ok) return null;
      const data: any = await res.json();
      return this.mapTmdbMovie(data);
    } catch (e) {
      console.error('TMDB details error:', e);
      return null;
    }
  }

  async getRecommendations(tmdbId: number): Promise<MovieDto[]> {
    if (!this.apiKey || this.apiKey.startsWith('YOUR_')) return [];
    try {
      const res = await fetch(this.buildUrl(`/movie/${tmdbId}/recommendations`), {
        headers: this.getAuthHeaders()
      });
      if (!res.ok) return [];
      const data: any = await res.json();
      return (data.results || []).map((m: any) => this.mapTmdbMovie(m));
    } catch (e) {
      console.error('TMDB recommendations error:', e);
      return [];
    }
  }
}
