package com.voiceyanga.citizen.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class PresignedUploadResponse {
    @SerializedName("uploadUrl")
    public String uploadUrl;

    @SerializedName(value = "fileUrl", alternate = {"url", "publicUrl"})
    public String fileUrl;

    @SerializedName("httpMethod")
    public String httpMethod;
}
