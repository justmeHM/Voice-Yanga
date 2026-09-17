package com.voiceyanga.citizen.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class VoiceNoteData {
    @SerializedName("fileUrl")
    public String fileUrl;

    @SerializedName("durationSeconds")
    public int durationSeconds;

    @SerializedName("mimeType")
    public String mimeType;
}
