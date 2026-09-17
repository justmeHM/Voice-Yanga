package com.voiceyanga.citizen.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class ComplaintRequest {
    @SerializedName("title")
    public String title;

    @SerializedName("description")
    public String description;

    @SerializedName("category")
    public String category;

    @SerializedName("location")
    public String location;

    @SerializedName("priority")
    public String priority;

    @SerializedName("clientUuid")
    public String clientUuid;

    @SerializedName("voiceNoteUrl")
    public String voiceNoteUrl;

    @SerializedName("voiceNoteDurationSeconds")
    public Integer voiceNoteDurationSeconds;

    public ComplaintRequest() {}

    public ComplaintRequest(String title, String description, String category, String location, String priority, String clientUuid) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.location = location;
        this.priority = priority != null ? priority : "MEDIUM";
        this.clientUuid = clientUuid;
    }

    public boolean isValid() {
        if (title == null || title.length() < 5 || title.length() > 150) return false;
        if (category == null || category.isEmpty()) return false;
        if (location == null || location.isEmpty()) return false;
        if (clientUuid == null || clientUuid.isEmpty()) return false;
        
        if (priority != null) {
            String p = priority.toUpperCase();
            if (!p.equals("LOW") && !p.equals("MEDIUM") && !p.equals("HIGH") && !p.equals("CRITICAL")) {
                return false;
            }
        }

        boolean hasVoiceNote = voiceNoteUrl != null && !voiceNoteUrl.isEmpty();
        if (!hasVoiceNote && (description == null || description.length() < 10)) return false;

        return true;
    }
}
