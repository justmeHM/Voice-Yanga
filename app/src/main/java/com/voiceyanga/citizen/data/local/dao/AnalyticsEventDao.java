package com.voiceyanga.citizen.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.voiceyanga.citizen.data.local.entity.AnalyticsEventEntity;

import java.util.List;

@Dao
public interface AnalyticsEventDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(AnalyticsEventEntity event);

    @Query("SELECT * FROM analytics_events WHERE nextAttemptAt <= :now ORDER BY localId ASC LIMIT :limit")
    List<AnalyticsEventEntity> getDueEvents(long now, int limit);

    @Query("DELETE FROM analytics_events WHERE localId = :localId")
    void deleteByLocalId(long localId);

    @Query("DELETE FROM analytics_events WHERE idempotencyKey = :idempotencyKey")
    void deleteByIdempotencyKey(String idempotencyKey);

    @Update
    void update(AnalyticsEventEntity event);

    @Query("SELECT COUNT(*) FROM analytics_events")
    int getQueueSize();

    @Query("DELETE FROM analytics_events WHERE localId IN (SELECT localId FROM analytics_events ORDER BY localId ASC LIMIT :limit)")
    void deleteOldestEvents(int limit);

    @Query("DELETE FROM analytics_events")
    void clearAll();
}
