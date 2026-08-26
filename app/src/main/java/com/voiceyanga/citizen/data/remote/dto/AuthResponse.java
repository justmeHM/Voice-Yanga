package com.voiceyanga.citizen.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class AuthResponse {
    @SerializedName(value = "accessToken", alternate = {"token", "access_token"})
    private String accessToken;

    @SerializedName(value = "refreshToken", alternate = {"refresh_token"})
    private String refreshToken;
    
    @SerializedName("user")
    private UserDto user;

    public String getAccessToken() { return accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public UserDto getUser() { return user; }
}