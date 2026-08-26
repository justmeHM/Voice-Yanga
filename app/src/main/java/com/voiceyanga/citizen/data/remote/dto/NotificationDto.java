package com.voiceyanga.citizen.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class NotificationDto {
    @SerializedName("id")
    private String id;

    @SerializedName("complaintUuid")
    private String complaintUuid;

    @SerializedName("title")
    private String title;

    @SerializedName("message")
    private String message;

    @SerializedName("type")
    private String type;

    @SerializedName("isRead")
    private boolean isRead;

    @SerializedName("timestamp")
    private long timestamp;

    public String getId() { return id; }
    public String getComplaintUuid() { return complaintUuid; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getType() { return type; }
    public boolean isRead() { return isRead; }
    public long getTimestamp() { return timestamp; }
}