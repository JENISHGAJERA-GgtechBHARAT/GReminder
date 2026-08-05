package com.gg_tech_bharat.gremaider.ui;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.gg_tech_bharat.gremaider.R;
import com.gg_tech_bharat.gremaider.ai.SmartAiEngine;
import com.gg_tech_bharat.gremaider.database.Reminder;
import com.gg_tech_bharat.gremaider.receiver.ReminderAlarmManager;
import com.gg_tech_bharat.gremaider.viewmodel.ReminderViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReminderCreationSheet extends BottomSheetDialogFragment {

    private static final String ARG_REMINDER_ID = "reminder_id";

    private ReminderViewModel viewModel;
    private SmartAiEngine aiEngine;
    private int reminderId = 0;
    private Reminder existingReminder;

    private EditText etTitle;
    private EditText etDescription;
    private EditText etLocation;
    private Button btnPickDate;
    private Button btnPickTime;
    private Spinner spinnerRepeat;
    
    private ChipGroup cgPriority;
    private ChipGroup cgCategory;

    private LinearLayout layoutChecklistItems;
    private LinearLayout layoutVoicePreview;
    private Button btnVoiceInput;
    private TextView tvVoiceStatus;
    
    private com.google.android.material.materialswitch.MaterialSwitch switchEnableAlert;
    private LinearLayout layoutDateTimePicker;
    
    private Calendar reminderCalendar;
    private String voicePath = "";
    private final List<ChecklistItemView> checklistItemViews = new ArrayList<>();

    private boolean isDateManuallySet = false;
    private boolean isTimeManuallySet = false;
    private boolean isRepeatManuallySet = false;
    private boolean isPriorityManuallySet = false;
    private boolean isCategoryManuallySet = false;

    public static ReminderCreationSheet newInstance(int reminderId) {
        ReminderCreationSheet fragment = new ReminderCreationSheet();
        Bundle args = new Bundle();
        args.putInt(ARG_REMINDER_ID, reminderId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            reminderId = getArguments().getInt(ARG_REMINDER_ID, 0);
        }
        reminderCalendar = Calendar.getInstance();
        aiEngine = new SmartAiEngine(requireContext());
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle bundle) {
        View view = inflater.inflate(R.layout.dialog_reminder_creation, container, false);

        viewModel = new ViewModelProvider(requireActivity()).get(ReminderViewModel.class);

        // Bind Views
        etTitle = view.findViewById(R.id.et_title);
        setupKeywordHighlighter();
        etDescription = view.findViewById(R.id.et_description);
        etLocation = view.findViewById(R.id.et_location);
        btnPickDate = view.findViewById(R.id.btn_pick_date);
        btnPickTime = view.findViewById(R.id.btn_pick_time);
        spinnerRepeat = view.findViewById(R.id.spinner_repeat);
        cgPriority = view.findViewById(R.id.cg_priority);
        cgCategory = view.findViewById(R.id.cg_category);
        layoutChecklistItems = view.findViewById(R.id.layout_checklist_items);
        layoutVoicePreview = view.findViewById(R.id.layout_voice_preview);
        btnVoiceInput = view.findViewById(R.id.btn_voice_input);
        tvVoiceStatus = view.findViewById(R.id.tv_voice_status);
        
        switchEnableAlert = view.findViewById(R.id.switch_enable_alert);
        layoutDateTimePicker = view.findViewById(R.id.layout_datetime_picker);

        switchEnableAlert.setOnCheckedChangeListener((buttonView, isChecked) -> {
            layoutDateTimePicker.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        // Setup Date & Time buttons text defaults
        updateDateTimeButtons();

        // Track user manual changes to override real-time auto-fill
        spinnerRepeat.setOnTouchListener((v, event) -> {
            isRepeatManuallySet = true;
            return false;
        });
        
        cgPriority.post(() -> {
            for (int i = 0; i < cgPriority.getChildCount(); i++) {
                cgPriority.getChildAt(i).setOnClickListener(v -> isPriorityManuallySet = true);
            }
        });

        cgCategory.post(() -> {
            for (int i = 0; i < cgCategory.getChildCount(); i++) {
                cgCategory.getChildAt(i).setOnClickListener(v -> isCategoryManuallySet = true);
            }
        });

        // Date Picker click
        btnPickDate.setOnClickListener(v -> showDatePicker());

        // Time Picker click
        btnPickTime.setOnClickListener(v -> showTimePicker());

        // Checklist Add Item click
        view.findViewById(R.id.btn_add_checklist_item).setOnClickListener(v -> addChecklistItem("", false));

        // Voice recorder simulator toggle
        btnVoiceInput.setOnClickListener(v -> toggleVoiceRecordingSimulation());
        view.findViewById(R.id.btn_delete_voice).setOnClickListener(v -> removeVoiceNote());

        // AI Autocomplete button
        view.findViewById(R.id.btn_ai_suggest).setOnClickListener(v -> runAiAutocomplete());

        // Save Button click
        view.findViewById(R.id.btn_save).setOnClickListener(v -> saveReminder());

        // If Editing, load the item
        if (reminderId > 0) {
            loadExistingReminder();
        }

        return view;
    }

    private void updateDateTimeButtons() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
        btnPickDate.setText(dateFormat.format(reminderCalendar.getTime()));
        btnPickTime.setText(timeFormat.format(reminderCalendar.getTime()));
    }

    private void showDatePicker() {
        DatePickerDialog dpd = new DatePickerDialog(requireContext(),
                (view, year, month, dayOfMonth) -> {
                    reminderCalendar.set(Calendar.YEAR, year);
                    reminderCalendar.set(Calendar.MONTH, month);
                    reminderCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    isDateManuallySet = true;
                    updateDateTimeButtons();
                },
                reminderCalendar.get(Calendar.YEAR),
                reminderCalendar.get(Calendar.MONTH),
                reminderCalendar.get(Calendar.DAY_OF_MONTH)
        );
        dpd.show();
    }

    private void showTimePicker() {
        TimePickerDialog tpd = new TimePickerDialog(requireContext(),
                (view, hourOfDay, minute) -> {
                    reminderCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    reminderCalendar.set(Calendar.MINUTE, minute);
                    reminderCalendar.set(Calendar.SECOND, 0);
                    reminderCalendar.set(Calendar.MILLISECOND, 0);
                    isTimeManuallySet = true;
                    updateDateTimeButtons();
                },
                reminderCalendar.get(Calendar.HOUR_OF_DAY),
                reminderCalendar.get(Calendar.MINUTE),
                false
        );
        tpd.show();
    }

    private void addChecklistItem(String text, boolean isChecked) {
        ChecklistItemView item = new ChecklistItemView(requireContext(), text, isChecked);
        layoutChecklistItems.addView(item);
        checklistItemViews.add(item);
    }

    private void toggleVoiceRecordingSimulation() {
        if (voicePath.isEmpty()) {
            voicePath = "/mock_audio/reminder_" + System.currentTimeMillis() + ".aac";
            layoutVoicePreview.setVisibility(View.VISIBLE);
            tvVoiceStatus.setText("Voice memo recorded (NPU Speech Parser Active)");
            btnVoiceInput.setText("Recorded");
            Toast.makeText(getContext(), "AI Voice memo recorded successfully", Toast.LENGTH_SHORT).show();
        } else {
            removeVoiceNote();
        }
    }

    private void removeVoiceNote() {
        voicePath = "";
        layoutVoicePreview.setVisibility(View.GONE);
        btnVoiceInput.setText("Voice Note");
        Toast.makeText(getContext(), "Voice note removed", Toast.LENGTH_SHORT).show();
    }

    private void runAiAutocomplete() {
        String title = etTitle.getText().toString();
        if (title.trim().isEmpty()) {
            Toast.makeText(getContext(), "Enter a title first to auto-fill details.", Toast.LENGTH_SHORT).show();
            return;
        }

        SmartAiEngine.ParseResult result = aiEngine.parseQuery(title);
        
        // Auto fill details
        etTitle.setText(result.title);
        if (result.hasTime) {
            reminderCalendar.setTimeInMillis(result.timestamp);
            switchEnableAlert.setChecked(true);
            layoutDateTimePicker.setVisibility(View.VISIBLE);
        } else {
            switchEnableAlert.setChecked(false);
            layoutDateTimePicker.setVisibility(View.GONE);
        }
        updateDateTimeButtons();
        
        // Repeat spinner
        int spinnerPos = 0;
        if ("DAILY".equals(result.repeatType)) spinnerPos = 1;
        else if ("WEEKLY".equals(result.repeatType)) spinnerPos = 2;
        else if ("MONTHLY".equals(result.repeatType)) spinnerPos = 3;
        spinnerRepeat.setSelection(spinnerPos);

        // Priority chips selection
        if ("HIGH".equals(result.priority)) cgPriority.check(R.id.chip_priority_high);
        else if ("MEDIUM".equals(result.priority)) cgPriority.check(R.id.chip_priority_medium);
        else cgPriority.check(R.id.chip_priority_low);

        // Category chips selection
        selectCategoryChip(result.category);

        // Location
        if (!result.location.isEmpty()) {
            etLocation.setText(result.location);
        }

        Toast.makeText(getContext(), "NPU Auto-fill completed for: " + result.title, Toast.LENGTH_SHORT).show();
    }

    private void selectCategoryChip(String categoryName) {
        switch (categoryName) {
            case "Work": cgCategory.check(R.id.chip_cat_work); break;
            case "Personal": cgCategory.check(R.id.chip_cat_personal); break;
            case "Shopping": cgCategory.check(R.id.chip_cat_shopping); break;
            case "Health": cgCategory.check(R.id.chip_cat_health); break;
            case "Finance": cgCategory.check(R.id.chip_cat_finance); break;
            case "Communication": cgCategory.check(R.id.chip_cat_communication); break;
        }
    }

    private void loadExistingReminder() {
        viewModel.getReminderById(reminderId, reminder -> {
            if (reminder == null) return;
            existingReminder = reminder;

            etTitle.setText(reminder.getTitle());
            etDescription.setText(reminder.getDescription());
            etLocation.setText(reminder.getLocation());
            if (reminder.getTimestamp() > 0) {
                reminderCalendar.setTimeInMillis(reminder.getTimestamp());
                switchEnableAlert.setChecked(true);
                layoutDateTimePicker.setVisibility(View.VISIBLE);
            } else {
                switchEnableAlert.setChecked(false);
                layoutDateTimePicker.setVisibility(View.GONE);
            }
            updateDateTimeButtons();

            // Set Priority selection
            if ("HIGH".equals(reminder.getPriority())) cgPriority.check(R.id.chip_priority_high);
            else if ("MEDIUM".equals(reminder.getPriority())) cgPriority.check(R.id.chip_priority_medium);
            else cgPriority.check(R.id.chip_priority_low);

            // Set Category
            selectCategoryChip(reminder.getCategory());

            // Set Repeat
            int spinnerPos = 0;
            switch (reminder.getRepeatType()) {
                case "DAILY": spinnerPos = 1; break;
                case "WEEKLY": spinnerPos = 2; break;
                case "MONTHLY": spinnerPos = 3; break;
            }
            spinnerRepeat.setSelection(spinnerPos);

            // Load Voice Preview if exists
            if (reminder.getVoicePath() != null && !reminder.getVoicePath().isEmpty()) {
                voicePath = reminder.getVoicePath();
                layoutVoicePreview.setVisibility(View.VISIBLE);
                btnVoiceInput.setText("Recorded");
            }

            // Load Checklist
            String json = reminder.getChecklistJson();
            if (json != null && !json.trim().isEmpty()) {
                try {
                    JSONArray arr = new JSONArray(json);
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject obj = arr.getJSONObject(i);
                        addChecklistItem(obj.getString("text"), obj.getBoolean("checked"));
                    }
                } catch (Exception ignored) {}
            }
        });
    }

    private void saveReminder() {
        String rawTitle = etTitle.getText().toString();
        if (rawTitle.trim().isEmpty()) {
            etTitle.setError("Title is required");
            return;
        }

        String parsedTitle = aiEngine.parseQuery(rawTitle).title;
        if (parsedTitle.isEmpty()) {
            parsedTitle = rawTitle.trim();
        }
        final String title = parsedTitle;

        String description = etDescription.getText().toString();
        String location = etLocation.getText().toString();

        // Get priority
        String priority = "LOW";
        int checkedPriorityId = cgPriority.getCheckedChipId();
        if (checkedPriorityId == R.id.chip_priority_high) priority = "HIGH";
        else if (checkedPriorityId == R.id.chip_priority_medium) priority = "MEDIUM";

        // Get Category
        String category = "Personal";
        int checkedCatId = cgCategory.getCheckedChipId();
        if (checkedCatId == R.id.chip_cat_work) category = "Work";
        else if (checkedCatId == R.id.chip_cat_shopping) category = "Shopping";
        else if (checkedCatId == R.id.chip_cat_health) category = "Health";
        else if (checkedCatId == R.id.chip_cat_finance) category = "Finance";
        else if (checkedCatId == R.id.chip_cat_communication) category = "Communication";

        // Get Repeat
        String repeatType = "NONE";
        int spinnerPos = spinnerRepeat.getSelectedItemPosition();
        if (spinnerPos == 1) repeatType = "DAILY";
        else if (spinnerPos == 2) repeatType = "WEEKLY";
        else if (spinnerPos == 3) repeatType = "MONTHLY";

        // Compile checklist to JSON
        JSONArray checklistArray = new JSONArray();
        for (ChecklistItemView item : checklistItemViews) {
            String itemText = item.getText();
            if (!itemText.trim().isEmpty()) {
                try {
                    JSONObject obj = new JSONObject();
                    obj.put("text", itemText);
                    obj.put("checked", item.isChecked());
                    checklistArray.put(obj);
                } catch (Exception ignored) {}
            }
        }
        String checklistJson = checklistArray.length() > 0 ? checklistArray.toString() : "";

        long finalTimestamp = 0;
        if (switchEnableAlert.isChecked()) {
            // Ensure date/time is in the future
            long alarmTime = reminderCalendar.getTimeInMillis();
            if (alarmTime < System.currentTimeMillis()) {
                Calendar today = Calendar.getInstance();
                boolean isToday = reminderCalendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                                  reminderCalendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR);
                if (isToday) {
                    reminderCalendar.setTimeInMillis(System.currentTimeMillis() + 1000); // Trigger instantly
                    alarmTime = reminderCalendar.getTimeInMillis();
                    Toast.makeText(getContext(), "Scheduled instantly", Toast.LENGTH_SHORT).show();
                } else {
                    reminderCalendar.add(Calendar.DAY_OF_YEAR, 1);
                    alarmTime = reminderCalendar.getTimeInMillis();
                    Toast.makeText(getContext(), "⚠️ Selected time was in the past. Automatically rescheduled for tomorrow.", Toast.LENGTH_LONG).show();
                }
                updateDateTimeButtons();
            }
            finalTimestamp = reminderCalendar.getTimeInMillis();
        }

        if (existingReminder != null) {
            // Edit existing
            existingReminder.setTitle(title);
            existingReminder.setDescription(description);
            existingReminder.setTimestamp(finalTimestamp);
            existingReminder.setRepeatType(repeatType);
            existingReminder.setRepeatInterval(1);
            existingReminder.setPriority(priority);
            existingReminder.setCategory(category);
            existingReminder.setLocation(location);
            existingReminder.setChecklistJson(checklistJson);
            existingReminder.setVoicePath(voicePath);
            existingReminder.setHasAttachment(!voicePath.isEmpty());

            viewModel.update(existingReminder);
            ReminderAlarmManager.scheduleAlarm(getContext(), existingReminder);
            Toast.makeText(getContext(), "Reminder updated", Toast.LENGTH_SHORT).show();
        } else {
            // Create new
            Reminder reminder = new Reminder(
                    title,
                    description,
                    finalTimestamp,
                    1,
                    repeatType,
                    priority,
                    category,
                    location,
                    false,
                    false,
                    false,
                    !voicePath.isEmpty(),
                    checklistJson,
                    voicePath
            );

            // Check duplicate check via AI Engine
            viewModel.getActiveRemindersSync(activeReminders -> {
                if (aiEngine.isDuplicate(title, activeReminders)) {
                    Toast.makeText(getContext(), "⚠️ AI Alert: A highly similar reminder already exists!", Toast.LENGTH_LONG).show();
                }
                
                viewModel.insert(reminder, id -> {
                    reminder.setId(id.intValue());
                    ReminderAlarmManager.scheduleAlarm(getContext(), reminder);
                    Toast.makeText(getContext(), "Reminder created", Toast.LENGTH_SHORT).show();
                });
            });
        }

        dismiss();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (aiEngine != null) {
            aiEngine.close();
        }
    }

    private void setupKeywordHighlighter() {
        etTitle.addTextChangedListener(new android.text.TextWatcher() {
            private boolean isUpdating = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                if (isUpdating) return;
                isUpdating = true;

                String originalText = s.toString();
                android.text.SpannableString spannable = new android.text.SpannableString(originalText);

                // Match keywords: date, repeat, and time patterns
                String[] patterns = {
                    "(?i)\\b(today|this\\s+day|tomorrow|next\\s+day|day\\s+after\\s+tomorrow|tonight|next\\s+week|next\\s+month|sunday|monday|tuesday|wednesday|thursday|friday|saturday)\\b",
                    "(?i)\\b(everyday|every\\s+day|daily|weekly|monthly|every\\s+(sunday|monday|tuesday|wednesday|thursday|friday|saturday))\\b",
                    "(?i)\\b(?:at\\s+)?\\d{1,2}(?::\\d{2})?\\s*(?:pm|am|o'clock|oclock)\\b",
                    "(?i)\\b(?:at\\s+)\\d{1,2}\\b"
                };

                int highlightColor = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.primary_light);

                for (String patStr : patterns) {
                    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(patStr);
                    java.util.regex.Matcher matcher = pattern.matcher(originalText);
                    while (matcher.find()) {
                        spannable.setSpan(
                            new android.text.style.ForegroundColorSpan(highlightColor),
                            matcher.start(),
                            matcher.end(),
                            android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        );
                        spannable.setSpan(
                            new android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                            matcher.start(),
                            matcher.end(),
                            android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        );
                    }
                }

                int selectionStart = etTitle.getSelectionStart();
                int selectionEnd = etTitle.getSelectionEnd();

                etTitle.setText(spannable);
                etTitle.setSelection(Math.min(selectionStart, spannable.length()), Math.min(selectionEnd, spannable.length()));

                isUpdating = false;

                // Trigger real-time auto-fill silently
                if (!originalText.trim().isEmpty()) {
                    runSilentRealtimeParser(originalText);
                }
            }
        });
    }

    private void runSilentRealtimeParser(String query) {
        SmartAiEngine.ParseResult result = aiEngine.parseQuery(query);
        
        // Auto fill Date & Time if not manually set
        if (!isDateManuallySet || !isTimeManuallySet) {
            Calendar parsedCal = Calendar.getInstance();
            parsedCal.setTimeInMillis(result.timestamp);
            
            if (!isDateManuallySet) {
                reminderCalendar.set(Calendar.YEAR, parsedCal.get(Calendar.YEAR));
                reminderCalendar.set(Calendar.MONTH, parsedCal.get(Calendar.MONTH));
                reminderCalendar.set(Calendar.DAY_OF_MONTH, parsedCal.get(Calendar.DAY_OF_MONTH));
            }
            if (!isTimeManuallySet) {
                reminderCalendar.set(Calendar.HOUR_OF_DAY, parsedCal.get(Calendar.HOUR_OF_DAY));
                reminderCalendar.set(Calendar.MINUTE, parsedCal.get(Calendar.MINUTE));
                reminderCalendar.set(Calendar.SECOND, 0);
                reminderCalendar.set(Calendar.MILLISECOND, 0);
            }
            updateDateTimeButtons();
        }

        // Repeat spinner if not manually set
        if (!isRepeatManuallySet) {
            int spinnerPos = 0;
            if ("DAILY".equals(result.repeatType)) spinnerPos = 1;
            else if ("WEEKLY".equals(result.repeatType)) spinnerPos = 2;
            else if ("MONTHLY".equals(result.repeatType)) spinnerPos = 3;
            spinnerRepeat.setSelection(spinnerPos);
        }

        // Priority chips if not manually set
        if (!isPriorityManuallySet) {
            if ("HIGH".equals(result.priority)) cgPriority.check(R.id.chip_priority_high);
            else if ("MEDIUM".equals(result.priority)) cgPriority.check(R.id.chip_priority_medium);
            else cgPriority.check(R.id.chip_priority_low);
        }

        // Category chips if not manually set
        if (!isCategoryManuallySet) {
            selectCategoryChip(result.category);
        }
        
        // Location if not manually set
        if (etLocation.getText().toString().trim().isEmpty() && !result.location.isEmpty()) {
            etLocation.setText(result.location);
        }
    }

    // Inner class representing dynamic checklist item
    private static class ChecklistItemView extends LinearLayout {
        private final EditText editText;
        private final CheckBox checkBox;

        ChecklistItemView(Context context, String text, boolean isChecked) {
            super(context);
            setOrientation(HORIZONTAL);
            setPadding(0, 4, 0, 4);

            checkBox = new CheckBox(context);
            checkBox.setChecked(isChecked);
            addView(checkBox);

            editText = new EditText(context);
            editText.setHint("Checklist item...");
            editText.setText(text);
            editText.setLayoutParams(new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
            addView(editText);

            Button btnRemove = new Button(context, null, android.R.attr.buttonStyleSmall);
            btnRemove.setText("✕");
            btnRemove.setOnClickListener(v -> ((ViewGroup) getParent()).removeView(this));
            addView(btnRemove);
        }

        String getText() { return editText.getText().toString(); }
        boolean isChecked() { return checkBox.isChecked(); }
    }
}
