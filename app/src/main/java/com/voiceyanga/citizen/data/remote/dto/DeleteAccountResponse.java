package com.voiceyanga.citizen.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/**
 * DTO for account deletion response.
 */
public class DeleteAccountResponse {
    @SerializedName("message")
    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
