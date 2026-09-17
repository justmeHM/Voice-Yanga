package com.voiceyanga.citizen.data.repository;

import com.voiceyanga.citizen.data.remote.api.ApiService;
import com.voiceyanga.citizen.data.remote.dto.ApiEnvelope;
import com.voiceyanga.citizen.data.remote.dto.CategoryDto;
import com.voiceyanga.citizen.data.remote.dto.LocationDto;
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
        apiService.getCategories().enqueue(new Callback<ApiEnvelope<List<CategoryDto>>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<List<CategoryDto>>> call, Response<ApiEnvelope<List<CategoryDto>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    callback.onSuccess(response.body().data);
                } else {
                    String msg = "Failed to fetch categories";
                    if (response.body() != null && response.body().message != null) {
                        msg = response.body().message;
                    }
                    callback.onError(msg + " (" + response.code() + ")");
                }
            }

            @Override
            public void onFailure(Call<ApiEnvelope<List<CategoryDto>>> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    @Override
    public void getLocations(ReferenceCallback<List<LocationDto>> callback) {
        apiService.getLocations().enqueue(new Callback<ApiEnvelope<List<LocationDto>>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<List<LocationDto>>> call, Response<ApiEnvelope<List<LocationDto>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    callback.onSuccess(response.body().data);
                } else {
                    String msg = "Failed to fetch locations";
                    if (response.body() != null && response.body().message != null) {
                        msg = response.body().message;
                    }
                    callback.onError(msg + " (" + response.code() + ")");
                }
            }

            @Override
            public void onFailure(Call<ApiEnvelope<List<LocationDto>>> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }
}
