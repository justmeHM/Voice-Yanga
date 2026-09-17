package com.voiceyanga.citizen.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class SupportResponse {
    @SerializedName("success")
    public boolean success;

    @SerializedName("newSupportCount")
    public int supportCount;
}
