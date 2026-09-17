package com.voiceyanga.citizen.data.remote.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class PaginatedComplaints {
    @SerializedName("data")
    public List<ComplaintDto> data;

    @SerializedName("meta")
    public PaginationMeta meta;
}
