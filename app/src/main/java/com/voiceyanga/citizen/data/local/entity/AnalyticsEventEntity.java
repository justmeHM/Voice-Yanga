package com.voiceyanga.citizen.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "analytics_events",
    indices = {@Index(value = {"idempotencyKey"}, unique = true)}
)
public class AnalyticsEventEntity {

    @PrimaryKey(autoGenerate = true)
    private long localId;

    @ColumnInfo(name = "idempotencyKey")
    @NonNull
    private String idempotencyKey;

    @ColumnInfo(name = "eventName")
    private String eventName;

    @ColumnInfo(name = "payloadJson")
    private String payloadJson;

    @ColumnInfo(name = "createdAt")
    private long createdAt;

    @ColumnInfo(name = "attemptCount", defaultValue = "0")
    private int attemptCount;

    @ColumnInfo(name = "nextAttemptAt", defaultValue = "0")
    private long nextAttemptAt;

    @ColumnInfo(name = "lastErrorClass")
    private String lastErrorClass;

    public AnalyticsEventEntity(@NonNull String idempotencyKey, String eventName, String payloadJson,
                                long createdAt, int attemptCount, long nextAttemptAt, String lastErrorClass) {
        this.idempotencyKey = idempotencyKey;
        this.eventName = eventName;
        this.payloadJson = payloadJson;
        this.createdAt = createdAt;
        this.attemptCount = attemptCount;
        this.nextAttemptAt = nextAttemptAt;
        this.lastErrorClass = lastErrorClass;
    }

    public long getLocalId() { return localId; }
    public void setLocalId(long localId) { this.localId = localId; }

    @NonNull
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(@NonNull String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }

    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public int getAttemptCount() { return attemptCount; }
    public void setAttemptCount(int attemptCount) { this.attemptCount = attemptCount; }

    public long getNextAttemptAt() { return nextAttemptAt; }
    public void setNextAttemptAt(long nextAttemptAt) { this.nextAttemptAt = nextAttemptAt; }

    public String getLastErrorClass() { return lastErrorClass; }
    public void setLastErrorClass(String lastErrorClass) { this.lastErrorClass = lastErrorClass; }
}
