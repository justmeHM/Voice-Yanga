package com.voiceyanga.citizen.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class ComplaintMedia {
    @SerializedName("id")
    public String id;

    @SerializedName("fileUrl")
    public String fileUrl;

    @SerializedName("mimeType")
    public String mimeType;

    @SerializedName("type")
    public String type; // PHOTO, PROOF_OF_RESOLUTION
}
