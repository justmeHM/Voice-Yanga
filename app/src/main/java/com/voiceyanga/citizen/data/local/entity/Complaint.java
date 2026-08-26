package com.voiceyanga.citizen.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "complaints")
public class Complaint {

    @PrimaryKey
    @NonNull
    private String clientUuid; // Local ID used for offline-first reconciliation
    
    private String serverId;
    private String referenceCode;
    private String title;
    private String description;
    private String category; // Display Name
    private String categoryId; // Backend UUID
    private String location; // Display Name
    private String locationId; // Backend UUID
    private String priority;
    private String status;
    private int supportCount;
    private String syncStatus; // PENDING, SYNCING, SYNCED, FAILED
    private String authorEmail;
    private long createdAt;
    private long updatedAt;
    private double latitude;
    private double longitude;
    private boolean supportedByMe;
    private int commentCount;
    private String assignedTo;

    public Complaint(@NonNull String clientUuid, String title, String description, String category, String location, String syncStatus, long createdAt) {
        this.clientUuid = clientUuid;
        this.title = title;
        this.description = description;
        this.category = category;
        this.location = location;
        this.syncStatus = syncStatus;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
        this.status = "SUBMITTED"; // Initial status
        this.priority = "MEDIUM";   // Default priority
    }

    // Getters and Setters
    @NonNull
    public String getClientUuid() { return clientUuid; }
    public void setClientUuid(@NonNull String clientUuid) { this.clientUuid = clientUuid; }

    public String getServerId() { return serverId; }
    public void setServerId(String serverId) { this.serverId = serverId; }

    public String getReferenceCode() { return referenceCode; }
    public void setReferenceCode(String referenceCode) { this.referenceCode = referenceCode; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getLocationId() { return locationId; }
    public void setLocationId(String locationId) { this.locationId = locationId; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getSupportCount() { return supportCount; }
    public void setSupportCount(int supportCount) { this.supportCount = supportCount; }

    public String getSyncStatus() { return syncStatus; }
    public void setSyncStatus(String syncStatus) { this.syncStatus = syncStatus; }

    public String getAuthorEmail() { return authorEmail; }
    public void setAuthorEmail(String authorEmail) { this.authorEmail = authorEmail; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public boolean isSupportedByMe() { return supportedByMe; }
    public void setSupportedByMe(boolean supportedByMe) { this.supportedByMe = supportedByMe; }

    public int getCommentCount() { return commentCount; }
    public void setCommentCount(int commentCount) { this.commentCount = commentCount; }

    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }

    public String getCalculatedPriority() {
        int engagementScore = supportCount + (commentCount * 2); // Comments carry more weight
        if (engagementScore >= 20) return "CRITICAL";
        if (engagementScore >= 10) return "HIGH";
        if (engagementScore >= 5) return "MEDIUM";
        return "LOW";
    }
}