package com.gg_tech_bharat.gremaider.database;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "reminders")
public class Reminder {

    @PrimaryKey(autoGenerate = true)
    private int id;
    
    private String title;
    private String description;
    private long timestamp;
    private int repeatInterval; // in days/weeks etc.
    private String repeatType;  // "NONE", "DAILY", "WEEKLY", "MONTHLY"
    private String priority;    // "LOW", "MEDIUM", "HIGH"
    private String category;    // "Work", "Personal", "Shopping", "Health", "Finance", "Communication"
    private String location;
    private boolean isCompleted;
    private boolean isPinned;
    private boolean isArchived;
    private boolean hasAttachment;
    private String checklistJson; // JSON representation of Checklist items
    private String voicePath;     // Audio recording file path
    private long calendarEventId; // System calendar event link ID

    // Constructor
    @Ignore
    public Reminder(String title, String description, long timestamp, int repeatInterval, 
                    String repeatType, String priority, String category, String location, 
                    boolean isCompleted, boolean isPinned, boolean isArchived, 
                    boolean hasAttachment, String checklistJson, String voicePath) {
        this.title = title;
        this.description = description;
        this.timestamp = timestamp;
        this.repeatInterval = repeatInterval;
        this.repeatType = repeatType;
        this.priority = priority;
        this.category = category;
        this.location = location;
        this.isCompleted = isCompleted;
        this.isPinned = isPinned;
        this.isArchived = isArchived;
        this.hasAttachment = hasAttachment;
        this.checklistJson = checklistJson;
        this.voicePath = voicePath;
    }

    // Default constructor for Room
    public Reminder() {}

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public int getRepeatInterval() { return repeatInterval; }
    public void setRepeatInterval(int repeatInterval) { this.repeatInterval = repeatInterval; }

    public String getRepeatType() { return repeatType; }
    public void setRepeatType(String repeatType) { this.repeatType = repeatType; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }

    public boolean isPinned() { return isPinned; }
    public void setPinned(boolean pinned) { isPinned = pinned; }

    public boolean isArchived() { return isArchived; }
    public void setArchived(boolean archived) { isArchived = archived; }

    public boolean isHasAttachment() { return hasAttachment; }
    public void setHasAttachment(boolean hasAttachment) { this.hasAttachment = hasAttachment; }

    public String getChecklistJson() { return checklistJson; }
    public void setChecklistJson(String checklistJson) { this.checklistJson = checklistJson; }

    public String getVoicePath() { return voicePath; }
    public void setVoicePath(String voicePath) { this.voicePath = voicePath; }

    public long getCalendarEventId() { return calendarEventId; }
    public void setCalendarEventId(long calendarEventId) { this.calendarEventId = calendarEventId; }
}
