package com.voiceyanga.citizen.core.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.Dispatcher;

public class AuthRefreshTest {

    private MockWebServer server;
    private SessionManager sessionManager;
    private Context context;
    private OkHttpClient client;
    private AtomicReference<String> accessToken;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();

        sessionManager = mock(SessionManager.class);
        context = mock(Context.class);
        
        accessToken = new AtomicReference<>("expired_token");
        when(sessionManager.getAccessToken()).thenAnswer(ignored -> accessToken.get());
        when(sessionManager.getRefreshToken()).thenReturn("valid_refresh_token");
        doAnswer(invocation -> {
            accessToken.set(invocation.getArgument(0));
            return null;
        }).when(sessionManager).saveTokens(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());

        NetworkModule networkModule = new NetworkModule();
        String mockApiUrl = server.url("/api/v1/").toString();
        String mockApiOrigin = server.url("/").toString();
        
        client = networkModule.createOkHttpClient(
                context,
                networkModule.provideLoggingInterceptor(),
                sessionManager,
                mockApiUrl,
                mockApiOrigin);
    }

    @After
    public void tearDown() throws IOException {
        if (client != null) {
            client.connectionPool().evictAll();
        }
        if (server != null) {
            server.shutdown();
        }
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

        try (Response response = client.newCall(request).execute()) {
            assertEquals(200, response.code());
        }
        
        // Verify refresh token was used
        RecordedRequest initialRequest = takeRequest();
        assertEquals("Bearer expired_token", initialRequest.getHeader("Authorization"));
        RecordedRequest refreshRequest = takeRequest();
        assertEquals("/api/v1/auth/refresh", refreshRequest.getPath());
        
        // Verify rotated tokens were saved
        verify(sessionManager).saveTokens("new_access_token", "new_refresh_token");

        // Verify retry used new token
        RecordedRequest retryRequest = takeRequest();
        assertEquals("Bearer new_access_token", retryRequest.getHeader("Authorization"));
    }

    @Test
    public void testConcurrent401TriggersOneRefresh() throws Exception {
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                if ("/api/v1/auth/refresh".equals(request.getPath())) {
                    return new MockResponse()
                            .setResponseCode(200)
                            .setBody("{\"success\":true,\"data\":{\"accessToken\":\"new_access_token\",\"refreshToken\":\"new_refresh_token\"}}");
                }
                if ("Bearer new_access_token".equals(request.getHeader("Authorization"))) {
                    return new MockResponse().setResponseCode(200).setBody("{\"success\":true}");
                }
                return new MockResponse()
                        .setResponseCode(401)
                        .setBody("{\"code\":\"TOKEN_EXPIRED\"}");
            }
        });

        // Run concurrent requests
        Thread t1 = new Thread(() -> {
            try {
                try (Response ignored = client.newCall(new Request.Builder().url(server.url("/api/v1/a")).build()).execute()) {}
            } catch (IOException ignored) {}
        });
        Thread t2 = new Thread(() -> {
            try {
                try (Response ignored = client.newCall(new Request.Builder().url(server.url("/api/v1/b")).build()).execute()) {}
            } catch (IOException ignored) {}
        });

        t1.start();
        t2.start();
        t1.join(TimeUnit.SECONDS.toMillis(10));
        t2.join(TimeUnit.SECONDS.toMillis(10));
        assertFalse("First request thread did not finish", t1.isAlive());
        assertFalse("Second request thread did not finish", t2.isAlive());

        // Verify only one refresh request was made
        int refreshCalls = 0;
        for (int i = 0; i < 5; i++) {
            RecordedRequest req = takeRequest();
            if (req.getPath().contains("/auth/refresh")) {
                refreshCalls++;
            }
        }
        assertEquals(1, refreshCalls);
    }

    private RecordedRequest takeRequest() throws InterruptedException {
        RecordedRequest request = server.takeRequest(10, TimeUnit.SECONDS);
        assertNotNull("Expected request was not received by MockWebServer", request);
        return request;
    }
}
