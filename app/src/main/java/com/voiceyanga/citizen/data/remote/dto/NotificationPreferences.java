package com.voiceyanga.citizen.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class NotificationPreferences {
    @SerializedName("inApp")
    public boolean inApp;

    @SerializedName("push")
    public boolean push;

    @SerializedName("email")
    public boolean email;

    @SerializedName("assignmentUpdates")
    public boolean assignmentUpdates;

    @SerializedName("statusUpdates")
    public boolean statusUpdates;

    public NotificationPreferences(boolean inApp, boolean push, boolean email, boolean assignmentUpdates, boolean statusUpdates) {
        this.inApp = inApp;
        this.push = push;
        this.email = email;
        this.assignmentUpdates = assignmentUpdates;
        this.statusUpdates = statusUpdates;
    }
}