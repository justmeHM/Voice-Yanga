package com.voiceyanga.citizen.data.remote;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.voiceyanga.citizen.data.remote.dto.ComplaintRequest;

import org.junit.Test;

public class ComplaintSubmissionTest {

    @Test
    public void testTextOnlyComplaintValidation() {
        ComplaintRequest request = new ComplaintRequest(
                "Blocked drainage",
                "The drainage has been blocked for several days.",
                "Water",
                "Lusaka",
                "MEDIUM",
                "stable-uuid"
        );
        assertTrue("Valid text complaint should pass", request.isValid());

        request.title = "Short";
        assertFalse("Title too short should fail", request.isValid());
    }

    @Test
    public void testVoiceOnlyComplaintValidation() {
        ComplaintRequest request = new ComplaintRequest(
                "Voice report",
                "", // empty description
                "Roads",
                "Kitwe",
                "HIGH",
                "stable-uuid"
        );
        request.voiceNoteUrl = "/uploads/voice-notes/123.m4a";
        request.voiceNoteDurationSeconds = 15;
        assertTrue("Valid voice report with empty description should pass", request.isValid());
    }

    @Test
    public void testStableClientUuid() {
        String uuid = "uuid-123";
        ComplaintRequest request1 = new ComplaintRequest("Title 1", "Desc 123456789", "Cat", "Loc", "LOW", uuid);
        ComplaintRequest request2 = new ComplaintRequest("Title 1", "Desc 123456789", "Cat", "Loc", "LOW", uuid);
        
        assertTrue(request1.isValid());
        assertTrue(request2.isValid());
        assertTrue("UUIDs must match across requests", request1.clientUuid.equals(request2.clientUuid));
    }

    @Test
    public void testInvalidPriority() {
        ComplaintRequest request = new ComplaintRequest(
                "Valid Title",
                "Valid Description",
                "Valid Category",
                "Valid Location",
                "INVALID_PRIORITY",
                "uuid"
        );
        assertFalse("Invalid priority should fail validation", request.isValid());
    }
}
