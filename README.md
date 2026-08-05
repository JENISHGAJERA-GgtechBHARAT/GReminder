# GReminder - Smart On-Device AI Checklist & Alarm Reminders

GReminder is a premium, secure, and smart personal task manager built for Android. It operates fully offline (zero internet permissions) with local database queries, speech/NLP processing, and custom inter-app signature communication guards.

## Features

### 1. Dual Mode Task Manager (To-Do & Reminders)
- **Optional Alerts**: Seamlessly switch between a pure checklist item (no alert) and an alarm reminder.
- **Collapsible Pickers**: Date & time buttons are hidden unless "Enable Alert" is toggled. Toggling it ON schedules exact background alarms.
- **Instant Scheduling**: Past-time inputs on the current day trigger exactly 1 second in the future for seamless testing.

### 2. Lockscreen Alarms & Smart Pop-ups
- **Locked State**: Wakes the screen and plays custom loop sound/vibration alerts over the lockscreen.
- **Swipe-to-Complete Gesture**: Drag-to-complete swipe controls on the alarm overlay prevent accidental dismissals.
- **Unlocked State (Pop-up Banner)**: Slides down a high-priority heads-up notification with action buttons ("Complete" & "Snooze").

### 3. Smart Missed Call & SMS Callback Alarms
- **Missed Call Detection**: Automatically monitors call status using standard telephony receivers.
- **Dynamic Context Timings**:
  - If you texted the caller a quick-response (e.g. *"I will call you later"*), it schedules a callback reminder in **1 hour**.
  - If you missed the call without texting, it schedules an immediate callback reminder in **10 minutes**.

### 4. Advanced App Security & Hardening
- **Signature Permission Guard**: Employs signature-level checks (`com.gg_tech_bharat.gremaider.permission.WRITE_REMINDERS`) to secure inter-app intents, blocking third-party spoofing attacks.
- **Code Obfuscation (R8/ProGuard)**: Enabled code minification and resource optimization rules in release builds.
- **Biometric Security Lock**: Fingerprint/Face biometric prompt on app startup. Hides all UI containers until authentication passes.
- **100% Offline Sandbox**: Declares zero network/internet permissions in the manifest, keeping your private database fully isolated.

### 5. Seamless UI/UX & Optimizations
- **Productivity Score Graph**: Tracks completion rates in real-time. Displays a custom Canvas arc diagram.
- **Confetti Celebration**: Triggers confetti overlays when all tasks are marked complete.
- **Auto-Pruning Database Cleanups**: Asynchronously deletes completed one-time reminders older than 24 hours on launch.

---

## Technical Stack & Dependencies
- **Platform**: Android SDK (Target API 36, Min API 30)
- **Architecture**: MVVM pattern with Room Database, LiveData, and ViewModels
- **Job Scheduling**: AlarmManager (Exact Alarm API 31+) & WorkManager
- **Language**: Java / XML Layouts
- **Theme Support**: Material 3 Day/Night theme override

## Build Configuration
Compile the debug package using Gradle:
```powershell
.\gradlew.bat assembleDebug
```
