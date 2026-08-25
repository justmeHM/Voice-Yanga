package com.voiceyanga.citizen.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class CommentResponse {
    @SerializedName("id")
    private String id;

    @SerializedName("authorName")
    private String authorName;

    @SerializedName("message")
    private String message;

    @SerializedName("isOfficial")
    private boolean isOfficial;

    @SerializedName("createdAt")
    private String createdAt;

    public String getId() { return id; }
    public String getAuthorName() { return authorName; }
    public String getMessage() { return message; }
    public boolean isOfficial() { return isOfficial; }
    public String getCreatedAt() { return createdAt; }
}