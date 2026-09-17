package com.voiceyanga.citizen.data.remote.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ApiError {
    @SerializedName("timestamp")
    public String timestamp;

    @SerializedName("status")
    public int status;

    @SerializedName("error")
    public boolean error;

    @SerializedName("code")
    public String code;

    @SerializedName("message")
    public String message;

    @SerializedName("path")
    public String path;

    @SerializedName("details")
    public List<FieldError> details;

    public static class FieldError {
        @SerializedName("field")
        public String field;

        @SerializedName("message")
        public String message;
    }
}
