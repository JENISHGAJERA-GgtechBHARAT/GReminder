package com.gg_tech_bharat.gremaider.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.gg_tech_bharat.gremaider.R;
import com.gg_tech_bharat.gremaider.ai.SmartAiEngine;
import com.gg_tech_bharat.gremaider.database.Reminder;
import com.gg_tech_bharat.gremaider.receiver.ReminderAlarmManager;
import com.gg_tech_bharat.gremaider.viewmodel.ReminderViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AiFragment extends Fragment {

    private ReminderViewModel viewModel;
    private SmartAiEngine aiEngine;

    private TextView tvNpuStatus;
    private EditText etAiQuery;
    private LinearLayout layoutParsedResults;

    private TextView tvParsedTitle;
    private TextView tvParsedDatetime;
    private TextView tvParsedRepeat;
    private TextView tvParsedPriority;
    private TextView tvParsedCategory;
    private TextView tvParsedLocation;

    private RadioGroup rgOcrTemplates;
    private LinearLayout layoutRoutineInsights;

    private SmartAiEngine.ParseResult activeParseResult;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle bundle) {
        View view = inflater.inflate(R.layout.fragment_ai, container, false);

        // Bind NLP UI
        tvNpuStatus = view.findViewById(R.id.tv_npu_status);
        etAiQuery = view.findViewById(R.id.et_ai_query);
        layoutParsedResults = view.findViewById(R.id.layout_parsed_results);

        tvParsedTitle = view.findViewById(R.id.tv_parsed_title);
        tvParsedDatetime = view.findViewById(R.id.tv_parsed_datetime);
        tvParsedRepeat = view.findViewById(R.id.tv_parsed_repeat);
        tvParsedPriority = view.findViewById(R.id.tv_parsed_priority);
        tvParsedCategory = view.findViewById(R.id.tv_parsed_category);
        tvParsedLocation = view.findViewById(R.id.tv_parsed_location);

        // Bind OCR UI
        rgOcrTemplates = view.findViewById(R.id.rg_ocr_templates);

        // Bind Insights Layout
        layoutRoutineInsights = view.findViewById(R.id.layout_routine_insights);

        // Clear button
        view.findViewById(R.id.btn_ai_clear).setOnClickListener(v -> {
            etAiQuery.setText("");
            layoutParsedResults.setVisibility(View.GONE);
            activeParseResult = null;
        });

        // Parse button
        view.findViewById(R.id.btn_ai_parse).setOnClickListener(v -> {
            String text = etAiQuery.getText().toString();
            if (!text.trim().isEmpty()) {
                parseInput(text);
            } else {
                Toast.makeText(getContext(), "Please type a reminder statement first.", Toast.LENGTH_SHORT).show();
            }
        });

        // Save parsed reminder
        view.findViewById(R.id.btn_add_parsed_task).setOnClickListener(v -> saveParsedReminder());

        // OCR Scan simulator button
        view.findViewById(R.id.btn_ocr_scan).setOnClickListener(v -> runOcrScanSimulation());

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(ReminderViewModel.class);
        aiEngine = new SmartAiEngine(requireContext());

        // Update NPU status UI
        if (aiEngine.isNpuActive()) {
            tvNpuStatus.setText("NPU Status: Active (NNAPI Accelerated)");
            tvNpuStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            tvNpuStatus.setText("NPU Status: Initialized (CPU Heuristic Fallback)");
            tvNpuStatus.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
        }

        // Fetch completed task history for Routine Insight learning
        viewModel.getCompletedTodayReminders().observe(getViewLifecycleOwner(), completedToday -> {
            viewModel.getActiveRemindersSync(activeList -> {
                // Combine completed + active to pass to suggestions
                List<Reminder> history = new ArrayList<>(activeList);
                loadRoutineSuggestions(history);
            });
        });
    }

    private void parseInput(String text) {
        activeParseResult = aiEngine.parseQuery(text);
        displayParsedResults();
    }

    private void displayParsedResults() {
        if (activeParseResult == null) return;

        tvParsedTitle.setText("Title: " + activeParseResult.title);
        
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault());
        tvParsedDatetime.setText("Time: " + sdf.format(new Date(activeParseResult.timestamp)));
        
        tvParsedRepeat.setText("Repeat: " + activeParseResult.repeatType + 
                (activeParseResult.repeatInterval > 0 ? " (every " + activeParseResult.repeatInterval + ")" : ""));
        tvParsedPriority.setText("Priority: " + activeParseResult.priority);
        tvParsedCategory.setText("Category: " + activeParseResult.category);
        tvParsedLocation.setText("Location: " + (activeParseResult.location.isEmpty() ? "None" : activeParseResult.location));

        layoutParsedResults.setVisibility(View.VISIBLE);
    }

    private void saveParsedReminder() {
        if (activeParseResult == null) return;

        Reminder reminder = new Reminder(
                activeParseResult.title,
                "AI generated parsed reminder",
                activeParseResult.timestamp,
                activeParseResult.repeatInterval,
                activeParseResult.repeatType,
                activeParseResult.priority,
                activeParseResult.category,
                activeParseResult.location,
                false,
                false,
                false,
                false,
                "",
                ""
        );

        viewModel.insert(reminder, id -> {
            reminder.setId(id.intValue());
            ReminderAlarmManager.scheduleAlarm(getContext(), reminder);
            Toast.makeText(getContext(), "AI Reminder created: " + reminder.getTitle(), Toast.LENGTH_SHORT).show();
            etAiQuery.setText("");
            layoutParsedResults.setVisibility(View.GONE);
            activeParseResult = null;
        });
    }

    private void runOcrScanSimulation() {
        int checkedId = rgOcrTemplates.getCheckedRadioButtonId();
        if (checkedId == -1) {
            Toast.makeText(getContext(), "Please select a template first.", Toast.LENGTH_SHORT).show();
            return;
        }

        RadioButton rb = getView().findViewById(checkedId);
        String text = rb.getText().toString();
        
        Toast.makeText(getContext(), "Scanning document using NPU vector model...", Toast.LENGTH_SHORT).show();
        
        // Simulating delay for OCR processing
        getView().postDelayed(() -> {
            if (isAdded()) {
                activeParseResult = aiEngine.parseOcrText(text);
                etAiQuery.setText(text);
                displayParsedResults();
                Toast.makeText(getContext(), "OCR Text scanned & parsed successfully!", Toast.LENGTH_SHORT).show();
            }
        }, 1000);
    }

    private void loadRoutineSuggestions(List<Reminder> history) {
        layoutRoutineInsights.removeAllViews();
        List<String> suggestions = aiEngine.generateRoutineSuggestions(history);

        for (String suggestion : suggestions) {
            TextView tv = new TextView(getContext());
            tv.setText("• " + suggestion);
            tv.setTextColor(getResources().getColor(R.color.text_subtitle_light));
            tv.setTextSize(13);
            tv.setPadding(0, 4, 0, 4);
            layoutRoutineInsights.addView(tv);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (aiEngine != null) {
            aiEngine.close();
        }
    }
}
