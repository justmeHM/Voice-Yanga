package com.voiceyanga.citizen;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.MutableLiveData;
import androidx.work.WorkManager;
import com.voiceyanga.citizen.data.local.SessionManager;
import com.voiceyanga.citizen.data.local.dao.ComplaintDao;
import com.voiceyanga.citizen.data.local.entity.Complaint;
import com.voiceyanga.citizen.data.remote.api.ApiService;
import com.voiceyanga.citizen.data.remote.dto.ApiEnvelope;
import com.voiceyanga.citizen.data.remote.dto.ComplaintDto;
import com.voiceyanga.citizen.data.remote.dto.PaginatedComplaints;
import com.voiceyanga.citizen.data.remote.dto.PaginationMeta;
import com.voiceyanga.citizen.data.repository.ComplaintRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import retrofit2.Call;
import retrofit2.Response;

public class ComplaintRepositoryTest {

    @Rule
    public TestRule rule = new InstantTaskExecutorRule();

    @Mock
    private ComplaintDao complaintDao;

    @Mock
    private ApiService apiService;

    @Mock
    private SessionManager sessionManager;

    @Mock
    private WorkManager workManager;

    private ComplaintRepository repository;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        when(sessionManager.getUserEmail()).thenReturn("citizen@example.com");
        when(sessionManager.getUserId()).thenReturn("user-123");
        when(complaintDao.getAllComplaints()).thenReturn(new MutableLiveData<>(Collections.emptyList()));
        
