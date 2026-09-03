package com.voiceyanga.citizen.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class ComplaintDto {
    @SerializedName("clientUuid")
    private String clientUuid;

    @SerializedName("serverId")
    private String serverId;

    @SerializedName("referenceCode")
    private String referenceCode;

    @SerializedName("title")
    private String title;

    @SerializedName("description")
    private String description;

    @SerializedName("category")
    private CategoryDto category;

    @SerializedName("location")
    private LocationDto location;

    @SerializedName("priority")
    private String priority;

    @SerializedName("status")
    private String status;

    @SerializedName("supportCount")
    private int supportCount;

    @SerializedName("createdAt")
    private long createdAt;

    @SerializedName("updatedAt")
    private long updatedAt;

    @SerializedName("latitude")
    private double latitude;

    @SerializedName("longitude")
    private double longitude;

    @SerializedName("authorEmail")
    private String authorEmail;

    @SerializedName("photos")
    private java.util.List<String> photos;

    public String getClientUuid() { return clientUuid; }
    public String getServerId() { return serverId; }
    public String getReferenceCode() { return referenceCode; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public CategoryDto getCategory() { return category; }
    public LocationDto getLocation() { return location; }
    public String getPriority() { return priority; }
    public String getStatus() { return status; }
    public int getSupportCount() { return supportCount; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getAuthorEmail() { return authorEmail; }
    public java.util.List<String> getPhotos() { return photos; }
}