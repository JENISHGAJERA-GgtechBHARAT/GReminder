package com.gg_tech_bharat.gremaider.receiver;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.util.Calendar;

import com.gg_tech_bharat.gremaider.database.Reminder;

public class ReminderAlarmManager {
    private static final String TAG = "ReminderAlarmManager";
    public static final String EXTRA_REMINDER_ID = "reminder_id";
    public static final String ACTION_ALARM_TRIGGER = "com.gg_tech_bharat.gremaider.ACTION_ALARM_TRIGGER";

    public static void scheduleAlarm(Context context, Reminder reminder) {
        if (reminder.isCompleted() || reminder.isArchived()) {
            cancelAlarm(context, reminder.getId());
            return;
        }

        long alarmTime = reminder.getTimestamp();
        if (alarmTime <= 0) {
            cancelAlarm(context, reminder.getId());
            return;
        }
        if (alarmTime < System.currentTimeMillis()) {
            // If the time is in the past and has no repeat, don't schedule
            if ("NONE".equals(reminder.getRepeatType())) {
                return;
            }
            // For repeating alarms in the past, we should compute the next occurrence
            alarmTime = getNextOccurrence(alarmTime, reminder.getRepeatType(), reminder.getRepeatInterval());
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.setAction(ACTION_ALARM_TRIGGER);
        intent.putExtra(EXTRA_REMINDER_ID, reminder.getId());
        
        // PendingIntent flags
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 
                reminder.getId(), 
                intent, 
                flags
        );

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            alarmTime,
                            pendingIntent
                    );
                    Log.d(TAG, "Scheduled exact alarm for reminder ID: " + reminder.getId() + " at " + alarmTime);
                } else {
                    alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            alarmTime,
                            pendingIntent
                    );
                    Log.d(TAG, "Scheduled inexact (setAndAllowWhileIdle) alarm for reminder ID: " + reminder.getId() + " at " + alarmTime);
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        alarmTime,
                        pendingIntent
                );
                Log.d(TAG, "Scheduled exact alarm for reminder ID: " + reminder.getId() + " at " + alarmTime);
            } else {
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        alarmTime,
                        pendingIntent
                );
                Log.d(TAG, "Scheduled exact alarm (SDK < M) for reminder ID: " + reminder.getId() + " at " + alarmTime);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to schedule alarm for reminder ID: " + reminder.getId(), e);
        }
    }

    public static void cancelAlarm(Context context, int reminderId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, ReminderReceiver.class);
        int flags = PendingIntent.FLAG_NO_CREATE;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                reminderId,
                intent,
                flags
        );

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
            Log.d(TAG, "Cancelled alarm for reminder ID: " + reminderId);
        }
    }

    private static long getNextOccurrence(long startTime, String repeatType, int interval) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(startTime);
        long now = System.currentTimeMillis();

        if (interval <= 0) interval = 1;

        while (cal.getTimeInMillis() < now) {
            switch (repeatType) {
                case "DAILY":
                    cal.add(Calendar.DAY_OF_YEAR, interval);
                    break;
                case "WEEKLY":
                    cal.add(Calendar.WEEK_OF_YEAR, interval);
                    break;
                case "MONTHLY":
                    cal.add(Calendar.MONTH, interval);
                    break;
                default:
                    cal.add(Calendar.DAY_OF_YEAR, 1); // fallback
                    break;
            }
        }
        return cal.getTimeInMillis();
    }
}
