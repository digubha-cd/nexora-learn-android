package com.example.core.model

import java.util.UUID

/**
 * Model representing a subject-specific daily study tip for Class 12 Commerce students.
 */
data class DailyStudyTip(
    val id: String = UUID.randomUUID().toString(),
    val subjectId: String,
    val subjectName: String,
    val headline: String,
    val tipContent: String,
    val actionStep: String,
    val language: String = "gu",
    val source: String = "Gemini 3.5 Flash",
    val timestampEpochMillis: Long = System.currentTimeMillis()
)
