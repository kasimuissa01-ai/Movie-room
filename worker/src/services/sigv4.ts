export interface SigV4Options {
  accessKeyId: string;
  secretAccessKey: string;
  region?: string;
  accountId: string;
  bucket: string;
  key: string;
  method?: string;
  expiresIn?: number;
  queryParams?: Record<string, string>;
}

/**
 * Generates an AWS SigV4 presigned URL using standard Web Crypto API natively available in Cloudflare Workers.
 */
export async function generatePresignedUrl(options: SigV4Options): Promise<string> {
  const region = options.region || 'auto';
  const method = (options.method || 'PUT').toUpperCase();
  const expiresIn = options.expiresIn || 3600;
  const now = new Date();
  const amzDate = now.toISOString().replace(/[:-]/g, '').replace(/\.\d{3}/, '');
  const dateStamp = amzDate.substring(0, 8);
  const host = `${options.accountId}.r2.cloudflarestorage.com`;

  const credentialScope = `${dateStamp}/${region}/s3/aws4_request`;

  const params: Record<string, string> = {
    'X-Amz-Algorithm': 'AWS4-HMAC-SHA256',
    'X-Amz-Credential': `${options.accessKeyId}/${credentialScope}`,
    'X-Amz-Date': amzDate,
    'X-Amz-Expires': expiresIn.toString(),
    'X-Amz-SignedHeaders': 'host',
    ...(options.queryParams || {})
  };

  const sortedKeys = Object.keys(params).sort();
  const canonicalQueryString = sortedKeys
    .map(k => `${encodeURIComponent(k)}=${encodeURIComponent(params[k])}`)
    .join('&');

  const cleanKey = options.key.replace(/^\/+/, '');
  const canonicalPath = `/${options.bucket}/${cleanKey.split('/').map(segment => encodeURIComponent(segment)).join('/')}`;

  const canonicalHeaders = `host:${host}\n`;
  const signedHeaders = 'host';
  const payloadHash = 'UNSIGNED-PAYLOAD';

  const canonicalRequest = [
    method,
    canonicalPath,
    canonicalQueryString,
    canonicalHeaders,
    signedHeaders,
    payloadHash
  ].join('\n');

  const canonicalRequestHash = await sha256Hex(canonicalRequest);
  const stringToSign = [
    'AWS4-HMAC-SHA256',
    amzDate,
    credentialScope,
    canonicalRequestHash
  ].join('\n');

  const signingKey = await getSignatureKey(options.secretAccessKey, dateStamp, region, 's3');
  const signature = await hmacSha256Hex(signingKey, stringToSign);

  const fullQueryString = `${canonicalQueryString}&X-Amz-Signature=${signature}`;
  return `https://${host}${canonicalPath}?${fullQueryString}`;
}

async function sha256Hex(message: string): Promise<string> {
  const msgBuffer = new TextEncoder().encode(message);
  const hashBuffer = await crypto.subtle.digest('SHA-256', msgBuffer);
  return Array.from(new Uint8Array(hashBuffer)).map(b => b.toString(16).padStart(2, '0')).join('');
}

async function hmacSha256(key: CryptoKey | ArrayBuffer, data: string): Promise<ArrayBuffer> {
  const keyObj = key instanceof ArrayBuffer
    ? await crypto.subtle.importKey('raw', key, { name: 'HMAC', hash: 'SHA-256' }, false, ['sign'])
    : key;
  const dataBuffer = new TextEncoder().encode(data);
  return await crypto.subtle.sign('HMAC', keyObj, dataBuffer);
}

async function hmacSha256Hex(key: ArrayBuffer, data: string): Promise<string> {
  const rawHash = await hmacSha256(key, data);
  return Array.from(new Uint8Array(rawHash)).map(b => b.toString(16).padStart(2, '0')).join('');
}

async function getSignatureKey(key: string, dateStamp: string, regionName: string, serviceName: string): Promise<ArrayBuffer> {
  const kSecret = new TextEncoder().encode('AWS4' + key);
  const kDate = await hmacSha256(await crypto.subtle.importKey('raw', kSecret, { name: 'HMAC', hash: 'SHA-256' }, false, ['sign']), dateStamp);
  const kRegion = await hmacSha256(kDate, regionName);
  const kService = await hmacSha256(kRegion, serviceName);
  const kSigning = await hmacSha256(kService, 'aws4_request');
  return kSigning;
}
