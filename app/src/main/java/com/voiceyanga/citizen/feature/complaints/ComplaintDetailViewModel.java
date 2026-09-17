package com.voiceyanga.citizen.feature.complaints;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.voiceyanga.citizen.data.local.entity.Comment;
import com.voiceyanga.citizen.data.local.entity.Complaint;
import com.voiceyanga.citizen.data.remote.dto.HistoryItem;
import com.voiceyanga.citizen.data.repository.ComplaintRepository;
import java.util.List;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class ComplaintDetailViewModel extends ViewModel {

    private final ComplaintRepository repository;
    private final MutableLiveData<Boolean> _commentSuccess = new MutableLiveData<>();
    public LiveData<Boolean> getCommentSuccess() { return _commentSuccess; }

    private final MutableLiveData<List<HistoryItem>> _history = new MutableLiveData<>();
    public LiveData<List<HistoryItem>> getHistory() { return _history; }

    @Inject
    public ComplaintDetailViewModel(ComplaintRepository repository) {
        this.repository = repository;
    }

    public LiveData<Complaint> getComplaint(String uuid) {
        repository.refreshComplaintDetail(uuid);
        
        // Also fetch history
        new Thread(() -> {
            Complaint c = repository.getComplaintSync(uuid);
            if (c != null && c.getServerId() != null) {
                repository.getHistory(c.getServerId(), new ComplaintRepository.ReferenceCallback<List<HistoryItem>>() {
                    @Override
                    public void onSuccess(List<HistoryItem> data) {
                        _history.postValue(data);
                    }

                    @Override
                    public void onError(String message) {}
                });
            }
        }).start();
        
        return repository.getComplaint(uuid);
    }

    public void refreshHistory(String uuid) {
        new Thread(() -> {
            Complaint c = repository.getComplaintSync(uuid);
            if (c != null && c.getServerId() != null) {
                repository.getHistory(c.getServerId(), new ComplaintRepository.ReferenceCallback<List<HistoryItem>>() {
                    @Override
                    public void onSuccess(List<HistoryItem> data) {
                        _history.postValue(data);
                    }

                    @Override
                    public void onError(String message) {}
                });
            }
        }).start();
    }

    public LiveData<List<Comment>> getComments(String uuid) {
        // Trigger a background refresh if we have a server ID
        new Thread(() -> {
            Complaint complaint = repository.getComplaintSync(uuid);
            if (complaint != null && complaint.getServerId() != null) {
                repository.refreshComments(complaint.getServerId(), uuid);
            }
        }).start();
        return repository.getComments(uuid);
    }

    public void postComment(String uuid, String message) {
        new Thread(() -> {
            repository.postComment(uuid, message);
            _commentSuccess.postValue(true);
        }).start();
    }

    public LiveData<List<com.voiceyanga.citizen.data.local.entity.ComplaintPhoto>> getPhotos(String uuid) {
        return repository.getPhotos(uuid);
    }

    public void supportComplaint(String uuid) {
        repository.supportComplaint(uuid);
    }

    public void retryComplaint(String uuid) {
        repository.retryComplaint(uuid);
    }
}