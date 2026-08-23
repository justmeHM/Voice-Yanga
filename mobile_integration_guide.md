# Voice Yanga API — Mobile Integration Guide

## Overview

There are **three phases** to connecting your API to the mobile app:

1. **Bundle** — compile TypeScript to production JavaScript
2. **Deploy** — host the API somewhere the phone can reach it
3. **Connect & Test** — point the mobile app at the URL and verify

---

## Phase 1 — Bundle the API for Production

Your API runs in development mode via `ts-node-dev`. For production you compile it to plain JavaScript.

### 1.1 Build the TypeScript

```bash
npm run build
```

This runs `tsc` and outputs compiled JS into `dist/`. Verify it worked:

```bash
ls dist/
# Should contain: server.js, app.js, modules/, middleware/, utils/, etc.
```

### 1.2 Start in Production Mode

```bash
npm start
```

This runs `node dist/server.js`. The server binds to the port in your `.env` file (default `3000`).

### 1.3 Check your `.env` before deploying

| Variable | Required | Example |
|---|---|---|
| `PORT` | ✅ | `3000` |
| `JWT_SECRET` | ✅ | long random string |
| `JWT_REFRESH_SECRET` | ✅ | different long random string |
| `JWT_EXPIRES_IN` | ✅ | `15m` |
| `JWT_REFRESH_EXPIRES_IN` | ✅ | `7d` |
| `NODE_ENV` | ✅ | `production` |
| `UPLOAD_DIR` | optional | `uploads` |

> [!IMPORTANT]
> Never use the same value for `JWT_SECRET` and `JWT_REFRESH_SECRET`. Generate them with:
> ```bash
> node -e "console.log(require('crypto').randomBytes(64).toString('hex'))"
> ```

---

## Phase 2 — Deploy Options

### Option A — Local Network (Development / Testing with Physical Device)

This is the **fastest way** to test with a real Android device on the same Wi-Fi.

**Step 1:** Find your PC's local IP address
```powershell
ipconfig
# Look for: IPv4 Address . . . . . . . . : 192.168.X.X
```

**Step 2:** Start the dev server (already running):
```bash
npm run dev
# Server is at http://192.168.X.X:3000
```

**Step 3:** In your Android app's `NetworkModule` or `RetrofitClient`, change the base URL:
```kotlin
// app/src/main/java/.../network/RetrofitClient.kt
private const val BASE_URL = "http://192.168.X.X:3000/api/v1/"
```

> [!WARNING]
> Android 9+ blocks plain `http://` connections by default. You must add a **Network Security Config**:
>
> Create `res/xml/network_security_config.xml`:
> ```xml
> <?xml version="1.0" encoding="utf-8"?>
> <network-security-config>
>     <domain-config cleartextTrafficPermitted="true">
>         <domain includeSubdomains="true">192.168.X.X</domain>
>     </domain-config>
> </network-security-config>
> ```
>
> Then reference it in `AndroidManifest.xml`:
> ```xml
> <application
>     android:networkSecurityConfig="@xml/network_security_config"
>     ...>
> ```

---

### Option B — ngrok Tunnel (Best for Quick Testing — No Firewall Config)

ngrok gives your local server a public HTTPS URL instantly.

**Step 1:** Install ngrok from https://ngrok.com/download

**Step 2:** Start the tunnel (while `npm run dev` is running):
```bash
ngrok http 3000
```

**Step 3:** Copy the HTTPS URL it gives you:
```
Forwarding  https://abc123.ngrok-free.app -> http://localhost:3000
```

**Step 4:** Update your Android app:
```kotlin
private const val BASE_URL = "https://abc123.ngrok-free.app/api/v1/"
```

> [!TIP]
> ngrok URLs change every time you restart. For persistent URLs, sign up for a free ngrok account and use a static domain.

---

### Option C — Cloud VPS (Production)

For a production deployment on a VPS (e.g. Render, Railway, DigitalOcean, AWS EC2):