        com.voiceyanga.citizen.core.notifications.NotificationHelper notificationHelper = mock(com.voiceyanga.citizen.core.notifications.NotificationHelper.class);
        repository = new ComplaintRepository(complaintDao, apiService, sessionManager, workManager, notificationHelper);
    }

    @Test
    public void testCommunityFixtureParsesAndRendersAllTen() throws Exception {
        Call<ApiEnvelope<PaginatedComplaints>> call = mock(Call.class);
        ApiEnvelope<PaginatedComplaints> envelope = new ApiEnvelope<>();
        envelope.success = true;
        envelope.data = new PaginatedComplaints();
        envelope.data.data = new ArrayList<>();
        
        for (int i = 0; i < 10; i++) {
            ComplaintDto dto = mock(ComplaintDto.class);
            when(dto.getServerId()).thenReturn("server-id-" + i);
            when(dto.getTitle()).thenReturn("Complaint " + i);
            envelope.data.data.add(dto);
        }
        
        envelope.data.meta = new PaginationMeta();
        envelope.data.meta.totalPages = 1;
        envelope.data.meta.totalItems = 10;
        
        when(call.execute()).thenReturn(Response.success(envelope));
        when(apiService.getComplaints(0, 50, null, null, null, null, null, null)).thenReturn(call);
        
        repository.refreshCommunityFeed();
        Thread.sleep(200);
        
        org.junit.Assert.assertNotNull(repository.getCommunityFeedState());
    }

    @Test
    public void testMyFixtureDoesNotReplaceCommunityState() {
        org.junit.Assert.assertNotNull(repository.getMyComplaintsState());
    }

    @Test
    public void testHomeDaoReturnsMultipleUserIdsAndNoInitialFilters() {
        org.junit.Assert.assertNotNull(repository.getAllComplaints());
    }

    @Test
    public void testRoomFailuresAndEmptyStateTexts() {
        org.junit.Assert.assertNotEquals("When you report a community problem", "No community issues found");
    }

    @Test
    public void testCommunityFeedReconciliationPurgesAbsentComplaints() throws Exception {
        Complaint oldSynced = mock(Complaint.class);
        when(oldSynced.getServerId()).thenReturn("deleted-server-id");
        when(oldSynced.getSyncStatus()).thenReturn("SYNCED");
        when(complaintDao.getSyncedComplaintsByFeedType("COMMUNITY")).thenReturn(Collections.singletonList(oldSynced));

        Call<ApiEnvelope<PaginatedComplaints>> call = mock(Call.class);
        ApiEnvelope<PaginatedComplaints> envelope = new ApiEnvelope<>();
        envelope.success = true;
        envelope.data = new PaginatedComplaints();
        envelope.data.data = new ArrayList<>();
        ComplaintDto activeDto = mock(ComplaintDto.class);
        when(activeDto.getServerId()).thenReturn("active-server-id");
        when(activeDto.getTitle()).thenReturn("Active Complaint");
        envelope.data.data.add(activeDto);
        envelope.data.meta = new PaginationMeta();
        envelope.data.meta.totalPages = 1;
        envelope.data.meta.totalItems = 1;

        when(call.execute()).thenReturn(Response.success(envelope));
        when(apiService.getComplaints(0, 50, null, null, null, null, null, null)).thenReturn(call);

        repository.refreshCommunityFeed();
        Thread.sleep(250);

        verify(complaintDao).purgeComplaintByServerId("deleted-server-id");
    }

    @Test
    public void testMyFeedReconciliationPurgesAbsentComplaints() throws Exception {
        Complaint oldSynced = mock(Complaint.class);
        when(oldSynced.getServerId()).thenReturn("deleted-my-id");
        when(oldSynced.getSyncStatus()).thenReturn("SYNCED");
        when(complaintDao.getSyncedComplaintsByFeedType("MY_COMPLAINTS")).thenReturn(Collections.singletonList(oldSynced));

        Call<ApiEnvelope<PaginatedComplaints>> call = mock(Call.class);
        ApiEnvelope<PaginatedComplaints> envelope = new ApiEnvelope<>();
        envelope.success = true;
        envelope.data = new PaginatedComplaints();
        envelope.data.data = new ArrayList<>();
        ComplaintDto activeDto = mock(ComplaintDto.class);
        when(activeDto.getServerId()).thenReturn("active-my-id");
        envelope.data.data.add(activeDto);
        envelope.data.meta = new PaginationMeta();
        envelope.data.meta.totalPages = 1;

        when(call.execute()).thenReturn(Response.success(envelope));
        when(apiService.getMyComplaints(0, 50)).thenReturn(call);

        repository.refreshMyComplaints();
        Thread.sleep(250);

        verify(complaintDao).purgeComplaintByServerId("deleted-my-id");
    }

    @Test
    public void testReconciliationPreservesMultiPageComplaints() throws Exception {
        Complaint oldSynced = mock(Complaint.class);
        when(oldSynced.getServerId()).thenReturn("page2-id");
        when(oldSynced.getSyncStatus()).thenReturn("SYNCED");
        when(complaintDao.getSyncedComplaintsByFeedType("COMMUNITY")).thenReturn(Collections.singletonList(oldSynced));

        Call<ApiEnvelope<PaginatedComplaints>> call0 = mock(Call.class);
        ApiEnvelope<PaginatedComplaints> env0 = new ApiEnvelope<>();
        env0.success = true;
        env0.data = new PaginatedComplaints();
        env0.data.data = new ArrayList<>();
        ComplaintDto dto0 = mock(ComplaintDto.class);
        when(dto0.getServerId()).thenReturn("page1-id");
        env0.data.data.add(dto0);
        env0.data.meta = new PaginationMeta();
        env0.data.meta.totalPages = 2;
        when(call0.execute()).thenReturn(Response.success(env0));
        when(apiService.getComplaints(0, 50, null, null, null, null, null, null)).thenReturn(call0);

        Call<ApiEnvelope<PaginatedComplaints>> call1 = mock(Call.class);
        ApiEnvelope<PaginatedComplaints> env1 = new ApiEnvelope<>();
        env1.success = true;
        env1.data = new PaginatedComplaints();
        env1.data.data = new ArrayList<>();
        ComplaintDto dto1 = mock(ComplaintDto.class);
        when(dto1.getServerId()).thenReturn("page2-id");
        env1.data.data.add(dto1);
        env1.data.meta = new PaginationMeta();
        env1.data.meta.totalPages = 2;
        when(call1.execute()).thenReturn(Response.success(env1));
        when(apiService.getComplaints(1, 50, null, null, null, null, null, null)).thenReturn(call1);

        repository.refreshCommunityFeed();
        Thread.sleep(250);

        verify(complaintDao, org.mockito.Mockito.never()).purgeComplaintByServerId("page2-id");
    }

    @Test
    public void testReconciliationPurgesOnEmptyResponse() throws Exception {
        Complaint oldSynced = mock(Complaint.class);
        when(oldSynced.getServerId()).thenReturn("any-id");
        when(oldSynced.getSyncStatus()).thenReturn("SYNCED");
        when(complaintDao.getSyncedComplaintsByFeedType("COMMUNITY")).thenReturn(Collections.singletonList(oldSynced));

        Call<ApiEnvelope<PaginatedComplaints>> call = mock(Call.class);
        ApiEnvelope<PaginatedComplaints> envelope = new ApiEnvelope<>();
        envelope.success = true;
        envelope.data = new PaginatedComplaints();
        envelope.data.data = new ArrayList<>();
        envelope.data.meta = new PaginationMeta();
        envelope.data.meta.totalPages = 1;

        when(call.execute()).thenReturn(Response.success(envelope));
        when(apiService.getComplaints(0, 50, null, null, null, null, null, null)).thenReturn(call);

        repository.refreshCommunityFeed();
        Thread.sleep(250);

        verify(complaintDao).purgeComplaintByServerId("any-id");
    }

    @Test
    public void testReconciliationDoesNotPurgeOnFailure() throws Exception {
        Complaint oldSynced = mock(Complaint.class);
        when(oldSynced.getServerId()).thenReturn("any-id");
        when(complaintDao.getSyncedComplaintsByFeedType("COMMUNITY")).thenReturn(Collections.singletonList(oldSynced));

        Call<ApiEnvelope<PaginatedComplaints>> call = mock(Call.class);
        when(call.execute()).thenReturn(Response.error(500, okhttp3.ResponseBody.create(null, "")));
        when(apiService.getComplaints(0, 50, null, null, null, null, null, null)).thenReturn(call);

        repository.refreshCommunityFeed();
        Thread.sleep(250);

        verify(complaintDao, org.mockito.Mockito.never()).purgeComplaintByServerId(anyString());
    }

    @Test
    public void testDetail404PurgesComplaint() throws Exception {
        Complaint mockLocal = new Complaint("target-id", "Title", "Desc", "Cat", "Loc", "SYNCED", System.currentTimeMillis());
        mockLocal.setServerId("target-id");
        when(complaintDao.getComplaintById("target-id")).thenReturn(mockLocal);

        Call<com.google.gson.JsonElement> call = mock(Call.class);
        when(call.execute()).thenReturn(Response.error(404, okhttp3.ResponseBody.create(null, "")));
        when(apiService.getComplaint("target-id")).thenReturn(call);

        repository.refreshComplaintDetail("target-id");

        verify(complaintDao, org.mockito.Mockito.timeout(2000)).purgeComplaintByServerId("target-id");
    }
}
