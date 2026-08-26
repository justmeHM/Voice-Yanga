package com.voiceyanga.citizen.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Entity to store local file references for complaint photos.
 * [FR-COMP-03] Up to 5 photos per complaint.
 */
@Entity(
    tableName = "complaint_photos",
    foreignKeys = @ForeignKey(
        entity = Complaint.class,
        parentColumns = "clientUuid",
        childColumns = "complaintUuid",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {@Index("complaintUuid")}
)
public class ComplaintPhoto {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @NonNull
    private String complaintUuid;

    @NonNull
    private String photoUri; // Local file path or content URI

    private String label; // "Close-up", "Wide-view", etc.

    public ComplaintPhoto(@NonNull String complaintUuid, @NonNull String photoUri) {
        this.complaintUuid = complaintUuid;
        this.photoUri = photoUri;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    @NonNull
    public String getComplaintUuid() { return complaintUuid; }
    public void setComplaintUuid(@NonNull String complaintUuid) { this.complaintUuid = complaintUuid; }

    @NonNull
    public String getPhotoUri() { return photoUri; }
    public void setPhotoUri(@NonNull String photoUri) { this.photoUri = photoUri; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
}
