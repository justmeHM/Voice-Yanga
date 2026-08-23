package com.voiceyanga.citizen.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class UserDto {
    @SerializedName("id")
    private String id;

    @SerializedName("firstName")
    private String firstName;

    @SerializedName("lastName")
    private String lastName;
    
    @SerializedName("email")
    private String email;
    
    @SerializedName("phone")
    private String phone;

    @SerializedName("role")
    private String role;

    public String getId() { return id; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getRole() { return role; }
    
    public String getFullName() {
        return firstName + " " + lastName;
    }
}