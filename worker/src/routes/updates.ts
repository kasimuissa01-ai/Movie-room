import { Env } from '../types';
import { createJsonResponse, createErrorResponse } from '../middleware/cache';

export async function handleGetLatestUpdate(request: Request, env: Env): Promise<Response> {
  try {
    const bucket = env.MOVIE_BUCKET;
    if (!bucket) {
      return createErrorResponse('Storage bucket not configured', 500);
    }

    const obj = await bucket.get('updates/latest.json');
    const downloadUrl = new URL('/api/updates/download', request.url).toString();

    if (!obj) {
      return createJsonResponse({
        success: true,
        versionCode: 1,
        versionName: "1.0",
        releaseNotes: "CineVault latest update release.",
        downloadUrl: downloadUrl,
        forceUpdate: false,
        fileSizeFormatted: "45 MB"
      });
    }

    const text = await obj.text();
    const data = JSON.parse(text);

    return createJsonResponse({
      success: true,
      versionCode: data.versionCode || 1,
      versionName: data.versionName || "1.0",
      releaseNotes: data.releaseNotes || "Performance enhancements and bug fixes.",
      downloadUrl: data.downloadUrl || downloadUrl,
      forceUpdate: data.forceUpdate || false,
      fileSizeFormatted: data.fileSizeFormatted || "45 MB"
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
      return createErrorResponse('Storage bucket not configured', 500);
    }

    let apkKey = 'updates/cinevault-latest.apk';
    const latestObj = await bucket.get('updates/latest.json');
    if (latestObj) {
      try {
        const text = await latestObj.text();
        const data = JSON.parse(text);
        if (data.apkKey) {
          apkKey = data.apkKey;
        }
      } catch (ignored) {}
    }

    let obj = await bucket.get(apkKey);
    if (!obj) {
      // Fallback: search for any .apk in updates/
      const listed = await bucket.list({ prefix: 'updates/' });
      const apkObj = listed.objects.find(o => o.key.endsWith('.apk'));
      if (apkObj) {
        obj = await bucket.get(apkObj.key);
      }
    }

    if (!obj) {
      return createErrorResponse('APK update file not found in storage.', 404);
    }

    const headers = new Headers();
    headers.set('Content-Type', 'application/vnd.android.package-archive');
    headers.set('Content-Disposition', 'attachment; filename="cinevault-update.apk"');
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
