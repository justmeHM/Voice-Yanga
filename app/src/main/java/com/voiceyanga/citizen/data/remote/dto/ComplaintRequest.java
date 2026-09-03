package com.voiceyanga.citizen.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class ComplaintRequest {
    @SerializedName("title")
    private String title;

    @SerializedName("description")
    private String description;

    @SerializedName("category")
    private String category;

    @SerializedName("location")
    private String location;

    @SerializedName("photos")
    private java.util.List<String> photos;

    @SerializedName("voiceNoteUrl")
    private String voiceNoteUrl;

    @SerializedName("voiceNoteDurationSeconds")
    private Integer voiceNoteDurationSeconds;

    @SerializedName("clientUuid")
    private String clientUuid;

    public ComplaintRequest(String title, String description, String category, String location, java.util.List<String> photos) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.location = location;
        this.photos = photos;
    }

    public void setVoiceNoteUrl(String voiceNoteUrl) {
        this.voiceNoteUrl = voiceNoteUrl;
    }

    public void setVoiceNoteDurationSeconds(Integer voiceNoteDurationSeconds) {
        this.voiceNoteDurationSeconds = voiceNoteDurationSeconds;
    }

    public void setClientUuid(String clientUuid) {
        this.clientUuid = clientUuid;
    }
}