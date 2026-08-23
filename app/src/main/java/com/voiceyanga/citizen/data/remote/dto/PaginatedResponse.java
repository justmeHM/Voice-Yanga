package com.voiceyanga.citizen.data.remote.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class PaginatedResponse<T> {
    @SerializedName("data")
    private List<T> data;

    @SerializedName("meta")
    private Meta meta;

    public List<T> getData() { return data; }
    public Meta getMeta() { return meta; }

    public static class Meta {
        @SerializedName("totalItems")
        private int totalItems;
        @SerializedName("totalPages")
        private int totalPages;
        @SerializedName("currentPage")
        private int currentPage;

        public int getTotalItems() { return totalItems; }
        public int getTotalPages() { return totalPages; }
        public int getCurrentPage() { return currentPage; }
    }
}