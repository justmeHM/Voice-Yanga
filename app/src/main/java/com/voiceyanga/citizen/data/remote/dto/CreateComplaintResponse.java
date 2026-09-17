package com.voiceyanga.citizen.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class CreateComplaintResponse {
    @SerializedName("serverId")
    public String serverId;

    @SerializedName("id")
    public String id;

    @SerializedName("referenceCode")
    public String referenceCode;

    public String getServerId() {
        return id != null ? id : serverId;
    }

    @SerializedName("status")
    public String status;

    @SerializedName("syncStatus")
    public String syncStatus;
}
