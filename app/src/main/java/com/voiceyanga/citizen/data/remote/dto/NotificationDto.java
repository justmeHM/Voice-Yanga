package com.voiceyanga.citizen.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class NotificationDto {
    @SerializedName("id")
    private String id;

    @SerializedName("complaintId")
    private String complaintId;

    @SerializedName("title")
    private String title;

    @SerializedName("message")
    private String message;

    @SerializedName("type")
    private String type;

    @SerializedName("readStatus")
    private boolean readStatus;

    @SerializedName("createdAt")
    private String createdAt;

    public String getId() { return id; }
    public String getComplaintId() { return complaintId; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getType() { return type; }
    public boolean isRead() { return readStatus; }
    public String getCreatedAt() { return createdAt; }
}