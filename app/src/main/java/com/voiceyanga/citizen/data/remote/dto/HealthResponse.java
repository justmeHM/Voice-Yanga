package com.voiceyanga.citizen.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class HealthResponse {
    @SerializedName("status")
    private String status;
    
    @SerializedName("version")
    private String version;

    @SerializedName("communityReadPolicy")
    private String communityReadPolicy;

    public String getStatus() { return status; }
    public String getVersion() { return version; }
    public String getCommunityReadPolicy() { return communityReadPolicy; }
}
