package com.gg_tech_bharat.gremaider.ui;

import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gg_tech_bharat.gremaider.R;
import com.gg_tech_bharat.gremaider.database.Reminder;
import com.gg_tech_bharat.gremaider.viewmodel.ReminderViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CalendarFragment extends Fragment {

    private ReminderViewModel viewModel;
    private GridView calendarGrid;
    private TextView tvCalendarTitle;
    private TextView tvAgendaHeader;
    private TextView tvAgendaEmpty;
    private RecyclerView rvAgenda;
    private ReminderAdapter agendaAdapter;

    private Calendar currentCalendar;
    private Calendar selectedDate;
    private List<Date> cells = new ArrayList<>();
    private List<Reminder> allActiveReminders = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle bundle) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);

        // Bind views
        calendarGrid = view.findViewById(R.id.calendar_grid);
        tvCalendarTitle = view.findViewById(R.id.tv_calendar_title);
        tvAgendaHeader = view.findViewById(R.id.tv_agenda_header);
        tvAgendaEmpty = view.findViewById(R.id.tv_agenda_empty);
        rvAgenda = view.findViewById(R.id.rv_agenda);

        ImageButton btnPrev = view.findViewById(R.id.btn_prev_month);
        ImageButton btnNext = view.findViewById(R.id.btn_next_month);

        // Setup layouts
        rvAgenda.setLayoutManager(new LinearLayoutManager(getContext()));
        agendaAdapter = new ReminderAdapter(getContext(), 
                reminder -> openEditSheet(reminder), 
                (reminder, isChecked) -> toggleReminderCompletion(reminder, isChecked),
                reminder -> showDeleteConfirmationDialog(reminder));
        rvAgenda.setAdapter(agendaAdapter);

        // Set calendar state
        currentCalendar = Calendar.getInstance();
        selectedDate = Calendar.getInstance();

        btnPrev.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, -1);
            rebuildCalendar();
        });

        btnNext.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, 1);
            rebuildCalendar();
        });

        calendarGrid.setOnItemClickListener((parent, view1, position, id) -> {
            Date date = cells.get(position);
            selectedDate.setTime(date);
            rebuildCalendar();
            updateAgendaList();
        });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(ReminderViewModel.class);

        // Observe reminders to place dot indicators and populate selected date's agenda
        viewModel.getActiveReminders().observe(getViewLifecycleOwner(), reminders -> {
            allActiveReminders = reminders;
            rebuildCalendar();
            updateAgendaList();
        });
    }

    private void rebuildCalendar() {
        cells.clear();
        Calendar calendar = (Calendar) currentCalendar.clone();
        
        // Determine start and length
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        int monthBeginningCell = calendar.get(Calendar.DAY_OF_WEEK) - 1;
        calendar.add(Calendar.DAY_OF_MONTH, -monthBeginningCell);

        // Fill cells (grid shows 6 rows of 7 days = 42 cells)
        while (cells.size() < 42) {
            cells.add(calendar.getTime());
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        // Set header month name
        SimpleDateFormat titleFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        tvCalendarTitle.setText(titleFormat.format(currentCalendar.getTime()));

        // Bind adapter
        calendarGrid.setAdapter(new CalendarGridAdapter());
    }

    private void updateAgendaList() {
        SimpleDateFormat agendaFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
        tvAgendaHeader.setText("Agenda - " + agendaFormat.format(selectedDate.getTime()));

        // Fetch reminders for selected date
        Calendar start = (Calendar) selectedDate.clone();
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);

        Calendar end = (Calendar) selectedDate.clone();
        end.set(Calendar.HOUR_OF_DAY, 23);
        end.set(Calendar.MINUTE, 59);
        end.set(Calendar.SECOND, 59);
        end.set(Calendar.MILLISECOND, 999);

        viewModel.getRemindersInTimeRangeSync(start.getTimeInMillis(), end.getTimeInMillis(), todayReminders -> {
            if (isAdded()) {
                agendaAdapter.setReminders(todayReminders);
                tvAgendaEmpty.setVisibility(todayReminders.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void toggleReminderCompletion(Reminder reminder, boolean isChecked) {
        reminder.setCompleted(isChecked);
        viewModel.update(reminder);
        updateAgendaList();
    }

    private void showDeleteConfirmationDialog(Reminder reminder) {
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle("Delete Reminder")
                .setMessage("Are you sure you want to delete this reminder?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // Cancel active alarm
                    com.gg_tech_bharat.gremaider.receiver.ReminderAlarmManager.cancelAlarm(getContext(), reminder.getId());
                    // Delete from database
                    viewModel.delete(reminder);
                    updateAgendaList();
                    android.widget.Toast.makeText(getContext(), "Reminder deleted", android.widget.Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void openEditSheet(Reminder reminder) {
        ReminderCreationSheet sheet = ReminderCreationSheet.newInstance(reminder.getId());
        sheet.show(getParentFragmentManager(), "ReminderCreationSheet");
    }

    private class CalendarGridAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return cells.size();
        }

        @Override
        public Object getItem(int position) {
            return cells.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Date date = cells.get(position);
            Calendar cellCal = Calendar.getInstance();
            cellCal.setTime(date);

            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
            }

            TextView textView = convertView.findViewById(android.R.id.text1);
            textView.setText(String.valueOf(cellCal.get(Calendar.DAY_OF_MONTH)));
            textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            textView.setPadding(0, 8, 0, 8);
            textView.setTextSize(14f);

            // Month text colors (dynamically adapted for light/dark theme)
            int colorCurrentMonth = androidx.core.content.ContextCompat.getColor(getContext(), R.color.text_title_light);
            int colorOtherMonth = androidx.core.content.ContextCompat.getColor(getContext(), R.color.text_subtitle_light);
            if (cellCal.get(Calendar.MONTH) == currentCalendar.get(Calendar.MONTH)) {
                textView.setTextColor(colorCurrentMonth);
            } else {
                textView.setTextColor(colorOtherMonth);
            }

            // Highlighting selected date
            if (cellCal.get(Calendar.YEAR) == selectedDate.get(Calendar.YEAR) &&
                cellCal.get(Calendar.MONTH) == selectedDate.get(Calendar.MONTH) &&
                cellCal.get(Calendar.DAY_OF_MONTH) == selectedDate.get(Calendar.DAY_OF_MONTH)) {
                textView.setBackgroundResource(R.drawable.bg_card); // Rounded card selector background
                textView.setTextColor(Color.WHITE);
                textView.setBackgroundColor(Color.parseColor("#3A7DF0")); // One UI Accent Blue
            } else {
                textView.setBackgroundColor(Color.TRANSPARENT);
            }

            // Check if reminders exist for this date to show active marker dot
            boolean hasReminders = false;
            for (Reminder r : allActiveReminders) {
                Calendar remCal = Calendar.getInstance();
                remCal.setTimeInMillis(r.getTimestamp());
                if (remCal.get(Calendar.YEAR) == cellCal.get(Calendar.YEAR) &&
                    remCal.get(Calendar.MONTH) == cellCal.get(Calendar.MONTH) &&
                    remCal.get(Calendar.DAY_OF_MONTH) == cellCal.get(Calendar.DAY_OF_MONTH)) {
                    hasReminders = true;
                    break;
                }
            }

            if (hasReminders && cellCal.get(Calendar.MONTH) == currentCalendar.get(Calendar.MONTH)) {
                // If it has reminders and it's not selected, draw a tiny dot underneath or underline it
                textView.setPaintFlags(textView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
            } else {
                textView.setPaintFlags(textView.getPaintFlags() & (~Paint.UNDERLINE_TEXT_FLAG));
            }

            return convertView;
        }
    }
}
