package com.voiceyanga.citizen.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class LocationDto {
    @SerializedName("id")
    private String id;

    @SerializedName("ward")
    private String ward;

    @SerializedName("district")
    private String district;

    @SerializedName("constituency")
    private String constituency;

    @SerializedName("province")
    private String province;

    @SerializedName("latitude")
    private Double latitude;

    @SerializedName("longitude")
    private Double longitude;

    public String getId() { return id; }
    public String getWard() { return ward; }
    public String getDistrict() { return district; }
    public String getConstituency() { return constituency; }
    public String getProvince() { return province; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    
    public String getDisplayName() {
        if (ward != null && !ward.isEmpty()) {
            return ward;
        } else if (constituency != null && !constituency.isEmpty()) {
            return constituency;
        } else {
            return district != null ? district : "Unknown Location";
        }
    }
}