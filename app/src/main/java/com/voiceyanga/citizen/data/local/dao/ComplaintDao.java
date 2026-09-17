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

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(Complaint complaint);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAll(List<Complaint> complaints);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertFeedMemberships(List<com.voiceyanga.citizen.data.local.entity.ComplaintFeedMembership> memberships);

    @Query("DELETE FROM complaint_feed_membership WHERE feedType = :feedType")
    void deleteFeedMemberships(String feedType);

    @Update
    void update(Complaint complaint);

    @Query("SELECT * FROM complaints ORDER BY createdAt DESC")
    LiveData<List<Complaint>> getAllComplaints();

    @Query("SELECT * FROM complaints WHERE (serverId IN (SELECT serverId FROM complaint_feed_membership WHERE feedType = 'MY_COMPLAINTS') OR (syncStatus != 'DRAFT' AND (userId = :userId OR authorEmail = :email))) AND status != 'RESOLVED' ORDER BY createdAt DESC")
    LiveData<List<Complaint>> getMyActiveComplaints(String userId, String email);

    @Query("SELECT * FROM complaints WHERE (serverId IN (SELECT serverId FROM complaint_feed_membership WHERE feedType = 'MY_COMPLAINTS') OR (syncStatus != 'DRAFT' AND (userId = :userId OR authorEmail = :email))) AND status = 'RESOLVED' ORDER BY createdAt DESC")
    LiveData<List<Complaint>> getMyResolvedComplaints(String userId, String email);

    @Query("SELECT * FROM complaints WHERE userId != :userId AND authorEmail != :email AND supportedByMe = 1 ORDER BY createdAt DESC")
    LiveData<List<Complaint>> getMySupportedComplaints(String userId, String email);

    @Query("SELECT * FROM complaints WHERE (userId = :userId OR authorEmail = :email) AND status != 'RESOLVED' AND syncStatus != 'DRAFT' ORDER BY createdAt DESC LIMIT 1")
    LiveData<Complaint> getLatestMyComplaint(String userId, String email);

    @Query("SELECT * FROM complaints WHERE syncStatus = 'DRAFT' LIMIT 1")
    Complaint getDraft();

    @Query("DELETE FROM complaints WHERE syncStatus = 'DRAFT'")
    void deleteDraft();

    @Query("SELECT COUNT(*) FROM complaints WHERE (userId = :userId OR authorEmail = :email) AND syncStatus != 'DRAFT'")
    LiveData<Integer> getMyReportsCount(String userId, String email);

    @Query("SELECT COUNT(*) FROM complaints WHERE supportedByMe = 1")
    LiveData<Integer> getSupportedCount();

    @Query("SELECT COUNT(*) FROM complaint_feed_membership WHERE feedType = 'COMMUNITY'")
    int getCommunityCountSync();

    @Query("SELECT COUNT(*) FROM complaint_feed_membership WHERE feedType = 'MY_COMPLAINTS'")
    int getMyCountSync();

    @Query("SELECT * FROM complaints WHERE authorEmail != :email AND syncStatus = 'SYNCED' ORDER BY createdAt DESC")
    LiveData<List<Complaint>> getCommunityComplaints(String email);

    @Query("SELECT * FROM complaints WHERE " +
           "serverId IN (SELECT serverId FROM complaint_feed_membership WHERE feedType = 'COMMUNITY') " +
           "AND (:status IS NULL OR :status = '' OR status = :status) " +
           "AND (:category IS NULL OR :category = '' OR category = :category) " +
           "AND (:query IS NULL OR :query = '' OR title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' OR location LIKE '%' || :query || '%') " +
           "AND (:ward IS NULL OR :ward = '' OR ward = :ward OR location LIKE '%' || :ward || '%') " +
           "AND (:district IS NULL OR :district = '' OR district = :district OR location LIKE '%' || :district || '%') " +
           "AND (:province IS NULL OR :province = '' OR province = :province OR location LIKE '%' || :province || '%') " +
           "ORDER BY createdAt DESC")
    LiveData<List<Complaint>> getFilteredCommunityComplaints(String status, String category, String query, String ward, String district, String province);

    @Query("SELECT * FROM complaints WHERE syncStatus != 'DRAFT' AND status != 'RESOLVED' AND (:district IS NULL OR district = :district OR location LIKE '%' || :district || '%') ORDER BY supportCount DESC LIMIT 10")
    LiveData<List<Complaint>> getTrendingComplaints(String district);

    @Query("SELECT * FROM complaints WHERE syncStatus IN ('DRAFT', 'PENDING', 'FAILED') ORDER BY createdAt DESC")
    LiveData<List<Complaint>> getOutboxComplaints();

    @Query("SELECT * FROM complaints WHERE syncStatus = 'PENDING'")
    List<Complaint> getPendingComplaints();

    @Query("SELECT * FROM complaints WHERE clientUuid = :uuid")
    Complaint getComplaintByUuid(String uuid);

    @Query("SELECT * FROM complaints WHERE serverId = :serverId")
    Complaint getComplaintByServerId(String serverId);

    @Query("SELECT * FROM complaints WHERE clientUuid = :uuid")
    LiveData<Complaint> getComplaintByUuidLiveData(String uuid);

    @Query("SELECT * FROM complaints WHERE title = :title AND authorEmail = :email AND syncStatus != 'SYNCED' LIMIT 1")
    Complaint findLocalPendingMatch(String title, String email);

    @Query("SELECT photoUri FROM complaint_photos WHERE complaintUuid = :complaintUuid LIMIT 1")
    String getFirstPhotoUri(String complaintUuid);

    @Query("SELECT * FROM complaints WHERE status = 'RESOLVED' ORDER BY updatedAt DESC")
    LiveData<List<Complaint>> getResolvedComplaints();

    @Query("SELECT * FROM complaints WHERE status = 'RESOLVED' AND proofOfResolutionUri IS NOT NULL ORDER BY updatedAt DESC")
    LiveData<List<Complaint>> getResolvedSuccessStories();

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

    @Query("DELETE FROM complaint_photos WHERE complaintUuid = :complaintUuid")
    void deletePhotosForComplaint(String complaintUuid);

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
