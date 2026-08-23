package com.voiceyanga.citizen.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class ComplaintResponse {
    @SerializedName("serverId")
    private String serverId;

    @SerializedName("referenceCode")
    private String referenceCode;

    @SerializedName("status")
    private String status;

    @SerializedName("syncStatus")
    private String syncStatus;

    public String getServerId() { return serverId; }
    public String getReferenceCode() { return referenceCode; }
    public String getStatus() { return status; }
    public String getSyncStatus() { return syncStatus; }
}