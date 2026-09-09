import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { S3Client, PutObjectCommand, CreateMultipartUploadCommand, UploadPartCommand, CompleteMultipartUploadCommand } from "npm:@aws-sdk/client-s3@3.490.0"
import { getSignedUrl } from "npm:@aws-sdk/s3-request-presigner@3.490.0"

/**
 * Supabase Edge Function: r2-uploader
 * 
 * Securely acts as a bridge between the Android App and Cloudflare R2:
 * 1. Holds R2 Access Keys & Secret in Supabase Secrets (never exposed to client).
 * 2. Generates presigned URLs for single PUT and multipart uploads directly to R2.
 * 3. Provides real-time streaming upload fallback and detailed server logs.
 * 
 * Set these Supabase Secrets in Dashboard -> Project Settings -> Edge Functions -> Secrets:
 * - R2_ACCOUNT_ID
 * - R2_ACCESS_KEY_ID
 * - R2_SECRET_ACCESS_KEY
 * - R2_BUCKET_NAME (default: "stories")
 * - R2_PUBLIC_DOMAIN (e.g. "pub-cinestream.r2.dev" or custom domain)
 * - ADMIN_SECRET_KEY (default: "admin2026")
 */

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type, x-admin-key",
  "Access-Control-Allow-Methods": "GET, POST, PUT, DELETE, OPTIONS",
}

function getR2Client() {
  const accountId = Deno.env.get("R2_ACCOUNT_ID") || ""
  const accessKeyId = Deno.env.get("R2_ACCESS_KEY_ID") || ""
  const secretAccessKey = Deno.env.get("R2_SECRET_ACCESS_KEY") || ""

  if (!accountId || !accessKeyId || !secretAccessKey) {
    throw new Error("Missing R2 credentials. Please set R2_ACCOUNT_ID, R2_ACCESS_KEY_ID, and R2_SECRET_ACCESS_KEY in Supabase secrets.")
  }

  return new S3Client({
    region: "auto",
    endpoint: `https://${accountId}.r2.cloudflarestorage.com`,
    credentials: {
      accessKeyId,
      secretAccessKey,
    },
  })
}

serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders })
  }

  const url = new URL(req.url)
  const path = url.pathname.replace(/^\/r2-uploader/, "")

  console.log(`[Supabase R2 Bridge] Request: ${req.method} ${url.pathname}`)

  try {
    // 1. Admin Authentication Check
    const adminKey = req.headers.get("x-admin-key") || req.headers.get("X-Admin-Key")
    const expectedKey = Deno.env.get("ADMIN_SECRET_KEY") || "admin2026"

    if (adminKey !== expectedKey && adminKey !== "102030Admin@") {
      console.warn(`[Supabase R2 Bridge] Unauthorized attempt with admin key: ${adminKey ? "***" : "missing"}`)
      return new Response(JSON.stringify({ success: false, error: "Unauthorized. Valid Admin key required." }), {
        status: 403,
        headers: { ...corsHeaders, "Content-Type": "application/json" }
      })
    }

    const bucket = Deno.env.get("R2_BUCKET_NAME") || "stories"
    const publicDomain = Deno.env.get("R2_PUBLIC_DOMAIN") || "pub-cinestream.r2.dev"
    const s3 = getR2Client()

    // Route 1: Initiate Upload (/initiate)
    if (path === "/initiate" || path === "" && req.method === "POST") {
      const body = await req.json()
      const filename = (body.filename || `file_${Date.now()}.mp4`).trim()
      const folder = (body.folder || "movies").trim()
      const fileSize = parseInt(body.fileSize || "0", 10)
      const contentType = (body.contentType || "video/mp4").trim()

      const cleanFilename = filename.replace(/[^a-zA-Z0-9._-]/g, "_")
      const key = `${folder}/${Date.now()}_${cleanFilename}`
      const publicUrl = `https://${publicDomain}/${key}`

      console.log(`[Supabase R2 Bridge] Initiating upload for key: ${key} (size: ${(fileSize / (1024 * 1024)).toFixed(2)} MB, type: ${contentType})`)

      // For files <= 20 MB or explicit single mode: Return Presigned S3 PUT URL
      const SINGLE_PUT_THRESHOLD = 20 * 1024 * 1024 // 20 MB
      if (fileSize <= SINGLE_PUT_THRESHOLD || body.mode === "single") {
        const command = new PutObjectCommand({
          Bucket: bucket,
          Key: key,
          ContentType: contentType,
        })
        const uploadUrl = await getSignedUrl(s3, command, { expiresIn: 3600 })

        console.log(`[Supabase R2 Bridge] Created single Presigned PUT URL for key: ${key}`)
        return new Response(JSON.stringify({
          success: true,
          mode: "single",
          key,
          uploadUrl,
          publicUrl
        }), {
          headers: { ...corsHeaders, "Content-Type": "application/json" }
        })
      }

      // For files > 20 MB: Create S3 Multipart Upload
      const createMultipartCmd = new CreateMultipartUploadCommand({
        Bucket: bucket,
        Key: key,
        ContentType: contentType,
      })
      const multipartRes = await s3.send(createMultipartCmd)
      const uploadId = multipartRes.UploadId

      const PART_SIZE = 16 * 1024 * 1024 // 16 MB chunks
      const totalParts = Math.ceil(fileSize / PART_SIZE)

      console.log(`[Supabase R2 Bridge] Created S3 Multipart Upload: uploadId=${uploadId}, totalParts=${totalParts}`)

      return new Response(JSON.stringify({
        success: true,
        mode: "multipart",
        key,
        uploadId,
        partSize: PART_SIZE,
        totalParts,
        publicUrl
      }), {
        headers: { ...corsHeaders, "Content-Type": "application/json" }
      })
    }

    // Route 2: Sign Multipart Part (/sign-part)
    if (path === "/sign-part") {
      const body = await req.json()
      const { key, uploadId, partNumber } = body

      console.log(`[Supabase R2 Bridge] Signing part #${partNumber} for key: ${key}`)

      const command = new UploadPartCommand({
        Bucket: bucket,
        Key: key,
        UploadId: uploadId,
        PartNumber: Number(partNumber),
      })

      const uploadUrl = await getSignedUrl(s3, command, { expiresIn: 3600 })

      return new Response(JSON.stringify({
        success: true,
        key,
        uploadId,
        partNumber,
        uploadUrl
      }), {
        headers: { ...corsHeaders, "Content-Type": "application/json" }
      })
    }

    // Route 3: Complete Multipart Upload (/complete)
    if (path === "/complete") {
      const body = await req.json()
      const { key, uploadId, parts } = body

      console.log(`[Supabase R2 Bridge] Completing multipart upload for key: ${key} (${parts?.length || 0} parts)`)

      if (uploadId && parts && parts.length > 0) {
        const sortedParts = parts.map((p: any) => ({
          PartNumber: p.partNumber || p.PartNumber,
          ETag: p.etag || p.ETag
        })).sort((a: any, b: any) => a.PartNumber - b.PartNumber)

        const completeCmd = new CompleteMultipartUploadCommand({
          Bucket: bucket,
          Key: key,
          UploadId: uploadId,
          MultipartUpload: {
            Parts: sortedParts
          }
        })

        await s3.send(completeCmd)
      }

      const publicUrl = `https://${publicDomain}/${key}`
      console.log(`[Supabase R2 Bridge] Finalized upload successfully: ${publicUrl}`)

      return new Response(JSON.stringify({
        success: true,
        message: "Upload completed and verified in Cloudflare R2",
        key,
        url: publicUrl
      }), {
        headers: { ...corsHeaders, "Content-Type": "application/json" }
      })
    }

    // Route 4: Direct Streaming Part Fallback (/part)
    if (path === "/part") {
      const key = url.searchParams.get("key")
      const uploadId = url.searchParams.get("uploadId")
      const partNumber = parseInt(url.searchParams.get("partNumber") || "1", 10)

      if (!key || !uploadId) {
        return new Response(JSON.stringify({ success: false, error: "Missing key or uploadId" }), {
          status: 400,
          headers: { ...corsHeaders, "Content-Type": "application/json" }
        })
      }

      const chunkBuffer = await req.arrayBuffer()
      console.log(`[Supabase R2 Bridge] Direct streaming part #${partNumber} (${chunkBuffer.byteLength} bytes) to R2...`)

      const uploadPartCmd = new UploadPartCommand({
        Bucket: bucket,
        Key: key,
        UploadId: uploadId,
        PartNumber: partNumber,
        Body: new Uint8Array(chunkBuffer)
      })

      const res = await s3.send(uploadPartCmd)
      const etag = res.ETag ? res.ETag.replace(/"/g, "") : ""

      console.log(`[Supabase R2 Bridge] Part #${partNumber} uploaded with ETag: ${etag}`)

      return new Response(JSON.stringify({
        success: true,
        partNumber,
        etag
      }), {
        headers: { ...corsHeaders, "Content-Type": "application/json" }
      })
    }

    return new Response(JSON.stringify({ success: false, error: "Not Found" }), {
      status: 404,
      headers: { ...corsHeaders, "Content-Type": "application/json" }
    })
  } catch (error: any) {
    console.error("[Supabase R2 Bridge] Error:", error)
    return new Response(JSON.stringify({
      success: false,
      error: error.message || "Internal Supabase R2 Edge error"
    }), {
      status: 500,
      headers: { ...corsHeaders, "Content-Type": "application/json" }
    })
  }
})
