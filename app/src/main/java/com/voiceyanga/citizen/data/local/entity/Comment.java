package com.voiceyanga.citizen.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "comments",
    foreignKeys = @ForeignKey(
        entity = Complaint.class,
        parentColumns = "clientUuid",
        childColumns = "complaintUuid",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {@Index(value = {"complaintUuid", "authorName", "content"}, unique = true)}
)
public class Comment {

    @PrimaryKey
    @NonNull
    private String id;
    
    @NonNull
    private String complaintUuid;
    
    private String authorName;
    private String content;
    private boolean isOfficial;
    private long createdAt;

    public Comment(@NonNull String id, @NonNull String complaintUuid, String authorName, String content, boolean isOfficial, long createdAt) {
        this.id = id;
        this.complaintUuid = complaintUuid;
        this.authorName = authorName;
        this.content = content;
        this.isOfficial = isOfficial;
        this.createdAt = createdAt;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    @NonNull
    public String getComplaintUuid() { return complaintUuid; }
    public void setComplaintUuid(@NonNull String complaintUuid) { this.complaintUuid = complaintUuid; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public boolean isOfficial() { return isOfficial; }
    public void setOfficial(boolean official) { isOfficial = official; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}