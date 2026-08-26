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

    public ComplaintRequest(String title, String description, String category, String location) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.location = location;
    }
}