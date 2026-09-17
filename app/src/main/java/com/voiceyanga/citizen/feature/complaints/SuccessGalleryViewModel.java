package com.voiceyanga.citizen.feature.complaints;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.voiceyanga.citizen.data.local.entity.Complaint;
import com.voiceyanga.citizen.data.repository.ComplaintRepository;
import java.util.List;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class SuccessGalleryViewModel extends ViewModel {

    private final ComplaintRepository repository;

    @Inject
    public SuccessGalleryViewModel(ComplaintRepository repository) {
        this.repository = repository;
    }

    public LiveData<List<Complaint>> getSuccessStories() {
        return repository.getSuccessStories();
    }
}
