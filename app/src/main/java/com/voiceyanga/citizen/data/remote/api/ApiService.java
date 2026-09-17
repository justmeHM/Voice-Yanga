package com.voiceyanga.citizen.data.remote.api;

import com.voiceyanga.citizen.data.remote.dto.ApiEnvelope;
import com.voiceyanga.citizen.data.remote.dto.AuthResponse;
import com.voiceyanga.citizen.data.remote.dto.CategoryDto;
import com.voiceyanga.citizen.data.remote.dto.CommentRequest;
import com.voiceyanga.citizen.data.remote.dto.CommentResponse;
import com.voiceyanga.citizen.data.remote.dto.ComplaintDto;
import com.voiceyanga.citizen.data.remote.dto.ComplaintRequest;
import com.voiceyanga.citizen.data.remote.dto.CreateComplaintResponse;
import com.voiceyanga.citizen.data.remote.dto.HealthResponse;
import com.voiceyanga.citizen.data.remote.dto.HistoryItem;
import com.voiceyanga.citizen.data.remote.dto.LocationDto;
import com.voiceyanga.citizen.data.remote.dto.LoginRequest;
import com.voiceyanga.citizen.data.remote.dto.NotificationDto;
import com.voiceyanga.citizen.data.remote.dto.NotificationPreferences;
import com.voiceyanga.citizen.data.remote.dto.PaginatedComplaints;
import com.voiceyanga.citizen.data.remote.dto.PaginatedNotifications;
import com.voiceyanga.citizen.data.remote.dto.RefreshData;
import com.voiceyanga.citizen.data.remote.dto.RefreshRequest;
import com.voiceyanga.citizen.data.remote.dto.RegisterRequest;
import com.voiceyanga.citizen.data.remote.dto.SupportResponse;
import com.voiceyanga.citizen.data.remote.dto.UserDto;
import com.voiceyanga.citizen.data.remote.dto.VoiceNoteData;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    @GET("/health")
    Call<Void> health();

    @GET("/ready")
    Call<HealthResponse> ready();

    @POST("auth/register")
    Call<AuthResponse> register(@Body RegisterRequest body);

    @POST("auth/login")
    Call<AuthResponse> login(@Body LoginRequest body);

    @POST("auth/refresh")
    Call<ApiEnvelope<RefreshData>> refresh(@Body RefreshRequest body);

    @POST("auth/logout")
    Call<ApiEnvelope<Object>> logout(@Body RefreshRequest body);

    @GET("users/profile")
    Call<UserDto> getProfile();

    @PATCH("users/profile")
    Call<ApiEnvelope<UserDto>> updateProfile(@Body Map<String, Object> body);

    @GET("categories")
    Call<ApiEnvelope<List<CategoryDto>>> getCategories();

    @GET("locations")
    Call<ApiEnvelope<List<LocationDto>>> getLocations();

    @POST("complaints")
    Call<CreateComplaintResponse> createComplaint(@Body ComplaintRequest body);

    @Multipart
    @POST("complaints")
    Call<CreateComplaintResponse> createComplaintWithPhotos(
        @Part("title") RequestBody title,
        @Part("description") RequestBody description,
        @Part("category") RequestBody category,
        @Part("location") RequestBody location,
        @Part("priority") RequestBody priority,
        @Part("clientUuid") RequestBody clientUuid,
        @Part("voiceNoteUrl") RequestBody voiceNoteUrl,
        @Part("voiceNoteDurationSeconds") RequestBody voiceNoteDurationSeconds,
        @Part List<MultipartBody.Part> photos
    );

    @Multipart
    @POST("voice-notes")
    Call<ApiEnvelope<VoiceNoteData>> uploadVoiceNote(
        @Part MultipartBody.Part file,
        @Part("durationSeconds") RequestBody durationSeconds
    );

    @GET("complaints/my")
    Call<ApiEnvelope<PaginatedComplaints>> getMyComplaints(
        @Query("page") int page,
        @Query("limit") int limit
    );

    @GET("complaints")
    Call<ApiEnvelope<PaginatedComplaints>> getComplaints(
        @Query("page") int page,
        @Query("limit") int limit,
        @Query("status") String status,
        @Query("category") String category,
        @Query("search") String search,
        @Query("ward") String ward,
        @Query("district") String district,
        @Query("province") String province
    );

    @GET("complaints/{id}")
    Call<com.google.gson.JsonElement> getComplaint(@Path("id") String id);

    @GET("complaints/{id}/history")
    Call<com.google.gson.JsonElement> getHistory(@Path("id") String id);

    @GET("complaints/{id}/comments")
    Call<List<CommentResponse>> getComments(@Path("id") String id);

    @POST("complaints/{id}/comments")
    Call<ApiEnvelope<CommentResponse>> addComment(
        @Path("id") String id,
        @Body CommentRequest body
    );

    @POST("complaints/{id}/support")
    Call<SupportResponse> support(@Path("id") String id);

    @GET("notifications")
    Call<ApiEnvelope<PaginatedNotifications>> getNotifications(
        @Query("page") int page,
        @Query("limit") int limit,
        @Query("unreadOnly") boolean unreadOnly
    );

    @PATCH("notifications/preferences")
    Call<ApiEnvelope<Object>> updateNotificationPreferences(@Body NotificationPreferences body);

    @PATCH("notifications/{id}/read")
    Call<ApiEnvelope<Object>> markNotificationRead(@Path("id") String id);

    @PATCH("notifications/read-all")
    Call<ApiEnvelope<Object>> markAllNotificationsRead();
}
