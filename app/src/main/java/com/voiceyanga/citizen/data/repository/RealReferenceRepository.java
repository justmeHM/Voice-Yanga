package com.voiceyanga.citizen.data.repository;

import com.voiceyanga.citizen.data.remote.api.ApiService;
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
        apiService.getCategories().enqueue(new Callback<List<CategoryDto>>() {
            @Override
            public void onResponse(Call<List<CategoryDto>> call, Response<List<CategoryDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Failed to fetch categories");
                }
            }

            @Override
            public void onFailure(Call<List<CategoryDto>> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    @Override
    public void getLocations(ReferenceCallback<List<LocationDto>> callback) {
        apiService.getLocations().enqueue(new Callback<List<LocationDto>>() {
            @Override
            public void onResponse(Call<List<LocationDto>> call, Response<List<LocationDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Failed to fetch locations");
                }
            }

            @Override
            public void onFailure(Call<List<LocationDto>> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }
}