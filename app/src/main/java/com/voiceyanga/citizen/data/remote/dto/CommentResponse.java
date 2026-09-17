package com.voiceyanga.citizen.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class CommentResponse {
    @SerializedName("id")
    private String id;

    @SerializedName("authorName")
    private String authorName;

    @SerializedName("content")
    private String message;

    @SerializedName("isOfficial")
    private boolean isOfficial;

    @SerializedName("createdAt")
    private Long createdAt;

    public String getId() { return id; }
    public String getAuthorName() { return authorName; }
    public String getMessage() { return message; }
    public boolean isOfficial() { return isOfficial; }
    public Long getCreatedAt() { return createdAt; }
}