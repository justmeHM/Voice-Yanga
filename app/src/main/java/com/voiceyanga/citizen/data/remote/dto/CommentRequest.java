package com.voiceyanga.citizen.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class CommentRequest {
    @SerializedName("message")
    private String message;

    @SerializedName("isCitizenVisible")
    private boolean isCitizenVisible;

    public CommentRequest(String message, boolean isCitizenVisible) {
        this.message = message;
        this.isCitizenVisible = isCitizenVisible;
    }
}