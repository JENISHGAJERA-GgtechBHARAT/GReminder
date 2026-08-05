package com.gg_tech_bharat.gremaider.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ReminderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Reminder reminder);

    @Update
    void update(Reminder reminder);

    @Delete
    void delete(Reminder reminder);

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    Reminder getReminderById(int id);

    @Query("SELECT * FROM reminders WHERE isCompleted = 0 AND isArchived = 0 ORDER BY isPinned DESC, timestamp ASC")
    LiveData<List<Reminder>> getActiveReminders();

    @Query("SELECT * FROM reminders WHERE isCompleted = 0 AND isArchived = 0 ORDER BY isPinned DESC, timestamp ASC")
    List<Reminder> getActiveRemindersSync();

    @Query("SELECT * FROM reminders WHERE isCompleted = 1 AND isArchived = 0 ORDER BY timestamp DESC")
    LiveData<List<Reminder>> getCompletedReminders();

    @Query("SELECT * FROM reminders WHERE isCompleted = 1 AND timestamp >= :startOfDay AND timestamp <= :endOfDay ORDER BY timestamp DESC")
    LiveData<List<Reminder>> getCompletedTodayReminders(long startOfDay, long endOfDay);

    @Query("SELECT * FROM reminders WHERE isPinned = 1 AND isCompleted = 0 AND isArchived = 0 ORDER BY timestamp ASC")
    LiveData<List<Reminder>> getPinnedReminders();

    @Query("SELECT * FROM reminders WHERE isArchived = 1 ORDER BY timestamp DESC")
    LiveData<List<Reminder>> getArchivedReminders();

    @Query("SELECT * FROM reminders WHERE category = :category AND isArchived = 0 ORDER BY isCompleted ASC, timestamp ASC")
    LiveData<List<Reminder>> getRemindersByCategory(String category);

    @Query("SELECT * FROM reminders WHERE (title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%') AND isArchived = 0")
    LiveData<List<Reminder>> searchReminders(String query);

    @Query("SELECT * FROM reminders WHERE timestamp >= :startTime AND timestamp <= :endTime AND isArchived = 0 ORDER BY timestamp ASC")
    LiveData<List<Reminder>> getRemindersInTimeRange(long startTime, long endTime);

    @Query("SELECT * FROM reminders WHERE timestamp >= :startTime AND timestamp <= :endTime AND isArchived = 0 ORDER BY timestamp ASC")
    List<Reminder> getRemindersInTimeRangeSync(long startTime, long endTime);

    @Query("SELECT COUNT(*) FROM reminders WHERE isCompleted = 1")
    LiveData<Integer> getCompletedCount();

    @Query("SELECT COUNT(*) FROM reminders")
    LiveData<Integer> getTotalCount();

    @Query("SELECT COUNT(*) FROM reminders WHERE isCompleted = 1")
    int getCompletedCountSync();

    @Query("SELECT COUNT(*) FROM reminders")
    int getTotalCountSync();

    @Query("DELETE FROM reminders WHERE isCompleted = 1 AND (repeatType = 'NONE' OR repeatType = '') AND timestamp <= :cutoffTime")
    void deleteCompletedOneTimeOlderThan(long cutoffTime);
}
