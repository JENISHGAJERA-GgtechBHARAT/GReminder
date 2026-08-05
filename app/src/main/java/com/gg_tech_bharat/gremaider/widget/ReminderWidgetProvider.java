package com.gg_tech_bharat.gremaider.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.text.format.DateFormat;
import android.widget.RemoteViews;

import com.gg_tech_bharat.gremaider.MainActivity;
import com.gg_tech_bharat.gremaider.R;
import com.gg_tech_bharat.gremaider.database.AppDatabase;
import com.gg_tech_bharat.gremaider.database.ReminderDao;

import java.util.Calendar;
import java.util.concurrent.Executors;

public class ReminderWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // Run update on all widgets
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.reminder_widget);

        // Date text
        Calendar cal = Calendar.getInstance();
        CharSequence dateStr = DateFormat.format("MMM d", cal.getTime());
        views.setTextViewText(R.id.widget_date, dateStr);

        // Standard Pending Intent to open MainActivity
        Intent intent = new Intent(context, MainActivity.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, flags);
        views.setOnClickPendingIntent(R.id.widget_title, pendingIntent);
        views.setOnClickPendingIntent(R.id.widget_btn_add, pendingIntent);

        // Query database on background thread to count active reminders
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(context);
            ReminderDao dao = db.reminderDao();
            int activeCount = dao.getActiveRemindersSync().size();

            String statusText;
            if (activeCount == 0) {
                statusText = "All cleared today!";
            } else if (activeCount == 1) {
                statusText = "1 reminder pending";
            } else {
                statusText = activeCount + " reminders pending";
            }

            views.setTextViewText(R.id.widget_status_text, statusText);

            // Update WidgetManager
            appWidgetManager.updateAppWidget(appWidgetId, views);
        });
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        // Refresh values when notified
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, ReminderWidgetProvider.class));
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }
}
