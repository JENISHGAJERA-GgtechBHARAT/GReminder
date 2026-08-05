package com.gg_tech_bharat.gremaider.ui;

import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gg_tech_bharat.gremaider.MainActivity;
import com.gg_tech_bharat.gremaider.R;
import com.gg_tech_bharat.gremaider.database.Reminder;
import com.gg_tech_bharat.gremaider.receiver.ReminderAlarmManager;
import com.gg_tech_bharat.gremaider.ui.custom.ProductivityGraphView;
import com.gg_tech_bharat.gremaider.viewmodel.ReminderViewModel;

import java.util.Calendar;
import java.util.List;

public class HomeFragment extends Fragment {

    private ReminderViewModel viewModel;
    private ReminderAdapter activeAdapter;
    private ReminderAdapter pinnedAdapter;

    private TextView tvGreeting;
    private TextView tvDate;
    private TextView tvStatsSummary;
    private TextView tvStreakInfo;
    private TextView tvEmptyState;
    private LinearLayout layoutPinned;
    private ProductivityGraphView productivityGraph;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle bundle) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Bind Views
        tvGreeting = view.findViewById(R.id.tv_greeting);
        tvDate = view.findViewById(R.id.tv_date);
        tvStatsSummary = view.findViewById(R.id.tv_stats_summary);
        tvStreakInfo = view.findViewById(R.id.tv_streak_info);
        tvEmptyState = view.findViewById(R.id.tv_empty_state);
        layoutPinned = view.findViewById(R.id.layout_pinned);
        productivityGraph = view.findViewById(R.id.productivity_graph);

        RecyclerView rvActive = view.findViewById(R.id.rv_active);
        RecyclerView rvPinned = view.findViewById(R.id.rv_pinned);

        // Configure Layout Managers
        rvActive.setLayoutManager(new LinearLayoutManager(getContext()));
        rvPinned.setLayoutManager(new LinearLayoutManager(getContext()));

        // Setup Adapters
        activeAdapter = new ReminderAdapter(getContext(), 
                reminder -> openEditSheet(reminder), 
                (reminder, isChecked) -> toggleReminderCompletion(reminder, isChecked));
        
        pinnedAdapter = new ReminderAdapter(getContext(), 
                reminder -> openEditSheet(reminder), 
                (reminder, isChecked) -> toggleReminderCompletion(reminder, isChecked));

        rvActive.setAdapter(activeAdapter);
        rvPinned.setAdapter(pinnedAdapter);

        // Set Up Swipe Actions (ItemTouchHelper)
        setupSwipeActions(rvActive);

        // Setup Greeting and Date
        updateGreetingAndDate();

        // FAB Click
        view.findViewById(R.id.fab_add).setOnClickListener(v -> openCreationSheet());

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(ReminderViewModel.class);

        // Observe active reminders
        viewModel.getActiveReminders().observe(getViewLifecycleOwner(), reminders -> {
            activeAdapter.setReminders(reminders);
            tvEmptyState.setVisibility(reminders.isEmpty() ? View.VISIBLE : View.GONE);
            updateProductivityScore();
        });

        // Observe pinned reminders
        viewModel.getPinnedReminders().observe(getViewLifecycleOwner(), pinnedReminders -> {
            pinnedAdapter.setReminders(pinnedReminders);
            layoutPinned.setVisibility(pinnedReminders.isEmpty() ? View.GONE : View.VISIBLE);
        });

        // Observe completed today
        viewModel.getCompletedTodayReminders().observe(getViewLifecycleOwner(), completedToday -> {
            updateProductivityScore();
        });
    }

    private void updateGreetingAndDate() {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        String greeting;
        if (hour < 12) {
            greeting = "Good Morning";
        } else if (hour < 17) {
            greeting = "Good Afternoon";
        } else {
            greeting = "Good Evening";
        }
        tvGreeting.setText(greeting);

        CharSequence dateStr = DateFormat.format("EEEE, MMMM d", cal.getTime());
        tvDate.setText(dateStr);
    }

    private void updateProductivityScore() {
        viewModel.getStatsSync((total, completed) -> {
            if (isAdded()) {
                tvStatsSummary.setText(completed + " of " + total + " Completed");
                int score = (total == 0) ? 0 : (int) (((float) completed / total) * 100);
                productivityGraph.setProgress(score);
                
                if (score == 100 && total > 0) {
                    tvStreakInfo.setText("Outstanding! You've cleared all scheduled reminders!");
                } else {
                    tvStreakInfo.setText("Complete all your tasks today to maintain your streak!");
                }
            }
        });
    }

    private void toggleReminderCompletion(Reminder reminder, boolean isChecked) {
        reminder.setCompleted(isChecked);
        viewModel.update(reminder);
        
        // Handle Alarms
        if (isChecked) {
            ReminderAlarmManager.cancelAlarm(getContext(), reminder.getId());
            // Check if all are completed today to show Confetti overlay!
            viewModel.getActiveRemindersSync(activeList -> {
                if (activeList.isEmpty() && getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).triggerConfetti();
                }
            });
        } else {
            ReminderAlarmManager.scheduleAlarm(getContext(), reminder);
        }
        updateProductivityScore();
    }

    private void openCreationSheet() {
        ReminderCreationSheet sheet = ReminderCreationSheet.newInstance(0);
        sheet.show(getParentFragmentManager(), "ReminderCreationSheet");
    }

    private void openEditSheet(Reminder reminder) {
        ReminderCreationSheet sheet = ReminderCreationSheet.newInstance(reminder.getId());
        sheet.show(getParentFragmentManager(), "ReminderCreationSheet");
    }

    private void setupSwipeActions(RecyclerView recyclerView) {
        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Reminder reminder = activeAdapter.getReminderAt(position);
                
                if (direction == ItemTouchHelper.LEFT) {
                    // Complete task
                    toggleReminderCompletion(reminder, true);
                    Toast.makeText(getContext(), "Task completed: " + reminder.getTitle(), Toast.LENGTH_SHORT).show();
                } else if (direction == ItemTouchHelper.RIGHT) {
                    // Toggle Pinned status
                    reminder.setPinned(!reminder.isPinned());
                    viewModel.update(reminder);
                    Toast.makeText(getContext(), reminder.isPinned() ? "Reminder pinned" : "Reminder unpinned", Toast.LENGTH_SHORT).show();
                }
            }
        };
        new ItemTouchHelper(callback).attachToRecyclerView(recyclerView);
    }
}
