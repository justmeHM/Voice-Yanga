package com.voiceyanga.citizen.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class RegisterRequest {
    @SerializedName("firstName")
    private final String firstName;
    
    @SerializedName("lastName")
    private final String lastName;
    
    @SerializedName("phone")
    private final String phone;

    @SerializedName("phoneNumber")
    private final String phoneNumber;
    
    @SerializedName("email")
    private final String email;
    
    @SerializedName("password")
    private final String password;

    @SerializedName("role")
    private final String role = "CITIZEN";

    public RegisterRequest(String firstName, String lastName, String phone, String email, String password) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.phoneNumber = phone;
        this.email = email;
        this.password = password;
    }
}