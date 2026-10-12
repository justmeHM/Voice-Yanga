package com.voiceyanga.citizen.feature.complaints;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.voiceyanga.citizen.data.local.SessionManager;
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
    private final SessionManager sessionManager;
    private final com.voiceyanga.citizen.data.repository.NotificationRepository notificationRepository;
    private final MutableLiveData<Boolean> _commentSuccess = new MutableLiveData<>();
    public LiveData<Boolean> getCommentSuccess() { return _commentSuccess; }

    private final MutableLiveData<List<HistoryItem>> _history = new MutableLiveData<>();
    public LiveData<List<HistoryItem>> getHistory() { return _history; }

    public enum CommentsState { LOADING, SUCCESS, RESTRICTED, ERROR }
    private final MutableLiveData<CommentsState> _commentsState = new MutableLiveData<>(CommentsState.LOADING);
    public LiveData<CommentsState> getCommentsState() { return _commentsState; }

    @Inject
    public ComplaintDetailViewModel(ComplaintRepository repository, SessionManager sessionManager, com.voiceyanga.citizen.data.repository.NotificationRepository notificationRepository) {
        this.repository = repository;
        this.sessionManager = sessionManager;
        this.notificationRepository = notificationRepository;
    }

    public LiveData<Complaint> getComplaint(String uuid) {
        repository.refreshComplaintDetail(uuid);
        
        // Only fetch history if it's our own complaint (community history is restricted)
        new Thread(() -> {
            Complaint c = repository.getComplaintSync(uuid);
            if (c != null && c.getServerId() != null) {
                String currentUser = sessionManager.getUserEmail();
                if (currentUser != null && currentUser.equalsIgnoreCase(c.getAuthorEmail())) {
                    repository.getHistory(c.getServerId(), new ComplaintRepository.ReferenceCallback<List<HistoryItem>>() {
                        @Override
                        public void onSuccess(List<HistoryItem> data) {
                            _history.postValue(data);
                        }

                        @Override
                        public void onError(String message) {}
                    });
                } else {
                    android.util.Log.d("ComplaintDetailVM", "Skipping history fetch for community complaint: " + uuid);
                }
            }
        }).start();
        
        return repository.getComplaint(uuid);
    }

    public void refreshHistory(String uuid) {
        new Thread(() -> {
            Complaint c = repository.getComplaintSync(uuid);
            if (c != null && c.getServerId() != null) {
                String currentUser = sessionManager.getUserEmail();
                if (currentUser != null && currentUser.equalsIgnoreCase(c.getAuthorEmail())) {
                    repository.getHistory(c.getServerId(), new ComplaintRepository.ReferenceCallback<List<HistoryItem>>() {
                        @Override
                        public void onSuccess(List<HistoryItem> data) {
                            _history.postValue(data);
                        }

                        @Override
                        public void onError(String message) {}
                    });
                }
            }
        }).start();
    }

    public LiveData<List<Comment>> getComments(String identifier) {
        // Trigger a background refresh if we have a server ID
        new Thread(() -> {
            Complaint complaint = repository.getComplaintSync(identifier);
            if (complaint != null && complaint.getServerId() != null) {
                _commentsState.postValue(CommentsState.LOADING);
                repository.refreshComments(complaint.getServerId(), complaint.getClientUuid(), _commentsState::postValue);
            }
        }).start();
        return repository.getComments(identifier);
    }

    public void postComment(String uuid, String message) {
        new Thread(() -> {
            repository.postComment(uuid, message);
            if (notificationRepository != null) {
                notificationRepository.refreshNotificationsAsync();
            }
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