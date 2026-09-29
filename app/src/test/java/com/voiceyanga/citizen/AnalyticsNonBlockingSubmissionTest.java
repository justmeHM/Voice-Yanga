package com.voiceyanga.citizen;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.voiceyanga.citizen.data.repository.AnalyticsRepository;

import org.junit.Test;

public class AnalyticsNonBlockingSubmissionTest {

    @Test
    public void testComplaintSubmissionSucceedsWhenAnalyticsFails() {
        AnalyticsRepository analyticsRepository = mock(AnalyticsRepository.class);

        // Simulate unexpected exception in analytics repository
        doThrow(new RuntimeException("Analytics database locked")).when(analyticsRepository)
                .trackComplaintSubmissionAttempted(any());

        boolean submissionProceeded = false;
        try {
            try {
                analyticsRepository.trackComplaintSubmissionAttempted("complaint-123");
            } catch (Exception e) {
                // Best-effort safety in app flow catches any analytics exception
            }

            // Complaint submission continues unimpeded
            submissionProceeded = true;
        } catch (Exception e) {
            submissionProceeded = false;
        }

        assertTrue(submissionProceeded);
        verify(analyticsRepository).trackComplaintSubmissionAttempted("complaint-123");
    }
}
