import { Env } from '../types';

interface RateLimitConfig {
  maxRequests: number;
  windowSeconds: number;
}

// In-memory sliding window cache for fast edge rate limiting
const memoryLimiter = new Map<string, { count: number; resetAt: number }>();

export async function checkRateLimit(
  key: string,
  config: RateLimitConfig,
  env?: Env
): Promise<{ allowed: boolean; remaining: number; reset: number }> {
  const now = Date.now();
  const windowMs = config.windowSeconds * 1000;

  // Cleanup old entries
  if (memoryLimiter.size > 10000) {
    for (const [k, v] of memoryLimiter.entries()) {
      if (v.resetAt < now) memoryLimiter.delete(k);
    }
  }

  const record = memoryLimiter.get(key);

  if (!record || record.resetAt < now) {
    memoryLimiter.set(key, {
      count: 1,
      resetAt: now + windowMs
    });
    return {
      allowed: true,
      remaining: config.maxRequests - 1,
      reset: Math.ceil((now + windowMs) / 1000)
    };
  }

  if (record.count >= config.maxRequests) {
    return {
      allowed: false,
      remaining: 0,
      reset: Math.ceil(record.resetAt / 1000)
    };
  }

  record.count += 1;
  return {
    allowed: true,
    remaining: config.maxRequests - record.count,
    reset: Math.ceil(record.resetAt / 1000)
  };
}

export function getClientIp(request: Request): string {
  return (
    request.headers.get('CF-Connecting-IP') ||
    request.headers.get('X-Forwarded-For')?.split(',')[0].trim() ||
    '127.0.0.1'
  );
}
