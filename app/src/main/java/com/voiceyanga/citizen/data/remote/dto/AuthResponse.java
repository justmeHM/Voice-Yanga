package com.voiceyanga.citizen.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class AuthResponse {
    @SerializedName("accessToken")
    private String accessToken;

    @SerializedName("token") // Alternative key used in some endpoints
    private String token;

    @SerializedName("refreshToken")
    private String refreshToken;
    
    @SerializedName("user")
    private UserDto user;

    public String getAccessToken() { 
        return accessToken != null ? accessToken : token; 
    }
    public String getRefreshToken() { return refreshToken; }
    public UserDto getUser() { return user; }
}