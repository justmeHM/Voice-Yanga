package com.voiceyanga.citizen.feature.complaints;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import com.voiceyanga.citizen.data.local.SessionManager;
import com.voiceyanga.citizen.data.local.entity.Complaint;
import com.voiceyanga.citizen.data.repository.ComplaintRepository;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class NearbyIssuesViewModel extends ViewModel {

    private final ComplaintRepository repository;
    private final SessionManager sessionManager;
    private final MutableLiveData<android.location.Location> userLocation = new MutableLiveData<>();

    @Inject
    public NearbyIssuesViewModel(ComplaintRepository repository, SessionManager sessionManager) {
        this.repository = repository;
        this.sessionManager = sessionManager;
    }

    public void setUserLocation(android.location.Location location) {
        userLocation.setValue(location);
    }

    public void refreshData() {
        repository.refreshCommunityFeed();
    }

    public LiveData<List<Complaint>> getNearbyComplaints() {
        return Transformations.switchMap(userLocation, location -> 
            Transformations.map(repository.getCommunityComplaints(null, null, null, null, null, null), complaints -> {
                if (location == null || complaints == null) return complaints;
                
                final float MAX_DISTANCE_METERS = 20000; // 20km

                return complaints.stream()
                    .filter(c -> {
                        if (!c.isHasValidCoordinates()) return false;
                        float[] results = new float[1];
                        android.location.Location.distanceBetween(location.getLatitude(), location.getLongitude(), c.getLatitude(), c.getLongitude(), results);
                        return results[0] <= MAX_DISTANCE_METERS;
                    })
                    .sorted((c1, c2) -> {
                        float[] results1 = new float[1];
                        android.location.Location.distanceBetween(location.getLatitude(), location.getLongitude(), c1.getLatitude(), c1.getLongitude(), results1);
                        
                        float[] results2 = new float[1];
                        android.location.Location.distanceBetween(location.getLatitude(), location.getLongitude(), c2.getLatitude(), c2.getLongitude(), results2);
                        
                        return Float.compare(results1[0], results2[0]);
                    })
                    .collect(Collectors.toList());
            })
        );
    }

    public void supportComplaint(String uuid) {
        repository.supportComplaint(uuid);
    }

    public void retryComplaint(String uuid) {
        repository.retryComplaint(uuid);
    }

    public void retrySync() {
        repository.scheduleSync();
    }
}
