package com.example.core.model

import kotlinx.serialization.Serializable

@Serializable
data class StudentProfile(
    val userId: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val avatarColorHex: String = "#00BCD4",
    val competitionPoints: Int = 0,
    val studyStreakDays: Int = 0,
    val totalFocusedMinutes: Int = 0,
    val weeklyStudyGoalHours: Int = 10,
    val targetScore: Int = 90,
    val subjects: List<String> = emptyList(),
    val medium: String = "Gujarati Medium",
    val classLevel: String = "Class 12 Commerce",
    val targetMarks: String = "90+ Marks",
    val completedChapterIds: List<String> = emptyList(),
    val completedSessionsCount: Int = 0,
    val completedTodoCount: Int = 0,
    val statusText: String = "",
    val leaderboardOptIn: Boolean = true,
    val phoneNumber: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
