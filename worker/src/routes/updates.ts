import { Env } from '../types';
import { createJsonResponse, createErrorResponse } from '../middleware/cache';

export async function handleGetLatestUpdate(request: Request, env: Env): Promise<Response> {
  try {
    const bucket = env.MOVIE_BUCKET;
    if (!bucket) {
      return createErrorResponse('Storage bucket (MOVIE_BUCKET) not configured on worker', 500);
    }

    let obj = await bucket.get('updates/latest.json');
    if (!obj) {
      obj = await bucket.get('latest.json');
    }

    if (!obj) {
      return createErrorResponse('No update metadata (updates/latest.json) found in storage bucket.', 404);
    }

    const text = await obj.text();
    const data = JSON.parse(text);

    const downloadUrl = new URL('/api/updates/download', request.url).toString();

    return createJsonResponse({
      success: true,
      versionCode: Number(data.versionCode),
      versionName: String(data.versionName),
      releaseNotes: data.releaseNotes || `CineVault release v${data.versionName}`,
      downloadUrl: data.downloadUrl || downloadUrl,
      forceUpdate: Boolean(data.forceUpdate),
      fileSizeFormatted: data.fileSizeFormatted || (obj.size ? `${(obj.size / (1024 * 1024)).toFixed(1)} MB` : undefined),
      apkKey: data.apkKey || 'updates/cinevault-latest.apk'
    });
  } catch (e: any) {
    console.error('Error fetching latest update metadata:', e);
    return createErrorResponse(`Failed to check updates: ${e.message}`, 500);
  }
}

export async function handleDownloadUpdateApk(request: Request, env: Env): Promise<Response> {
  try {
    const bucket = env.MOVIE_BUCKET;
    if (!bucket) {
      return createErrorResponse('Storage bucket (MOVIE_BUCKET) not configured on worker', 500);
    }

    let apkKey = 'updates/cinevault-latest.apk';
    let latestObj = await bucket.get('updates/latest.json');
    if (!latestObj) {
      latestObj = await bucket.get('latest.json');
    }

    if (latestObj) {
      try {
        const text = await latestObj.text();
        const data = JSON.parse(text);
        if (data.apkKey && typeof data.apkKey === 'string') {
          apkKey = data.apkKey;
        }
      } catch (ignored) {}
    }

    let obj = await bucket.get(apkKey);
    if (!obj && apkKey !== 'updates/cinevault-latest.apk') {
      obj = await bucket.get('updates/cinevault-latest.apk');
    }

    if (!obj) {
      // Fallback: search for any .apk in updates/ prefix
      const listed = await bucket.list({ prefix: 'updates/' });
      const apkObj = listed.objects.find(o => o.key.endsWith('.apk'));
      if (apkObj) {
        obj = await bucket.get(apkObj.key);
        apkKey = apkObj.key;
      }
    }

    if (!obj) {
      return createErrorResponse('APK update file not found in storage bucket.', 404);
    }

    const filename = apkKey.split('/').pop() || 'cinevault-update.apk';
    const headers = new Headers();
    headers.set('Content-Type', 'application/vnd.android.package-archive');
    headers.set('Content-Disposition', `attachment; filename="${filename}"`);
    headers.set('Cache-Control', 'no-store, no-cache, must-revalidate');
    if (obj.size) {
      headers.set('Content-Length', obj.size.toString());
    }

    return new Response(obj.body, {
      status: 200,
      headers
    });
  } catch (e: any) {
    console.error('Error streaming update APK:', e);
    return createErrorResponse(`Failed to download APK: ${e.message}`, 500);
  }
}
