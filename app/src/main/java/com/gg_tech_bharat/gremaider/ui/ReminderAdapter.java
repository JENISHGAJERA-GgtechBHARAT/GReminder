package com.gg_tech_bharat.gremaider.ui;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.gg_tech_bharat.gremaider.R;
import com.gg_tech_bharat.gremaider.database.Reminder;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReminderAdapter extends RecyclerView.Adapter<ReminderAdapter.ViewHolder> {

    private List<Reminder> reminders = new ArrayList<>();
    private final OnReminderClickListener clickListener;
    private final OnReminderCheckChangeListener checkListener;
    private final OnReminderLongClickListener longClickListener;
    private final Context context;

    public interface OnReminderClickListener {
        void onReminderClick(Reminder reminder);
    }

    public interface OnReminderCheckChangeListener {
        void onReminderCheckChange(Reminder reminder, boolean isChecked);
    }

    public interface OnReminderLongClickListener {
        void onReminderLongClick(Reminder reminder);
    }

    public ReminderAdapter(Context context, OnReminderClickListener clickListener, OnReminderCheckChangeListener checkListener, OnReminderLongClickListener longClickListener) {
        this.context = context;
        this.clickListener = clickListener;
        this.checkListener = checkListener;
        this.longClickListener = longClickListener;
    }

    public void setReminders(List<Reminder> reminders) {
        this.reminders = reminders;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reminder, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Reminder reminder = reminders.get(position);
        holder.bind(reminder);
    }

    @Override
    public int getItemCount() {
        return reminders.size();
    }

    public Reminder getReminderAt(int position) {
        return reminders.get(position);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final View priorityIndicator;
        private final CheckBox cbComplete;
        private final TextView tvTitle;
        private final TextView tvDescription;
        private final TextView tvDatetime;
        private final TextView tvCategory;
        private final TextView tvLocation;
        private final ImageView ivCategoryIcon;
        private final ImageView ivPinnedIcon;
        private final LinearLayout layoutChecklistProgress;
        private final TextView tvChecklistStatus;
        private final ProgressBar pbChecklist;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            priorityIndicator = itemView.findViewById(R.id.priority_indicator);
            cbComplete = itemView.findViewById(R.id.cb_complete);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvDescription = itemView.findViewById(R.id.tv_description);
            tvDatetime = itemView.findViewById(R.id.tv_datetime);
            tvCategory = itemView.findViewById(R.id.tv_category);
            tvLocation = itemView.findViewById(R.id.tv_location);
            ivCategoryIcon = itemView.findViewById(R.id.iv_category_icon);
            ivPinnedIcon = itemView.findViewById(R.id.iv_pinned_icon);
            layoutChecklistProgress = itemView.findViewById(R.id.layout_checklist_progress);
            tvChecklistStatus = itemView.findViewById(R.id.tv_checklist_status);
            pbChecklist = itemView.findViewById(R.id.pb_checklist);
        }

        void bind(Reminder reminder) {
            // Set Title & Strike-through if completed
            tvTitle.setText(reminder.getTitle());
            if (reminder.isCompleted()) {
                tvTitle.setPaintFlags(tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                cbComplete.setChecked(true);
            } else {
                tvTitle.setPaintFlags(tvTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                cbComplete.setChecked(false);
            }

            // Description
            if (reminder.getDescription() != null && !reminder.getDescription().trim().isEmpty()) {
                tvDescription.setText(reminder.getDescription());
                tvDescription.setVisibility(View.VISIBLE);
            } else {
                tvDescription.setVisibility(View.GONE);
            }

            // Date / Time Format
            if (reminder.getTimestamp() > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault());
                String datetimeText = sdf.format(new Date(reminder.getTimestamp()));
                
                // Append repeat type if present
                if (!"NONE".equals(reminder.getRepeatType())) {
                    datetimeText += " (🔁 " + reminder.getRepeatType().toLowerCase() + ")";
                }
                tvDatetime.setText(datetimeText);
                tvDatetime.setVisibility(View.VISIBLE);
            } else {
                tvDatetime.setVisibility(View.GONE);
            }

            // Category Info & Color
            tvCategory.setText(reminder.getCategory());
            int catColor = ContextCompat.getColor(context, R.color.text_subtitle_light);
            int catIconRes = R.drawable.ic_person;
            
            switch (reminder.getCategory()) {
                case "Work":
                    catColor = ContextCompat.getColor(context, R.color.cat_work);
                    catIconRes = R.drawable.ic_work;
                    break;
                case "Personal":
                    catColor = ContextCompat.getColor(context, R.color.cat_personal);
                    catIconRes = R.drawable.ic_person;
                    break;
                case "Shopping":
                    catColor = ContextCompat.getColor(context, R.color.cat_shopping);
                    catIconRes = R.drawable.ic_shopping;
                    break;
                case "Health":
                    catColor = ContextCompat.getColor(context, R.color.cat_health);
                    catIconRes = R.drawable.ic_health;
                    break;
                case "Finance":
                    catColor = ContextCompat.getColor(context, R.color.cat_finance);
                    catIconRes = R.drawable.ic_finance;
                    break;
                case "Communication":
                    catColor = ContextCompat.getColor(context, R.color.cat_communication);
                    catIconRes = R.drawable.ic_communication;
                    break;
            }
            tvCategory.setTextColor(catColor);
            ivCategoryIcon.setImageResource(catIconRes);
            ivCategoryIcon.setColorFilter(catColor);

            // Location Badge
            if (reminder.getLocation() != null && !reminder.getLocation().trim().isEmpty()) {
                tvLocation.setText("📍 " + reminder.getLocation());
                tvLocation.setVisibility(View.VISIBLE);
            } else {
                tvLocation.setVisibility(View.GONE);
            }

            // Priority Indicator
            int priorityColor = ContextCompat.getColor(context, R.color.priority_low);
            if ("HIGH".equals(reminder.getPriority())) {
                priorityColor = ContextCompat.getColor(context, R.color.priority_high);
            } else if ("MEDIUM".equals(reminder.getPriority())) {
                priorityColor = ContextCompat.getColor(context, R.color.priority_medium);
            }
            priorityIndicator.setBackgroundColor(priorityColor);

            // Pinned State icon
            if (reminder.isPinned()) {
                ivPinnedIcon.setVisibility(View.VISIBLE);
            } else {
                ivPinnedIcon.setVisibility(View.GONE);
            }

            // Checklist Parser
            String checklistJson = reminder.getChecklistJson();
            if (checklistJson != null && !checklistJson.trim().isEmpty()) {
                try {
                    JSONArray jsonArray = new JSONArray(checklistJson);
                    int total = jsonArray.length();
                    int checkedCount = 0;
                    for (int i = 0; i < total; i++) {
                        JSONObject obj = jsonArray.getJSONObject(i);
                        if (obj.optBoolean("checked", false)) {
                            checkedCount++;
                        }
                    }
                    if (total > 0) {
                        tvChecklistStatus.setText(checkedCount + " of " + total + " completed");
                        int progressPercent = (int) (((float) checkedCount / total) * 100);
                        pbChecklist.setProgress(progressPercent);
                        layoutChecklistProgress.setVisibility(View.VISIBLE);
                    } else {
                        layoutChecklistProgress.setVisibility(View.GONE);
                    }
                } catch (Exception e) {
                    layoutChecklistProgress.setVisibility(View.GONE);
                }
            } else {
                layoutChecklistProgress.setVisibility(View.GONE);
            }

            // Listeners
            itemView.setOnClickListener(v -> {
                if (clickListener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    clickListener.onReminderClick(reminder);
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (longClickListener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    longClickListener.onReminderLongClick(reminder);
                    return true;
                }
                return false;
            });

            cbComplete.setOnClickListener(v -> {
                if (checkListener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    checkListener.onReminderCheckChange(reminder, cbComplete.isChecked());
                }
            });
        }
    }
}
