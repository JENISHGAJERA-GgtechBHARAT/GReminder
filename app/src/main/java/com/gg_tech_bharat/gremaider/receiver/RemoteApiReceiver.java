package com.gg_tech_bharat.gremaider.receiver;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.gg_tech_bharat.gremaider.database.AppDatabase;
import com.gg_tech_bharat.gremaider.database.Reminder;
import com.gg_tech_bharat.gremaider.database.ReminderDao;
import com.gg_tech_bharat.gremaider.widget.ReminderWidgetProvider;

import java.util.concurrent.Executors;

public class RemoteApiReceiver extends BroadcastReceiver {
    private static final String TAG = "RemoteApiReceiver";
    public static final String ACTION_ADD_REMINDER = "com.gg_tech_bharat.gremaider.ACTION_ADD_REMINDER";

    public static final String EXTRA_TITLE = "extra_title";
    public static final String EXTRA_DESCRIPTION = "extra_description";
    public static final String EXTRA_TIMESTAMP = "extra_timestamp";
    public static final String EXTRA_CATEGORY = "extra_category";
    public static final String EXTRA_PRIORITY = "extra_priority";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !ACTION_ADD_REMINDER.equals(intent.getAction())) {
            return;
        }

        String title = intent.getStringExtra(EXTRA_TITLE);
        String description = intent.getStringExtra(EXTRA_DESCRIPTION);
        long timestamp = intent.getLongExtra(EXTRA_TIMESTAMP, System.currentTimeMillis() + 3600000); // Default 1 hour from now
        String category = intent.getStringExtra(EXTRA_CATEGORY);
        String priority = intent.getStringExtra(EXTRA_PRIORITY);

        if (title == null || title.trim().isEmpty()) {
            title = "Follow-up Reminder";
        }
        if (category == null) {
            category = "Communication";
        }
        if (priority == null) {
            priority = "MEDIUM";
        }

        Log.d(TAG, "Remote broadcast received. Title: " + title + ", Time: " + timestamp);

        final String finalTitle = title;
        final String finalDesc = description;
        final String finalCategory = category;
        final String finalPriority = priority;

        AppDatabase db = AppDatabase.getDatabase(context);
        ReminderDao dao = db.reminderDao();

        Executors.newSingleThreadExecutor().execute(() -> {
            Reminder reminder = new Reminder(
                    finalTitle,
                    finalDesc,
                    timestamp,
                    0,
                    "NONE",
                    finalPriority,
                    finalCategory,
                    "",
                    false,
                    false,
                    false,
                    false,
                    "",
                    ""
            );

            long id = dao.insert(reminder);
            reminder.setId((int) id);
            
            // Schedule Alarm
            ReminderAlarmManager.scheduleAlarm(context, reminder);
            
            Log.d(TAG, "Successfully created remote reminder. ID: " + id);

            // Notify homescreen widget to update
            Intent widgetIntent = new Intent(context, ReminderWidgetProvider.class);
            widgetIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            int[] ids = AppWidgetManager.getInstance(context).getAppWidgetIds(
                    new ComponentName(context, ReminderWidgetProvider.class)
            );
            widgetIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
            context.sendBroadcast(widgetIntent);
        });
    }
}
