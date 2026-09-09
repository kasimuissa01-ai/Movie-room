import { Env, MovieDto } from '../types';
import { authenticateRequest } from '../middleware/auth';
import { R2Service } from '../services/r2';
import { FirestoreService } from '../services/firestore';
import { createJsonResponse, createErrorResponse } from '../middleware/cache';
import { checkRateLimit, getClientIp } from '../middleware/rateLimiter';
import { generatePresignedUrl } from '../services/sigv4';

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
    const contentLengthStr = request.headers.get('content-length') || 'unknown';

    console.log(`[Diagnostic] Upload request received for key: ${cleanKey}, content-type: ${contentType}, content-length: ${contentLengthStr}`);

    const r2 = new R2Service(env);
    if (!request.body) {
      console.error(`[Diagnostic] Empty request.body received for key: ${cleanKey}`);
      return createErrorResponse('Empty upload body received.', 400);
    }

    console.log(`[Diagnostic] Beginning stream transfer to R2 putObject for key: ${cleanKey}...`);
    const publicUrl = await r2.putObject(cleanKey, request.body, contentType);
    console.log(`[Diagnostic] R2 put completed successfully for key: ${cleanKey}`);

    const sizeBytes = contentLengthStr !== 'unknown' ? parseInt(contentLengthStr, 10) : 0;

    return createJsonResponse({
      success: true,
      message: 'File uploaded successfully to Cloudflare R2 storage',
      key: cleanKey,
      url: publicUrl,
      sizeBytes
    });
  } catch (e: any) {
    console.error(`[Diagnostic] R2/Worker upload error:`, e?.message || e);
    return createErrorResponse(`Failed to upload file to Cloudflare storage: ${e?.message || 'Worker error'}`, 500);
  }
}

/**
 * 1. Initiate Direct-to-R2 Upload
 * Validates admin, creates key, returns presigned single PUT or initiates multipart upload.
 */
export async function handleAdminUploadInitiate(request: Request, env: Env): Promise<Response> {
  const user = await authenticateRequest(request, env);
  if (!user || user.role !== 'admin') {
    return createErrorResponse('Forbidden. Admin authorization required.', 403);
  }

  try {
    const body: any = await request.json();
    const filename = (body.filename || `media_${Date.now()}.mp4`).trim();
    const folder = (body.folder || 'movies').trim().toLowerCase(); // 'movies', 'trailers', 'posters'
    const fileSize = parseInt(body.fileSize || '0', 10);
    const contentType = (body.contentType || 'video/mp4').trim();

    if (!['movies', 'trailers', 'posters', 'updates'].includes(folder)) {
      return createErrorResponse(`Invalid storage folder '${folder}'.`, 400);
    }

    const cleanFilename = filename.replace(/[^a-zA-Z0-9._-]/g, '_');
    const cleanKey = `${folder}/${Date.now()}_${cleanFilename}`;

    const accountId = env.R2_ACCOUNT_ID || '693099206806';
    const accessKeyId = env.R2_ACCESS_KEY_ID || env.R2_ACCESS_KEY || '';
    const secretAccessKey = env.R2_SECRET_ACCESS_KEY || env.R2_SECRET_KEY || '';
    const bucketName = env.R2_BUCKET_NAME || 'stories';
    const publicDomain = env.R2_PUBLIC_DOMAIN || 'pub-cinestream.r2.dev';
    const publicUrl = `https://${publicDomain}/${cleanKey}`;

    // For files <= 20 MB (like posters or short clips), use single presigned PUT
    const SINGLE_PUT_THRESHOLD = 20 * 1024 * 1024; // 20 MB

    if (fileSize <= SINGLE_PUT_THRESHOLD || body.mode === 'single') {
      let uploadUrl = '';
      if (accessKeyId && secretAccessKey) {
        uploadUrl = await generatePresignedUrl({
          accessKeyId,
          secretAccessKey,
          accountId,
          bucket: bucketName,
          key: cleanKey,
          method: 'PUT',
          expiresIn: 3600
        });
      }

      return createJsonResponse({
        success: true,
        mode: 'single',
        key: cleanKey,
        uploadUrl,
        publicUrl
      });
    }

    // For large files (>20 MB up to 1 GB+), initialize R2 Multipart Upload natively
    if (!env.MOVIE_BUCKET) {
      return createErrorResponse('R2 bucket binding unavailable on server.', 500);
    }

    const multipart = await env.MOVIE_BUCKET.createMultipartUpload(cleanKey, {
      httpMetadata: { contentType }
    });

    const PART_SIZE = 16 * 1024 * 1024; // 16 MB per chunk
    const totalParts = Math.ceil(fileSize / PART_SIZE);

    return createJsonResponse({
      success: true,
      mode: 'multipart',
      key: cleanKey,
      uploadId: multipart.uploadId,
      partSize: PART_SIZE,
      totalParts,
      publicUrl
    });
  } catch (e: any) {
    console.error('Initiate upload error:', e);
    return createErrorResponse(`Failed to initiate upload: ${e.message}`, 500);
  }
}

/**
 * 2. Sign Multipart Upload Part
 * Generates temporary presigned PUT URL for a specific part number.
 */
