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
import java.util.List;

@Dao
public interface ComplaintDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Complaint complaint);

    @Update
    void update(Complaint complaint);

    @Query("SELECT * FROM complaints ORDER BY createdAt DESC")
    LiveData<List<Complaint>> getAllComplaints();

    @Query("SELECT * FROM complaints WHERE authorEmail = :email ORDER BY createdAt DESC")
    LiveData<List<Complaint>> getMyComplaints(String email);

    @Query("SELECT * FROM complaints WHERE syncStatus = 'PENDING'")
    List<Complaint> getPendingComplaints();

    @Query("SELECT * FROM complaints WHERE clientUuid = :uuid")
    Complaint getComplaintByUuid(String uuid);

    @Query("SELECT * FROM complaints WHERE clientUuid = :uuid")
    LiveData<Complaint> getComplaintByUuidLiveData(String uuid);

    @Query("UPDATE complaints SET supportCount = supportCount + 1 WHERE clientUuid = :uuid")
    void incrementSupportCount(String uuid);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertComment(Comment comment);

    @Query("SELECT * FROM comments WHERE complaintUuid = :complaintUuid ORDER BY createdAt ASC")
    LiveData<List<Comment>> getCommentsForComplaint(String complaintUuid);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertPhoto(ComplaintPhoto photo);

    @Query("SELECT * FROM complaint_photos WHERE complaintUuid = :complaintUuid")
    LiveData<List<ComplaintPhoto>> getPhotosForComplaint(String complaintUuid);

    @Query("SELECT * FROM complaint_photos WHERE complaintUuid = :complaintUuid")
    List<ComplaintPhoto> getPhotosForComplaintSync(String complaintUuid);
}