import { Env, MovieDto } from '../types';
import { authenticateRequest } from '../middleware/auth';
import { R2Service } from '../services/r2';
import { FirestoreService } from '../services/firestore';
import { createJsonResponse, createErrorResponse } from '../middleware/cache';
import { checkRateLimit, getClientIp } from '../middleware/rateLimiter';

export async function handleAdminCreateMovie(request: Request, env: Env): Promise<Response> {
  const user = await authenticateRequest(request, env);
  if (!user || user.role !== 'admin') {
    return createErrorResponse('Forbidden. Admin authorization required.', 403);
  }

  try {
    const body: any = await request.json();
    const cleanTitle = (body.title || 'untitled').trim().toLowerCase().replace(/[^a-z0-9]/g, '_').substring(0, 30);
    const timestamp = Date.now();
    const id = body.id || `movie_${timestamp}_${cleanTitle}`;
    
    const moviePayload = {
      id,
      tmdbId: body.tmdbId || body.tmdb_id,
      title: body.title || 'Untitled',
      overview: body.overview || body.description || '',
      posterUrl: body.posterUrl || body.poster || '',
      backdropUrl: body.backdropUrl || body.backdrop || body.posterUrl || '',
      videoKey: body.videoKey || body.video_key || '',
      trailerKey: body.trailerKey || body.trailer_key || '',
      releaseYear: body.releaseYear || body.release_date || '2026',
      rating: parseFloat(body.rating) || 8.0,
      durationMinutes: parseInt(body.durationMinutes) || parseInt(body.runtime) || 120,
      genres: Array.isArray(body.genres) ? body.genres : [body.category || 'Action'],
      category: body.category || 'Action',
      isTrending: body.isTrending ?? true,
      isPopular: body.isPopular ?? true,
      isFeatured: body.isFeatured ?? false,
      published: body.published ?? false,
      createdAt: new Date().toISOString()
    };

    const firestore = new FirestoreService(env);
    const success = await firestore.saveMovie(id, moviePayload);
    if (!success) {
      return createErrorResponse('Failed to persist movie document to Firestore catalog.', 500);
    }

    return createJsonResponse({
      success: true,
      message: 'Movie successfully created in Firestore catalog',
      data: moviePayload
    });
  } catch (e: any) {
    console.error('Admin create movie error:', e);
    return createErrorResponse(`Failed to create movie: ${e.message || 'Invalid payload.'}`, 400);
  }
}

export async function handleAdminPublishMovie(request: Request, env: Env, id: string): Promise<Response> {
  const user = await authenticateRequest(request, env);
  if (!user || user.role !== 'admin') {
    return createErrorResponse('Forbidden. Admin authorization required.', 403);
  }

  try {
    const firestore = new FirestoreService(env);
    const existing = await firestore.getMovieById(id);
    if (!existing) {
      return createErrorResponse('Movie not found in catalog.', 404);
    }

    const updated = {
      ...existing,
      published: true
    };

    const success = await firestore.saveMovie(id, updated);
    if (!success) {
      return createErrorResponse('Failed to publish movie in Firestore.', 500);
    }

    return createJsonResponse({
      success: true,
      message: `Movie ${id} successfully published`,
      data: updated
    });
  } catch (e: any) {
    return createErrorResponse(`Failed to publish movie: ${e.message}`, 500);
  }
}

export async function handleAdminDeleteMovie(request: Request, env: Env, id: string): Promise<Response> {
  const user = await authenticateRequest(request, env);
  if (!user || user.role !== 'admin') {
    return createErrorResponse('Forbidden. Admin authorization required.', 403);
  }

  try {
    const firestore = new FirestoreService(env);
    await firestore.deleteMovie(id);
    return createJsonResponse({
      success: true,
      message: `Movie ${id} deleted successfully`
    });
  } catch (e: any) {
    return createErrorResponse(`Failed to delete movie: ${e.message}`, 500);
  }
}

export async function handleAdminR2Upload(request: Request, env: Env): Promise<Response> {
  const user = await authenticateRequest(request, env);
  if (!user || user.role !== 'admin') {
    return createErrorResponse('Forbidden. Admin authorization required for uploads.', 403);
  }

  const ip = getClientIp(request);
  const rate = await checkRateLimit(`upload:${ip}`, { maxRequests: 50, windowSeconds: 60 }, env);
  if (!rate.allowed) {
    return createErrorResponse('Upload rate limit reached. Please wait a moment.', 429);
  }

  try {
    const url = new URL(request.url);
    const filename = url.searchParams.get('filename') || `upload_${Date.now()}.mp4`;
    const folder = url.searchParams.get('folder') || 'movies'; // 'movies' or 'trailers'
    const contentType = request.headers.get('Content-Type') || 'application/octet-stream';
    const cleanKey = `${folder}/${Date.now()}_${filename.replace(/[^a-zA-Z0-9._-]/g, '_')}`;

    const r2 = new R2Service(env);
    const arrayBuffer = await request.arrayBuffer();

    const publicUrl = await r2.putObject(cleanKey, arrayBuffer, contentType);

    return createJsonResponse({
      success: true,
      message: 'File uploaded successfully to Cloudflare R2 stories',
      key: cleanKey,
      url: publicUrl,
      sizeBytes: arrayBuffer.byteLength
    });
  } catch (e) {
    console.error('R2 upload failed:', e);
    return createErrorResponse('Failed to upload file to Cloudflare storage.', 500);
  }
}
