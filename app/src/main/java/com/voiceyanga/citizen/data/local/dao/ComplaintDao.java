package com.voiceyanga.citizen.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;
import com.voiceyanga.citizen.data.local.entity.Comment;
import com.voiceyanga.citizen.data.local.entity.Complaint;
import com.voiceyanga.citizen.data.local.entity.ComplaintPhoto;
import com.voiceyanga.citizen.data.local.entity.PendingAction;
import java.util.List;

@Dao
public interface ComplaintDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Complaint complaint);

    @Update
    void update(Complaint complaint);

    @Query("SELECT * FROM complaints ORDER BY createdAt DESC")
    LiveData<List<Complaint>> getAllComplaints();

    @Query("SELECT * FROM complaints WHERE authorEmail = :email AND status != 'RESOLVED' ORDER BY createdAt DESC")
    LiveData<List<Complaint>> getMyActiveComplaints(String email);

    @Query("SELECT * FROM complaints WHERE authorEmail = :email AND status = 'RESOLVED' ORDER BY createdAt DESC")
    LiveData<List<Complaint>> getMyResolvedComplaints(String email);

    @Query("SELECT * FROM complaints WHERE authorEmail != :email AND supportedByMe = 1 ORDER BY createdAt DESC")
    LiveData<List<Complaint>> getMySupportedComplaints(String email);

    @Query("SELECT * FROM complaints WHERE authorEmail = :email ORDER BY createdAt DESC LIMIT 1")
    LiveData<Complaint> getLatestMyComplaint(String email);

    @Query("SELECT * FROM complaints WHERE syncStatus = 'DRAFT' LIMIT 1")
    Complaint getDraft();

    @Query("DELETE FROM complaints WHERE syncStatus = 'DRAFT'")
    void deleteDraft();

    @Query("SELECT COUNT(*) FROM complaints WHERE authorEmail = :email")
    LiveData<Integer> getMyReportsCount(String email);

    @Query("SELECT COUNT(*) FROM complaints WHERE supportedByMe = 1")
    LiveData<Integer> getSupportedCount();

    @Query("SELECT * FROM complaints WHERE authorEmail != :email AND syncStatus = 'SYNCED' ORDER BY createdAt DESC")
    LiveData<List<Complaint>> getCommunityComplaints(String email);

    @Query("SELECT * FROM complaints WHERE " +
           "syncStatus = 'SYNCED' " +
           "AND (:status IS NULL OR status = :status) " +
           "AND (:category IS NULL OR category = :category) " +
           "AND (:query IS NULL OR title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' OR location LIKE '%' || :query || '%') " +
           "AND (" +
           "   (:ward IS NULL OR ward = '' OR location LIKE '%' || :ward || '%') " +
           "   OR (:district IS NULL OR district = '' OR location LIKE '%' || :district || '%') " +
           "   OR (:province IS NULL OR province = '' OR location LIKE '%' || :province || '%')" +
           ") " +
           "ORDER BY createdAt DESC")
    LiveData<List<Complaint>> getFilteredCommunityComplaints(String status, String category, String query, String ward, String district, String province);

    @Query("SELECT * FROM complaints WHERE syncStatus IN ('DRAFT', 'PENDING', 'FAILED') ORDER BY createdAt DESC")
    LiveData<List<Complaint>> getOutboxComplaints();

    @Query("SELECT * FROM complaints WHERE syncStatus = 'PENDING'")
    List<Complaint> getPendingComplaints();

    @Query("SELECT * FROM complaints WHERE clientUuid = :uuid")
    Complaint getComplaintByUuid(String uuid);

    @Query("SELECT * FROM complaints WHERE clientUuid = :uuid")
    LiveData<Complaint> getComplaintByUuidLiveData(String uuid);

    @Query("SELECT * FROM complaints WHERE title = :title AND authorEmail = :email AND syncStatus != 'SYNCED' LIMIT 1")
    Complaint findLocalPendingMatch(String title, String email);

    @Query("SELECT photoUri FROM complaint_photos WHERE complaintUuid = :complaintUuid LIMIT 1")
    String getFirstPhotoUri(String complaintUuid);

    @Query("SELECT * FROM complaints WHERE status = 'RESOLVED' ORDER BY updatedAt DESC")
    LiveData<List<Complaint>> getResolvedComplaints();

    @Query("UPDATE complaints SET supportCount = supportCount + 1, supportedByMe = 1 WHERE clientUuid = :uuid AND supportedByMe = 0")
    void incrementSupportCount(String uuid);

    @Query("UPDATE complaints SET commentCount = (SELECT COUNT(*) FROM comments WHERE complaintUuid = :uuid) WHERE clientUuid = :uuid")
    void updateCommentCount(String uuid);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertComment(Comment comment);

    @Query("DELETE FROM comments WHERE complaintUuid = :complaintUuid AND authorName = :author AND content = :content")
    void deleteLocalComment(String complaintUuid, String author, String content);

    @Query("SELECT * FROM comments WHERE complaintUuid = :complaintUuid ORDER BY createdAt ASC")
    LiveData<List<Comment>> getCommentsForComplaint(String complaintUuid);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertPhoto(ComplaintPhoto photo);

    @Query("SELECT * FROM complaint_photos WHERE complaintUuid = :complaintUuid")
    LiveData<List<ComplaintPhoto>> getPhotosForComplaint(String complaintUuid);

    @Query("SELECT * FROM complaint_photos WHERE complaintUuid = :complaintUuid")
    List<ComplaintPhoto> getPhotosForComplaintSync(String complaintUuid);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertPendingAction(PendingAction action);

    @Query("SELECT * FROM pending_actions ORDER BY createdAt ASC")
    List<PendingAction> getAllPendingActions();

    @Query("DELETE FROM pending_actions WHERE id = :id")
    void deletePendingAction(int id);
}
