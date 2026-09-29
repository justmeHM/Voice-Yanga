package com.voiceyanga.citizen;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.SharedPreferences;

import com.voiceyanga.citizen.data.local.SessionManager;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

public class AnalyticsIdentityAndSessionTest {

    @Mock
    private Context context;

    @Mock
    private SharedPreferences sharedPreferences;

    @Mock
    private SharedPreferences.Editor editor;

    private final Map<String, String> preferenceMap = new HashMap<>();

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);

        when(context.getSharedPreferences(anyString(), anyInt())).thenReturn(sharedPreferences);
        when(sharedPreferences.edit()).thenReturn(editor);
        when(editor.putBoolean(anyString(), anyBoolean())).thenReturn(editor);

        when(editor.putString(anyString(), anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            String value = invocation.getArgument(1);
            preferenceMap.put(key, value);
            return editor;
        });

        when(editor.remove(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            preferenceMap.remove(key);
            return editor;
        });

        when(sharedPreferences.getString(anyString(), any())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            String defaultValue = invocation.getArgument(1);
            return preferenceMap.getOrDefault(key, defaultValue);
        });
    }

    private String anyString() {
        return org.mockito.ArgumentMatchers.anyString();
    }

    private int anyInt() {
        return org.mockito.ArgumentMatchers.anyInt();
    }

    private boolean anyBoolean() {
        return org.mockito.ArgumentMatchers.anyBoolean();
    }

    private <T> T any() {
        return org.mockito.ArgumentMatchers.any();
    }

    @Test
    public void testAnonymousUserIdIsStableAcrossAppRestarts() {
        SessionManager sessionManager = new SessionManager(context);
        
        String firstId = sessionManager.getAnonymousUserId();
        assertNotNull(firstId);

        // Simulate app restart with same persistent storage
        SessionManager newSessionManager = new SessionManager(context);
        String secondId = newSessionManager.getAnonymousUserId();

        assertEquals(firstId, secondId);
    }

    @Test
    public void testSessionIdChangesOnlyAtSessionBoundary() {
        SessionManager sessionManager = new SessionManager(context);

        String session1 = sessionManager.getSessionId();
        String session1Again = sessionManager.getSessionId();

        assertEquals(session1, session1Again);

        String session2 = sessionManager.rotateSessionId();

        assertNotNull(session2);
        assertNotEquals(session1, session2);
    }

    @Test
    public void testAccountDeletionClearsSessionAndAnonymousIdentity() {
        SessionManager sessionManager = new SessionManager(context);
        sessionManager.saveTokens("access-token-123", "refresh-token-456");
        String anonIdBefore = sessionManager.getAnonymousUserId();
        assertNotNull(anonIdBefore);

        sessionManager.clearSession();
        sessionManager.clearAnonymousUserId();

        assertNull(sessionManager.getAccessToken());
        assertNull(sessionManager.getRefreshToken());
        assertNull(preferenceMap.get("anonymous_user_id"));
    }
}
