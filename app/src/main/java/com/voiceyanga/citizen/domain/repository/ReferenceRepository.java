package com.voiceyanga.citizen.domain.repository;

import com.voiceyanga.citizen.data.remote.dto.CategoryDto;
import com.voiceyanga.citizen.data.remote.dto.LocationDto;
import java.util.List;

public interface ReferenceRepository {
    void getCategories(ReferenceCallback<List<CategoryDto>> callback);
    void getLocations(ReferenceCallback<List<LocationDto>> callback);

    interface ReferenceCallback<T> {
        void onSuccess(T data);
        void onError(String message);
    }
}