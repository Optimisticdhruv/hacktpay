# Production deployment checklist

Use a permanent HTTPS domain and a managed reverse proxy (for example Cloudflare DNS/proxy, Render, Railway, Fly.io, or a VPS with Caddy/Nginx). A Cloudflare Quick Tunnel is only for temporary local testing.

Before deployment:

1. Create a production Firebase service account outside Git and provide it as a mounted secret.
2. Set `RECOVERAI_STORAGE_MODE=firestore` and production Razorpay credentials in the host's secret manager.
3. Set `RECOVERAI_AUTH_ENABLED=true` only after Firebase Authentication is configured in the frontend.
4. Set `RECOVERAI_PUBLIC_WEBHOOK_ONLY=false` for a permanent authenticated deployment.
5. Set `FRONTEND_URL=https://your-domain.example`.
6. In Razorpay Dashboard, set `https://your-domain.example/api/webhooks/razorpay` as the webhook URL.
7. Verify `https://your-domain.example/actuator/health` returns `UP`.

Local container smoke test:

```powershell
docker compose up --build
```

Never commit `backend/.env`, the Firebase service-account JSON, or any Razorpay secret.
