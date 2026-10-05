package com.voiceyanga.citizen.data.remote.dto;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import com.voiceyanga.citizen.data.repository.ComplaintRepository;

public class CommentResponse {
    @SerializedName("id")
    private String id;

    @SerializedName(value = "authorName", alternate = {"author", "name", "userName", "user", "author_name"})
    private String authorName;

    @SerializedName(value = "content", alternate = {"message", "text", "body"})
    private String content;

    @SerializedName(value = "isOfficial", alternate = {"official", "is_official"})
    private boolean isOfficial;

    @SerializedName("createdAt")
    private JsonElement createdAt;

    public String getId() { return id; }

    public String getAuthorName() {
        if (authorName != null && !authorName.trim().isEmpty()) return authorName;
        return "Citizen";
    }

    public String getContent() { return content; }

    public boolean isOfficial() { return isOfficial; }

    public long getCreatedAtMs() {
        if (createdAt == null || createdAt.isJsonNull()) return System.currentTimeMillis();
        if (createdAt.isJsonPrimitive() && createdAt.getAsJsonPrimitive().isNumber()) {
            return createdAt.getAsLong();
        }
        return ComplaintRepository.parseServerDate(createdAt.getAsString());
    }

    public Long getCreatedAt() {
        return getCreatedAtMs();
    }
}