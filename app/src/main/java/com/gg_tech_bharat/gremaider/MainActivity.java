package com.gg_tech_bharat.gremaider;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.gg_tech_bharat.gremaider.ui.AiFragment;
import com.gg_tech_bharat.gremaider.ui.CalendarFragment;
import com.gg_tech_bharat.gremaider.ui.CategoriesFragment;
import com.gg_tech_bharat.gremaider.ui.HomeFragment;
import com.gg_tech_bharat.gremaider.ui.SettingsFragment;
import com.gg_tech_bharat.gremaider.ui.custom.ConfettiView;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private ConfettiView confettiView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Initialize Theme from SharedPreferences before activity creation
        android.content.SharedPreferences prefs = getSharedPreferences("GReminderPrefs", MODE_PRIVATE);
        int themeMode = prefs.getInt("pref_theme", 0);
        int nightMode = androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        if (themeMode == 1) nightMode = androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO;
        else if (themeMode == 2) nightMode = androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES;
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(nightMode);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        confettiView = findViewById(R.id.confetti_view);

        // Auto-clean completed one-time reminders older than 24 hours
        java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
            try {
                com.gg_tech_bharat.gremaider.database.AppDatabase db = com.gg_tech_bharat.gremaider.database.AppDatabase.getDatabase(this);
                long cutoff = System.currentTimeMillis() - (24L * 60L * 60L * 1000L); // 24 hours ago
                db.reminderDao().deleteCompletedOneTimeOlderThan(cutoff);
                android.util.Log.d("MainActivity", "Cleaned up completed reminders older than 1 day");
            } catch (Exception e) {
                android.util.Log.e("MainActivity", "Failed to auto-clean old completed reminders", e);
            }
        });

        // Bind Bottom Navigation Listener
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();
            
            if (itemId == R.id.navigation_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.navigation_calendar) {
                selectedFragment = new CalendarFragment();
            } else if (itemId == R.id.navigation_ai) {
                selectedFragment = new AiFragment();
            } else if (itemId == R.id.navigation_categories) {
                selectedFragment = new CategoriesFragment();
            } else if (itemId == R.id.navigation_settings) {
                selectedFragment = new SettingsFragment();
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
                return true;
            }
            return false;
        });

        // Load Default Fragment on launch
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }

        checkAndRequestPermissions();

        // Biometrics Security Lock check
        boolean useBiometrics = prefs.getBoolean("biometrics_lock", false);
        if (useBiometrics) {
            findViewById(R.id.fragment_container).setVisibility(android.view.View.GONE);
            bottomNavigationView.setVisibility(android.view.View.GONE);
            showBiometricPrompt();
        }
    }

    private void showBiometricPrompt() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            android.hardware.biometrics.BiometricPrompt prompt = new android.hardware.biometrics.BiometricPrompt.Builder(this)
                    .setTitle("GReminder Lock")
                    .setSubtitle("Authenticate to access your reminders")
                    .setNegativeButton("Exit", getMainExecutor(), (dialog, which) -> {
                        finish();
                    })
                    .build();

            android.os.CancellationSignal cancellationSignal = new android.os.CancellationSignal();
            
            prompt.authenticate(cancellationSignal, getMainExecutor(), new android.hardware.biometrics.BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationError(int errorCode, CharSequence errString) {
                    super.onAuthenticationError(errorCode, errString);
                    runOnUiThread(() -> {
                        android.widget.Toast.makeText(MainActivity.this, "Authentication error: " + errString, android.widget.Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }

                @Override
                public void onAuthenticationSucceeded(android.hardware.biometrics.BiometricPrompt.AuthenticationResult result) {
                    super.onAuthenticationSucceeded(result);
                    runOnUiThread(() -> {
                        findViewById(R.id.fragment_container).setVisibility(android.view.View.VISIBLE);
                        bottomNavigationView.setVisibility(android.view.View.VISIBLE);
                        android.widget.Toast.makeText(MainActivity.this, "Access Granted", android.widget.Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onAuthenticationFailed() {
                    super.onAuthenticationFailed();
                }
            });
        } else {
            findViewById(R.id.fragment_container).setVisibility(android.view.View.VISIBLE);
            bottomNavigationView.setVisibility(android.view.View.VISIBLE);
        }
    }

    private void checkAndRequestPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            java.util.List<String> permissions = new java.util.ArrayList<>();
            permissions.add(android.Manifest.permission.READ_CALENDAR);
            permissions.add(android.Manifest.permission.WRITE_CALENDAR);
            permissions.add(android.Manifest.permission.RECORD_AUDIO);
            permissions.add(android.Manifest.permission.READ_PHONE_STATE);
            permissions.add(android.Manifest.permission.READ_CALL_LOG);
            permissions.add(android.Manifest.permission.READ_SMS);
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                permissions.add(android.Manifest.permission.POST_NOTIFICATIONS);
            }
            
            java.util.List<String> listPermissionsNeeded = new java.util.ArrayList<>();
            for (String p : permissions) {
                if (androidx.core.content.ContextCompat.checkSelfPermission(this, p) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    listPermissionsNeeded.add(p);
                }
            }
            
            if (!listPermissionsNeeded.isEmpty()) {
                androidx.core.app.ActivityCompat.requestPermissions(
                        this,
                        listPermissionsNeeded.toArray(new String[0]),
                        101
                );
            }

            // Request exact alarm permission on Android 12+ (API 31+) if not granted
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                android.app.AlarmManager alarmManager = (android.app.AlarmManager) getSystemService(android.content.Context.ALARM_SERVICE);
                if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                    android.content.Intent intent = new android.content.Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                    intent.setData(android.net.Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                }
            }
        }
    }

    private void loadFragment(Fragment fragment) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        
        // Add subtle animation transitions
        ft.setCustomAnimations(
                android.R.anim.fade_in, 
                android.R.anim.fade_out
        );
        
        ft.replace(R.id.fragment_container, fragment);
        ft.commit();
    }

    /**
     * Triggers the fullscreen confetti animation.
     */
    public void triggerConfetti() {
        if (confettiView != null) {
            confettiView.startConfetti();
        }
    }
}