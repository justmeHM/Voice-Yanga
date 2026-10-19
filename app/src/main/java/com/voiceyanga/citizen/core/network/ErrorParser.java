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
            case 400: return "Invalid request. Please check your details and try again.";
            case 401: return "Invalid email or password. Please try again.";
            case 403: return "Access Denied: You don't have permission.";
            case 404: return "Not Found: The requested item does not exist.";
            case 409: return "Conflict: An account or record with these details already exists.";
            case 413: return "File too large: Please select a smaller attachment.";
            case 429: return "Too Many Requests: Please wait a moment and try again.";
            case 500: case 502: case 503: case 504: return "Server is undergoing maintenance. Please try again shortly.";
            default: return "An unexpected network error occurred (" + response.code() + ")";
        }
    }

    public static String parseThrowable(Throwable t) {
        if (t == null) return "An unexpected network error occurred.";
        String msg = t.getMessage() != null ? t.getMessage().toLowerCase() : "";
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
            return "Connection timed out. Please check your network connection and try again.";
        }
        if (t instanceof java.net.ConnectException ||
            msg.contains("failed to connect") ||
            msg.contains("connection refused")) {
            return "Unable to connect to Voice Yanga server. Please try again later.";
        }
        if (t instanceof java.io.IOException) {
            return "Network communication error. Please check your connection.";
        }
        return "Network connection issue. Please try again.";
    }
}
