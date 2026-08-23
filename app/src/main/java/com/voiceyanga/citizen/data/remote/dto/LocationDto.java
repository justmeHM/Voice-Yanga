package com.voiceyanga.citizen.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class LocationDto {
    @SerializedName("id")
    private String id;

    @SerializedName("ward")
    private String ward;

    @SerializedName("district")
    private String district;

    @SerializedName("province")
    private String province;

    public String getId() { return id; }
    public String getWard() { return ward; }
    public String getDistrict() { return district; }
    public String getProvince() { return province; }
    
    public String getDisplayName() {
        return ward + ", " + district;
    }
}