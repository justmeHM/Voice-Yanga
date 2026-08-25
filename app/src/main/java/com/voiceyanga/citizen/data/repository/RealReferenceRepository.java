package com.voiceyanga.citizen.data.repository;

import com.voiceyanga.citizen.data.remote.api.ApiService;
import com.voiceyanga.citizen.data.remote.dto.BaseResponse;
import com.voiceyanga.citizen.data.remote.dto.CategoryDto;
import com.voiceyanga.citizen.data.remote.dto.LocationDto;
import com.voiceyanga.citizen.data.remote.dto.PaginatedResponse;
import com.voiceyanga.citizen.domain.repository.ReferenceRepository;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@Singleton
public class RealReferenceRepository implements ReferenceRepository {

    private final ApiService apiService;

    @Inject
    public RealReferenceRepository(ApiService apiService) {
        this.apiService = apiService;
    }

    @Override
    public void getCategories(ReferenceCallback<List<CategoryDto>> callback) {
        apiService.getCategories().enqueue(new Callback<BaseResponse<List<CategoryDto>>>() {
            @Override
            public void onResponse(Call<BaseResponse<List<CategoryDto>>> call, Response<BaseResponse<List<CategoryDto>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    callback.onSuccess(response.body().getData());
                } else {
                    callback.onError("Failed to fetch categories");
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<List<CategoryDto>>> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    @Override
    public void getLocations(ReferenceCallback<List<LocationDto>> callback) {
        apiService.getLocations().enqueue(new Callback<BaseResponse<List<LocationDto>>>() {
            @Override
            public void onResponse(Call<BaseResponse<List<LocationDto>>> call, Response<BaseResponse<List<LocationDto>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    callback.onSuccess(response.body().getData());
                } else {
                    callback.onError("Failed to fetch locations");
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<List<LocationDto>>> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }
}