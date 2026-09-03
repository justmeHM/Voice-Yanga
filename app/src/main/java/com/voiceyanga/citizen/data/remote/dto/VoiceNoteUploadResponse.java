package com.voiceyanga.citizen.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class VoiceNoteUploadResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("data")
    private VoiceNoteData data;

    public boolean isSuccess() { return success; }
    public VoiceNoteData getData() { return data; }

    public static class VoiceNoteData {
        @SerializedName("fileUrl")
        private String fileUrl;

        @SerializedName("durationSeconds")
        private int durationSeconds;

        @SerializedName("mimeType")
        private String mimeType;

        public String getFileUrl() { return fileUrl; }
        public int getDurationSeconds() { return durationSeconds; }
        public String getMimeType() { return mimeType; }
    }
}
