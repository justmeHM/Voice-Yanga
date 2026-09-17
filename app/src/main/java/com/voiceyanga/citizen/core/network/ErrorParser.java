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
}
