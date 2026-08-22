package com.voiceyanga.citizen.feature.complaints;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.voiceyanga.citizen.data.local.entity.Comment;
import com.voiceyanga.citizen.data.local.entity.Complaint;
import com.voiceyanga.citizen.data.repository.ComplaintRepository;
import java.util.List;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class ComplaintDetailViewModel extends ViewModel {

    private final ComplaintRepository repository;

    @Inject
    public ComplaintDetailViewModel(ComplaintRepository repository) {
        this.repository = repository;
    }

    public LiveData<Complaint> getComplaint(String uuid) {
        return repository.getComplaint(uuid);
    }

    public LiveData<List<Comment>> getComments(String uuid) {
        return repository.getComments(uuid);
    }

    public LiveData<List<com.voiceyanga.citizen.data.local.entity.ComplaintPhoto>> getPhotos(String uuid) {
        return repository.getPhotos(uuid);
    }

    public void supportComplaint(String uuid) {
        repository.supportComplaint(uuid);
    }
}