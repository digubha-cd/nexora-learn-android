package com.example.core.model

import kotlinx.serialization.Serializable

@Serializable
data class StudyMilestone(
    val id: String = "",
    val subjectId: String = "",
    val title: String = "",
    val description: String = "",
    val isCompleted: Boolean = false
)
