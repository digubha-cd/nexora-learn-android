package com.example.core.model

/**
 * Summary metrics for the weekly study notification.
 */
data class WeeklySummaryData(
    val totalMinutes: Long,
    val subjectBreakdown: Map<String, Long>,
    val currentStreak: Int,
    val nextWeekGoalCount: Int
)
