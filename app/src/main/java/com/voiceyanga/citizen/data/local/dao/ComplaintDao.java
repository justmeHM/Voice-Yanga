package com.voiceyanga.citizen.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.voiceyanga.citizen.data.local.entity.Complaint;
import java.util.List;

@Dao
public interface ComplaintDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Complaint complaint);

    @Update
    void update(Complaint complaint);

    @Query("SELECT * FROM complaints ORDER BY createdAt DESC")
    LiveData<List<Complaint>> getAllComplaints();

    @Query("SELECT * FROM complaints WHERE syncStatus = 'PENDING'")
    List<Complaint> getPendingComplaints();

    @Query("SELECT * FROM complaints WHERE clientUuid = :uuid")
    Complaint getComplaintByUuid(String uuid);
}