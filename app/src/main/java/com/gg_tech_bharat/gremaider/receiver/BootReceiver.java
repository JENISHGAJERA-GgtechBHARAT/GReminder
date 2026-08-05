package com.gg_tech_bharat.gremaider.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.gg_tech_bharat.gremaider.database.AppDatabase;
import com.gg_tech_bharat.gremaider.database.Reminder;
import com.gg_tech_bharat.gremaider.database.ReminderDao;

import java.util.List;
import java.util.concurrent.Executors;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "Boot Completed received. Rescheduling active reminders...");

            AppDatabase db = AppDatabase.getDatabase(context);
            ReminderDao dao = db.reminderDao();

            Executors.newSingleThreadExecutor().execute(() -> {
                List<Reminder> activeReminders = dao.getActiveRemindersSync();
                if (activeReminders != null) {
                    for (Reminder reminder : activeReminders) {
                        ReminderAlarmManager.scheduleAlarm(context, reminder);
                    }
                    Log.d(TAG, "Successfully rescheduled " + activeReminders.size() + " active reminders.");
                }
            });
        }
    }
}
