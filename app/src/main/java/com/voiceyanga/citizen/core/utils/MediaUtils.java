package com.voiceyanga.citizen.core.utils;

public class MediaUtils {

    /**
     * Resolves a photo path/URI to a format Glide can load.
     * Handles local absolute paths, content URIs, and server-side relative paths.
     */
    public static String resolvePhotoUrl(String path) {
        if (path == null || path.isEmpty()) return null;
        
        String result;
        String origin = com.voiceyanga.citizen.BuildConfig.API_ORIGIN;
        if (origin.endsWith("/")) {
            origin = origin.substring(0, origin.length() - 1);
        }

        if (path.startsWith("content://") || path.startsWith("file://")) {
            result = path;
        } else if (path.startsWith("http://") || path.startsWith("https://")) {
            // Replace localhost/127.0.0.1 or local Wi-Fi IPs in host with BuildConfig.API_ORIGIN host
            result = replaceLocalHostWithOrigin(path, origin);
        } else if (path.startsWith("/uploads") || path.startsWith("uploads/") || 
                   path.startsWith("/public") || path.startsWith("public/")) {
            String normalizedPath = path.startsWith("/") ? path : "/" + path;
            result = origin + normalizedPath;
        } else if (path.startsWith("/data/") || path.startsWith("/storage/") || path.startsWith("/emulated/")) {
            // Local absolute path
            result = path;
        } else if (path.startsWith("/")) {
            result = origin + path;
        } else {
            // Fallback for names that might be on the server root or in /uploads
            result = origin + "/" + path;
        }
        
        // Final sanity check: encode spaces
        result = result.replace(" ", "%20");
        
        return result;
    }

    private static String replaceLocalHostWithOrigin(String url, String origin) {
        if (url == null) return null;
        // If url starts with http://localhost:3000, http://127.0.0.1:3000, or any local IP on port 3000/similar,
        // rewrite the protocol+host+port prefix using API_ORIGIN so emulator can resolve it.
        return url.replaceAll("^http://(localhost|127\\.0\\.0\\.1|10\\.\\d+\\.\\d+\\.\\d+|192\\.168\\.\\d+\\.\\d+):(\\d+)", origin);
    }
}
