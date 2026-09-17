package com.voiceyanga.citizen.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class ApiEnvelope<T> {
    @SerializedName("success")
    public boolean success;

    @SerializedName("data")
    public T data;

    @SerializedName("message")
    public String message;
}
