package com.example.core.model

data class DailyStudyPlan(
    val dateStr: String,
    val dateFormatted: String,
    val totalHours: Int,
    val totalMinutes: Int,
    val tasks: List<StudyTask>,
    val schoolExamDaysRemaining: Long,
    val boardExamDaysRemaining: Long
)
