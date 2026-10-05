package com.voiceyanga.citizen.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class RegisterRequest {
    @SerializedName("firstName")
    private final String firstName;
    
    @SerializedName("lastName")
    private final String lastName;
    
    @SerializedName("phone")
    private final String phone;
    
    @SerializedName("email")
    private final String email;
    
    @SerializedName("password")
    private final String password;

    public RegisterRequest(String firstName, String lastName, String phone, String email, String password) {
        this.firstName = firstName != null ? firstName.trim() : "";
        this.lastName = lastName != null ? lastName.trim() : "";
        
        String cleanPhone = phone != null ? phone.trim() : "";
        this.phone = !cleanPhone.isEmpty() ? cleanPhone : null;
        
        String cleanEmail = email != null ? email.trim() : "";
        this.email = !cleanEmail.isEmpty() ? cleanEmail : null;
        
        this.password = password;
    }
}
