package com.gg_tech_bharat.gremaider.ai;

import android.content.Context;
import android.util.Log;

import com.gg_tech_bharat.gremaider.database.Reminder;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmartAiEngine {
    private static final String TAG = "SmartAiEngine";

    private final NpuManager npuManager;
    private final boolean isNpuActive;

    public SmartAiEngine(Context context) {
        this.npuManager = new NpuManager(context);
        this.isNpuActive = npuManager.isNpuAccelerated();
        Log.i(TAG, "SmartAiEngine initialized. NPU Active: " + isNpuActive);
    }

    public boolean isNpuActive() {
        return isNpuActive;
    }

    /**
     * Parsed result container for NLP queries.
     */
    public static class ParseResult {
        public String title;
        public long timestamp;
        public boolean hasTime = false;
        public String repeatType = "NONE";
        public int repeatInterval = 0;
        public String priority = "LOW";
        public String category = "Personal";
        public String location = "";
    }

    /**
     * Natural Language Parser.
     * Extracts Title, Date, Time, Repeat, Priority, Category, and Location from string.
     */
    public ParseResult parseQuery(String query) {
        ParseResult result = new ParseResult();
        if (query == null || query.trim().isEmpty()) {
            result.title = "New Reminder";
            result.timestamp = System.currentTimeMillis();
            return result;
        }

        String lowerQuery = query.toLowerCase(Locale.getDefault()).trim();
        Calendar calendar = Calendar.getInstance();

        boolean dateSet = false;
        boolean timeSet = false;

        // --- 1. PRIORITY PREDICTION ---
        // Apply heuristic labels (Rule engine matches/fallback)
        if (lowerQuery.contains("urgent") || lowerQuery.contains("emergency") || 
            lowerQuery.contains("immediately") || lowerQuery.contains("critical") || 
            lowerQuery.contains("must")) {
            result.priority = "HIGH";
        } else if (lowerQuery.contains("soon") || lowerQuery.contains("should") || 
                   lowerQuery.contains("important") || lowerQuery.contains("asap")) {
            result.priority = "MEDIUM";
        } else {
            result.priority = "LOW";
        }

        // --- 2. CATEGORY PREDICTION ---
        if (lowerQuery.contains("buy") || lowerQuery.contains("groceries") || 
            lowerQuery.contains("shop") || lowerQuery.contains("store") || 
            lowerQuery.contains("milk") || lowerQuery.contains("market")) {
            result.category = "Shopping";
        } else if (lowerQuery.contains("gym") || lowerQuery.contains("medicine") || 
                   lowerQuery.contains("pill") || lowerQuery.contains("doctor") || 
                   lowerQuery.contains("health") || lowerQuery.contains("workout") || 
                   lowerQuery.contains("dentist") || lowerQuery.contains("wakeup") ||
                   lowerQuery.contains("wake up")) {
            result.category = "Health";
        } else if (lowerQuery.contains("pay") || lowerQuery.contains("bill") || 
                   lowerQuery.contains("rent") || lowerQuery.contains("bank") || 
                   lowerQuery.contains("money") || lowerQuery.contains("card")) {
            result.category = "Finance";
        } else if (lowerQuery.contains("call") || lowerQuery.contains("email") || 
                   lowerQuery.contains("meet") || lowerQuery.contains("zoom") || 
                   lowerQuery.contains("chat") || lowerQuery.contains("message")) {
            result.category = "Communication";
        } else if (lowerQuery.contains("project") || lowerQuery.contains("work") || 
                   lowerQuery.contains("meeting") || lowerQuery.contains("office") || 
                   lowerQuery.contains("report") || lowerQuery.contains("task")) {
            result.category = "Work";
        } else {
            result.category = "Personal";
        }

        // --- 3. REPEAT & DATE/TIME EXTRACTION ---
        // Check for repeat pattern
        if (lowerQuery.contains("every sunday")) {
            result.repeatType = "WEEKLY";
            result.repeatInterval = 1;
            setDayOfWeek(calendar, Calendar.SUNDAY);
            dateSet = true;
        } else if (lowerQuery.contains("every monday")) {
            result.repeatType = "WEEKLY";
            result.repeatInterval = 1;
            setDayOfWeek(calendar, Calendar.MONDAY);
            dateSet = true;
        } else if (lowerQuery.contains("every tuesday")) {
            result.repeatType = "WEEKLY";
            result.repeatInterval = 1;
            setDayOfWeek(calendar, Calendar.TUESDAY);
            dateSet = true;
        } else if (lowerQuery.contains("every wednesday")) {
            result.repeatType = "WEEKLY";
            result.repeatInterval = 1;
            setDayOfWeek(calendar, Calendar.WEDNESDAY);
            dateSet = true;
        } else if (lowerQuery.contains("every thursday")) {
            result.repeatType = "WEEKLY";
            result.repeatInterval = 1;
            setDayOfWeek(calendar, Calendar.THURSDAY);
            dateSet = true;
        } else if (lowerQuery.contains("every friday")) {
            result.repeatType = "WEEKLY";
            result.repeatInterval = 1;
            setDayOfWeek(calendar, Calendar.FRIDAY);
            dateSet = true;
        } else if (lowerQuery.contains("every saturday")) {
            result.repeatType = "WEEKLY";
            result.repeatInterval = 1;
            setDayOfWeek(calendar, Calendar.SATURDAY);
            dateSet = true;
        } else if (lowerQuery.contains("every day") || lowerQuery.contains("everyday") || lowerQuery.contains("daily")) {
            result.repeatType = "DAILY";
            result.repeatInterval = 1;
        } else if (lowerQuery.contains("every month") || lowerQuery.contains("monthly")) {
            result.repeatType = "MONTHLY";
            result.repeatInterval = 1;
        }

        // Parse absolute keywords: tomorrow, today, next week, day after tomorrow, tonight, next day, this day, next month
        if (!dateSet) {
            if (lowerQuery.contains("tomorrow") || lowerQuery.contains("next day") || lowerQuery.contains("next-day")) {
                calendar.add(Calendar.DAY_OF_YEAR, 1);
                dateSet = true;
            } else if (lowerQuery.contains("day after tomorrow")) {
                calendar.add(Calendar.DAY_OF_YEAR, 2);
                dateSet = true;
            } else if (lowerQuery.contains("today") || lowerQuery.contains("this day") || lowerQuery.contains("tonight")) {
                dateSet = true;
                if (lowerQuery.contains("tonight") && !timeSet) {
                    calendar.set(Calendar.HOUR_OF_DAY, 20); // default 8:00 PM for tonight
                    calendar.set(Calendar.MINUTE, 0);
                    timeSet = true;
                }
            } else if (lowerQuery.contains("next week")) {
                calendar.add(Calendar.WEEK_OF_YEAR, 1);
                dateSet = true;
            } else if (lowerQuery.contains("next month")) {
                calendar.add(Calendar.MONTH, 1);
                dateSet = true;
            } else {
                // Look for days of week
                if (lowerQuery.contains("sunday")) { setDayOfWeek(calendar, Calendar.SUNDAY); dateSet = true; }
                else if (lowerQuery.contains("monday")) { setDayOfWeek(calendar, Calendar.MONDAY); dateSet = true; }
                else if (lowerQuery.contains("tuesday")) { setDayOfWeek(calendar, Calendar.TUESDAY); dateSet = true; }
                else if (lowerQuery.contains("wednesday")) { setDayOfWeek(calendar, Calendar.WEDNESDAY); dateSet = true; }
                else if (lowerQuery.contains("thursday")) { setDayOfWeek(calendar, Calendar.THURSDAY); dateSet = true; }
                else if (lowerQuery.contains("friday")) { setDayOfWeek(calendar, Calendar.FRIDAY); dateSet = true; }
                else if (lowerQuery.contains("saturday")) { setDayOfWeek(calendar, Calendar.SATURDAY); dateSet = true; }
            }
        }

        // Parse Time: "at 8 pm", "at 9:30 am", "at 18:00", "at 7 o'clock", "at 7 oclock"
        Pattern timePattern = Pattern.compile("(?i)(?:at\\s+)?(\\d{1,2})(?::(\\d{2}))?\\s*(pm|am|o'clock|oclock)?");
        Matcher timeMatcher = timePattern.matcher(lowerQuery);
        String matchedTimeText = "";
        if (timeMatcher.find()) {
            matchedTimeText = timeMatcher.group(0);
            int hour = Integer.parseInt(timeMatcher.group(1));
            int minute = 0;
            if (timeMatcher.group(2) != null) {
                minute = Integer.parseInt(timeMatcher.group(2));
            }
            String ampm = timeMatcher.group(3);
            boolean isPm = false;
            boolean ampmSpecified = false;
            if (ampm != null) {
                String ampmLower = ampm.toLowerCase();
                if (ampmLower.equals("pm")) {
                    isPm = true;
                    ampmSpecified = true;
                } else if (ampmLower.equals("am")) {
                    ampmSpecified = true;
                }
            }

            if (ampmSpecified) {
                if (isPm && hour < 12) hour += 12;
                if (!isPm && hour == 12) hour = 0;
                calendar.set(Calendar.HOUR_OF_DAY, hour);
            } else {
                // AM/PM not specified (e.g. "at 7" or "at 7 o'clock")
                // Let's decide contextually so that the time is in the future:
                // Try AM first
                Calendar calAm = (Calendar) calendar.clone();
                int hAm = (hour == 12) ? 0 : hour;
                calAm.set(Calendar.HOUR_OF_DAY, hAm);
                calAm.set(Calendar.MINUTE, minute);
                calAm.set(Calendar.SECOND, 0);
                calAm.set(Calendar.MILLISECOND, 0);
                
                // Try PM
                Calendar calPm = (Calendar) calendar.clone();
                int hPm = (hour < 12) ? hour + 12 : hour;
                calPm.set(Calendar.HOUR_OF_DAY, hPm);
                calPm.set(Calendar.MINUTE, minute);
                calPm.set(Calendar.SECOND, 0);
                calPm.set(Calendar.MILLISECOND, 0);
                
                // Choose the one that is closest in the future
                long now = System.currentTimeMillis();
                if (calAm.getTimeInMillis() > now && calPm.getTimeInMillis() > now) {
                    hour = hAm;
                } else if (calPm.getTimeInMillis() > now) {
                    hour = hPm;
                } else {
                    hour = hAm;
                    calAm.add(Calendar.DAY_OF_YEAR, 1);
                }
                
                calendar.set(Calendar.HOUR_OF_DAY, hour);
                if (calendar.getTimeInMillis() < calAm.getTimeInMillis()) {
                    calendar.setTimeInMillis(calAm.getTimeInMillis());
                }
            }
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            timeSet = true;
        }

        // Relative time: "in 2 hours", "in 30 minutes", "in 30 mins"
        String matchedRelativeTimeText = "";
        if (!timeSet) {
            Pattern relativePattern = Pattern.compile("in (\\d+)\\s*(hour|hr|minute|min)s?");
            Matcher relativeMatcher = relativePattern.matcher(lowerQuery);
            if (relativeMatcher.find()) {
                matchedRelativeTimeText = relativeMatcher.group(0);
                int amount = Integer.parseInt(relativeMatcher.group(1));
                String unit = relativeMatcher.group(2);
                if (unit.startsWith("hour") || unit.startsWith("hr")) {
                    calendar.add(Calendar.HOUR_OF_DAY, amount);
                } else {
                    calendar.add(Calendar.MINUTE, amount);
                }
                timeSet = true;
                dateSet = true;
            }
        }

        // Support for instant alerts (now, instant, immediately)
        if (!timeSet) {
            if (lowerQuery.contains("now") || 
                lowerQuery.contains("instant") || 
                lowerQuery.contains("immediately") || 
                lowerQuery.contains("right now")) {
                calendar.setTimeInMillis(System.currentTimeMillis() + 1000); // Trigger in 1 second
                timeSet = true;
                dateSet = true;
            }
        }

        // Context time triggers: "after dinner", "after lunch", "after breakfast"
        if (!timeSet) {
            if (lowerQuery.contains("after dinner")) {
                calendar.set(Calendar.HOUR_OF_DAY, 20); // 8:00 PM
                calendar.set(Calendar.MINUTE, 0);
                timeSet = true;
            } else if (lowerQuery.contains("after lunch")) {
                calendar.set(Calendar.HOUR_OF_DAY, 14); // 2:00 PM
                calendar.set(Calendar.MINUTE, 0);
                timeSet = true;
            } else if (lowerQuery.contains("after breakfast")) {
                calendar.set(Calendar.HOUR_OF_DAY, 9);  // 9:00 AM
                calendar.set(Calendar.MINUTE, 0);
                timeSet = true;
            } else if (lowerQuery.contains("morning")) {
                calendar.set(Calendar.HOUR_OF_DAY, 8);  // 8:00 AM
                calendar.set(Calendar.MINUTE, 0);
                timeSet = true;
            } else if (lowerQuery.contains("evening")) {
                calendar.set(Calendar.HOUR_OF_DAY, 18); // 6:00 PM
                calendar.set(Calendar.MINUTE, 0);
                timeSet = true;
            } else if (lowerQuery.contains("night")) {
                calendar.set(Calendar.HOUR_OF_DAY, 21); // 9:00 PM
                calendar.set(Calendar.MINUTE, 0);
                timeSet = true;
            }
        }

        // Default to a default time if date was set but time was not
        if (dateSet && !timeSet) {
            calendar.set(Calendar.HOUR_OF_DAY, 9); // default 9:00 AM
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
        }

        result.timestamp = calendar.getTimeInMillis();

        // --- 4. TITLE EXTRACTION ---
        // Let's strip all recognized keywords anywhere in the query to leave only the core action title!
        String cleanTitle = query;
        
        // Strip repeat keywords
        cleanTitle = cleanTitle.replaceAll("(?i)\\b(every\\s*day|everyday|daily|every\\s*month|monthly|every\\s*week|weekly|every\\s+(sunday|monday|tuesday|wednesday|thursday|friday|saturday))\\b", "");
        
        // Strip date keywords
        cleanTitle = cleanTitle.replaceAll("(?i)\\b(tomorrow|today|next\\s+week|this\\s+day|next\\s+day|day\\s+after\\s+tomorrow|tonight|next\\s+month|sunday|monday|tuesday|wednesday|thursday|friday|saturday)\\b", "");
        
        // Strip matched time text
        if (!matchedTimeText.isEmpty()) {
            cleanTitle = cleanTitle.replace(matchedTimeText, "");
        }
        if (!matchedRelativeTimeText.isEmpty()) {
            cleanTitle = cleanTitle.replace(matchedRelativeTimeText, "");
        }
        
        // Strip extra prepositions and context triggers
        cleanTitle = cleanTitle.replaceAll("(?i)\\b(after\\s+dinner|after\\s+lunch|after\\s+breakfast|morning|evening|night)\\b", "");
        
        // Clean common prepositions/preambles
        cleanTitle = cleanTitle.replaceAll("(?i)^(remind me to|remind me|remind|reminds me to|reminds me|create a reminder to|create a reminder|to)\\s+", "");
        cleanTitle = cleanTitle.replaceAll("(?i)\\b(reminds me|remind me|remind|to)\\b", "");
        cleanTitle = cleanTitle.replaceAll("(?i)\\b(at|on|in|for|about)\\b", "");
        
        // Replace multiple spaces and trim
        cleanTitle = cleanTitle.replaceAll("\\s+", " ").trim();
        
        // Capitalize first letter
        if (cleanTitle.length() > 0) {
            cleanTitle = cleanTitle.substring(0, 1).toUpperCase(Locale.getDefault()) + cleanTitle.substring(1);
        } else {
            cleanTitle = "New Reminder";
        }
        result.title = cleanTitle;

        // --- 5. LOCATION EXTRACTION ---
        Pattern locPattern = Pattern.compile("(?i)when i reach (\\w+)|at the (\\w+)|near (\\w+)");
        Matcher locMatcher = locPattern.matcher(query);
        if (locMatcher.find()) {
            String loc = locMatcher.group(1);
            if (loc == null) loc = locMatcher.group(2);
            if (loc == null) loc = locMatcher.group(3);
            result.location = loc.substring(0, 1).toUpperCase() + loc.substring(1);
        }

        result.hasTime = timeSet || dateSet;
        return result;
    }

    private void setDayOfWeek(Calendar calendar, int dayOfWeek) {
        int currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        int difference = dayOfWeek - currentDayOfWeek;
        if (difference <= 0) {
            difference += 7; // force next week
        }
        calendar.add(Calendar.DAY_OF_YEAR, difference);
    }

    /**
     * OCR Document Text Parser.
     * Decodes structured data from parsed lines (e.g. from receipt, prescription).
     */
    public ParseResult parseOcrText(String ocrText) {
        ParseResult result = new ParseResult();
        result.title = "Document Scanned Task";
        result.timestamp = System.currentTimeMillis() + 3600000; // 1 hour later default
        result.priority = "MEDIUM";
        result.category = "Work";

        if (ocrText == null || ocrText.trim().isEmpty()) {
            return result;
        }

        String[] lines = ocrText.split("\\n");
        for (String line : lines) {
            String lower = line.toLowerCase(Locale.getDefault());
            // Look for Date pattern
            Pattern datePattern = Pattern.compile("(?i)(due date|due|date|pay by)[:\\s]+(\\d{2}[-/]\\d{2}[-/]\\d{4}|\\d{4}[-/]\\d{2}[-/]\\d{2})");
            Matcher dateMatcher = datePattern.matcher(lower);
            if (dateMatcher.find()) {
                String dateStr = dateMatcher.group(2);
                result.title = "Pay Bill (" + line.replaceAll("(?i)(due date|due|date|pay by)[:\\s]+", "").trim() + ")";
                result.category = "Finance";
                result.priority = "HIGH";
                // Set calendar date based on dateStr (simple simulation parse)
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DAY_OF_YEAR, 7); // Default to 7 days from now for parsed due date
                result.timestamp = calendar.getTimeInMillis();
            }

            // Prescription check
            if (lower.contains("rx") || lower.contains("take") || lower.contains("tablet") || lower.contains("capsule")) {
                result.title = "Take Medicine: " + line.replaceAll("(?i)(rx|take|tablet|capsule|daily|twice|mg)[:\\s]*", "").trim();
                result.category = "Health";
                result.priority = "HIGH";
                result.repeatType = "DAILY";
                result.repeatInterval = 1;
            }

            // Store/Receipt check
            if (lower.contains("receipt") || lower.contains("total") || lower.contains("items")) {
                result.title = "Review Receipt Details";
                result.category = "Shopping";
            }
        }
        return result;
    }

    /**
     * Duplicate Reminder Detector.
     * Checks if a new reminder is similar to any existing pending reminder.
     */
    public boolean isDuplicate(String title, List<Reminder> activeReminders) {
        if (title == null || activeReminders == null) return false;
        String cleanTitle = title.toLowerCase(Locale.getDefault()).trim();
        for (Reminder reminder : activeReminders) {
            if (reminder.isCompleted() || reminder.isArchived()) continue;
            String existingTitle = reminder.getTitle().toLowerCase(Locale.getDefault()).trim();
            double similarity = getSimilarity(cleanTitle, existingTitle);
            if (similarity > 0.82) {
                Log.d(TAG, "Duplicate detected: '" + title + "' matches '" + reminder.getTitle() + "' (Similarity: " + similarity + ")");
                return true;
            }
        }
        return false;
    }

    private double getSimilarity(String s1, String s2) {
        int len1 = s1.length();
        int len2 = s2.length();
        if (len1 == 0 && len2 == 0) return 1.0;
        if (len1 == 0 || len2 == 0) return 0.0;

        int[][] dp = new int[len1 + 1][len2 + 1];
        for (int i = 0; i <= len1; i++) dp[i][0] = i;
        for (int j = 0; j <= len2; j++) dp[0][j] = j;

        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }
        int distance = dp[len1][len2];
        return 1.0 - ((double) distance / Math.max(len1, len2));
    }

    /**
     * Routine patterns detector.
     * Learns patterns based on completion times and suggests habits.
     */
    public List<String> generateRoutineSuggestions(List<Reminder> history) {
        List<String> suggestions = new ArrayList<>();
        if (history == null || history.size() < 3) {
            suggestions.add("Add and complete more reminders to unlock smart AI routine insights.");
            suggestions.add("Try scheduling standard routines like Gym, Medicine, or Study.");
            return suggestions;
        }

        // Count completion hours
        Map<Integer, Integer> hourCounts = new HashMap<>();
        Map<String, Integer> categoryCounts = new HashMap<>();
        
        for (Reminder reminder : history) {
            if (!reminder.isCompleted()) continue;
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(reminder.getTimestamp());
            int hour = cal.get(Calendar.HOUR_OF_DAY);
            hourCounts.put(hour, hourCounts.getOrDefault(hour, 0) + 1);
            categoryCounts.put(reminder.getCategory(), categoryCounts.getOrDefault(reminder.getCategory(), 0) + 1);
        }

        // Find peak hour
        int peakHour = -1;
        int maxHourCount = 0;
        for (Map.Entry<Integer, Integer> entry : hourCounts.entrySet()) {
            if (entry.getValue() > maxHourCount) {
                maxHourCount = entry.getValue();
                peakHour = entry.getKey();
            }
        }

        // Find peak category
        String peakCategory = "Personal";
        int maxCatCount = 0;
        for (Map.Entry<String, Integer> entry : categoryCounts.entrySet()) {
            if (entry.getValue() > maxCatCount) {
                maxCatCount = entry.getValue();
                peakCategory = entry.getKey();
            }
        }

        if (peakHour != -1) {
            String timeText = formatHour(peakHour);
            suggestions.add("Pattern: You complete most tasks around " + timeText + ". Schedule tasks at this time for optimal productivity.");
        }

        suggestions.add("Favorite Category: You complete '" + peakCategory + "' reminders the most. Keep it up!");
        suggestions.add("Recommendation: You have completed " + history.size() + " reminders! Keep completing tasks daily to improve your routine streak.");

        return suggestions;
    }

    private String formatHour(int hour) {
        if (hour == 0) return "12 AM";
        if (hour == 12) return "12 PM";
        return (hour > 12) ? (hour - 12) + " PM" : hour + " AM";
    }

    public void close() {
        if (npuManager != null) {
            npuManager.close();
        }
    }
}
