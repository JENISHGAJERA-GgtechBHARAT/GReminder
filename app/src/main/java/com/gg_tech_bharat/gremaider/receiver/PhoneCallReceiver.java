package com.gg_tech_bharat.gremaider.receiver;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.gg_tech_bharat.gremaider.database.AppDatabase;
import com.gg_tech_bharat.gremaider.database.Reminder;
import com.gg_tech_bharat.gremaider.database.ReminderDao;
import com.gg_tech_bharat.gremaider.widget.ReminderWidgetProvider;

import java.util.concurrent.Executors;

public class PhoneCallReceiver extends BroadcastReceiver {
    private static final String TAG = "PhoneCallReceiver";
    
    public static final String ACTION_CHECK_MISSED_CALL = "com.gg_tech_bharat.gremaider.ACTION_CHECK_MISSED_CALL";
    public static final String EXTRA_PHONE_NUMBER = "extra_phone_number";
    
    private static final String PREF_NAME = "GReminderCallPrefs";
    private static final String KEY_INCOMING_NUMBER = "last_incoming_number";
    private static final String KEY_WAS_RINGING = "was_ringing";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        
        String action = intent.getAction();
        if (TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(action)) {
            handlePhoneStateChanged(context, intent);
        } else if (ACTION_CHECK_MISSED_CALL.equals(action)) {
            handleCheckMissedCall(context, intent);
        }
    }

    private void handlePhoneStateChanged(Context context, Intent intent) {
        String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
        Log.d(TAG, "Phone State changed to: " + state);

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        
        if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
            String incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
            if (incomingNumber != null && !incomingNumber.isEmpty()) {
                Log.d(TAG, "Ringing from number: " + incomingNumber);
                prefs.edit()
                        .putString(KEY_INCOMING_NUMBER, incomingNumber)
                        .putBoolean(KEY_WAS_RINGING, true)
                        .apply();
            } else {
                prefs.edit().putBoolean(KEY_WAS_RINGING, true).apply();
            }
        } else if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)) {
            // Call was answered/placed, so not a missed call
            prefs.edit().putBoolean(KEY_WAS_RINGING, false).apply();
        } else if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
            boolean wasRinging = prefs.getBoolean(KEY_WAS_RINGING, false);
            String incomingNumber = prefs.getString(KEY_INCOMING_NUMBER, "");
            
            if (wasRinging && !incomingNumber.isEmpty()) {
                Log.d(TAG, "Missed call detected from: " + incomingNumber + ". Scheduling check in 2 minutes.");
                scheduleMissedCallCheck(context, incomingNumber);
            }
            
            // Reset state
            prefs.edit().putBoolean(KEY_WAS_RINGING, false).apply();
        }
    }

    private void scheduleMissedCallCheck(Context context, String phoneNumber) {
        Intent checkIntent = new Intent(context, PhoneCallReceiver.class);
        checkIntent.setAction(ACTION_CHECK_MISSED_CALL);
        checkIntent.putExtra(EXTRA_PHONE_NUMBER, phoneNumber);
        
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 1024, checkIntent, flags);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        
        long triggerTime = System.currentTimeMillis() + (2 * 60 * 1000); // 2 minutes delay
        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            }
        }
    }

    private void handleCheckMissedCall(Context context, Intent intent) {
        String phoneNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER);
        if (phoneNumber == null || phoneNumber.isEmpty()) return;

        Log.d(TAG, "Checking missed call response for: " + phoneNumber);

        Executors.newSingleThreadExecutor().execute(() -> {
            boolean repliedWithSms = checkRecentSentSms(context, phoneNumber);
            if (repliedWithSms) {
                Log.d(TAG, "User sent a quick reply message to " + phoneNumber + ". Creating 1-hour callback reminder.");
                createCallbackReminder(context, phoneNumber, 60L * 60L * 1000L, "Callback reminder (Quick-response sent).");
            } else {
                Log.d(TAG, "No quick reply SMS found. Creating 10-minute callback reminder.");
                createCallbackReminder(context, phoneNumber, 10L * 60L * 1000L, "Missed call callback reminder.");
            }
        });
    }

    private boolean checkRecentSentSms(Context context, String phoneNumber) {
        Uri uri = Uri.parse("content://sms/sent");
        String[] projection = new String[]{"address", "body", "date"};
        
        // Check sent messages in the last 4 minutes
        long cutoffTime = System.currentTimeMillis() - (4 * 60 * 1000);
        String selection = "date > ?";
        String[] selectionArgs = new String[]{String.valueOf(cutoffTime)};
        String sortOrder = "date DESC";

        try (Cursor cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, sortOrder)) {
            if (cursor != null && cursor.moveToFirst()) {
                int addressCol = cursor.getColumnIndexOrThrow("address");
                int bodyCol = cursor.getColumnIndexOrThrow("body");
                
                do {
                    String address = cursor.getString(addressCol);
                    String body = cursor.getString(bodyCol);
                    
                    if (phoneNumbersMatch(address, phoneNumber)) {
                        String lowerBody = body.toLowerCase();
                        if (lowerBody.contains("call") || 
                            lowerBody.contains("later") || 
                            lowerBody.contains("busy") || 
                            lowerBody.contains("talk") || 
                            lowerBody.contains("drive") || 
                            lowerBody.contains("meeting") ||
                            lowerBody.contains("text")) {
                            return true;
                        }
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error querying SMS content provider", e);
        }
        return false;
    }

    private boolean phoneNumbersMatch(String num1, String num2) {
        if (num1 == null || num2 == null) return false;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                return PhoneNumberUtils.areSamePhoneNumber(num1, num2, "IN");
            } else {
                return PhoneNumberUtils.compare(num1, num2);
            }
        } catch (Exception e) {
            return num1.replaceAll("[^0-9]", "").contains(num2.replaceAll("[^0-9]", "")) ||
                   num2.replaceAll("[^0-9]", "").contains(num1.replaceAll("[^0-9]", ""));
        }
    }

    private void createCallbackReminder(Context context, String phoneNumber, long delayMillis, String desc) {
        AppDatabase db = AppDatabase.getDatabase(context);
        ReminderDao dao = db.reminderDao();

        long scheduleTime = System.currentTimeMillis() + delayMillis;
        String title = "Call back " + phoneNumber;

        Reminder reminder = new Reminder(
                title,
                desc,
                scheduleTime,
                0,
                "NONE",
                "MEDIUM",
                "Communication",
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

        // Schedule the alarm trigger
        ReminderAlarmManager.scheduleAlarm(context, reminder);

        // Notify widget update
        Intent widgetIntent = new Intent(context, ReminderWidgetProvider.class);
        widgetIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] ids = AppWidgetManager.getInstance(context).getAppWidgetIds(
                new ComponentName(context, ReminderWidgetProvider.class)
        );
        widgetIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        context.sendBroadcast(widgetIntent);
    }
}
