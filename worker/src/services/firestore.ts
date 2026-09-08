import { Env, MovieDto } from '../types';

export class FirestoreService {
  private projectId = 'movieroom-334fb';
  private baseUrl = `https://firestore.googleapis.com/v1/projects/${this.projectId}/databases/(default)/documents`;

  constructor(env: Env) {
    // Can use env secrets if needed for service account auth, or public API key
  }

  async getPublishedMovies(): Promise<MovieDto[]> {
    try {
      const url = `${this.baseUrl}/movies`;
      const response = await fetch(url);
      if (!response.ok) {
        return [];
      }
      const data: any = await response.json();
      const documents = data.documents || [];
      const movies: MovieDto[] = [];

      for (const doc of documents) {
        const fields = doc.fields || {};
        const movie = this.parseFirestoreDocument(doc.name, fields);
        if (movie && movie.isPublished) {
          movies.push(movie);
        }
      }
      return movies;
    } catch (e) {
      console.error('Failed to fetch movies from Firestore:', e);
      return [];
    }
  }

  async getMovieById(id: string): Promise<MovieDto | null> {
    try {
      const url = `${this.baseUrl}/movies/${id}`;
      const response = await fetch(url);
      if (!response.ok) {
        return null;
      }
      const doc: any = await response.json();
      return this.parseFirestoreDocument(doc.name, doc.fields || {});
    } catch (e) {
      console.error(`Failed to fetch movie ${id} from Firestore:`, e);
      return null;
    }
  }

  async saveMovie(id: string, movieData: any): Promise<boolean> {
    try {
      const url = `${this.baseUrl}/movies/${id}`;
      const firestoreFields = this.toFirestoreFields(movieData);

      const response = await fetch(url, {
        method: 'PATCH',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          fields: firestoreFields
        })
      });

      return response.ok;
    } catch (e) {
      console.error(`Failed to save movie ${id} to Firestore:`, e);
      return false;
    }
  }

  async deleteMovie(id: string): Promise<boolean> {
    try {
      const url = `${this.baseUrl}/movies/${id}`;
      const response = await fetch(url, {
        method: 'DELETE'
      });
      return response.ok;
    } catch (e) {
      console.error(`Failed to delete movie ${id} from Firestore:`, e);
      return false;
    }
  }

  private parseFirestoreDocument(docName: string, fields: any): MovieDto & { isPublished?: boolean } {
    const id = docName ? docName.split('/').pop() || '' : '';
    
    const getString = (key: string) => fields[key]?.stringValue || '';
    const getNumber = (key: string) => {
      if (fields[key]?.doubleValue !== undefined) return fields[key].doubleValue;
      if (fields[key]?.integerValue !== undefined) return parseFloat(fields[key].integerValue);
      return 0;
    };
    const getBoolean = (key: string) => fields[key]?.booleanValue ?? false;
    const getArray = (key: string) => {
      const arr = fields[key]?.arrayValue?.values;
      if (!arr || !Array.isArray(arr)) return [];
      return arr.map((v: any) => v.stringValue || '').filter(Boolean);
    };

    const videoKey = getString('video_key');
    const trailerKey = getString('trailer_key');
    const poster = getString('poster') || getString('posterUrl');
    const backdrop = getString('backdrop') || getString('backdropUrl');

    const videoUrl = videoKey ? `https://pub-cinestream.r2.dev/${videoKey}` : getString('videoUrl');
    const trailerUrl = trailerKey ? `https://pub-cinestream.r2.dev/${trailerKey}` : getString('trailerUrl');

    const isPublished = getBoolean('published');

    return {
      id,
      tmdbId: fields['tmdb_id']?.integerValue ? parseInt(fields['tmdb_id'].integerValue, 10) : undefined,
      title: getString('title') || 'Untitled',
      overview: getString('description') || getString('overview') || '',
      posterUrl: poster || 'https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=800&q=80',
      backdropUrl: backdrop || poster || '',
      videoUrl: videoUrl || 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4',
      releaseYear: getString('release_date') || getString('releaseYear') || '2026',
      rating: getNumber('rating') || 8.0,
      durationMinutes: getNumber('runtime') || getNumber('durationMinutes') || 120,
      genres: getArray('genres').length > 0 ? getArray('genres') : ['Action'],
      category: getString('category') || 'Action',
      isTrending: true,
      isPopular: true,
      isFeatured: false,
      isPublished
    };
  }

  private toFirestoreFields(data: any): any {
    const fields: any = {};

    const setString = (k: string, v: any) => { if (v !== undefined && v !== null) fields[k] = { stringValue: String(v) }; };
    const setInteger = (k: string, v: any) => { if (v !== undefined && v !== null) fields[k] = { integerValue: String(Math.floor(Number(v))) }; };
    const setDouble = (k: string, v: any) => { if (v !== undefined && v !== null) fields[k] = { doubleValue: Number(v) }; };
    const setBoolean = (k: string, v: any) => { if (v !== undefined && v !== null) fields[k] = { booleanValue: Boolean(v) }; };
    const setArray = (k: string, arr: any[]) => {
      if (Array.isArray(arr)) {
        fields[k] = {
          arrayValue: {
            values: arr.map(item => ({ stringValue: String(item) }))
          }
        };
      }
    };

    setString('title', data.title);
    setString('description', data.overview || data.description);
    setString('poster', data.posterUrl || data.poster);
    setString('backdrop', data.backdropUrl || data.backdrop);
    setString('release_date', data.releaseYear || data.release_date);
    setDouble('rating', data.rating);
    setInteger('runtime', data.durationMinutes || data.runtime);
    setArray('genres', data.genres);
    setString('category', data.category || 'Action');
    if (data.tmdbId || data.tmdb_id) {
      setInteger('tmdb_id', data.tmdbId || data.tmdb_id);
    }
    setString('video_key', data.videoKey || data.video_key || '');
    setString('trailer_key', data.trailerKey || data.trailer_key || '');
    setBoolean('published', data.published ?? false);
    setString('created_at', data.createdAt || new Date().toISOString());

    return fields;
  }
}
