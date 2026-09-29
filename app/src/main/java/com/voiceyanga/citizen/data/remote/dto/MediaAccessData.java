package com.voiceyanga.citizen.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class MediaAccessData {
    @SerializedName("url")
    public String url;

    @SerializedName("expiresAt")
    public String expiresAt;
}
