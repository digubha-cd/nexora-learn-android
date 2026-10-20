# Weekly Study Summary Notifications

Implementation plan for a scheduled notification system that generates and delivers a weekly study performance summary to the student every Sunday at 7:00 PM.

## User Review & Critical Decisions

- **Schedule**: Sundays at 7:00 PM.
- **Content**: Total study hours, subject-wise breakdown, current streak status, and next week's goals.

---

## 1. Overview & Core Concept

- **What It Does**: Automates a weekly push notification that summarizes the student's learning progress. It calculates the past 7 days of activity and prepares a personalized summary report.
- **Key Value**: Provides a consistent, automated check-in that reinforces study habits, highlights progress toward the 90+ marks goal, and keeps the student accountable.

---

## 2. Technical Architecture & Data Strategy

### Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                  StudyReminderManager                        │
│ - Schedules recurring alarm for Sunday 7 PM                 │
│ - Uses WorkManager for background task execution            │
└──────────────────────────────┬───────────────────────────────┘
                               │
                               ▼
┌──────────────────────────────────────────────────────────────┐
│                  StudySummaryWorker                          │
│ - Triggered by WorkManager                                   │
│ - Queries StudyPlannerRepository for weekly statistics      │
│ - Formats the summary text                                  │
└──────────────────────────────┬───────────────────────────────┘
                               │
                               ▼
┌──────────────────────────────────────────────────────────────┐
│                  NotificationManager                         │
│ - Constructs and shows the notification                      │
└──────────────────────────────┘
```

### Key Logic
- **Scheduling**: Utilize `WorkManager` with `PeriodicWorkRequestBuilder` to schedule the task. Use `Constraints` to ensure network or battery conditions don't interfere, although this task is local-only.
- **Data Aggregation**: `StudyPlannerRepository` will need a new method: `getWeeklySummaryStats()` returning a data class with the requested metrics.
- **Formatting**: The summary text will be dynamically generated based on current data.

### Data Models
- **`WeeklySummaryData`**:
  - `totalHours: Long`
  - `subjectBreakdown: Map<String, Long>` (Subject ID to total minutes)
  - `currentStreak: Int`
  - `nextWeekGoalCount: Int`
