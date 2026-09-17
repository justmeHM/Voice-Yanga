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

        // If it's already an absolute URL, return it
        if (fileUrl.startsWith("http")) {
            return fileUrl;
        }

        Request request = new Request.Builder()
                .url(BuildConfig.API_ORIGIN + fileUrl)
                .header("Authorization", "Bearer " + sessionManager.getAccessToken())
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Media request failed: " + response.code());
            }

            // OkHttp follows redirects by default, so the final URL will be the signed one
            return response.request().url().toString();
        }
    }
}
