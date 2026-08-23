package com.voiceyanga.citizen.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class LoginRequest {
    @SerializedName("identifier")
    private String identifier;
    
    @SerializedName("password")
    private String password;

    public LoginRequest(String identifier, String password) {
        this.identifier = identifier;
        this.password = password;
    }
}