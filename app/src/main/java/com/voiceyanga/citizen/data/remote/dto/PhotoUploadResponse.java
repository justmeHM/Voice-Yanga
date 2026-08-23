package com.voiceyanga.citizen.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class PhotoUploadResponse {
    @SerializedName("photoUrl")
    private String photoUrl;

    public String getPhotoUrl() { return photoUrl; }
}