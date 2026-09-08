# CineStream Mobile Client

The mobile application connects exclusively to our Cloudflare Worker backend.

## Security Overview
- **Zero Third-Party Secrets**: No TMDB API keys, Cloudflare R2 secrets, or database service-role keys are compiled into the application.
- **Single Source of Truth**: All requests go to `API_BASE_URL` (`https://api.YOURDOMAIN.com`).
- **Standardized JWT Authorization**: User & Admin sessions use `Authorization: Bearer <token>` to interact with private endpoints.
