package com.gg_tech_bharat.gremaider.database;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.gg_tech_bharat.gremaider.calendar.CalendarSyncManager;

import androidx.lifecycle.LiveData;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReminderRepository {
    private final Context context;
    private final ReminderDao reminderDao;
    private final ExecutorService executorService;
    private final Handler mainThreadHandler;

    public ReminderRepository(Application application) {
        this.context = application.getApplicationContext();
        AppDatabase db = AppDatabase.getDatabase(application);
        reminderDao = db.reminderDao();
        executorService = Executors.newSingleThreadExecutor();
        mainThreadHandler = new Handler(Looper.getMainLooper());
    }

    public LiveData<List<Reminder>> getActiveReminders() {
        return reminderDao.getActiveReminders();
    }

    public LiveData<List<Reminder>> getCompletedReminders() {
        return reminderDao.getCompletedReminders();
    }

    public LiveData<List<Reminder>> getCompletedTodayReminders() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long start = cal.getTimeInMillis();
        
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        long end = cal.getTimeInMillis();
        
        return reminderDao.getCompletedTodayReminders(start, end);
    }

    public LiveData<List<Reminder>> getPinnedReminders() {
        return reminderDao.getPinnedReminders();
    }

    public LiveData<List<Reminder>> getArchivedReminders() {
        return reminderDao.getArchivedReminders();
    }

    public LiveData<List<Reminder>> getRemindersByCategory(String category) {
        return reminderDao.getRemindersByCategory(category);
    }

    public LiveData<List<Reminder>> searchReminders(String query) {
        return reminderDao.searchReminders(query);
    }

    public LiveData<List<Reminder>> getRemindersInTimeRange(long start, long end) {
        return reminderDao.getRemindersInTimeRange(start, end);
    }

    public void insert(Reminder reminder, OnCompleteListener<Long> listener) {
        executorService.execute(() -> {
            // Sync with system calendar
            long eventId = CalendarSyncManager.syncReminderToCalendar(context, reminder);
            if (eventId > 0) {
                reminder.setCalendarEventId(eventId);
            }
            long id = reminderDao.insert(reminder);
            if (listener != null) {
                mainThreadHandler.post(() -> listener.onComplete(id));
            }
        });
    }

    public void update(Reminder reminder) {
        executorService.execute(() -> {
            // Update linked calendar event
            long eventId = CalendarSyncManager.syncReminderToCalendar(context, reminder);
            if (eventId > 0) {
                reminder.setCalendarEventId(eventId);
            }
            reminderDao.update(reminder);
        });
    }

    public void delete(Reminder reminder) {
        executorService.execute(() -> {
            // Delete linked calendar event
            CalendarSyncManager.deleteCalendarEvent(context, reminder);
            reminderDao.delete(reminder);
        });
    }

    public void getReminderById(int id, OnCompleteListener<Reminder> listener) {
        executorService.execute(() -> {
            Reminder reminder = reminderDao.getReminderById(id);
            if (listener != null) {
                mainThreadHandler.post(() -> listener.onComplete(reminder));
            }
        });
    }

    public void getActiveRemindersSync(OnCompleteListener<List<Reminder>> listener) {
        executorService.execute(() -> {
            List<Reminder> reminders = reminderDao.getActiveRemindersSync();
            if (listener != null) {
                mainThreadHandler.post(() -> listener.onComplete(reminders));
            }
        });
    }

    public void getRemindersInTimeRangeSync(long start, long end, OnCompleteListener<List<Reminder>> listener) {
        executorService.execute(() -> {
            List<Reminder> reminders = reminderDao.getRemindersInTimeRangeSync(start, end);
            if (listener != null) {
                mainThreadHandler.post(() -> listener.onComplete(reminders));
            }
        });
    }

    public void getStatsSync(OnStatsListener listener) {
        executorService.execute(() -> {
            int total = reminderDao.getTotalCountSync();
            int completed = reminderDao.getCompletedCountSync();
            if (listener != null) {
                mainThreadHandler.post(() -> listener.onStats(total, completed));
            }
        });
    }

    public interface OnCompleteListener<T> {
        void onComplete(T result);
    }

    public interface OnStatsListener {
        void onStats(int total, int completed);
    }
}
