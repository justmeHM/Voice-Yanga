package com.voiceyanga.citizen;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.work.WorkManager;
import androidx.work.OneTimeWorkRequest;
import com.voiceyanga.citizen.data.local.SessionManager;
import com.voiceyanga.citizen.data.local.dao.ComplaintDao;
import com.voiceyanga.citizen.data.local.entity.Complaint;
import com.voiceyanga.citizen.data.remote.api.ApiService;
import com.voiceyanga.citizen.data.repository.ComplaintRepository;
import java.util.Collections;
import java.util.UUID;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

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
        when(sessionManager.getUserEmail()).thenReturn("test@example.com");
        
        repository = new ComplaintRepository(complaintDao, apiService, sessionManager, workManager);
    }

    @Test
    public void testSaveComplaint() throws InterruptedException {
        String uuid = UUID.randomUUID().toString();
        Complaint complaint = new Complaint(uuid, "Title", "Desc", "Water", "Loc", "PENDING", System.currentTimeMillis());
        
        repository.saveComplaint(complaint, Collections.singletonList("photo_uri"), Collections.emptyMap());
        
        // Wait for executor
        Thread.sleep(500);
        
        verify(complaintDao).insert(complaint);
        verify(complaintDao, atLeastOnce()).insertPhoto(any());
        verify(workManager).enqueueUniqueWork(anyString(), any(), any(OneTimeWorkRequest.class));
    }
}
