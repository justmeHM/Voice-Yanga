package com.voiceyanga.citizen.feature.home;

import com.voiceyanga.citizen.data.local.entity.Complaint;
import java.util.Collections;
import java.util.List;

public class CommunityFeedState {
    private final List<Complaint> items;
    private final boolean isInitialLoading;
    private final boolean isRefreshing;
    private final boolean hasCompletedSuccessfulRefresh;
    private final boolean isStale;
    private final String error;
    private final int page;
    private final int totalPages;
    private final int totalItems;

    public CommunityFeedState(List<Complaint> items, boolean isInitialLoading, boolean isRefreshing,
                              boolean hasCompletedSuccessfulRefresh, boolean isStale, String error,
                              int page, int totalPages, int totalItems) {
        this.items = items != null ? items : Collections.emptyList();
        this.isInitialLoading = isInitialLoading;
        this.isRefreshing = isRefreshing;
        this.hasCompletedSuccessfulRefresh = hasCompletedSuccessfulRefresh;
        this.isStale = isStale;
        this.error = error;
        this.page = page;
        this.totalPages = totalPages;
        this.totalItems = totalItems;
    }

    public static CommunityFeedState createInitial() {
        return new CommunityFeedState(Collections.emptyList(), false, false, false, false, null, 0, 0, 0);
    }

    public List<Complaint> getItems() { return items; }
    public boolean isInitialLoading() { return isInitialLoading; }
    public boolean isRefreshing() { return isRefreshing; }
    public boolean hasCompletedSuccessfulRefresh() { return hasCompletedSuccessfulRefresh; }
    public boolean isStale() { return isStale; }
    public String getError() { return error; }
    public int getPage() { return page; }
    public int getTotalPages() { return totalPages; }
    public int getTotalItems() { return totalItems; }
}
