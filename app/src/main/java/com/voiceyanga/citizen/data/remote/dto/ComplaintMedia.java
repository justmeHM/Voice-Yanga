package com.voiceyanga.citizen.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class ComplaintMedia {
    @SerializedName(value = "id", alternate = {"mediaId", "_id"})
    public String id;

    @SerializedName(value = "fileUrl", alternate = {"url", "photoUrl", "mediaUrl", "path", "filePath"})
    public String fileUrl;

    @SerializedName("mimeType")
    public String mimeType;

    @SerializedName(value = "type", alternate = {"mediaType"})
    public String type; // PHOTO, PROOF_OF_RESOLUTION

    public String getFileUrl() {
        return fileUrl;
    }
}
