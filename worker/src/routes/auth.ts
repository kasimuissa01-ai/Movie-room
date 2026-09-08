import { Env, UserPayload } from '../types';
import { signJwt, authenticateRequest } from '../middleware/auth';
import { createJsonResponse, createErrorResponse } from '../middleware/cache';
import { checkRateLimit, getClientIp } from '../middleware/rateLimiter';

export async function handleLogin(request: Request, env: Env): Promise<Response> {
  const ip = getClientIp(request);
  // Max 10 login attempts per minute
  const rate = await checkRateLimit(`login:${ip}`, { maxRequests: 10, windowSeconds: 60 }, env);
  if (!rate.allowed) {
    return createErrorResponse('Too many login attempts. Please try again in 1 minute.', 429);
  }

  try {
    const body: any = await request.json();
    const email = body.email || `user_${Math.random().toString(36).substring(2, 9)}@cinestream.app`;
    const name = body.name || 'CineStream VIP Member';

    const userId = `usr_${Math.abs(hashString(email))}`;
    const payload: UserPayload = {
      userId,
      email,
      name,
      role: 'user'
    };

    const token = await signJwt(payload, env.JWT_SECRET);

    return createJsonResponse({
      success: true,
      token,
      user: payload
    });
  } catch (e) {
    return createErrorResponse('Invalid login credentials format', 400);
  }
}

export async function handleAdminLogin(request: Request, env: Env): Promise<Response> {
  const ip = getClientIp(request);
  // Max 5 admin attempts per minute
  const rate = await checkRateLimit(`admin_login:${ip}`, { maxRequests: 5, windowSeconds: 60 }, env);
  if (!rate.allowed) {
    return createErrorResponse('Too many attempts. Admin portal locked for 1 minute.', 429);
  }

  try {
    const body: any = await request.json();
    const passcode = (body.passcode || body.password || '').trim();
    const expected = (env.ADMIN_API_KEY || 'admin2026').trim();

    if (passcode !== expected) {
      return createErrorResponse('Invalid admin passcode.', 403);
    }

    const payload: UserPayload = {
      userId: 'admin_root',
      email: 'admin@cinestream.internal',
      name: 'System Administrator',
      role: 'admin'
    };

    const token = await signJwt(payload, env.JWT_SECRET, 60 * 60 * 12); // 12 hours

    return createJsonResponse({
      success: true,
      token,
      user: payload
    });
  } catch (e) {
    return createErrorResponse('Invalid request format', 400);
  }
}

export async function handleGetMe(request: Request, env: Env): Promise<Response> {
  const user = await authenticateRequest(request, env);
  if (!user) {
    return createErrorResponse('Unauthorized', 401);
  }
  return createJsonResponse({ success: true, user });
}

function hashString(str: string): number {
  let hash = 0;
  for (let i = 0; i < str.length; i++) {
    hash = (hash << 5) - hash + str.charCodeAt(i);
    hash |= 0;
  }
  return hash;
}
