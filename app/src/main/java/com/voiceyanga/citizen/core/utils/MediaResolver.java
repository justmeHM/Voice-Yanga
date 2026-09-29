package com.voiceyanga.citizen.core.utils;

import com.voiceyanga.citizen.BuildConfig;
import com.voiceyanga.citizen.data.local.SessionManager;
import java.io.IOException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MediaResolver {

    /**
     * Resolves a relative media URL (e.g., /uploads/...) to a temporary signed URL.
     * This should be called off the main thread.
     */
    public static String resolveMediaUrl(
            String fileUrl,
            SessionManager sessionManager,
            OkHttpClient httpClient
    ) throws IOException {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return null;
        }

        String resolved = MediaUtils.resolvePhotoUrl(fileUrl);
        if (resolved != null && resolved.startsWith("http")) {
            return resolved;
        }

        String targetUrl = resolved != null ? resolved : (BuildConfig.API_ORIGIN + (fileUrl.startsWith("/") ? fileUrl : "/" + fileUrl));

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