export async function handleAdminUploadSignPart(request: Request, env: Env): Promise<Response> {
  const user = await authenticateRequest(request, env);
  if (!user || user.role !== 'admin') {
    return createErrorResponse('Forbidden. Admin authorization required.', 403);
  }

  try {
    const body: any = await request.json();
    const key = (body.key || '').trim();
    const uploadId = (body.uploadId || '').trim();
    const partNumber = parseInt(body.partNumber || '0', 10);

    if (!key || !uploadId || partNumber < 1) {
      return createErrorResponse('Missing key, uploadId, or valid partNumber.', 400);
    }

    const accountId = env.R2_ACCOUNT_ID || '693099206806';
    const accessKeyId = env.R2_ACCESS_KEY_ID || env.R2_ACCESS_KEY || '';
    const secretAccessKey = env.R2_SECRET_ACCESS_KEY || env.R2_SECRET_KEY || '';
    const bucketName = env.R2_BUCKET_NAME || 'stories';

    let uploadUrl = '';
    if (accessKeyId && secretAccessKey) {
      uploadUrl = await generatePresignedUrl({
        accessKeyId,
        secretAccessKey,
        accountId,
        bucket: bucketName,
        key,
        method: 'PUT',
        expiresIn: 3600,
        queryParams: {
          uploadId,
          partNumber: partNumber.toString()
        }
      });
    }

    return createJsonResponse({
      success: true,
      key,
      uploadId,
      partNumber,
      uploadUrl
    });
  } catch (e: any) {
    console.error('Sign part error:', e);
    return createErrorResponse(`Failed to sign upload part: ${e.message}`, 500);
  }
}

/**
 * 3. Complete Direct Upload
 * Finalizes R2 multipart upload and verifies object existence.
 */
export async function handleAdminUploadComplete(request: Request, env: Env): Promise<Response> {
  const user = await authenticateRequest(request, env);
  if (!user || user.role !== 'admin') {
    return createErrorResponse('Forbidden. Admin authorization required.', 403);
  }

  try {
    const body: any = await request.json();
    const key = (body.key || '').trim();
    const uploadId = (body.uploadId || '').trim();
    const parts = Array.isArray(body.parts) ? body.parts : [];

    if (!key) {
      return createErrorResponse('Missing object key.', 400);
    }

    const publicDomain = env.R2_PUBLIC_DOMAIN || 'pub-cinestream.r2.dev';
    const publicUrl = `https://${publicDomain}/${key}`;

    if (uploadId && parts.length > 0) {
      if (!env.MOVIE_BUCKET) {
        return createErrorResponse('R2 bucket binding unavailable.', 500);
      }

      const multipart = env.MOVIE_BUCKET.resumeMultipartUpload(key, uploadId);
      const sortedParts = parts
        .map((p: any) => ({
          partNumber: parseInt(p.partNumber, 10),
          etag: (p.etag || '').replace(/^"/, '').replace(/"$/, '')
        }))
        .sort((a: any, b: any) => a.partNumber - b.partNumber);

      await multipart.complete(sortedParts);
      console.log(`[Diagnostic] Multipart upload completed for key: ${key}`);
    }

    // Verify final object in R2
    if (env.MOVIE_BUCKET) {
      const head = await env.MOVIE_BUCKET.head(key);
      if (!head) {
        return createErrorResponse(`Uploaded object '${key}' not found in R2 storage.`, 404);
      }
    }

    // Final confirmation step: update / finalize object record in Firestore if movieId or movieData supplied
    const movieId = body.movieId ? String(body.movieId).trim() : null;
    const movieData = body.movieData || null;
    const isTrailer = body.isTrailer === true;
    let firestoreUpdated = false;

    if (movieId || movieData) {
      try {
        const firestore = new FirestoreService(env);
        const targetId = movieId || (movieData && movieData.id);
        if (targetId) {
          const existing = await firestore.getMovieById(targetId);
          let payloadToSave: any = existing ? { ...existing } : (movieData || {});
          
          payloadToSave.id = targetId;
          if (isTrailer) {
            payloadToSave.trailerKey = key;
            payloadToSave.trailer_key = key;
          } else {
            payloadToSave.videoKey = key;
            payloadToSave.video_key = key;
          }

          if (movieData) {
            payloadToSave = { ...payloadToSave, ...movieData };
            if (isTrailer) {
              payloadToSave.trailerKey = key;
              payloadToSave.trailer_key = key;
            } else {
              payloadToSave.videoKey = key;
              payloadToSave.video_key = key;
            }
          }

          firestoreUpdated = await firestore.saveMovie(targetId, payloadToSave);
          console.log(`[Diagnostic] Firestore finalized movie ${targetId} with R2 key '${key}': success=${firestoreUpdated}`);
        }
      } catch (fsErr: any) {
        console.error('[Diagnostic] Error updating Firestore in complete upload:', fsErr);
      }
    }

    return createJsonResponse({
      success: true,
      message: 'Direct upload finalized and verified successfully.',
      key,
      url: publicUrl,
      firestoreUpdated
    });
  } catch (e: any) {
    console.error('Complete upload error:', e);
    return createErrorResponse(`Failed to complete upload: ${e.message}`, 500);
  }
}
