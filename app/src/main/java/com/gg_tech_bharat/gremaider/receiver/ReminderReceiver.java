package com.gg_tech_bharat.gremaider.receiver;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.BroadcastReceiver.PendingResult;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.media.RingtoneManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.gg_tech_bharat.gremaider.MainActivity;
import com.gg_tech_bharat.gremaider.R;
import com.gg_tech_bharat.gremaider.database.AppDatabase;
import com.gg_tech_bharat.gremaider.database.Reminder;
import com.gg_tech_bharat.gremaider.database.ReminderDao;

import java.util.Calendar;
import java.util.concurrent.Executors;

public class ReminderReceiver extends BroadcastReceiver {
    private static final String TAG = "ReminderReceiver";

    public static final String ACTION_COMPLETE = "com.gg_tech_bharat.gremaider.ACTION_COMPLETE";
    public static final String ACTION_SNOOZE = "com.gg_tech_bharat.gremaider.ACTION_SNOOZE";
    private static final String CHANNEL_ID = "greminder_alerts";

    @Override
    public void onReceive(Context context, Intent intent) {
        final PendingResult pendingResult = goAsync();
        String action = intent.getAction();
        int reminderId = intent.getIntExtra(ReminderAlarmManager.EXTRA_REMINDER_ID, -1);
        
        Log.d(TAG, "onReceive - Action: " + action + ", Reminder ID: " + reminderId);

        if (reminderId == -1) {
            pendingResult.finish();
            return;
        }

        AppDatabase db = AppDatabase.getDatabase(context);
        ReminderDao dao = db.reminderDao();

        if (ACTION_COMPLETE.equals(action)) {
            // Handle complete button click from notification
            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    Reminder reminder = dao.getReminderById(reminderId);
                    if (reminder != null) {
                        reminder.setCompleted(true);
                        dao.update(reminder);
                        
                        // Reschedule repeating reminder if necessary
                        if (!"NONE".equals(reminder.getRepeatType())) {
                            rescheduleRepeatingReminder(context, reminder);
                        }
                        
                        // Cancel notification
                        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                        if (notificationManager != null) {
                            notificationManager.cancel(reminderId);
                        }
                    }
                } finally {
                    pendingResult.finish();
                }
            });
        } else if (ACTION_SNOOZE.equals(action)) {
            // Handle snooze button click
            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    Reminder reminder = dao.getReminderById(reminderId);
                    if (reminder != null) {
                        // Snooze for 10 minutes
                        reminder.setTimestamp(System.currentTimeMillis() + 600000); // 10 minutes
                        dao.update(reminder);
                        ReminderAlarmManager.scheduleAlarm(context, reminder);

                        // Cancel notification
                        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                        if (notificationManager != null) {
                            notificationManager.cancel(reminderId);
                        }
                    }
                } finally {
                    pendingResult.finish();
                }
            });
        } else if (ReminderAlarmManager.ACTION_ALARM_TRIGGER.equals(action) || action == null) {
            // Default show notification trigger (Alarm fired)
            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    Reminder reminder = dao.getReminderById(reminderId);
                    if (reminder != null && !reminder.isCompleted() && !reminder.isArchived()) {
                        showNotification(context, reminder);
                        
                        if (!"NONE".equals(reminder.getRepeatType())) {
                            rescheduleRepeatingReminder(context, reminder);
                        }
                    }
                } finally {
                    pendingResult.finish();
                }
            });
        } else {
            pendingResult.finish();
        }
    }

    private void showNotification(Context context, Reminder reminder) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) return;

        android.content.SharedPreferences prefs = context.getSharedPreferences("GReminderPrefs", Context.MODE_PRIVATE);
        String soundUriStr = prefs.getString("pref_notification_sound_uri", "");
        boolean isVibrate = prefs.getBoolean("pref_vibrate_enabled", true);

        android.net.Uri soundUri;
        if (soundUriStr.isEmpty()) {
            soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (soundUri == null) {
                soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }
        } else {
            soundUri = android.net.Uri.parse(soundUriStr);
        }

        // Create Channel for Android O+ with dynamic settings based on user preferences
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Use a unique channel ID if the user wants an alarm sound to ensure it's updated
            String channelId = CHANNEL_ID;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        channelId,
                        "Reminder Alerts",
                        NotificationManager.IMPORTANCE_HIGH
                );
                channel.setDescription("Shows priority on-device AI reminders");
                channel.enableLights(true);
                channel.setLightColor(Color.BLUE);
                
                if (isVibrate) {
                    channel.enableVibration(true);
                    channel.setVibrationPattern(new long[]{0, 800, 800});
                } else {
                    channel.enableVibration(false);
                    channel.setVibrationPattern(null);
                }

                if (soundUri != null) {
                    android.media.AudioAttributes audioAttributes = new android.media.AudioAttributes.Builder()
                            .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build();
                    channel.setSound(soundUri, audioAttributes);
                }

                notificationManager.createNotificationChannel(channel);
            }
        }

        // Tap Action (Open Main Screen)
        Intent tapIntent = new Intent(context, MainActivity.class);
        int tapFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            tapFlags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent tapPendingIntent = PendingIntent.getActivity(context, reminder.getId(), tapIntent, tapFlags);

        // Complete Action
        Intent completeIntent = new Intent(context, ReminderReceiver.class);
        completeIntent.setAction(ACTION_COMPLETE);
        completeIntent.putExtra(ReminderAlarmManager.EXTRA_REMINDER_ID, reminder.getId());
        int actionFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            actionFlags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent completePendingIntent = PendingIntent.getBroadcast(context, reminder.getId() + 100000, completeIntent, actionFlags);

        // Snooze Action
        Intent snoozeIntent = new Intent(context, ReminderReceiver.class);
        snoozeIntent.setAction(ACTION_SNOOZE);
        snoozeIntent.putExtra(ReminderAlarmManager.EXTRA_REMINDER_ID, reminder.getId());
        PendingIntent snoozePendingIntent = PendingIntent.getBroadcast(context, reminder.getId() + 200000, snoozeIntent, actionFlags);

        // Full Screen Alert Activity Intent (Magic for Lock Screen alerts)
        Intent alertIntent = new Intent(context, com.gg_tech_bharat.gremaider.ui.ReminderAlertActivity.class);
        alertIntent.putExtra("reminder_id", reminder.getId());
        alertIntent.putExtra("reminder_title", reminder.getTitle());
        alertIntent.putExtra("reminder_desc", reminder.getDescription());
        alertIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        int alertFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alertFlags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent alertPendingIntent = PendingIntent.getActivity(context, reminder.getId() + 300000, alertIntent, alertFlags);

        // Priority text representation
        String priorityText = "";
        if ("HIGH".equals(reminder.getPriority())) {
            priorityText = "[🔴 Urgent] ";
        } else if ("MEDIUM".equals(reminder.getPriority())) {
            priorityText = "[🟡 Medium] ";
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification) 
                .setContentTitle(priorityText + reminder.getTitle())
                .setContentText(reminder.getDescription() != null && !reminder.getDescription().isEmpty() 
                        ? reminder.getDescription() : "AI scheduled reminder alarm")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setContentIntent(tapPendingIntent)
                .setFullScreenIntent(alertPendingIntent, true) // Launches full screen on lock screen
                .addAction(R.drawable.ic_check_circle, "Complete", completePendingIntent) 
                .addAction(R.drawable.ic_snooze, "Snooze (10m)", snoozePendingIntent)    
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        if (soundUri != null) {
            builder.setSound(soundUri);
        }
        if (isVibrate) {
            builder.setVibrate(new long[]{0, 500, 500});
        }

        notificationManager.notify(reminder.getId(), builder.build());
    }

    private void rescheduleRepeatingReminder(Context context, Reminder reminder) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(reminder.getTimestamp());
        int interval = reminder.getRepeatInterval();
        if (interval <= 0) interval = 1;

        switch (reminder.getRepeatType()) {
            case "DAILY":
                cal.add(Calendar.DAY_OF_YEAR, interval);
                break;
            case "WEEKLY":
                cal.add(Calendar.WEEK_OF_YEAR, interval);
                break;
            case "MONTHLY":
                cal.add(Calendar.MONTH, interval);
                break;
        }

        // Update database and reschedule alarm
        reminder.setTimestamp(cal.getTimeInMillis());
        // A repeating reminder is reset to active status when rescheduled
        reminder.setCompleted(false);
        
        AppDatabase db = AppDatabase.getDatabase(context);
        db.reminderDao().update(reminder);
        
        ReminderAlarmManager.scheduleAlarm(context, reminder);
        Log.d(TAG, "Rescheduled repeating reminder ID: " + reminder.getId() + " to new timestamp: " + cal.getTimeInMillis());
    }
}
