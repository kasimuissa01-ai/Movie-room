import { Env } from '../types';

export class R2Service {
  private bucket?: R2Bucket;
  private publicUrlBase: string;

  constructor(env: Env) {
    this.bucket = env.MOVIE_BUCKET;
    this.publicUrlBase = 'https://pub-cinestream.r2.dev';
  }

  /**
   * Directly get an object from R2 bucket
   */
  async getObject(key: string, range?: string): Promise<R2ObjectBody | null> {
    if (!this.bucket) return null;
    const cleanKey = key.replace(/^\/+/, '');
    
    const options: R2GetOptions = {};
    if (range) {
      options.range = parseRange(range);
    }
    
    return await this.bucket.get(cleanKey, options);
  }

  /**
   * Upload object directly via Worker
   */
  async putObject(key: string, data: ReadableStream | ArrayBuffer, contentType: string): Promise<string> {
    const cleanKey = key.replace(/^\/+/, '');
    if (this.bucket) {
      await this.bucket.put(cleanKey, data, {
        httpMetadata: { contentType }
      });
      return `${this.publicUrlBase}/${cleanKey}`;
    }
    return `${this.publicUrlBase}/${cleanKey}`;
  }

  /**
   * Delete object
   */
  async deleteObject(key: string): Promise<boolean> {
    if (!this.bucket) return false;
    const cleanKey = key.replace(/^\/+/, '');
    await this.bucket.delete(cleanKey);
    return true;
  }
}

function parseRange(rangeHeader: string): R2Range | undefined {
  const match = rangeHeader.match(/bytes=(\d*)-(\d*)/);
  if (!match) return undefined;
  const start = match[1] ? parseInt(match[1], 10) : undefined;
  const end = match[2] ? parseInt(match[2], 10) : undefined;
  if (start !== undefined && end !== undefined) {
    return { offset: start, length: end - start + 1 };
  } else if (start !== undefined) {
    return { offset: start };
  } else if (end !== undefined) {
    return { suffix: end };
  }
  return undefined;
}
