package com.voiceyanga.citizen.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Standard envelope for all API responses.
 */
public class BaseResponse<T> {
    @SerializedName("success")
    private boolean success;

    @SerializedName("data")
    private T data;

    @SerializedName("message")
    private String message;

    public boolean isSuccess() {
        return success;
    }

    public T getData() {
        return data;
    }

    public String getMessage() {
        return message;
    }
}
