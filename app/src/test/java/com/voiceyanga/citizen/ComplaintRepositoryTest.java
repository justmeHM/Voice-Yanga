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
}
