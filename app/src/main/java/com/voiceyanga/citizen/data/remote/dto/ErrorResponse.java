package com.voiceyanga.citizen.data.remote.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ErrorResponse {
    @SerializedName("message")
    private String message;

    @SerializedName("error")
    private String error;

    @SerializedName("status")
    private int status;

    @SerializedName("details")
    private List<ErrorDetail> details;

    public String getMessage() { return message; }
    public String getError() { return error; }
    public int getStatus() { return status; }
    public List<ErrorDetail> getDetails() { return details; }

    public static class ErrorDetail {
        @SerializedName("field")
        private String field;
        @SerializedName("message")
        private String message;

        public String getField() { return field; }
        public String getMessage() { return message; }
    }
}