**Using Railway (easiest — free tier):**

1. Push your code to GitHub
2. Go to https://railway.app → New Project → Deploy from GitHub
3. Set environment variables in the Railway dashboard
4. Railway auto-detects Node.js and runs `npm start`
5. You get a public URL like `https://voice-yanga-api.up.railway.app`

**Using PM2 on a VPS:**
```bash
npm install -g pm2
npm run build
pm2 start dist/server.js --name voice-yanga-api
pm2 save
pm2 startup   # makes it survive reboots
```

---

## Phase 3 — Connection Testing

### 3.1 Test the API is reachable (from any device/browser)

Open in a browser or `curl`:
```
GET http://<YOUR_IP_OR_URL>:3000/health
```

Expected response:
```json
{ "status": "ok", "timestamp": "...", "version": "1.0.0" }
```

If this works, your API is reachable.

---

### 3.2 Test auth from Postman / API client

**Register a test user:**
```
POST http://<URL>/api/v1/auth/register
Content-Type: application/json

{
  "firstName": "Test",
  "lastName": "User",
  "email": "test@example.com",
  "password": "Test@1234"
}
```

Expected:
```json
{
  "token": "<JWT>",
  "refreshToken": "<token>",
  "user": { "id": "...", "role": "CITIZEN", "name": "Test User", ... }
}
```

**Login:**
```
POST http://<URL>/api/v1/auth/login
Content-Type: application/json

{
  "identifier": "test@example.com",
  "password": "Test@1234"
}
```

---

### 3.3 Test from the Android App

In your Android project, add a connectivity check at startup (e.g., in `SplashActivity` or `MainViewModel`):

```kotlin
// In your ViewModel or Repository
suspend fun testConnection(): Boolean {
    return try {
        val response = apiService.healthCheck() // GET /health
        response.isSuccessful
    } catch (e: Exception) {
        false
    }
}
```

Add the health endpoint to your Retrofit interface:
```kotlin
@GET("/health")
suspend fun healthCheck(): Response<HealthResponse>

data class HealthResponse(val status: String, val version: String)
```

---

### 3.4 Common Connection Errors & Fixes

| Error | Cause | Fix |
|---|---|---|
| `CLEARTEXT_NOT_PERMITTED` | Android blocks `http://` | Add Network Security Config (see Phase 2A) |
| `Connection refused` | Server not running / wrong port | Run `npm run dev`, check PORT in .env |
| `Network unreachable` | Device not on same Wi-Fi | Connect phone and PC to same network |
| `401 Unauthorized` | Missing or expired JWT | Login first and use the `token` in the `Authorization: Bearer <token>` header |
| `404 Not Found` | Wrong base URL path | Ensure URL ends with `/api/v1/` |
| `CORS error` | Cross-origin blocked | Already handled — `app.ts` has `cors({ origin: '*' })` |
| Timeout | ngrok session expired | Restart ngrok and update base URL |

---

## Quick Checklist

- [ ] `npm run build` succeeds without errors
- [ ] `npm start` runs without errors and logs "Server running on port..."
- [ ] `GET /health` returns `{ status: "ok" }`
- [ ] `POST /auth/login` returns a `token`
- [ ] Android `BASE_URL` points to the correct IP/URL with `/api/v1/` suffix
- [ ] Network Security Config added for plain HTTP (local testing)
- [ ] `Authorization: Bearer <token>` header included in all authenticated requests
- [ ] `POST /complaints` sends JSON body with `category` (string) and `location` (string)

---

## Retrofit Base Configuration (Reference)

```kotlin
object RetrofitClient {
    // Change this to your server URL
    private const val BASE_URL = "http://192.168.X.X:3000/api/v1/"

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val token = TokenManager.getToken() // your local storage
            val request = if (token != null) {
                chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
            } else chain.request()
            chain.proceed(request)
        }
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
```
