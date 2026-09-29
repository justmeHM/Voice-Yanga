package com.voiceyanga.citizen.data.remote.dto;

public class AnalyticsResponse {
    public boolean success;
    public AnalyticsData data;

    public static class AnalyticsData {
        public boolean accepted;
        public String eventId;
    }
}
