package com.voiceyanga.citizen.core.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import com.voiceyanga.citizen.BuildConfig;
import com.voiceyanga.citizen.core.di.NetworkModule;
import com.voiceyanga.citizen.data.local.SessionManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

public class AuthRefreshTest {

    private MockWebServer server;
    private SessionManager sessionManager;
    private Context context;
    private OkHttpClient client;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();

        sessionManager = mock(SessionManager.class);
        context = mock(Context.class);
        
        when(sessionManager.getAccessToken()).thenReturn("expired_token");
        when(sessionManager.getRefreshToken()).thenReturn("valid_refresh_token");

        NetworkModule networkModule = new NetworkModule();
        // We need to bypass the BuildConfig.API_BASE_URL check in Interceptor or point it to the mock server
        // Since BuildConfig is final, we'll ensure our requests hit the mock server's host
        
        client = networkModule.provideOkHttpClient(context, networkModule.provideLoggingInterceptor(), sessionManager);
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    public void testTokenRefreshOn401() throws Exception {
        // 1. Initial 401 response with TOKEN_EXPIRED
        server.enqueue(new MockResponse()
                .setResponseCode(401)
                .setBody("{\"code\":\"TOKEN_EXPIRED\"}"));

        // 2. Refresh response
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"success\":true,\"data\":{\"accessToken\":\"new_access_token\",\"refreshToken\":\"new_refresh_token\"}}"));

        // 3. Retry success response
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"success\":true}"));

        Request request = new Request.Builder()
                .url(server.url("/api/v1/voice-notes"))
                .build();

        Response response = client.newCall(request).execute();

        assertEquals(200, response.code());
        
        // Verify refresh token was used
        RecordedRequest refreshRequest = server.takeRequest(); // Initial request
        refreshRequest = server.takeRequest(); // Refresh request
        assertEquals("/api/v1/auth/refresh", refreshRequest.getPath());
        
        // Verify rotated tokens were saved
        verify(sessionManager).saveTokens("new_access_token", "new_refresh_token");

        // Verify retry used new token
        RecordedRequest retryRequest = server.takeRequest();
        assertEquals("Bearer new_access_token", retryRequest.getHeader("Authorization"));
    }

    @Test
    public void testConcurrent401TriggersOneRefresh() throws Exception {
        // Enqueue responses for two concurrent requests
        // Request 1: 401 -> Refresh -> Success
        // Request 2: 401 -> Wait -> Success
        
        server.enqueue(new MockResponse().setResponseCode(401).setBody("{\"code\":\"TOKEN_EXPIRED\"}"));
        server.enqueue(new MockResponse().setResponseCode(401).setBody("{\"code\":\"TOKEN_EXPIRED\"}"));
        
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"success\":true,\"data\":{\"accessToken\":\"new_access_token\",\"refreshToken\":\"new_refresh_token\"}}"));

        server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"success\":true}"));
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"success\":true}"));

        // Run concurrent requests
        Thread t1 = new Thread(() -> {
            try {
                client.newCall(new Request.Builder().url(server.url("/api/v1/a")).build()).execute();
            } catch (IOException ignored) {}
        });
        Thread t2 = new Thread(() -> {
            try {
                client.newCall(new Request.Builder().url(server.url("/api/v1/b")).build()).execute();
            } catch (IOException ignored) {}
        });

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        // Verify only one refresh request was made
        int refreshCalls = 0;
        for (int i = 0; i < 5; i++) {
            RecordedRequest req = server.takeRequest();
            if (req.getPath().contains("/auth/refresh")) {
                refreshCalls++;
            }
        }
        assertEquals(1, refreshCalls);
    }
}
