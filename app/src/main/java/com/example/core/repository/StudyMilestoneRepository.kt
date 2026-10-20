package com.example.core.repository

import com.example.core.model.StudyMilestone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

object StudyMilestoneRepository {
    private val _milestones = MutableStateFlow<List<StudyMilestone>>(emptyList())
    val milestones: StateFlow<List<StudyMilestone>> = _milestones.asStateFlow()

    fun getMilestonesForSubject(subjectId: String, allMilestones: List<StudyMilestone>): List<StudyMilestone> {
        return allMilestones.filter { it.subjectId == subjectId }
    }

    fun toggleMilestone(id: String) {
        _milestones.value = _milestones.value.map {
            if (it.id == id) it.copy(isCompleted = !it.isCompleted) else it
        }
    }

    fun deleteMilestone(id: String) {
        _milestones.value = _milestones.value.filterNot { it.id == id }
    }

    fun addMilestone(subjectId: String, title: String, description: String) {
        val newMilestone = StudyMilestone(
            id = UUID.randomUUID().toString(),
            subjectId = subjectId,
            title = title,
            description = description,
            isCompleted = false
        )
        _milestones.value = _milestones.value + newMilestone
    }
}
