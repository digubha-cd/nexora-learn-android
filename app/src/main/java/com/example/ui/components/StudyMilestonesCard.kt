package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.model.StudyMilestone
import com.example.core.repository.StudyMilestoneRepository
import com.example.ui.theme.NexoraBorder
import com.example.ui.theme.NexoraCyan
import com.example.ui.theme.NexoraSurface
import com.example.ui.theme.NexoraSurfaceElevated
import com.example.ui.theme.NexoraSurfaceVariant
import com.example.ui.theme.NexoraTextMuted
import com.example.ui.theme.NexoraTextPrimary
import com.example.ui.theme.NexoraTextSecondary

/**
 * Component for managing actionable study milestones and sub-tasks for a specific subject.
 */
@Composable
fun StudyMilestonesCard(
    subjectId: String,
    subjectColor: Color,
    modifier: Modifier = Modifier
) {
    val allMilestones by StudyMilestoneRepository.milestones.collectAsStateWithLifecycle()
    val milestones = remember(allMilestones, subjectId) {
        StudyMilestoneRepository.getMilestonesForSubject(subjectId, allMilestones)
    }

    val completedCount = milestones.count { it.isCompleted }
    val totalCount = milestones.size
    val progressFraction = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f

    var showAddDialog by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    var newDescription by remember { mutableStateOf("") }

    NexoraCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("study_milestones_card_$subjectId"),
        cornerRadius = 18.dp,
        contentPadding = 16.dp,
        elevation = 2.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(subjectColor.copy(alpha = 0.12f), CircleShape)
                            .border(1.dp, subjectColor.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Flag,
                            contentDescription = null,
                            tint = subjectColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Subject Study Milestones",
                            color = NexoraTextPrimary,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$completedCount of $totalCount Sub-tasks Completed",
                            color = NexoraTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                // Add Button
                OutlinedButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("add_milestone_button"),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add",
                        tint = subjectColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Add Goal",
                        color = subjectColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress Bar
            if (totalCount > 0) {
                LinearProgressIndicator(
                    progress = { progressFraction.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = subjectColor,
                    trackColor = NexoraSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Milestone List
            if (milestones.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No custom milestones yet. Add sub-tasks like 'Revise formulas' or 'Solve Past Paper'!",
                        color = NexoraTextMuted,
                        fontSize = 11.5.sp
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    milestones.forEach { milestone ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(NexoraSurfaceVariant.copy(alpha = 0.5f))
                                .border(0.8.dp, NexoraBorder, RoundedCornerShape(10.dp))
                                .clickable { StudyMilestoneRepository.toggleMilestone(milestone.id) }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                                .testTag("milestone_item_${milestone.id}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Checkbox(
                                    checked = milestone.isCompleted,
                                    onCheckedChange = { StudyMilestoneRepository.toggleMilestone(milestone.id) },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = subjectColor,
                                        uncheckedColor = NexoraTextMuted
                                    ),
                                    modifier = Modifier.size(24.dp).testTag("milestone_checkbox_${milestone.id}")
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = milestone.title,
                                        color = if (milestone.isCompleted) NexoraTextMuted else NexoraTextPrimary,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        textDecoration = if (milestone.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                    )
                                    if (milestone.description.isNotBlank()) {
                                        Text(
                                            text = milestone.description,
                                            color = NexoraTextSecondary,
                                            fontSize = 10.5.sp
                                        )
                                    }
                                }
                            }

                            IconButton(
                                onClick = { StudyMilestoneRepository.deleteMilestone(milestone.id) },
                                modifier = Modifier.size(28.dp).testTag("delete_milestone_${milestone.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = "Delete",
                                    tint = NexoraTextMuted,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Milestone Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text(text = "Add Study Milestone / Sub-task", color = NexoraTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Define an actionable sub-task for this subject (e.g., 'Memorize accounting rules', 'Practice 5 partnership sums').",
                        color = NexoraTextSecondary,
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Milestone Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("milestone_title_input")
                    )
                    OutlinedTextField(
                        value = newDescription,
                        onValueChange = { newDescription = it },
                        label = { Text("Description (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("milestone_desc_input")
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newTitle.isNotBlank()) {
                            StudyMilestoneRepository.addMilestone(subjectId, newTitle, newDescription)
                            newTitle = ""
                            newDescription = ""
                            showAddDialog = false
                        }
                    },
                    modifier = Modifier.testTag("save_milestone_button")
                ) {
                    Text(text = "Add", color = NexoraCyan, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text(text = "Cancel", color = NexoraTextSecondary)
                }
            },
            containerColor = NexoraSurfaceElevated,
            tonalElevation = 6.dp
        )
    }
}
