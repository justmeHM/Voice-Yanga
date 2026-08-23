package com.voiceyanga.citizen.data.remote.api;

import com.voiceyanga.citizen.data.remote.dto.AuthResponse;
import com.voiceyanga.citizen.data.remote.dto.CategoryDto;
import com.voiceyanga.citizen.data.remote.dto.ComplaintRequest;
import com.voiceyanga.citizen.data.remote.dto.ComplaintResponse;
import com.voiceyanga.citizen.data.remote.dto.HealthResponse;
import com.voiceyanga.citizen.data.remote.dto.LocationDto;
import com.voiceyanga.citizen.data.remote.dto.LoginRequest;
import com.voiceyanga.citizen.data.remote.dto.PhotoUploadResponse;
import com.voiceyanga.citizen.data.remote.dto.RegisterRequest;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface ApiService {
    @GET("/health")
    retrofit2.Call<HealthResponse> healthCheck();

    // Authentication
    @POST("auth/login")
    retrofit2.Call<AuthResponse> login(@Body LoginRequest request);

    @POST("auth/register")
    retrofit2.Call<AuthResponse> register(@Body RegisterRequest request);

    @POST("auth/refresh")
    retrofit2.Call<AuthResponse> refreshToken(@Body Map<String, String> body);

    @POST("auth/logout")
    retrofit2.Call<Void> logout(@Body Map<String, String> body);

    @POST("auth/password-reset/request")
    retrofit2.Call<Void> requestPasswordReset(@Body Map<String, String> body);

    // Reference Data
    @GET("categories")
    retrofit2.Call<List<CategoryDto>> getCategories();

    @GET("locations")
    retrofit2.Call<List<LocationDto>> getLocations();

    // Complaints
    @POST("complaints")
    retrofit2.Call<ComplaintResponse> createComplaint(@Body ComplaintRequest request);

    @Multipart
    @POST("photos/upload")
    retrofit2.Call<PhotoUploadResponse> uploadPhoto(
            @Part MultipartBody.Part file,
            @Part("complaintUuid") RequestBody complaintUuid
    );

    @GET("complaints")
    retrofit2.Call<java.util.List<com.voiceyanga.citizen.data.local.entity.Complaint>> getComplaints();
}