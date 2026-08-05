package com.gg_tech_bharat.gremaider.ui;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.gg_tech_bharat.gremaider.R;
import com.gg_tech_bharat.gremaider.database.AppDatabase;
import com.gg_tech_bharat.gremaider.database.Reminder;
import com.gg_tech_bharat.gremaider.database.ReminderRepository;
import com.gg_tech_bharat.gremaider.receiver.ReminderAlarmManager;

public class ReminderAlertActivity extends AppCompatActivity {

    private TextView tvTitle, tvDesc;
    private Button btnComplete, btnSnooze, btnDismiss;

    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;

    private int reminderId;
    private String title;
    private String desc;
    private Reminder mReminder;
    private ReminderRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Configure flags to show over lockscreen and wake up the screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
            KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if (km != null) {
                km.requestDismissKeyguard(this, null);
            }
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                    | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_reminder_alert);

        repository = new ReminderRepository(getApplication());

        tvTitle = findViewById(R.id.tv_alert_title);
        tvDesc = findViewById(R.id.tv_alert_desc);
        btnComplete = findViewById(R.id.btn_alert_complete);
        btnSnooze = findViewById(R.id.btn_alert_snooze);
        btnDismiss = findViewById(R.id.btn_alert_dismiss);

        // Extract extras
        Intent intent = getIntent();
        if (intent != null) {
            reminderId = intent.getIntExtra("reminder_id", -1);
            title = intent.getStringExtra("reminder_title");
            desc = intent.getStringExtra("reminder_desc");
        }

        tvTitle.setText(title != null ? title : "Reminder Alert");
        tvDesc.setText(desc != null && !desc.isEmpty() ? desc : "Time to take action!");

        // Load reminder from database
        if (reminderId != -1) {
            repository.getReminderById(reminderId, reminder -> {
                if (reminder != null) {
                    mReminder = reminder;
                    runOnUiThread(() -> {
                        tvTitle.setText(reminder.getTitle());
                        tvDesc.setText(reminder.getDescription());
                    });
                }
            });
        }

        // Setup actions
        btnComplete.setText("↔ Swipe to Complete");
        btnComplete.setOnTouchListener(new android.view.View.OnTouchListener() {
            private float startX, startY;
            private final float SWIPE_THRESHOLD = 200f; // pixel distance in any direction to trigger complete

            @Override
            public boolean onTouch(android.view.View v, android.view.MotionEvent event) {
                switch (event.getAction()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        startX = event.getRawX();
                        startY = event.getRawY();
                        return true;
                        
                    case android.view.MotionEvent.ACTION_MOVE:
                        float dx = event.getRawX() - startX;
                        float dy = event.getRawY() - startY;
                        // Slide the button matching the finger movement
                        v.setTranslationX(dx);
                        v.setTranslationY(dy);
                        return true;
                        
                    case android.view.MotionEvent.ACTION_UP:
                    case android.view.MotionEvent.ACTION_CANCEL:
                        float endX = event.getRawX();
                        float endY = event.getRawY();
                        double distance = Math.sqrt(Math.pow(endX - startX, 2) + Math.pow(endY - startY, 2));
                        
                        if (distance >= SWIPE_THRESHOLD) {
                            // Trigger action
                            completeReminder();
                        } else {
                            // Smoothly animate back to normal position
                            v.animate()
                                    .translationX(0)
                                    .translationY(0)
                                    .setDuration(250)
                                    .start();
                        }
                        return true;
                }
                return false;
            }
        });

        btnSnooze.setOnClickListener(v -> {
            snoozeReminder();
        });

        btnDismiss.setOnClickListener(v -> {
            dismissAlert();
        });

        // Start alert sound & vibration
        startAlertEffects();
    }

    private void startAlertEffects() {
        SharedPreferences prefs = getSharedPreferences("GReminderPrefs", MODE_PRIVATE);
        
        // 1. Play ringing sound
        String ringtoneUriStr = prefs.getString("pref_ringtone_uri", "");
        Uri ringtoneUri;
        if (ringtoneUriStr.isEmpty()) {
            ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (ringtoneUri == null) {
                ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            }
        } else {
            ringtoneUri = Uri.parse(ringtoneUriStr);
        }

        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(this, ringtoneUri);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build());
            } else {
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
            }
            mediaPlayer.setLooping(true);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback to default alarm if custom fails
            try {
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setDataSource(this, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));
                mediaPlayer.prepare();
                mediaPlayer.start();
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }

        // 2. Play vibration pattern
        boolean isVibrateEnabled = prefs.getBoolean("pref_vibrate_enabled", true);
        if (isVibrateEnabled) {
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                long[] pattern = {0, 800, 800};
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
                } else {
                    vibrator.vibrate(pattern, 0);
                }
            }
        }
    }

    private void stopAlertEffects() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
                mediaPlayer.release();
            } catch (Exception ignored) {}
            mediaPlayer = null;
        }
        if (vibrator != null) {
            try {
                vibrator.cancel();
            } catch (Exception ignored) {}
            vibrator = null;
        }
    }

    private void openAppDashboard() {
        Intent dashboardIntent = new Intent(this, com.gg_tech_bharat.gremaider.MainActivity.class);
        dashboardIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(dashboardIntent);
    }

    private void completeReminder() {
        stopAlertEffects();
        if (mReminder != null) {
            mReminder.setCompleted(true);
            repository.update(mReminder);
        }
        openAppDashboard();
        finish();
    }

    private void snoozeReminder() {
        stopAlertEffects();
        if (mReminder != null) {
            // Snooze by adding 10 minutes
            mReminder.setTimestamp(System.currentTimeMillis() + 600000);
            repository.update(mReminder);
            ReminderAlarmManager.scheduleAlarm(this, mReminder);
        }
        finish();
    }

    private void dismissAlert() {
        stopAlertEffects();
        openAppDashboard();
        finish();
    }

    @Override
    protected void onDestroy() {
        stopAlertEffects();
        super.onDestroy();
    }
}
