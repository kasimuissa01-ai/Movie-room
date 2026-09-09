/**
 * Lightweight AWS SigV4 Presigned URL Generator for Cloudflare Workers.
 * Uses Web Crypto API (SubtleCrypto) - zero external dependencies, 100% compatible
 * with Cloudflare Workers bundler and wrangler without @aws-sdk.
 */

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

async function hmacSha256(key: ArrayBuffer | Uint8Array, data: string): Promise<ArrayBuffer> {
  const cryptoKey = await crypto.subtle.importKey(
    'raw',
    key,
    { name: 'HMAC', hash: 'SHA-256' },
    false,
    ['sign']
  );
  return await crypto.subtle.sign('HMAC', cryptoKey, new TextEncoder().encode(data));
}

async function sha256Hex(data: string): Promise<string> {
  const hash = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(data));
  return Array.from(new Uint8Array(hash))
    .map((b) => b.toString(16).padStart(2, '0'))
    .join('');
}

function toHex(buffer: ArrayBuffer): string {
  return Array.from(new Uint8Array(buffer))
    .map((b) => b.toString(16).padStart(2, '0'))
    .join('');
}

function uriEncode(string: string, encodeSlash: boolean = true): string {
  let result = '';
  for (let i = 0; i < string.length; i++) {
    const ch = string[i];
    if (
      (ch >= 'A' && ch <= 'Z') ||
      (ch >= 'a' && ch <= 'z') ||
      (ch >= '0' && ch <= '9') ||
      ch === '_' ||
      ch === '-' ||
      ch === '~' ||
      ch === '.'
    ) {
      result += ch;
    } else if (ch === '/' && !encodeSlash) {
      result += '/';
    } else {
      const hex = ch.charCodeAt(0).toString(16).toUpperCase();
      result += '%' + (hex.length < 2 ? '0' : '') + hex;
    }
  }
  return result;
}

export async function generatePresignedUrl(options: SigV4Options): Promise<string> {
  if (!options.accessKeyId || !options.secretAccessKey) {
    return '';
  }

  const method = (options.method || 'PUT').toUpperCase();
  const region = options.region || 'auto';
  const service = 's3';
  const host = `${options.accountId}.r2.cloudflarestorage.com`;
  const expiresIn = options.expiresIn || 3600;

  const now = new Date();
  const amzDate = now.toISOString().replace(/[:-]|\.\d{3}/g, '');
  const dateStamp = amzDate.substring(0, 8);

  const cleanKey = options.key.startsWith('/') ? options.key.substring(1) : options.key;
  const canonicalUri = `/${uriEncode(options.bucket, false)}/${uriEncode(cleanKey, false)}`;

  const credentialScope = `${dateStamp}/${region}/${service}/aws4_request`;

  // Standard S3 query parameters for presigned URL
  const queryParams: Record<string, string> = {
    'X-Amz-Algorithm': 'AWS4-HMAC-SHA256',
    'X-Amz-Credential': `${options.accessKeyId}/${credentialScope}`,
    'X-Amz-Date': amzDate,
    'X-Amz-Expires': expiresIn.toString(),
    'X-Amz-SignedHeaders': 'host',
    ...(options.queryParams || {})
  };

  // Canonical query string sorted alphabetically by key
  const canonicalQueryString = Object.keys(queryParams)
    .sort()
    .map((key) => `${uriEncode(key, true)}=${uriEncode(queryParams[key], true)}`)
    .join('&');

  const canonicalHeaders = `host:${host}\n`;
  const signedHeaders = 'host';
  const payloadHash = 'UNSIGNED-PAYLOAD';

  const canonicalRequest = `${method}\n${canonicalUri}\n${canonicalQueryString}\n${canonicalHeaders}\n${signedHeaders}\n${payloadHash}`;
  const canonicalRequestHash = await sha256Hex(canonicalRequest);

  const stringToSign = `AWS4-HMAC-SHA256\n${amzDate}\n${credentialScope}\n${canonicalRequestHash}`;

  // Derive signing key
  const kDate = await hmacSha256(new TextEncoder().encode(`AWS4${options.secretAccessKey}`), dateStamp);
  const kRegion = await hmacSha256(kDate, region);
  const kService = await hmacSha256(kRegion, service);
  const kSigning = await hmacSha256(kService, 'aws4_request');

  // Calculate signature
  const signature = toHex(await hmacSha256(kSigning, stringToSign));

  return `https://${host}${canonicalUri}?${canonicalQueryString}&X-Amz-Signature=${signature}`;
}
