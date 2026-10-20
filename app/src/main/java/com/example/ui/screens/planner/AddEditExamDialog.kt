package com.example.ui.screens.planner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.ExamItem
import com.example.core.model.ExamType
import com.example.core.model.Subject
import com.example.core.repository.ExamRepository
import com.example.ui.components.NexoraPrimaryButton
import com.example.ui.components.NexoraSecondaryButton
import com.example.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditExamDialog(
    examToEdit: ExamItem? = null,
    onDismiss: () -> Unit,
    onSave: (ExamItem) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val isEditing = examToEdit != null
    var name by remember { mutableStateOf(examToEdit?.name ?: "") }
    var selectedType by remember { mutableStateOf(examToEdit?.type ?: ExamType.UNIT_TEST) }
    var dateStr by remember { mutableStateOf(examToEdit?.date?.format(DateTimeFormatter.ISO_LOCAL_DATE) ?: com.example.core.util.IstTimeUtil.getCurrentLocalDate().plusDays(7).format(DateTimeFormatter.ISO_LOCAL_DATE)) }
    var startTime by remember { mutableStateOf(examToEdit?.startTime ?: "10:00 AM") }
    var endTime by remember { mutableStateOf(examToEdit?.endTime ?: "01:00 PM") }
    var location by remember { mutableStateOf(examToEdit?.location ?: "") }
    var notes by remember { mutableStateOf(examToEdit?.notes ?: "") }
    var selectedSubjects by remember {
        mutableStateOf(examToEdit?.subjectIds?.toSet() ?: Subject.OFFICIAL_SUBJECTS.map { it.id }.toSet())
    }

    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) "Edit Exam" else "Add New Exam",
                color = NexoraTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Exam Name
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Exam Name *") },
                        placeholder = { Text("e.g. First Term Account Exam") },
                        modifier = Modifier.fillMaxWidth().testTag("exam_name_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NexoraCyan,
                            unfocusedBorderColor = NexoraBorder,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = NexoraSurfaceVariant
                        )
                    )
                }

                // Exam Type selector
                item {
                    Text("Exam Type", color = NexoraTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            ExamType.SCHOOL_EXAM to "School Exam",
                            ExamType.BOARD_EXAM to "Board Exam",
                            ExamType.UNIT_TEST to "Unit Test",
                            ExamType.PRELIM to "Prelim",
                            ExamType.OTHER to "Other"
                        ).forEach { (type, label) ->
                            val isSelected = selectedType == type
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedType = type },
                                label = { Text(label, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NexoraCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = NexoraCyan,
                                    containerColor = NexoraSurfaceVariant,
                                    labelColor = NexoraTextPrimary
                                )
                            )
                        }
                    }
                }

                // Date
                item {
                    OutlinedTextField(
                        value = dateStr,
                        onValueChange = { dateStr = it },
                        label = { Text("Date (YYYY-MM-DD) *") },
                        modifier = Modifier.fillMaxWidth().testTag("exam_date_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NexoraCyan,
                            unfocusedBorderColor = NexoraBorder,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = NexoraSurfaceVariant
                        )
                    )
                }

                // Time
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = startTime,
                            onValueChange = { startTime = it },
                            label = { Text("Start Time") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp)
                        )
                        OutlinedTextField(
                            value = endTime,
                            onValueChange = { endTime = it },
                            label = { Text("End Time") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }

                // Location
                item {
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Location / Hall") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp)
                    )
                }

                // Notes
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes / Syllabus Details") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        shape = RoundedCornerShape(16.dp)
                    )
                }

                // Subjects included
                item {
                    Text("Included Subjects (${selectedSubjects.size}/7)", color = NexoraTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Subject.OFFICIAL_SUBJECTS.forEach { subject ->
                            val isIncluded = selectedSubjects.contains(subject.id)
                            FilterChip(
                                selected = isIncluded,
                                onClick = {
                                    selectedSubjects = if (isIncluded) {
                                        selectedSubjects - subject.id
                                    } else {
                                        selectedSubjects + subject.id
                                    }
                                },
                                label = { Text(subject.name, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NexoraGold.copy(alpha = 0.2f),
                                    selectedLabelColor = NexoraGold,
                                    containerColor = NexoraSurfaceVariant,
                                    labelColor = NexoraTextPrimary
                                )
                            )
                        }
                    }
                }

                if (showError) {
                    item {
                        Text(text = errorMessage, color = NexoraError, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isEditing && onDelete != null && examToEdit?.isDefault != true) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = NexoraError)
                    ) {
                        Text("Delete", fontWeight = FontWeight.Bold)
                    }
                }
                NexoraPrimaryButton(
                    text = if (isEditing) "Save Changes" else "Add Exam",
                    onClick = {
                        val trimmedName = name.trim()
                        if (trimmedName.isEmpty()) {
                            showError = true
                            errorMessage = "Please enter an exam name."
                            return@NexoraPrimaryButton
                        }
                        val parsedDate = try {
                            LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE)
                        } catch (_: Exception) {
                            null
                        }
                        if (parsedDate == null) {
                            showError = true
                            errorMessage = "Invalid date format. Use YYYY-MM-DD."
                            return@NexoraPrimaryButton
                        }

                        val exam = ExamItem(
                            id = examToEdit?.id ?: java.util.UUID.randomUUID().toString(),
                            name = trimmedName,
                            type = selectedType,
                            date = parsedDate,
                            startTime = startTime.ifBlank { null },
                            endTime = endTime.ifBlank { null },
                            location = location.ifBlank { null },
                            notes = notes.ifBlank { null },
                            subjectIds = selectedSubjects.toList(),
                            isDefault = examToEdit?.isDefault ?: false
                        )
                        onSave(exam)
                    },
                    modifier = Modifier.testTag("exam_save_button")
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = NexoraTextSecondary)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp)
    )
}
