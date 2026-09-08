import { Env } from './types';
import {
  handleTrending,
  handlePopular,
  handleNowPlaying,
  handleUpcoming,
  handleCategory,
  handleSearch,
  handleGetMovieById,
  handleGetRecommendations
} from './routes/movies';
import {
  handleUserProfile,
  handleGetWatchlist,
  handleAddToWatchlist,
  handleRemoveFromWatchlist,
  handleGetHistory,
  handleUpdateHistory
} from './routes/user';
import {
  handleLogin,
  handleAdminLogin,
  handleGetMe
} from './routes/auth';
import {
  handleAdminCreateMovie,
  handleAdminPublishMovie,
  handleAdminDeleteMovie,
  handleAdminR2Upload
} from './routes/admin';
import {
  handleGetLatestUpdate,
  handleDownloadUpdateApk
} from './routes/updates';
import { createJsonResponse, createErrorResponse } from './middleware/cache';

export default {
  async fetch(request: Request, env: Env, ctx: ExecutionContext): Promise<Response> {
    const url = new URL(request.url);
    const path = url.pathname;
    const method = request.method.toUpperCase();

    // 1. Handle CORS Preflight OPTIONS requests
    if (method === 'OPTIONS') {
      return new Response(null, {
        status: 204,
        headers: {
          'Access-Control-Allow-Origin': '*',
          'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, OPTIONS',
          'Access-Control-Allow-Headers': 'Content-Type, Authorization, X-Requested-With',
          'Access-Control-Max-Age': '86400'
        }
      });
    }

    try {
      // 2. Health & Info
      if (path === '/' || path === '/api' || path === '/api/health') {
        return createJsonResponse({
          status: 'ok',
          service: 'CineStream Backend API',
          version: '1.0.0',
          timestamp: Date.now()
        });
      }

      // 3. Authentication Routes
      if (path === '/api/auth/login' && method === 'POST') {
        return await handleLogin(request, env);
      }
      if (path === '/api/auth/admin-login' && method === 'POST') {
        return await handleAdminLogin(request, env);
      }
      if (path === '/api/auth/me' && method === 'GET') {
        return await handleGetMe(request, env);
      }

      // 4. Public Movie Catalog Endpoints
      if (path === '/api/movies/trending' && method === 'GET') {
        return await handleTrending(request, env);
      }
      if (path === '/api/movies/popular' && method === 'GET') {
        return await handlePopular(request, env);
      }
      if ((path === '/api/movies/now-playing' || path === '/api/movies/now_playing') && method === 'GET') {
        return await handleNowPlaying(request, env);
      }
      if (path === '/api/movies/upcoming' && method === 'GET') {
        return await handleUpcoming(request, env);
      }
      if (path === '/api/movies/action' && method === 'GET') {
        return await handleCategory(request, env, 'Action', 28);
      }
      if (path === '/api/movies/comedy' && method === 'GET') {
        return await handleCategory(request, env, 'Comedy', 35);
      }
      if (path === '/api/movies/scifi' && method === 'GET') {
        return await handleCategory(request, env, 'Sci-Fi', 878);
      }
      if (path === '/api/movies/horror' && method === 'GET') {
        return await handleCategory(request, env, 'Horror', 27);
      }
      if (path === '/api/movies/drama' && method === 'GET') {
        return await handleCategory(request, env, 'Drama', 18);
      }
      if (path === '/api/movies/search' && method === 'GET') {
        return await handleSearch(request, env);
      }

      // Dynamic movie routes: /api/movies/movie/:id, /api/movies/movie/:id/recommendations, /api/movies/:id, /api/movies/:id/recommendations
      if (path.startsWith('/api/movies/')) {
        let subpath = path.replace('/api/movies/', '');
        if (subpath.startsWith('movie/')) {
          subpath = subpath.replace('movie/', '');
        }
        if (subpath.endsWith('/recommendations') && method === 'GET') {
          const id = subpath.replace('/recommendations', '');
          return await handleGetRecommendations(request, env, id);
        }
        if (method === 'GET' && subpath.length > 0) {
          return await handleGetMovieById(request, env, subpath);
        }
      }

      // 5. Authenticated User Endpoints
      if (path === '/api/user/profile' && method === 'GET') {
        return await handleUserProfile(request, env);
      }
      if (path === '/api/user/watchlist' && method === 'GET') {
        return await handleGetWatchlist(request, env);
      }
      if (path === '/api/user/watchlist' && method === 'POST') {
        return await handleAddToWatchlist(request, env);
      }
      if (path.startsWith('/api/user/watchlist/') && method === 'DELETE') {
        const movieId = path.replace('/api/user/watchlist/', '');
        return await handleRemoveFromWatchlist(request, env, movieId);
      }
      if (path === '/api/user/history' && method === 'GET') {
        return await handleGetHistory(request, env);
      }
      if (path === '/api/user/history' && method === 'POST') {
        return await handleUpdateHistory(request, env);
      }

      // 6. Admin Endpoints
      if (path === '/api/admin/movies' && method === 'POST') {
        return await handleAdminCreateMovie(request, env);
      }
      if (path.startsWith('/api/admin/movies/') && path.endsWith('/publish') && method === 'POST') {
        const id = path.replace('/api/admin/movies/', '').replace('/publish', '');
        return await handleAdminPublishMovie(request, env, id);
      }
      if (path.startsWith('/api/admin/movies/') && method === 'DELETE') {
        const id = path.replace('/api/admin/movies/', '');
        return await handleAdminDeleteMovie(request, env, id);
      }
      if (path === '/api/admin/r2/upload' && method === 'POST') {
        return await handleAdminR2Upload(request, env);
      }

      // 7. App Update Endpoints
      if (path === '/api/updates/latest' && method === 'GET') {
        return await handleGetLatestUpdate(request, env);
      }
      if (path === '/api/updates/download' && method === 'GET') {
        return await handleDownloadUpdateApk(request, env);
      }

      // 404 Route Not Found
      return createErrorResponse(`API route '${method} ${path}' not found.`, 404);
    } catch (err: any) {
      // Catch-all: Log on server, return safe friendly message to client
      console.error('Unhandled Server Error:', err);
      return createErrorResponse('An unexpected internal error occurred. Please try again later.', 500);
    }
  }
};
