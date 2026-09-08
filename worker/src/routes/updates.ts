import { Env } from '../types';
import { createJsonResponse, createErrorResponse } from '../middleware/cache';

async function findLatestJsonObject(bucket: R2Bucket): Promise<{ obj: R2ObjectBody; key: string } | null> {
  const directKeys = [
    'updates/latest.json',
    '/updates/latest.json',
    'latest.json',
    '/latest.json',
    'stories/updates/latest.json'
  ];

  for (const key of directKeys) {
    try {
      const obj = await bucket.get(key);
      if (obj) return { obj, key };
    } catch (_) {}
  }

  try {
    const listUpdates = await bucket.list({ prefix: 'updates/' });
    const matchUpdates = listUpdates.objects.find(o => o.key.endsWith('latest.json'));
    if (matchUpdates) {
      const obj = await bucket.get(matchUpdates.key);
      if (obj) return { obj, key: matchUpdates.key };
    }

    const listRoot = await bucket.list();
    const matchRoot = listRoot.objects.find(o => o.key.endsWith('latest.json'));
    if (matchRoot) {
      const obj = await bucket.get(matchRoot.key);
      if (obj) return { obj, key: matchRoot.key };
    }
  } catch (_) {}

  return null;
}

async function findApkObject(bucket: R2Bucket, requestedKey?: string): Promise<{ obj: R2ObjectBody; key: string } | null> {
  const candidateKeys: string[] = [];
  if (requestedKey) {
    candidateKeys.push(
      requestedKey,
      requestedKey.startsWith('/') ? requestedKey.slice(1) : `/${requestedKey}`,
      requestedKey.replace(/^updates\//, ''),
      `updates/${requestedKey.replace(/^updates\//, '')}`,
      `stories/${requestedKey}`
    );
  }
  candidateKeys.push(
    'updates/cinevault-latest.apk',
    '/updates/cinevault-latest.apk',
    'cinevault-latest.apk',
    '/cinevault-latest.apk',
    'stories/updates/cinevault-latest.apk'
  );

  for (const key of candidateKeys) {
    try {
      const obj = await bucket.get(key);
      if (obj) return { obj, key };
    } catch (_) {}
  }

  try {
    const listUpdates = await bucket.list({ prefix: 'updates/' });
    const apkUpdates = listUpdates.objects.filter(o => o.key.endsWith('.apk'));
    const preferred = apkUpdates.find(o => o.key.includes('latest')) || apkUpdates[0];
    if (preferred) {
      const obj = await bucket.get(preferred.key);
      if (obj) return { obj, key: preferred.key };
    }

    const listRoot = await bucket.list();
    const apkRoot = listRoot.objects.filter(o => o.key.endsWith('.apk'));
    const preferredRoot = apkRoot.find(o => o.key.includes('latest')) || apkRoot[0];
    if (preferredRoot) {
      const obj = await bucket.get(preferredRoot.key);
      if (obj) return { obj, key: preferredRoot.key };
    }
  } catch (_) {}

  return null;
}

export async function handleGetLatestUpdate(request: Request, env: Env): Promise<Response> {
  try {
    const bucket = env.MOVIE_BUCKET;
    if (!bucket) {
      return createErrorResponse('Storage bucket (MOVIE_BUCKET) not configured on worker', 500);
    }

    const result = await findLatestJsonObject(bucket);
    if (!result) {
      return createErrorResponse('No update metadata (updates/latest.json) found in storage bucket.', 404);
    }

    const { obj } = result;
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

    let apkKeyCandidate = 'updates/cinevault-latest.apk';
    const latestResult = await findLatestJsonObject(bucket);
    if (latestResult) {
      try {
        const text = await latestResult.obj.text();
        const data = JSON.parse(text);
        if (data.apkKey && typeof data.apkKey === 'string') {
          apkKeyCandidate = data.apkKey;
        }
      } catch (ignored) {}
    }

    const apkResult = await findApkObject(bucket, apkKeyCandidate);
    if (!apkResult) {
      return createErrorResponse('APK update file not found in storage bucket.', 404);
    }

    const { obj, key } = apkResult;
    const filename = key.split('/').pop() || 'cinevault-update.apk';
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
