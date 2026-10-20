package com.example.core.model

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

/**
 * Calendar Event model supporting:
 * - Full calendar events with title, description, date, start/end time
 * - All-day events
 * - Subject and chapter linking
 * - Optional reminders
 * - Bi-directional synchronization with Study Planner tasks and Exam dates
 */
data class CalendarEvent(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val date: LocalDate,
    val startTime: String? = null,
    val endTime: String? = null,
    val isAllDay: Boolean = true,
    val subjectId: String? = null,
    val chapterId: String? = null,
    val reminderEpochMillis: Long? = null,
    val taskId: String? = null,
    val examId: String? = null
) {
    val dateFormatted: String
        get() = date.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH))
}
