package com.voiceyanga.citizen.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class AuthResponse {
    @SerializedName("token")
    private String token;

    @SerializedName("accessToken")
    private String accessToken;

    @SerializedName("refreshToken")
    private String refreshToken;
    
    @SerializedName("user")
    private UserDto user;

    public String getToken() { 
        return accessToken != null ? accessToken : token; 
    }
    public String getRefreshToken() { return refreshToken; }
    public UserDto getUser() { return user; }
}