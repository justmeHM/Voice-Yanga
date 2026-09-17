package com.voiceyanga.citizen.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class HistoryItem {
    @SerializedName("id")
    public String id;

    @SerializedName("status")
    public String status;

    @SerializedName("comment")
    public String comment;

    @SerializedName("createdAt")
    public String createdAt;

    @SerializedName("updatedBy")
    public String updatedBy;
}
