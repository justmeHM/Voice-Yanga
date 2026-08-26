package com.voiceyanga.citizen.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class UserDto {
    @SerializedName("id")
    private String id;

    @SerializedName(value = "firstName", alternate = {"first_name"})
    private String firstName;

    @SerializedName(value = "lastName", alternate = {"last_name"})
    private String lastName;

    @SerializedName(value = "name", alternate = {"full_name"})
    private String name;
    
    @SerializedName("email")
    private String email;
    
    @SerializedName("phone")
    private String phone;

    @SerializedName("role")
    private String role;

    @SerializedName(value = "locationId", alternate = {"location_id"})
    private String locationId;

    @SerializedName(value = "fcmToken", alternate = {"fcm_token"})
    private String fcmToken;

    public String getId() { return id; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getRole() { return role != null ? role : "CITIZEN"; }
    public String getLocationId() { return locationId; }
    public String getFcmToken() { return fcmToken; }

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