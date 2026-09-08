export function createJsonResponse(data: any, status = 200, cacheControl?: string): Response {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json; charset=UTF-8',
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, OPTIONS',
    'Access-Control-Allow-Headers': 'Content-Type, Authorization, X-Requested-With'
  };

  if (cacheControl) {
    headers['Cache-Control'] = cacheControl;
  } else {
    // Default: Private, no-cache for user endpoints
    headers['Cache-Control'] = 'no-store, no-cache, must-revalidate, proxy-revalidate';
  }

  return new Response(JSON.stringify(data), {
    status,
    headers
  });
}

export function createErrorResponse(message: string, status = 500): Response {
  return createJsonResponse(
    {
      success: false,
      error: message
    },
    status,
    'no-store, no-cache'
  );
}

export const CACHE_PUBLIC_METADATA = 'public, max-age=300, s-maxage=1800, stale-while-revalidate=86400';
export const CACHE_SEARCH = 'public, max-age=120, s-maxage=600';
export const CACHE_NO_STORE = 'no-store, no-cache, must-revalidate';
