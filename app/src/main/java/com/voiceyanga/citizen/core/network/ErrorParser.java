package com.voiceyanga.citizen.core.network;

import com.google.gson.Gson;
import com.voiceyanga.citizen.data.remote.dto.ApiError;
import retrofit2.Response;

public class ErrorParser {
    private static final Gson gson = new Gson();

    public static String parseError(Response<?> response) {
        try {
            if (response.errorBody() != null) {
                String errorJson = response.errorBody().string();
                ApiError apiError = gson.fromJson(errorJson, ApiError.class);
                if (apiError != null && apiError.message != null) {
                    StringBuilder sb = new java.lang.StringBuilder(apiError.message);
                    if (apiError.details != null && !apiError.details.isEmpty()) {
                        sb.append("\n");
                        for (ApiError.FieldError detail : apiError.details) {
                            sb.append("- ").append(detail.message).append("\n");
                        }
                    }
                    return sb.toString().trim();
                }
            }
        } catch (Exception e) {
            // Fallback
        }

        switch (response.code()) {
            case 400: return "Bad Request: Please check your input.";
            case 401: return "Unauthorized: Please log in again.";
            case 403: return "Access Denied: You don't have permission.";
            case 404: return "Not Found: The requested item does not exist.";
            case 409: return "Conflict: This record already exists.";
            case 413: return "Payload Too Large: Please use a smaller file.";
            case 429: return "Too Many Requests: Please slow down.";
            case 500: case 503: return "Server Error: We're having trouble on our end. Please try again later.";
            default: return "An unexpected error occurred (" + response.code() + ")";
        }
    }

    public static String parseThrowable(Throwable t) {
        if (t == null) return "An unexpected error occurred.";
        String msg = t.getMessage() != null ? t.getMessage().toLowerCase() : "";
        if (msg.contains("vercel") || msg.contains("4.5mb") || msg.contains("payload too large") || msg.contains("upload-url")) {
            return "Video file size is too large for upload. Please select a shorter video clip and try again.";
        }
        if (t instanceof java.net.UnknownHostException || 
            msg.contains("no address associated") || 
            msg.contains("unable to resolve host") ||
            msg.contains("hostname") ||
            msg.contains("dns")) {
            return "No internet connection. Please check your network settings and try again.";
        }
        if (t instanceof java.net.SocketTimeoutException || 
            msg.contains("timeout") ||
            msg.contains("timed out")) {
            return "Network request timed out. Please check your connection and try again.";
        }
        if (t instanceof java.net.ConnectException ||
            msg.contains("failed to connect") ||
            msg.contains("connection refused")) {
            return "Unable to connect to Voice Yanga servers. Please try again later.";
        }
        if (t instanceof java.io.IOException) {
            return "Network error occurred. Please check your connection.";
        }
        return t.getLocalizedMessage() != null ? t.getLocalizedMessage() : "An unexpected error occurred.";
    }
}
