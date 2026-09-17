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

        if (path.startsWith("http") || path.startsWith("content://") || path.startsWith("file://")) {
            result = path;
        } else if (path.startsWith("/uploads") || path.startsWith("uploads/") || 
                   path.startsWith("/public") || path.startsWith("public/")) {
            String normalizedPath = path.startsWith("/") ? path : "/" + path;
            result = origin + normalizedPath;
        } else if (path.startsWith("/")) {
            // Local absolute path from File.getAbsolutePath()
            result = path;
        } else {
            // Fallback for names that might be on the server root or in /uploads
            result = origin + "/" + path;
        }
        
        // Final sanity check: encode spaces
        result = result.replace(" ", "%20");
        
        android.util.Log.d("MediaUtils", "Resolved path: [" + path + "] -> [" + result + "]");
        return result;
    }
}
