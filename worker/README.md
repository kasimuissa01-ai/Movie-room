# CineStream Cloudflare Worker Backend

Production-ready, zero-trust backend for CineStream / MovieRoom.

## Architecture

```
Mobile App (Android / React Native / Web)
        │
        ▼ (HTTPS + Bearer JWT)
Cloudflare Worker (api.yourdomain.com)
  ├── Edge Rate Limiting
  ├── Caching (Cache-Control & Cloudflare CDN)
  ├── JWT Auth & Admin Authorization Verification
        │
        ├── TMDB Private API (TMDB_API_KEY secret)
        ├── Cloudflare R2 Storage (Zero-trust signed storage & streaming)
        └── Database / KV (User watchlist & watch history)
```

## Security Guarantees
1. **Zero Client Secrets**: Mobile app contains NO TMDB API keys, Cloudflare keys, or database service secrets.
2. **Server-Side Authorization**: Admin operations (publishing movies, deleting content, uploading to R2) are strictly validated via JWT role claims on the Worker.
3. **No Error Leaks**: Sensitive internal errors and provider codes are masked before sending responses to the client.

## Deployment Instructions

### 1. Install Wrangler
```bash
npm install -g wrangler
# or
npm install
```

### 2. Configure Cloudflare Secrets
Run these commands to securely store your credentials in Cloudflare:

```bash
wrangler secret put TMDB_API_KEY
# Enter your TMDB v3 or v4 key when prompted

wrangler secret put JWT_SECRET
# Enter a strong 256-bit random string (e.g., openssl rand -hex 32)

wrangler secret put ADMIN_API_KEY
# Enter your admin master passcode
```

### 3. Deploy to Cloudflare
```bash
wrangler deploy
```

Your API will be live at `https://cinestream-api-worker.<your-subdomain>.workers.dev` (or your custom domain `https://api.yourdomain.com`).
