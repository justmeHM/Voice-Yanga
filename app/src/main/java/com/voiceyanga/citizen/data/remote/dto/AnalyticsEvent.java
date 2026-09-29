package com.voiceyanga.citizen.data.remote.dto;

import java.util.Map;

public class AnalyticsEvent {
    private String idempotencyKey;
    private int schemaVersion = 1;
    private String eventName;
    private String occurredAt;
    private String anonymousUserId;
    private String sessionId;
    private String platform = "ANDROID";
    private String appVersion;
    private String locationId;
    private String complaintId;
    private String requestId;
    private Map<String, Object> properties;

    public AnalyticsEvent() {}

    public AnalyticsEvent(String idempotencyKey, int schemaVersion, String eventName,
                          String occurredAt, String anonymousUserId, String sessionId,
                          String platform, String appVersion, String locationId,
                          String complaintId, String requestId, Map<String, Object> properties) {
        this.idempotencyKey = idempotencyKey;
        this.schemaVersion = schemaVersion;
        this.eventName = eventName;
        this.occurredAt = occurredAt;
        this.anonymousUserId = anonymousUserId;
        this.sessionId = sessionId;
        this.platform = platform != null ? platform : "ANDROID";
        this.appVersion = appVersion;
        this.locationId = locationId;
        this.complaintId = complaintId;
        this.requestId = requestId;
        this.properties = properties;
    }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public int getSchemaVersion() { return schemaVersion; }
    public void setSchemaVersion(int schemaVersion) { this.schemaVersion = schemaVersion; }

    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }

    public String getOccurredAt() { return occurredAt; }
    public void setOccurredAt(String occurredAt) { this.occurredAt = occurredAt; }

    public String getAnonymousUserId() { return anonymousUserId; }
    public void setAnonymousUserId(String anonymousUserId) { this.anonymousUserId = anonymousUserId; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }

    public String getAppVersion() { return appVersion; }
    public void setAppVersion(String appVersion) { this.appVersion = appVersion; }

    public String getLocationId() { return locationId; }
    public void setLocationId(String locationId) { this.locationId = locationId; }

    public String getComplaintId() { return complaintId; }
    public void setComplaintId(String complaintId) { this.complaintId = complaintId; }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public Map<String, Object> getProperties() { return properties; }
    public void setProperties(Map<String, Object> properties) { this.properties = properties; }
}
