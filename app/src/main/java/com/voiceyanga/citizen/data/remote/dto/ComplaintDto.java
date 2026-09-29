package com.voiceyanga.citizen.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class ComplaintDto {
    @SerializedName("id")
    private String id;

    @SerializedName("serverId")
    private String serverId;

    @SerializedName("userId")
    private String userId;

    @SerializedName("referenceCode")
    private String referenceCode;

    @SerializedName("clientUuid")
    private String clientUuid;

    @SerializedName("title")
    private String title;

    @SerializedName("description")
    private String description;

    @SerializedName("status")
    private String status;

    @SerializedName("priority")
    private String priority;

    @SerializedName("createdAt")
    private com.google.gson.JsonElement createdAt;

    @SerializedName("updatedAt")
    private com.google.gson.JsonElement updatedAt;

    @SerializedName("slaDueAt")
    private Long slaDueAt;

    @SerializedName("category")
    private CategoryDto category;

    @SerializedName("location")
    private LocationDto location;

    @SerializedName("media")
    private java.util.List<ComplaintMedia> media;

    @SerializedName("reporter")
    private UserDto reporter;

    @SerializedName("assignedTo")
    private UserDto assignedTo;

    @SerializedName("organization")
    private OrganizationDto organization;

    @SerializedName("assignedAt")
    private Long assignedAt;

    @SerializedName("assignmentAcceptedAt")
    private Long assignmentAcceptedAt;

    @SerializedName("assignmentId")
    private String assignmentId;

    @SerializedName("isOverdue")
    private boolean isOverdue;

    @SerializedName("latitude")
    private double latitude;

    @SerializedName("longitude")
    private double longitude;

    @SerializedName("authorEmail")
    private String authorEmail;

    @SerializedName("proofOfResolutionUrl")
    private String proofOfResolutionUrl;

    @SerializedName(value = "voiceNoteUrl", alternate = {"voiceUrl", "audioUrl", "voiceNote"})
    private String voiceNoteUrl;

    @SerializedName(value = "voiceNoteDurationSeconds", alternate = {"voiceNoteDuration", "duration", "voiceDuration"})
    private Integer voiceNoteDurationSeconds;

    @SerializedName("assignedOrganization")
    private String assignedOrganization;

    @SerializedName("supportCount")
    private int supportCount;

    public String getUserId() { return userId; }
    public String getClientUuid() { return clientUuid; }
    public String getServerId() { 
        return id != null ? id : serverId; 
    }
    public String getReferenceCode() { return referenceCode; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public CategoryDto getCategory() { return category; }
    public LocationDto getLocation() { return location; }
    public String getPriority() { return priority; }
    public String getStatus() { return status; }
    
    public int getSupportCount() { 
        return supportCount; 
    }

    public long getCreatedAt() {
        if (createdAt == null || createdAt.isJsonNull()) return System.currentTimeMillis();
        if (createdAt.isJsonPrimitive() && createdAt.getAsJsonPrimitive().isNumber()) {
            return createdAt.getAsLong();
        }
        return com.voiceyanga.citizen.data.repository.ComplaintRepository.parseServerDate(createdAt.getAsString());
    }

    public long getUpdatedAt() {
        if (updatedAt == null || updatedAt.isJsonNull()) return System.currentTimeMillis();
        if (updatedAt.isJsonPrimitive() && updatedAt.getAsJsonPrimitive().isNumber()) {
            return updatedAt.getAsLong();
        }
        return com.voiceyanga.citizen.data.repository.ComplaintRepository.parseServerDate(updatedAt.getAsString());
    }
    public Long getSlaDueAt() { return slaDueAt; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    
    public String getAuthorEmail() { 
        if (authorEmail != null) return authorEmail;
        if (reporter != null) return reporter.getEmail();
        return null;
    }

    public java.util.List<ComplaintMedia> getMedia() {
        return media != null ? media : java.util.Collections.emptyList();
    }

    public java.util.List<String> getPhotos() { 
        if (media == null) return java.util.Collections.emptyList();
        java.util.List<String> photos = new java.util.ArrayList<>();
        for (ComplaintMedia item : media) {
            if ("PHOTO".equals(item.type) || item.type == null) {
                photos.add(item.fileUrl);
            }
        }
        return photos;
    }

    public String getProofOfResolutionUrl() { 
        if (proofOfResolutionUrl != null) return proofOfResolutionUrl;
        if (media != null) {
            for (ComplaintMedia item : media) {
                if ("PROOF_OF_RESOLUTION".equalsIgnoreCase(item.type)) return item.getFileUrl();
            }
        }
        return null;
    }

    public String getVoiceNoteUrl() {
        if (voiceNoteUrl != null && !voiceNoteUrl.isEmpty()) {
            return voiceNoteUrl;
        }
        if (media != null) {
            for (ComplaintMedia item : media) {
                if ("AUDIO".equalsIgnoreCase(item.type) || "VOICE_NOTE".equalsIgnoreCase(item.type)) {
                    String url = item.getFileUrl();
                    if (url != null && !url.isEmpty()) return url;
                }
            }
        }
        return null;
    }

    public int getVoiceNoteDuration() {
        return voiceNoteDurationSeconds != null ? voiceNoteDurationSeconds : 0;
    }

    public String getAssignedToName() {
        return assignedTo != null ? assignedTo.getFullName() : null;
    }

    public String getOrganizationName() { 
        return organization != null ? organization.getName() : assignedOrganization; 
    }

    public Long getAssignedAt() { return assignedAt; }
    public Long getAssignmentAcceptedAt() { return assignmentAcceptedAt; }
    public String getAssignmentId() { return assignmentId; }
    public boolean isOverdue() { return isOverdue; }
}
