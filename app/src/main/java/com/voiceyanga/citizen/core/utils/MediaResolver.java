package com.voiceyanga.citizen.core.utils;

import android.content.Context;
import com.voiceyanga.citizen.BuildConfig;
import com.voiceyanga.citizen.data.local.SessionManager;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MediaResolver {

    /**
     * Resolves a media file (local path or remote URL) to a local cached audio file path for reliable playback.
     * Must be called off the main thread.
     */
    public static String resolveAudioFile(
            Context context,
            String fileUrl,
            SessionManager sessionManager,
            OkHttpClient httpClient
    ) throws IOException {
        if (fileUrl == null || fileUrl.trim().isEmpty()) {
            return null;
        }

        // 1. If it's already a valid local file, return it directly
        if (fileUrl.startsWith("/") || fileUrl.startsWith("file://")) {
            String path = fileUrl.startsWith("file://") ? fileUrl.substring(7) : fileUrl;
            File localFile = new File(path);
            if (localFile.exists() && localFile.length() > 0) {
                return localFile.getAbsolutePath();
            }
        }

        // 2. Check local disk cache for previously downloaded voice note
        String cacheName = "vn_cache_" + Math.abs(fileUrl.hashCode()) + ".m4a";
        File cacheFile = new File(context.getCacheDir(), cacheName);
        if (cacheFile.exists() && cacheFile.length() > 0) {
            return cacheFile.getAbsolutePath();
        }

        // 3. Resolve target HTTP URL
        String targetUrl = MediaUtils.resolvePhotoUrl(fileUrl);
        if (targetUrl == null) {
            targetUrl = BuildConfig.API_ORIGIN + (fileUrl.startsWith("/") ? fileUrl : "/" + fileUrl);
        }

        // 4. Execute authenticated request using OkHttpClient
        String token = sessionManager != null ? sessionManager.getAccessToken() : null;
        Request.Builder requestBuilder = new Request.Builder().url(targetUrl);
        if (token != null && !token.isEmpty()) {
            requestBuilder.header("Authorization", "Bearer " + token);
        }

        try (Response response = httpClient.newCall(requestBuilder.build()).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                return targetUrl;
            }

            // Write response stream into cache file
            try (InputStream is = response.body().byteStream();
                 FileOutputStream os = new FileOutputStream(cacheFile)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }

            if (cacheFile.exists() && cacheFile.length() > 0) {
                return cacheFile.getAbsolutePath();
            }
            return targetUrl;
        } catch (Exception e) {
            android.util.Log.e("MediaResolver", "Failed to cache remote voice note", e);
            return targetUrl;
        }
    }

    public static String resolveMediaUrl(
            String fileUrl,
            SessionManager sessionManager,
            OkHttpClient httpClient
    ) throws IOException {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return null;
        }

        String targetUrl = MediaUtils.resolvePhotoUrl(fileUrl);
        if (targetUrl == null) {
            targetUrl = BuildConfig.API_ORIGIN + (fileUrl.startsWith("/") ? fileUrl : "/" + fileUrl);
        }

        Request request = new Request.Builder()
                .url(targetUrl)
                .header("Authorization", "Bearer " + sessionManager.getAccessToken())
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return targetUrl;
            }

            return response.request().url().toString();
        } catch (Exception e) {
            return targetUrl;
        }
    }
}
