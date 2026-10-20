package com.example.core.model

import java.time.LocalDate

/**
 * Subject-wise completed tasks model for student-created To-Do items.
 */
data class SubjectProgressItem(
    val subjectId: String,
    val subjectName: String,
    val completedCount: Int,
    val pendingCount: Int,
    val totalCount: Int,
    val progressPercentage: Int
)

/**
 * Study Streak statistics.
 * Based on completing at least one student-created To-Do on a day.
 */
data class StudyStreak(
    val currentStreak: Int,
    val bestStreak: Int,
    val isCompletedToday: Boolean, // true if at least 1 task completed today
    val todayStatusText: String, // "Completed today" or "Pending today"
    val streakDays: List<LocalDate> = emptyList()
)

/**
 * Daily study activity for a specific day in the streak/weekly timeline.
 */
data class DayStudyActivity(
    val date: LocalDate,
    val dateStr: String,
    val dayLabel: String,
    val dayNumberFormatted: String,
    val completedCount: Int,
    val totalCount: Int,
    val durationMinutes: Int,
    val isStreakAchieved: Boolean,
    val isToday: Boolean,
    val completedTaskCount: Int = 0,
    val sessionCount: Int = 0,
    val sessionMinutes: Int = 0
) {
    /**
     * 5 activity levels (0..4) based on recorded study duration & completed tasks:
     * 0 = No activity
     * 1 = Very low (1-29 mins or 1 task)
     * 2 = Low (30-59 mins or 2 tasks)
     * 3 = Medium (60-119 mins or 3-4 tasks)
     * 4 = High (120+ mins or 5+ tasks)
     */
    val activityLevel: Int
        get() = when {
            durationMinutes >= 120 || completedCount >= 5 -> 4
            durationMinutes >= 60 || completedCount >= 3 -> 3
            durationMinutes >= 30 || completedCount >= 2 -> 2
            durationMinutes > 0 || completedCount > 0 -> 1
            else -> 0
        }
}

/**
 * Summary metrics for the 365-day study consistency heatmap.
 */
data class HeatmapSummaryStats(
    val activeDaysCount: Int,
    val totalStudyMinutes: Long,
    val currentStreak: Int,
    val bestStreak: Int
)

/**
 * Complete Progress summary based exclusively on the student's own To-Do activity.
 */
data class StudyProgressSummary(
    val completedCount: Int,
    val pendingCount: Int,
    val missedCount: Int,
    val totalCount: Int,
    val dailyPercentage: Int,
    val weeklyPercentage: Int,
    val overallPercentage: Int,
    val subjectProgressList: List<SubjectProgressItem>,
    val streak: StudyStreak
)
