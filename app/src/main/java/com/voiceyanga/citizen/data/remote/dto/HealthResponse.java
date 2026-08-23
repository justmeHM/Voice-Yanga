package com.voiceyanga.citizen.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class HealthResponse {
    @SerializedName("status")
    private String status;
    
    @SerializedName("version")
    private String version;

    public String getStatus() { return status; }
    public String getVersion() { return version; }
}