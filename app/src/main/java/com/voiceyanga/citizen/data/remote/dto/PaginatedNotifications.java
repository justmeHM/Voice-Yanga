package com.voiceyanga.citizen.data.remote.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class PaginatedNotifications {
    @SerializedName("data")
    public List<NotificationDto> data;

    @SerializedName("meta")
    public PaginationMeta meta;
}
