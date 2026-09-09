import { S3Client, PutObjectCommand, UploadPartCommand } from '@aws-sdk/client-s3';
import { getSignedUrl } from '@aws-sdk/s3-request-presigner';

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
 * Generates an AWS SigV4 presigned URL using official @aws-sdk/client-s3 and @aws-sdk/s3-request-presigner.
 */
export async function generatePresignedUrl(options: SigV4Options): Promise<string> {
  if (!options.accessKeyId || !options.secretAccessKey) {
    console.warn('[Diagnostic] Cannot generate presigned URL: R2 accessKeyId or secretAccessKey missing.');
    return '';
  }

  const client = new S3Client({
    region: options.region || 'auto',
    endpoint: `https://${options.accountId}.r2.cloudflarestorage.com`,
    credentials: {
      accessKeyId: options.accessKeyId,
      secretAccessKey: options.secretAccessKey,
    },
  });

  const uploadId = options.queryParams?.uploadId;
  const partNumberStr = options.queryParams?.partNumber;
  const partNumber = partNumberStr ? parseInt(partNumberStr, 10) : undefined;

  if (uploadId && partNumber) {
    const command = new UploadPartCommand({
      Bucket: options.bucket,
      Key: options.key,
      UploadId: uploadId,
      PartNumber: partNumber,
    });
    return await getSignedUrl(client, command, { expiresIn: options.expiresIn || 3600 });
  } else {
    const command = new PutObjectCommand({
      Bucket: options.bucket,
      Key: options.key,
    });
    return await getSignedUrl(client, command, { expiresIn: options.expiresIn || 3600 });
  }
}

