package com.gg_tech_bharat.gremaider.calendar;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.gg_tech_bharat.gremaider.database.Reminder;

import java.util.TimeZone;

public class CalendarSyncManager {
    private static final String TAG = "CalendarSyncManager";

    /**
     * Checks if calendar read/write permissions are granted.
     */
    public static boolean hasCalendarPermissions(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Query for the user's primary calendar ID.
     */
    private static long getDefaultCalendarId(Context context) {
        if (!hasCalendarPermissions(context)) return -1;

        ContentResolver resolver = context.getContentResolver();
        Uri uri = CalendarContract.Calendars.CONTENT_URI;
        String[] projection = new String[]{
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.VISIBLE
        };

        Cursor cursor = null;
        try {
            cursor = resolver.query(uri, projection, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                // Return first visible calendar
                do {
                    int visibleIdx = cursor.getColumnIndex(CalendarContract.Calendars.VISIBLE);
                    int idIdx = cursor.getColumnIndex(CalendarContract.Calendars._ID);
                    if (visibleIdx != -1 && idIdx != -1) {
                        if (cursor.getInt(visibleIdx) == 1) {
                            return cursor.getLong(idIdx);
                        }
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to query system calendars", e);
        } finally {
            if (cursor != null) cursor.close();
        }
        return 1; // Fallback to calendar ID 1
    }

    /**
     * Syncs a reminder to the default system calendar provider.
     * Returns the updated calendar event ID if successful, or -1.
     */
    public static long syncReminderToCalendar(Context context, Reminder reminder) {
        if (!hasCalendarPermissions(context)) {
            Log.w(TAG, "Missing calendar permissions. Sync skipped.");
            return -1;
        }

        long calendarId = getDefaultCalendarId(context);
        if (calendarId == -1) {
            return -1;
        }

        ContentResolver resolver = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(CalendarContract.Events.CALENDAR_ID, calendarId);
        
        // Mark as completed in calendar if it is completed
        String eventTitle = reminder.isCompleted() ? "[Done] " + reminder.getTitle() : reminder.getTitle();
        values.put(CalendarContract.Events.TITLE, eventTitle);
        
        values.put(CalendarContract.Events.DESCRIPTION, reminder.getDescription() != null ? reminder.getDescription() : "Scheduled by GReminder App");
        values.put(CalendarContract.Events.EVENT_LOCATION, reminder.getLocation() != null ? reminder.getLocation() : "");
        values.put(CalendarContract.Events.DTSTART, reminder.getTimestamp());
        values.put(CalendarContract.Events.DTEND, reminder.getTimestamp() + 1800000); // Default 30 min duration
        values.put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().getID());

        long eventId = reminder.getCalendarEventId();
        if (eventId > 0) {
            // Update existing event
            Uri updateUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId);
            try {
                int rows = resolver.update(updateUri, values, null, null);
                if (rows > 0) {
                    Log.d(TAG, "Updated existing calendar event ID: " + eventId);
                    return eventId;
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed to update calendar event ID: " + eventId + ", trying insertion.", e);
            }
        }

        // Insert new event
        try {
            Uri resultUri = resolver.insert(CalendarContract.Events.CONTENT_URI, values);
            if (resultUri != null) {
                long newEventId = Long.parseLong(resultUri.getLastPathSegment());
                Log.d(TAG, "Created new system calendar event ID: " + newEventId);
                return newEventId;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to insert event into device calendar", e);
        }

        return -1;
    }

    /**
     * Deletes a calendar event linked to a reminder.
     */
    public static void deleteCalendarEvent(Context context, Reminder reminder) {
        long eventId = reminder.getCalendarEventId();
        if (eventId <= 0 || !hasCalendarPermissions(context)) {
            return;
        }

        ContentResolver resolver = context.getContentResolver();
        Uri deleteUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId);
        try {
            int rows = resolver.delete(deleteUri, null, null);
            if (rows > 0) {
                Log.d(TAG, "Successfully deleted linked calendar event ID: " + eventId);
                reminder.setCalendarEventId(0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to delete calendar event ID: " + eventId, e);
        }
    }
}
