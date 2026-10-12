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

    public ComplaintRepository getRepository() {
        return repository;
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
                if (complaints == null || complaints.isEmpty()) return Collections.emptyList();
                
                final float MAX_DISTANCE_METERS = 50000; // 50km radius

                return complaints.stream()
                    .filter(c -> {
                        boolean hasCoords = (c.isHasValidCoordinates() || (c.getLatitude() != 0.0 && c.getLongitude() != 0.0)) && location != null;
                        if (hasCoords) {
                            float[] results = new float[1];
                            android.location.Location.distanceBetween(
                                    location.getLatitude(), location.getLongitude(),
                                    c.getLatitude(), c.getLongitude(),
                                    results
                            );
                            return results[0] <= MAX_DISTANCE_METERS;
                        }
                        return true;
                    })
                    .sorted((c1, c2) -> {
                        boolean c1HasCoords = (c1.isHasValidCoordinates() || (c1.getLatitude() != 0.0 && c1.getLongitude() != 0.0)) && location != null;
                        boolean c2HasCoords = (c2.isHasValidCoordinates() || (c2.getLatitude() != 0.0 && c2.getLongitude() != 0.0)) && location != null;
                        
                        if (c1HasCoords && c2HasCoords) {
                            float[] results1 = new float[1];
                            float[] results2 = new float[1];
                            android.location.Location.distanceBetween(location.getLatitude(), location.getLongitude(), c1.getLatitude(), c1.getLongitude(), results1);
                            android.location.Location.distanceBetween(location.getLatitude(), location.getLongitude(), c2.getLatitude(), c2.getLongitude(), results2);
                            return Float.compare(results1[0], results2[0]);
                        } else if (c1HasCoords) {
                            return -1;
                        } else if (c2HasCoords) {
                            return 1;
                        } else {
                            return Long.compare(c2.getCreatedAt(), c1.getCreatedAt());
                        }
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
