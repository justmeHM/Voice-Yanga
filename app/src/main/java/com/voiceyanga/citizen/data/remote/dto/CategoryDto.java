package com.voiceyanga.citizen.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class CategoryDto {
    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("isActive")
    private boolean isActive;

    @SerializedName("iconResName")
    private String iconResName;

    public String getId() { return id; }
    public String getName() { return name; }
    public boolean isActive() { return isActive; }
    public String getIconResName() { return iconResName; }

    public CategoryDto() {}

    public CategoryDto(String id, String name) {
        this.id = id;
        this.name = name;
        this.isActive = true;
    }

    public CategoryDto(String id, String name, String iconResName) {
        this.id = id;
        this.name = name;
        this.iconResName = iconResName;
        this.isActive = true;
    }
}