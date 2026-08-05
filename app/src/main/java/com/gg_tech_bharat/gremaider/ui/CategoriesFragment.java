package com.gg_tech_bharat.gremaider.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gg_tech_bharat.gremaider.R;
import com.gg_tech_bharat.gremaider.database.Reminder;
import com.gg_tech_bharat.gremaider.receiver.ReminderAlarmManager;
import com.gg_tech_bharat.gremaider.viewmodel.ReminderViewModel;

import java.util.List;

public class CategoriesFragment extends Fragment {

    private ReminderViewModel viewModel;
    private TextView tvSelectedHeader;
    private TextView tvEmptyState;
    private ReminderAdapter adapter;

    // Category Count TextViews
    private TextView tvCountWork;
    private TextView tvCountPersonal;
    private TextView tvCountShopping;
    private TextView tvCountHealth;
    private TextView tvCountFinance;
    private TextView tvCountComm;

    private String selectedCategory = "Personal";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle bundle) {
        View view = inflater.inflate(R.layout.fragment_categories, container, false);

        // Bind counts
        tvCountWork = view.findViewById(R.id.tv_cat_work_count);
        tvCountPersonal = view.findViewById(R.id.tv_cat_personal_count);
        tvCountShopping = view.findViewById(R.id.tv_cat_shopping_count);
        tvCountHealth = view.findViewById(R.id.tv_cat_health_count);
        tvCountFinance = view.findViewById(R.id.tv_cat_finance_count);
        tvCountComm = view.findViewById(R.id.tv_cat_communication_count);

        tvSelectedHeader = view.findViewById(R.id.tv_selected_cat_header);
        tvEmptyState = view.findViewById(R.id.tv_cat_reminders_empty);

        RecyclerView rvCat = view.findViewById(R.id.rv_cat_reminders);
        rvCat.setLayoutManager(new LinearLayoutManager(getContext()));
        
        adapter = new ReminderAdapter(getContext(), 
                reminder -> openEditSheet(reminder), 
                (reminder, isChecked) -> toggleReminderCompletion(reminder, isChecked));
        rvCat.setAdapter(adapter);

        // Bind Card Clicks to change filter
        view.findViewById(R.id.card_cat_work).setOnClickListener(v -> selectCategory("Work"));
        view.findViewById(R.id.card_cat_personal).setOnClickListener(v -> selectCategory("Personal"));
        view.findViewById(R.id.card_cat_shopping).setOnClickListener(v -> selectCategory("Shopping"));
        view.findViewById(R.id.card_cat_health).setOnClickListener(v -> selectCategory("Health"));
        view.findViewById(R.id.card_cat_finance).setOnClickListener(v -> selectCategory("Finance"));
        view.findViewById(R.id.card_cat_communication).setOnClickListener(v -> selectCategory("Communication"));

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(ReminderViewModel.class);

        // Observe active reminders to count categories dynamically
        viewModel.getActiveReminders().observe(getViewLifecycleOwner(), reminders -> {
            updateCategoryCounts(reminders);
            loadCategoryReminders();
        });
    }

    private void selectCategory(String category) {
        selectedCategory = category;
        tvSelectedHeader.setText("Reminders: " + category);
        loadCategoryReminders();
    }

    private void loadCategoryReminders() {
        if (viewModel == null) return;
        viewModel.getRemindersByCategory(selectedCategory).observe(getViewLifecycleOwner(), reminders -> {
            adapter.setReminders(reminders);
            tvEmptyState.setVisibility(reminders.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    private void updateCategoryCounts(List<Reminder> reminders) {
        int work = 0, personal = 0, shopping = 0, health = 0, finance = 0, comm = 0;
        for (Reminder r : reminders) {
            if (r.isCompleted() || r.isArchived()) continue;
            switch (r.getCategory()) {
                case "Work": work++; break;
                case "Personal": personal++; break;
                case "Shopping": shopping++; break;
                case "Health": health++; break;
                case "Finance": finance++; break;
                case "Communication": comm++; break;
            }
        }

        tvCountWork.setText(work + " active");
        tvCountPersonal.setText(personal + " active");
        tvCountShopping.setText(shopping + " active");
        tvCountHealth.setText(health + " active");
        tvCountFinance.setText(finance + " active");
        tvCountComm.setText(comm + " active");
    }

    private void toggleReminderCompletion(Reminder reminder, boolean isChecked) {
        reminder.setCompleted(isChecked);
        viewModel.update(reminder);
        if (isChecked) {
            ReminderAlarmManager.cancelAlarm(getContext(), reminder.getId());
        } else {
            ReminderAlarmManager.scheduleAlarm(getContext(), reminder);
        }
    }

    private void openEditSheet(Reminder reminder) {
        ReminderCreationSheet sheet = ReminderCreationSheet.newInstance(reminder.getId());
        sheet.show(getParentFragmentManager(), "ReminderCreationSheet");
    }
}
