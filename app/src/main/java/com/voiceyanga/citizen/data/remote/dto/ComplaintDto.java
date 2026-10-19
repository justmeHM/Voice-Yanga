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

    @SerializedName(value = "location", alternate = {"locationData"})
    private com.google.gson.JsonElement rawLocation;

    @SerializedName(value = "locationName", alternate = {"address", "locationString", "rawLocationName"})
    private String rawLocationName;

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

    @SerializedName(value = "videoUrl", alternate = {"video", "mediaVideoUrl"})
    private String videoUrl;

    @SerializedName(value = "videoThumbnailUrl", alternate = {"videoThumbnail", "thumbnailUrl"})
    private String videoThumbnailUrl;

    @SerializedName(value = "videoDurationSeconds", alternate = {"videoDuration"})
    private Integer videoDurationSeconds;

    @SerializedName("assignedOrganization")
    private String assignedOrganization;

    @SerializedName("supportCount")
    private int supportCount;

    @SerializedName(value = "supportedByMe", alternate = {"hasSupported", "isSupportedByMe", "hasVoted", "supported", "isSupported", "voted"})
    private Boolean supportedByMe;

    public String getUserId() { return userId; }
    public String getClientUuid() { return clientUuid; }
    public String getServerId() { 
        return id != null ? id : serverId; 
    }
    public String getReferenceCode() { return referenceCode; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public CategoryDto getCategory() { return category; }
    
    public LocationDto getLocation() {
        if (rawLocation != null && rawLocation.isJsonObject()) {
            try {
                return new com.google.gson.Gson().fromJson(rawLocation, LocationDto.class);
            } catch (Exception ignored) {}
        }
        return null;
    }

    public String getLocationName() {
        if (rawLocationName != null && !rawLocationName.trim().isEmpty()) {
            return rawLocationName.trim();
        }
        if (rawLocation != null && rawLocation.isJsonPrimitive()) {
            String s = rawLocation.getAsString();
            if (s != null && !s.trim().isEmpty()) return s.trim();
        }
        if (getLocation() != null) {
            String name = getLocation().getDisplayName();
            if (name != null && !name.trim().isEmpty()) return name.trim();
        }
        return null;
    }
    public String getPriority() { return priority; }
    public String getStatus() { return status; }
    
    public int getSupportCount() { 
        return supportCount; 
    }

    public Boolean getSupportedByMe() {
        return supportedByMe;
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

    public String getVideoUrl() {
        if (videoUrl != null && !videoUrl.isEmpty()) {
            return videoUrl;
        }
        if (media != null) {
            for (ComplaintMedia item : media) {
                String typeStr = item.type != null ? item.type.toLowerCase() : "";
                String mimeStr = item.mimeType != null ? item.mimeType.toLowerCase() : "";
                String urlStr = item.getFileUrl() != null ? item.getFileUrl().toLowerCase() : "";
                boolean isVideo = typeStr.contains("video") || 
                                  mimeStr.contains("video") || 
                                  urlStr.endsWith(".mp4") || 
                                  urlStr.endsWith(".webm") || 
                                  urlStr.endsWith(".3gp") || 
                                  urlStr.contains("/videos/");
                if (isVideo) {
                    String url = item.getFileUrl();
                    if (url != null && !url.isEmpty()) return url;
                }
            }
        }
        return null;
    }

    public String getVideoThumbnailUrl() {
        return videoThumbnailUrl;
    }

    public int getVideoDuration() {
        return videoDurationSeconds != null ? videoDurationSeconds : 0;
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
