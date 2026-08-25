package com.voiceyanga.citizen.data.remote.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ComplaintRequest {
    @SerializedName("title")
    private String title;

    @SerializedName("description")
    private String description;

    @SerializedName("category")
    private String category;

    @SerializedName("location")
    private String location;

    @SerializedName("categoryId")
    private String categoryId;

    @SerializedName("locationId")
    private String locationId;

    @SerializedName("photoUrls")
    private List<String> photoUrls;

    public ComplaintRequest(String title, String description, String category, String location) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.location = location;
    }

    public void setPhotoUrls(java.util.List<String> photoUrls) {
        this.photoUrls = photoUrls;
    }
}