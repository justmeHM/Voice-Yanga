package com.voiceyanga.citizen.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entity for storing in-app notifications.
 * [FR-NOTIF-01] Push notifications.
 */
@Entity(tableName = "notifications")
public class Notification {

    @PrimaryKey
    @NonNull
    private String id;
    
    private String complaintUuid;
    private String title;
    private String message;
    private String type; // STATUS_CHANGE, NEW_COMMENT, SYSTEM
    private boolean isRead;
    private long timestamp;

    public Notification(@NonNull String id, String complaintUuid, String title, String message, String type, long timestamp) {
        this.id = id;
        this.complaintUuid = complaintUuid;
        this.title = title;
        this.message = message;
        this.type = type;
        this.timestamp = timestamp;
        this.isRead = false;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getComplaintUuid() { return complaintUuid; }
    public void setComplaintUuid(String complaintUuid) { this.complaintUuid = complaintUuid; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}