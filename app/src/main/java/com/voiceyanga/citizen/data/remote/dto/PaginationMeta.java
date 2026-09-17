package com.voiceyanga.citizen.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class PaginationMeta {
    @SerializedName("totalItems")
    public int totalItems;

    @SerializedName("totalPages")
    public int totalPages;

    @SerializedName("page")
    public int page;

    @SerializedName("limit")
    public int limit;
}
