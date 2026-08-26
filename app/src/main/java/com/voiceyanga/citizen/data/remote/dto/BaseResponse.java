package com.voiceyanga.citizen.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Standard envelope for all API responses.
 */
public class BaseResponse<T> {
    @SerializedName("success")
    private Boolean success;

    @SerializedName("error")
    private Boolean error;

    @SerializedName("data")
    private T data;

    @SerializedName("message")
    private String message;

    public boolean isSuccess() {
        if (success != null) return success;
        if (error != null) return !error;
        return true; // Assume success if neither field is present (direct data)
    }

    public T getData() {
        return data;
    }

    public String getMessage() {
        return message;
    }
}
