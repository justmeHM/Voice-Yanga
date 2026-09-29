package com.voiceyanga.citizen;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.voiceyanga.citizen.data.remote.dto.AnalyticsEvent;
import com.voiceyanga.citizen.data.repository.AnalyticsRepository;

import org.junit.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class AnalyticsSerializationTest {

    private final Gson gson = new Gson();

    @Test
    public void testEventSerializationUsesUtcIso8601Timestamp() {
        Date now = new Date(1774347330000L); // Fixed timestamp
        String formatted = AnalyticsRepository.formatIso8601Utc(now);
        
        assertNotNull(formatted);
        assertTrue(formatted.endsWith("Z"));
        assertTrue(formatted.contains("T"));
    }

    @Test
    public void testNoForbiddenFieldsInSerializedProperties() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("failureClass", "NETWORK");
        properties.put("connectivity", "ONLINE");
        
        AnalyticsEvent event = new AnalyticsEvent(
                "key-123",
                1,
                "complaint_submission_failed",
                AnalyticsRepository.formatIso8601Utc(new Date()),
                "anon-user-uuid",
                "session-uuid",
                "ANDROID",
                "1.0.0",
                null,
                "complaint-uuid",
                null,
                properties
        );

        String json = gson.toJson(event);
        JsonObject jsonObject = gson.fromJson(json, JsonObject.class);

        assertNotNull(jsonObject);
        assertEquals("complaint_submission_failed", jsonObject.get("eventName").getAsString());
        assertEquals("ANDROID", jsonObject.get("platform").getAsString());

        JsonObject propsJson = jsonObject.getAsJsonObject("properties");
        assertNotNull(propsJson);
        assertTrue(propsJson.has("failureClass"));
        assertFalse(propsJson.has("title"));
        assertFalse(propsJson.has("description"));
        assertFalse(propsJson.has("email"));
        assertFalse(propsJson.has("phone"));
        assertFalse(propsJson.has("password"));
        assertFalse(propsJson.has("token"));
        assertFalse(propsJson.has("locationText"));
        assertFalse(propsJson.has("mediaBytes"));
        assertFalse(propsJson.has("rawAudio"));
        assertFalse(propsJson.has("verificationCode"));
    }

    @Test
    public void testAuthAndLifecycleEventsSerialization() {
        AnalyticsEvent event = new AnalyticsEvent(
                "key-login",
                1,
                "login_succeeded",
                AnalyticsRepository.formatIso8601Utc(new Date()),
                "anon-user-uuid",
                "session-uuid",
                "ANDROID",
                "1.0.0",
                null,
                null,
                null,
                new HashMap<>()
        );

        String json = gson.toJson(event);
        JsonObject jsonObject = gson.fromJson(json, JsonObject.class);

        assertNotNull(jsonObject);
        assertEquals("login_succeeded", jsonObject.get("eventName").getAsString());
        assertEquals("ANDROID", jsonObject.get("platform").getAsString());
    }
}
