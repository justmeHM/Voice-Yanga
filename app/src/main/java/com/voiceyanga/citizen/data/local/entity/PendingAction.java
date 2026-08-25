package com.voiceyanga.citizen.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "pending_actions")
public class PendingAction {
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    @NonNull
    private String actionType; // "SUPPORT", "COMMENT"
    
    @NonNull
    private String complaintUuid;
    
    private String data; // For comment: the message. For support: null.
    
    private long createdAt;

    public PendingAction(@NonNull String actionType, @NonNull String complaintUuid, String data) {
        this.actionType = actionType;
        this.complaintUuid = complaintUuid;
        this.data = data;
        this.createdAt = System.currentTimeMillis();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    @NonNull
    public String getActionType() { return actionType; }
    public void setActionType(@NonNull String actionType) { this.actionType = actionType; }

    @NonNull
    public String getComplaintUuid() { return complaintUuid; }
    public void setComplaintUuid(@NonNull String complaintUuid) { this.complaintUuid = complaintUuid; }

    public String getData() { return data; }
    public void setData(String data) { this.data = data; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}