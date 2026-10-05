package com.voiceyanga.citizen.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class NotificationPreferences {
    @SerializedName("inApp")
    public boolean inApp = true;

    @SerializedName("push")
    public boolean push = false;

    @SerializedName("email")
    public boolean email = false;

    @SerializedName("assignmentUpdates")
    public boolean assignmentUpdates = true;

    @SerializedName("statusUpdates")
    public boolean statusUpdates = true;

    @SerializedName("slaAlerts")
    public boolean slaAlerts = true;

    public NotificationPreferences() {}

    public NotificationPreferences(boolean inApp, boolean push, boolean email, boolean assignmentUpdates, boolean statusUpdates, boolean slaAlerts) {
        this.inApp = inApp;
        this.push = push;
        this.email = email;
        this.assignmentUpdates = assignmentUpdates;
        this.statusUpdates = statusUpdates;
        this.slaAlerts = slaAlerts;
    }

    public NotificationPreferences(boolean inApp, boolean push, boolean email, boolean assignmentUpdates, boolean statusUpdates) {
        this(inApp, push, email, assignmentUpdates, statusUpdates, true);
    }
}