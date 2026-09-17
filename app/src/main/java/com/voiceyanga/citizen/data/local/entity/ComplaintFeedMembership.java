package com.voiceyanga.citizen.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;

@Entity(tableName = "complaint_feed_membership", primaryKeys = {"serverId", "feedType"})
public class ComplaintFeedMembership {

    @NonNull
    private String serverId;
    @NonNull
    private String feedType; // "COMMUNITY" or "MY_COMPLAINTS"
    private int page;

    public ComplaintFeedMembership(@NonNull String serverId, @NonNull String feedType, int page) {
        this.serverId = serverId;
        this.feedType = feedType;
        this.page = page;
    }

    @NonNull
    public String getServerId() { return serverId; }
    public void setServerId(@NonNull String serverId) { this.serverId = serverId; }

    @NonNull
    public String getFeedType() { return feedType; }
    public void setFeedType(@NonNull String feedType) { this.feedType = feedType; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
}
