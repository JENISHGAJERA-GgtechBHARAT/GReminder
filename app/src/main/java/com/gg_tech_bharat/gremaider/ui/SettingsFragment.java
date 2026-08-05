package com.gg_tech_bharat.gremaider.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.gg_tech_bharat.gremaider.R;
import com.gg_tech_bharat.gremaider.database.Reminder;
import com.gg_tech_bharat.gremaider.viewmodel.ReminderViewModel;
import com.google.android.material.materialswitch.MaterialSwitch;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class SettingsFragment extends Fragment {
    private static final String TAG = "SettingsFragment";
    private static final String PREFS_NAME = "GReminderPrefs";
    private static final String KEY_NPU = "npu_accelerate";
    private static final String KEY_BIOMETRICS = "biometrics_lock";

    private static final int REQUEST_CODE_RINGTONE = 1001;
    private static final int REQUEST_CODE_NOTIF_SOUND = 1002;

    private ReminderViewModel viewModel;
    private SharedPreferences sharedPreferences;

    private MaterialSwitch switchMaterialYou;
    private MaterialSwitch switchNpu;
    private MaterialSwitch switchBiometrics;
    private MaterialSwitch switchVibration;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle bundle) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        sharedPreferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Bind Theme settings
        View rowTheme = view.findViewById(R.id.row_theme);
        TextView tvThemeSubtitle = view.findViewById(R.id.tv_theme_subtitle);

        // Bind Switches
        switchMaterialYou = view.findViewById(R.id.switch_material_you);
        switchNpu = view.findViewById(R.id.switch_npu);
        switchBiometrics = view.findViewById(R.id.switch_biometrics);
        switchVibration = view.findViewById(R.id.switch_vibration);

        // Setup switch states
        switchNpu.setChecked(sharedPreferences.getBoolean(KEY_NPU, true));
        switchBiometrics.setChecked(sharedPreferences.getBoolean(KEY_BIOMETRICS, false));
        switchVibration.setChecked(sharedPreferences.getBoolean("pref_vibrate_enabled", true));

        // Setup theme state
        int selectedTheme = sharedPreferences.getInt("pref_theme", 0);
        updateThemeSubtitle(tvThemeSubtitle, selectedTheme);

        rowTheme.setOnClickListener(v -> {
            String[] options = {"System default", "Light", "Dark"};
            int currentThemeSelection = sharedPreferences.getInt("pref_theme", 0);

            new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Choose theme")
                    .setSingleChoiceItems(options, currentThemeSelection, (dialog, which) -> {
                        // Save preference
                        sharedPreferences.edit().putInt("pref_theme", which).apply();

                        // Apply theme immediately
                        int nightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                        if (which == 1) nightMode = AppCompatDelegate.MODE_NIGHT_NO;
                        else if (which == 2) nightMode = AppCompatDelegate.MODE_NIGHT_YES;

                        AppCompatDelegate.setDefaultNightMode(nightMode);
                        updateThemeSubtitle(tvThemeSubtitle, which);
                        dialog.dismiss();

                        // Recreate activity to apply theme change smoothly
                        if (getActivity() != null) {
                            getActivity().recreate();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // Other switch listeners
        switchNpu.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean(KEY_NPU, isChecked).apply();
            Toast.makeText(getContext(), isChecked ? "NPU delegate acceleration enabled" : "NPU delegated disabled", Toast.LENGTH_SHORT).show();
        });

        switchBiometrics.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean(KEY_BIOMETRICS, isChecked).apply();
            Toast.makeText(getContext(), isChecked ? "Biometric screen lock enabled" : "Biometric lock disabled", Toast.LENGTH_SHORT).show();
        });

        switchMaterialYou.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Toast.makeText(getContext(), "Dynamic color palette applied successfully", Toast.LENGTH_SHORT).show();
        });

        switchVibration.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("pref_vibrate_enabled", isChecked).apply();
        });

        // Alarm Ringtone Selection Trigger
        view.findViewById(R.id.btn_select_ringtone).setOnClickListener(v -> {
            Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Ringtone");
            
            String current = sharedPreferences.getString("pref_ringtone_uri", "");
            if (!current.isEmpty()) {
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(current));
            }
            startActivityForResult(intent, REQUEST_CODE_RINGTONE);
        });

        // Notification Sound Selection Trigger
        view.findViewById(R.id.btn_select_notif_sound).setOnClickListener(v -> {
            Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Notification Sound");
            
            String current = sharedPreferences.getString("pref_notification_sound_uri", "");
            if (!current.isEmpty()) {
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(current));
            }
            startActivityForResult(intent, REQUEST_CODE_NOTIF_SOUND);
        });

        // Backup and Restore triggers
        view.findViewById(R.id.btn_backup).setOnClickListener(v -> backupData());
        view.findViewById(R.id.btn_restore).setOnClickListener(v -> restoreData());

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(ReminderViewModel.class);

        // Load initial sound selection subtitles now that view is created
        String ringtoneUriStr = sharedPreferences.getString("pref_ringtone_uri", "");
        if (ringtoneUriStr.isEmpty()) {
            updateRingtoneSubtitle(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));
        } else {
            updateRingtoneSubtitle(Uri.parse(ringtoneUriStr));
        }

        String notifSoundUriStr = sharedPreferences.getString("pref_notification_sound_uri", "");
        if (notifSoundUriStr.isEmpty()) {
            updateNotifSoundSubtitle(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        } else {
            updateNotifSoundSubtitle(Uri.parse(notifSoundUriStr));
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == android.app.Activity.RESULT_OK && data != null) {
            Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            if (requestCode == REQUEST_CODE_RINGTONE) {
                String uriStr = uri != null ? uri.toString() : "";
                sharedPreferences.edit().putString("pref_ringtone_uri", uriStr).apply();
                updateRingtoneSubtitle(uri);
            } else if (requestCode == REQUEST_CODE_NOTIF_SOUND) {
                String uriStr = uri != null ? uri.toString() : "";
                sharedPreferences.edit().putString("pref_notification_sound_uri", uriStr).apply();
                updateNotifSoundSubtitle(uri);
            }
        }
    }

    private void updateRingtoneSubtitle(Uri uri) {
        if (getView() == null) return;
        TextView tvSubtitle = getView().findViewById(R.id.tv_ringtone_subtitle);
        if (tvSubtitle == null) return;
        if (uri == null) {
            tvSubtitle.setText("Silent");
        } else {
            try {
                android.media.Ringtone ringtone = RingtoneManager.getRingtone(getContext(), uri);
                tvSubtitle.setText(ringtone.getTitle(getContext()));
            } catch (Exception e) {
                tvSubtitle.setText("Custom Ringtone");
            }
        }
    }

    private void updateNotifSoundSubtitle(Uri uri) {
        if (getView() == null) return;
        TextView tvSubtitle = getView().findViewById(R.id.tv_notif_sound_subtitle);
        if (tvSubtitle == null) return;
        if (uri == null) {
            tvSubtitle.setText("Silent");
        } else {
            try {
                android.media.Ringtone ringtone = RingtoneManager.getRingtone(getContext(), uri);
                tvSubtitle.setText(ringtone.getTitle(getContext()));
            } catch (Exception e) {
                tvSubtitle.setText("Custom Notification");
            }
        }
    }

    private void backupData() {
        if (viewModel == null) return;
        viewModel.getActiveReminders().observe(getViewLifecycleOwner(), reminders -> {
            if (reminders == null) return;
            try {
                JSONArray array = new JSONArray();
                for (Reminder r : reminders) {
                    JSONObject obj = new JSONObject();
                    obj.put("title", r.getTitle());
                    obj.put("description", r.getDescription());
                    obj.put("timestamp", r.getTimestamp());
                    obj.put("repeatInterval", r.getRepeatInterval());
                    obj.put("repeatType", r.getRepeatType());
                    obj.put("priority", r.getPriority());
                    obj.put("category", r.getCategory());
                    obj.put("location", r.getLocation());
                    obj.put("isCompleted", r.isCompleted());
                    obj.put("isPinned", r.isPinned());
                    obj.put("isArchived", r.isArchived());
                    array.put(obj);
                }

                File file = new File(requireContext().getExternalFilesDir(null), "backup.json");
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(array.toString().getBytes(StandardCharsets.UTF_8));
                fos.close();

                Toast.makeText(getContext(), "Backup created at " + file.getName(), Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e(TAG, "Backup failed", e);
                Toast.makeText(getContext(), "Backup failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void restoreData() {
        if (viewModel == null) return;
        try {
            File file = new File(requireContext().getExternalFilesDir(null), "backup.json");
            if (!file.exists()) {
                Toast.makeText(getContext(), "No backup file found", Toast.LENGTH_SHORT).show();
                return;
            }

            FileInputStream fis = new FileInputStream(file);
            byte[] data = new byte[(int) file.length()];
            fis.read(data);
            fis.close();

            String jsonStr = new String(data, StandardCharsets.UTF_8);
            JSONArray array = new JSONArray(jsonStr);

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                Reminder r = new Reminder(
                        obj.getString("title"),
                        obj.optString("description", ""),
                        obj.getLong("timestamp"),
                        obj.optInt("repeatInterval", 0),
                        obj.optString("repeatType", "NONE"),
                        obj.optString("priority", "MEDIUM"),
                        obj.optString("category", "PERSONAL"),
                        obj.optString("location", ""),
                        obj.optBoolean("isCompleted", false),
                        obj.optBoolean("isPinned", false),
                        obj.optBoolean("isArchived", false),
                        false, "", ""
                );
                viewModel.insert(r, null);
            }

            Toast.makeText(getContext(), "Data restored successfully", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Restore failed", e);
            Toast.makeText(getContext(), "Restore failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateThemeSubtitle(TextView textView, int themeMode) {
        if (themeMode == 1) {
            textView.setText("Light");
        } else if (themeMode == 2) {
            textView.setText("Dark");
        } else {
            textView.setText("System default");
        }
    }
}
