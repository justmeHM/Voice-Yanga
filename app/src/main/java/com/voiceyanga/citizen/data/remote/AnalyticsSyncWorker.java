package com.voiceyanga.citizen.data.remote;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.hilt.work.HiltWorker;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.gson.Gson;
import com.voiceyanga.citizen.data.local.dao.AnalyticsEventDao;
import com.voiceyanga.citizen.data.local.entity.AnalyticsEventEntity;
import com.voiceyanga.citizen.data.remote.api.ApiService;
import com.voiceyanga.citizen.data.remote.dto.AnalyticsEvent;
import com.voiceyanga.citizen.data.remote.dto.AnalyticsResponse;

import java.io.IOException;
import java.util.List;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;
import retrofit2.Response;

@HiltWorker
public class AnalyticsSyncWorker extends Worker {

    private static final String TAG = "AnalyticsSyncWorker";
    private final AnalyticsEventDao analyticsEventDao;
    private final ApiService apiService;
    private final Gson gson = new Gson();

    @AssistedInject
    public AnalyticsSyncWorker(
            @Assisted @NonNull Context context,
            @Assisted @NonNull WorkerParameters params,
            AnalyticsEventDao analyticsEventDao,
            ApiService apiService) {
        super(context, params);
        this.analyticsEventDao = analyticsEventDao;
        this.apiService = apiService;
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Starting analytics worker doWork");
        try {
            long now = System.currentTimeMillis();
            List<AnalyticsEventEntity> dueEvents = analyticsEventDao.getDueEvents(now, 50);
            if (dueEvents == null || dueEvents.isEmpty()) {
                return Result.success();
            }

            for (AnalyticsEventEntity entity : dueEvents) {
                try {
                    AnalyticsEvent event = gson.fromJson(entity.getPayloadJson(), AnalyticsEvent.class);
                    if (event == null) {
                        analyticsEventDao.deleteByLocalId(entity.getLocalId());
                        continue;
                    }

                    Response<AnalyticsResponse> response = apiService.sendAnalyticsEvent(event).execute();
                    int code = response.code();

                    if (code == 202 || (code >= 200 && code < 300) || code == 409 || code == 400) {
                        // 202 Success, 409 Duplicate (already exists), or 400 Validation failure -> remove from queue permanently
                        analyticsEventDao.deleteByLocalId(entity.getLocalId());
                    } else {
                        // Transient failure: 408, 429, 5xx etc.
                        handleTransientFailure(entity, null, code);
                    }
                } catch (IOException e) {
                    handleTransientFailure(entity, e, 0);
                } catch (Exception e) {
                    // Malformed JSON or unexpected error -> drop event safely
                    analyticsEventDao.deleteByLocalId(entity.getLocalId());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing analytics queue", e);
        }
        return Result.success();
    }

    private void handleTransientFailure(AnalyticsEventEntity entity, Exception e, int httpCode) {
        int nextAttempt = entity.getAttemptCount() + 1;
        if (nextAttempt >= 10) {
            analyticsEventDao.deleteByLocalId(entity.getLocalId());
            return;
        }
        long delay = Math.min(300_000L, 5000L * (1L << (nextAttempt - 1)));
        entity.setAttemptCount(nextAttempt);
        entity.setNextAttemptAt(System.currentTimeMillis() + delay);
        entity.setLastErrorClass(e != null ? e.getClass().getSimpleName() : "HTTP_" + httpCode);
        analyticsEventDao.update(entity);
    }
}
