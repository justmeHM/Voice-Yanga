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

    @SerializedName("data")
    private AuthData data;

    public static class AuthData {
        @SerializedName("token")
        public String token;

        @SerializedName("accessToken")
        public String accessToken;

        @SerializedName("refreshToken")
        public String refreshToken;

        @SerializedName("user")
        public UserDto user;
    }

    public String getToken() { 
        if (data != null) {
            String t = data.accessToken != null ? data.accessToken : data.token;
            if (t != null && !t.isEmpty()) return t;
        }
        return accessToken != null ? accessToken : token; 
    }

    public String getRefreshToken() { 
        if (data != null && data.refreshToken != null && !data.refreshToken.isEmpty()) {
            return data.refreshToken;
        }
        return refreshToken; 
    }

    public UserDto getUser() { 
        if (data != null && data.user != null) {
            return data.user;
        }
        return user; 
    }
}