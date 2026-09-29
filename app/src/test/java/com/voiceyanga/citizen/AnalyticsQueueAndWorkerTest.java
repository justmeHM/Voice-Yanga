package com.voiceyanga.citizen;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;

import com.google.gson.Gson;
import com.voiceyanga.citizen.data.local.dao.AnalyticsEventDao;
import com.voiceyanga.citizen.data.local.entity.AnalyticsEventEntity;
import com.voiceyanga.citizen.data.remote.AnalyticsSyncWorker;
import com.voiceyanga.citizen.data.remote.api.ApiService;
import com.voiceyanga.citizen.data.remote.dto.AnalyticsEvent;
import com.voiceyanga.citizen.data.remote.dto.AnalyticsResponse;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.Collections;

import retrofit2.Call;
import retrofit2.Response;

public class AnalyticsQueueAndWorkerTest {

    @Mock
    private Context context;

    @Mock
    private WorkerParameters workerParameters;

    @Mock
    private AnalyticsEventDao analyticsEventDao;

    @Mock
    private ApiService apiService;

    @Mock
    private Call<AnalyticsResponse> apiCall;

    private AnalyticsSyncWorker worker;
    private final Gson gson = new Gson();

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        worker = new AnalyticsSyncWorker(context, workerParameters, analyticsEventDao, apiService);
    }

    @Test
    public void testSuccessful202RemovesEventFromQueue() throws Exception {
        AnalyticsEvent event = new AnalyticsEvent("key-202", 1, "app_session_started", "2026-09-23T10:15:30.000Z", "anon-1", "sess-1", "ANDROID", "1.0.0", null, null, null, Collections.emptyMap());
        AnalyticsEventEntity entity = new AnalyticsEventEntity("key-202", "app_session_started", gson.toJson(event), System.currentTimeMillis(), 0, 0, null);
        entity.setLocalId(101L);

        when(analyticsEventDao.getDueEvents(any(Long.class), eq(50))).thenReturn(Collections.singletonList(entity));
        when(apiService.sendAnalyticsEvent(any())).thenReturn(apiCall);

        AnalyticsResponse responseObj = new AnalyticsResponse();
        responseObj.success = true;
        when(apiCall.execute()).thenReturn(Response.success(202, responseObj));

        ListenableWorker.Result result = worker.doWork();

        assertEquals(ListenableWorker.Result.success(), result);
        verify(analyticsEventDao).deleteByLocalId(101L);
    }

    @Test
    public void testDuplicate409RemovesEventWithoutRetry() throws Exception {
        AnalyticsEvent event = new AnalyticsEvent("key-409", 1, "complaint_supported", "2026-09-23T10:15:30.000Z", "anon-1", "sess-1", "ANDROID", "1.0.0", null, "comp-1", null, Collections.emptyMap());
        AnalyticsEventEntity entity = new AnalyticsEventEntity("key-409", "complaint_supported", gson.toJson(event), System.currentTimeMillis(), 0, 0, null);
        entity.setLocalId(102L);

        when(analyticsEventDao.getDueEvents(any(Long.class), eq(50))).thenReturn(Collections.singletonList(entity));
        when(apiService.sendAnalyticsEvent(any())).thenReturn(apiCall);
        when(apiCall.execute()).thenReturn(Response.error(409, okhttp3.ResponseBody.create(null, "")));

        ListenableWorker.Result result = worker.doWork();

        assertEquals(ListenableWorker.Result.success(), result);
        verify(analyticsEventDao).deleteByLocalId(102L);
        verify(analyticsEventDao, never()).update(any());
    }

    @Test
    public void test400PermanentlyRejectsEvent() throws Exception {
        AnalyticsEvent event = new AnalyticsEvent("key-400", 1, "complaint_viewed", "2026-09-23T10:15:30.000Z", "anon-1", "sess-1", "ANDROID", "1.0.0", null, "comp-1", null, Collections.emptyMap());
        AnalyticsEventEntity entity = new AnalyticsEventEntity("key-400", "complaint_viewed", gson.toJson(event), System.currentTimeMillis(), 0, 0, null);
        entity.setLocalId(103L);

        when(analyticsEventDao.getDueEvents(any(Long.class), eq(50))).thenReturn(Collections.singletonList(entity));
        when(apiService.sendAnalyticsEvent(any())).thenReturn(apiCall);
        when(apiCall.execute()).thenReturn(Response.error(400, okhttp3.ResponseBody.create(null, "")));

        ListenableWorker.Result result = worker.doWork();

        assertEquals(ListenableWorker.Result.success(), result);
        verify(analyticsEventDao).deleteByLocalId(103L);
        verify(analyticsEventDao, never()).update(any());
    }

    @Test
    public void testNetworkFailureRetainsEventAndSchedulesBackoff() throws Exception {
        AnalyticsEvent event = new AnalyticsEvent("key-net-fail", 1, "registration_started", "2026-09-23T10:15:30.000Z", "anon-1", "sess-1", "ANDROID", "1.0.0", null, null, null, Collections.emptyMap());
        AnalyticsEventEntity entity = new AnalyticsEventEntity("key-net-fail", "registration_started", gson.toJson(event), System.currentTimeMillis(), 0, 0, null);
        entity.setLocalId(104L);

        when(analyticsEventDao.getDueEvents(any(Long.class), eq(50))).thenReturn(Collections.singletonList(entity));
        when(apiService.sendAnalyticsEvent(any())).thenReturn(apiCall);
        when(apiCall.execute()).thenThrow(new IOException("Network timeout"));

        ListenableWorker.Result result = worker.doWork();

        assertEquals(ListenableWorker.Result.success(), result);
        verify(analyticsEventDao, never()).deleteByLocalId(104L);

        ArgumentCaptor<AnalyticsEventEntity> captor = ArgumentCaptor.forClass(AnalyticsEventEntity.class);
        verify(analyticsEventDao).update(captor.capture());

        AnalyticsEventEntity updated = captor.getValue();
        assertEquals(1, updated.getAttemptCount());
        assertTrue(updated.getNextAttemptAt() > System.currentTimeMillis());
        assertEquals("IOException", updated.getLastErrorClass());
    }
}
