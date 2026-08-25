package com.voiceyanga.citizen.core.network;

import com.google.gson.Gson;
import com.voiceyanga.citizen.data.remote.dto.ErrorResponse;
import okhttp3.ResponseBody;
import retrofit2.Response;

public class ErrorParser {
    private static final Gson gson = new Gson();

    public static String parseError(Response<?> response) {
        try {
            if (response.errorBody() != null) {
                String errorJson = response.errorBody().string();
                ErrorResponse errorBody = gson.fromJson(errorJson, ErrorResponse.class);
                if (errorBody != null && errorBody.getMessage() != null) {
                    return errorBody.getMessage();
                }
            }
        } catch (Exception e) {
            // Fallback
        }

        switch (response.code()) {
            case 401: return "Unauthorized: Please log in again.";
            case 403: return "Forbidden: You don't have permission.";
            case 404: return "Not found on server.";
            case 500: return "Server error. Please try again later.";
            default: return "An unexpected error occurred (" + response.code() + ")";
        }
    }
}