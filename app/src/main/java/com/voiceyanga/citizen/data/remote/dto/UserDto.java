package com.voiceyanga.citizen.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class UserDto {
    @SerializedName("id")
    private String id;

    @SerializedName("role")
    private String role;

    @SerializedName("name")
    private String name;

    @SerializedName("firstName")
    private String firstName;

    @SerializedName("lastName")
    private String lastName;
    
    @SerializedName("email")
    private String email;
    
    @SerializedName("phone")
    private String phone;

    @SerializedName("permissions")
    private java.util.List<String> permissions;

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("locationId")
    private String locationId;

    @SerializedName("fcmToken")
    private String fcmToken;

    public String getId() { return id; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getRole() { return role != null ? role : "CITIZEN"; }
    public String getLocationId() { return locationId; }
    public String getFcmToken() { return fcmToken; }
    public java.util.List<String> getPermissions() { return permissions != null ? permissions : new java.util.ArrayList<>(); }
    public String getCreatedAt() { return createdAt; }

    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public String getFullName() {
        if (name != null && !name.isEmpty()) return name;
        if (firstName != null && lastName != null) return firstName + " " + lastName;
        if (firstName != null) return firstName;
        return "Citizen";
    }
}