package com.voiceyanga.citizen.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class PresignedUploadRequest {
    @SerializedName("filename")
    public String filename;

    @SerializedName("mimeType")
    public String mimeType;

    @SerializedName("fileSize")
    public long fileSize;

    public PresignedUploadRequest(String filename, String mimeType, long fileSize) {
        this.filename = filename;
        this.mimeType = mimeType != null ? mimeType : "video/mp4";
        this.fileSize = fileSize;
    }
}
