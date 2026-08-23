package com.voiceyanga.citizen.data.remote.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ComplaintRequest {
    @SerializedName("clientUuid")
    private String clientUuid;

    @SerializedName("title")
    private String title;

    @SerializedName("description")
    private String description;

    @SerializedName("categoryId")
    private String categoryId;

    @SerializedName("locationId")
    private String locationId;

    @SerializedName("priority")
    private String priority;

    @SerializedName("authorEmail")
    private String authorEmail;

    @SerializedName("createdAt")
    private long createdAt;

    @SerializedName("photoUrls")
    private List<String> photoUrls;

    public ComplaintRequest(String clientUuid, String title, String description, String categoryId, String locationId, String priority, String authorEmail, long createdAt, List<String> photoUrls) {
        this.clientUuid = clientUuid;
        this.title = title;
        this.description = description;
        this.categoryId = categoryId;
        this.locationId = locationId;
        this.priority = priority;
        this.authorEmail = authorEmail;
        this.createdAt = createdAt;
        this.photoUrls = photoUrls;
    }
}