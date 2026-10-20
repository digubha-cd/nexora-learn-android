package com.example.core.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class StudyTask(
    val taskId: String = "",
    val title: String = "",
    val description: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val chapterId: String? = null,
    val chapterTitle: String? = null,
    val chapterIds: List<String> = emptyList(),
    val scheduledDateStr: String = "",
    val scheduledDateFormatted: String = "",
    @Contextual val scheduledDate: LocalDate? = null,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val durationMinutes: Int = 30,
    val examPhase: ExamType? = null,
    val notes: String = "",
    val reminderTime: String? = null,
    val reminderDateStr: String? = null,
    val reminderEpochMillis: Long? = null,
    val isMissed: Boolean = false,
    val rescheduled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val status: TaskStatus
        get() = when {
            isCompleted -> TaskStatus.COMPLETED
            isMissed -> TaskStatus.MISSED
            else -> TaskStatus.PENDING
        }

    val durationFormatted: String
        get() = if (durationMinutes >= 60) {
            val h = durationMinutes / 60
            val m = durationMinutes % 60
            if (m > 0) "${h}h ${m}m" else "${h}h"
        } else {
            "${durationMinutes}m"
        }
}
