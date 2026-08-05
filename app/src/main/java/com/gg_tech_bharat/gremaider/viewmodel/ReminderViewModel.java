package com.gg_tech_bharat.gremaider.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.gg_tech_bharat.gremaider.database.Reminder;
import com.gg_tech_bharat.gremaider.database.ReminderRepository;

import java.util.List;

public class ReminderViewModel extends AndroidViewModel {

    private final ReminderRepository repository;
    private final LiveData<List<Reminder>> activeReminders;
    private final LiveData<List<Reminder>> completedTodayReminders;
    private final LiveData<List<Reminder>> pinnedReminders;

    public ReminderViewModel(@NonNull Application application) {
        super(application);
        repository = new ReminderRepository(application);
        activeReminders = repository.getActiveReminders();
        completedTodayReminders = repository.getCompletedTodayReminders();
        pinnedReminders = repository.getPinnedReminders();
    }

    public LiveData<List<Reminder>> getActiveReminders() {
        return activeReminders;
    }

    public LiveData<List<Reminder>> getCompletedReminders() {
        return repository.getCompletedReminders();
    }

    public LiveData<List<Reminder>> getCompletedTodayReminders() {
        return completedTodayReminders;
    }

    public LiveData<List<Reminder>> getPinnedReminders() {
        return pinnedReminders;
    }

    public LiveData<List<Reminder>> getRemindersByCategory(String category) {
        return repository.getRemindersByCategory(category);
    }

    public LiveData<List<Reminder>> searchReminders(String query) {
        return repository.searchReminders(query);
    }

    public LiveData<List<Reminder>> getRemindersInTimeRange(long start, long end) {
        return repository.getRemindersInTimeRange(start, end);
    }

    public void insert(Reminder reminder, ReminderRepository.OnCompleteListener<Long> listener) {
        repository.insert(reminder, listener);
    }

    public void update(Reminder reminder) {
        repository.update(reminder);
    }

    public void delete(Reminder reminder) {
        repository.delete(reminder);
    }

    public void getReminderById(int id, ReminderRepository.OnCompleteListener<Reminder> listener) {
        repository.getReminderById(id, listener);
    }

    public void getActiveRemindersSync(ReminderRepository.OnCompleteListener<List<Reminder>> listener) {
        repository.getActiveRemindersSync(listener);
    }

    public void getRemindersInTimeRangeSync(long start, long end, ReminderRepository.OnCompleteListener<List<Reminder>> listener) {
        repository.getRemindersInTimeRangeSync(start, end, listener);
    }

    public void getStatsSync(ReminderRepository.OnStatsListener listener) {
        repository.getStatsSync(listener);
    }
}
