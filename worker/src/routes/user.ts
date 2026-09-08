import { Env, UserPayload, WatchlistItem, WatchHistoryItem } from '../types';
import { authenticateRequest } from '../middleware/auth';
import { createJsonResponse, createErrorResponse } from '../middleware/cache';

// In-memory / KV based storage for user data
const userWatchlists = new Map<string, Set<string>>();
const userHistories = new Map<string, Map<string, WatchHistoryItem>>();

export async function handleUserProfile(request: Request, env: Env): Promise<Response> {
  const user = await authenticateRequest(request, env);
  if (!user) {
    return createErrorResponse('Unauthorized. Invalid or missing authentication token.', 401);
  }

  return createJsonResponse({
    success: true,
    data: {
      userId: user.userId,
      email: user.email,
      name: user.name || 'CineStream User',
      role: user.role,
      isVip: true,
      joinedAt: user.iat ? user.iat * 1000 : Date.now()
    }
  });
}

export async function handleGetWatchlist(request: Request, env: Env): Promise<Response> {
  const user = await authenticateRequest(request, env);
  if (!user) {
    return createErrorResponse('Unauthorized.', 401);
  }

  const list = userWatchlists.get(user.userId) || new Set<string>();
  const movieIds = Array.from(list);

  return createJsonResponse({
    success: true,
    count: movieIds.length,
    data: movieIds.map(id => ({ movieId: id, addedAt: Date.now() }))
  });
}

export async function handleAddToWatchlist(request: Request, env: Env): Promise<Response> {
  const user = await authenticateRequest(request, env);
  if (!user) {
    return createErrorResponse('Unauthorized.', 401);
  }

  try {
    const body: any = await request.json();
    const movieId = body.movieId;
    if (!movieId) {
      return createErrorResponse('Missing movieId in request body.', 400);
    }

    if (!userWatchlists.has(user.userId)) {
      userWatchlists.set(user.userId, new Set<string>());
    }
    userWatchlists.get(user.userId)!.add(movieId);

    return createJsonResponse({
      success: true,
      message: 'Movie added to watchlist',
      movieId
    });
  } catch (e) {
    return createErrorResponse('Invalid request body.', 400);
  }
}

export async function handleRemoveFromWatchlist(request: Request, env: Env, movieId: string): Promise<Response> {
  const user = await authenticateRequest(request, env);
  if (!user) {
    return createErrorResponse('Unauthorized.', 401);
  }

  if (userWatchlists.has(user.userId)) {
    userWatchlists.get(user.userId)!.delete(movieId);
  }

  return createJsonResponse({
    success: true,
    message: 'Movie removed from watchlist',
    movieId
  });
}

export async function handleGetHistory(request: Request, env: Env): Promise<Response> {
  const user = await authenticateRequest(request, env);
  if (!user) {
    return createErrorResponse('Unauthorized.', 401);
  }

  const historyMap = userHistories.get(user.userId) || new Map<string, WatchHistoryItem>();
  const items = Array.from(historyMap.values()).sort((a, b) => b.lastWatchedAt - a.lastWatchedAt);

  return createJsonResponse({
    success: true,
    count: items.length,
    data: items
  });
}

export async function handleUpdateHistory(request: Request, env: Env): Promise<Response> {
  const user = await authenticateRequest(request, env);
  if (!user) {
    return createErrorResponse('Unauthorized.', 401);
  }

  try {
    const body: any = await request.json();
    const { movieId, watchedDurationSeconds, totalDurationSeconds } = body;
    if (!movieId) {
      return createErrorResponse('Missing movieId', 400);
    }

    if (!userHistories.has(user.userId)) {
      userHistories.set(user.userId, new Map<string, WatchHistoryItem>());
    }

    const item: WatchHistoryItem = {
      movieId,
      watchedDurationSeconds: watchedDurationSeconds || 0,
      totalDurationSeconds: totalDurationSeconds || 0,
      lastWatchedAt: Date.now(),
      completed: (watchedDurationSeconds || 0) >= (totalDurationSeconds || 1) * 0.9
    };

    userHistories.get(user.userId)!.set(movieId, item);

    return createJsonResponse({
      success: true,
      data: item
    });
  } catch (e) {
    return createErrorResponse('Invalid request body.', 400);
  }
}
