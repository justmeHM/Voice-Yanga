package com.voiceyanga.citizen.data.remote.api;

import com.voiceyanga.citizen.data.remote.dto.AuthResponse;
import com.voiceyanga.citizen.data.remote.dto.BaseResponse;
import com.voiceyanga.citizen.data.remote.dto.CategoryDto;
import com.voiceyanga.citizen.data.remote.dto.CommentRequest;
import com.voiceyanga.citizen.data.remote.dto.CommentResponse;
import com.voiceyanga.citizen.data.remote.dto.ComplaintRequest;
import com.voiceyanga.citizen.data.remote.dto.ComplaintResponse;
import com.voiceyanga.citizen.data.remote.dto.HealthResponse;
import com.voiceyanga.citizen.data.remote.dto.LocationDto;
import com.voiceyanga.citizen.data.remote.dto.LoginRequest;
import com.voiceyanga.citizen.data.remote.dto.PaginatedResponse;
import com.voiceyanga.citizen.data.remote.dto.PhotoUploadResponse;
import com.voiceyanga.citizen.data.remote.dto.RegisterRequest;
import com.voiceyanga.citizen.data.remote.dto.UserDto;
import com.voiceyanga.citizen.data.local.entity.Notification;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.QueryMap;

public interface ApiService {
    @GET("/health")
    retrofit2.Call<HealthResponse> healthCheck();

    // Authentication
    @POST("auth/login")
    retrofit2.Call<AuthResponse> login(@Body LoginRequest request);

    @POST("auth/register")
    retrofit2.Call<Void> register(@Body RegisterRequest request);

    @POST("auth/refresh")
    retrofit2.Call<AuthResponse> refreshToken(@Body Map<String, String> body);

    @POST("auth/logout")
    retrofit2.Call<Void> logout(@Body Map<String, String> body);

    @POST("auth/password-reset/request")
    retrofit2.Call<Void> requestPasswordReset(@Body Map<String, String> body);

    // Reference Data
    @GET("categories")
    retrofit2.Call<BaseResponse<List<CategoryDto>>> getCategories();

    @GET("locations")
    retrofit2.Call<BaseResponse<List<LocationDto>>> getLocations();

    // Users
    @GET("users/profile")
    retrofit2.Call<BaseResponse<UserDto>> getProfile();

    @PATCH("users/profile")
    retrofit2.Call<BaseResponse<UserDto>> updateProfile(@Body Map<String, Object> body);

    // Complaints
    @POST("complaints")
    retrofit2.Call<ComplaintResponse> createComplaint(@Body ComplaintRequest request);

    @Multipart
    @POST("photos/upload")
    retrofit2.Call<PhotoUploadResponse> uploadPhoto(@Part MultipartBody.Part file);

    // Notifications
    @GET("notifications")
    retrofit2.Call<BaseResponse<PaginatedResponse<Notification>>> getNotifications();

    @PATCH("notifications/{id}/read")
    retrofit2.Call<Void> markNotificationRead(@Path("id") String id);

    @PATCH("notifications/read-all")
    retrofit2.Call<Void> markAllNotificationsRead();

    @GET("complaints")
    retrofit2.Call<BaseResponse<PaginatedResponse<com.voiceyanga.citizen.data.local.entity.Complaint>>> getComplaints(
            @QueryMap Map<String, String> filters);

    @POST("complaints/{serverId}/support")
    retrofit2.Call<Void> supportComplaint(@retrofit2.http.Path("serverId") String serverId);

    // Comments
    @GET("complaints/{serverId}/comments")
    retrofit2.Call<List<CommentResponse>> getComments(@Path("serverId") String serverId);

    @POST("complaints/{serverId}/comments")
    retrofit2.Call<CommentResponse> postComment(
            @Path("serverId") String serverId,
            @Body CommentRequest request);
}