package com.example.ui.screens.planner

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.CalendarEvent
import com.example.core.repository.CalendarRepository
import com.example.ui.components.NexoraPrimaryButton
import com.example.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun AddEditCalendarEventDialog(
    initialDate: LocalDate,
    eventToEdit: CalendarEvent? = null,
    onDismiss: () -> Unit,
    onSave: (CalendarEvent) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val isEditing = eventToEdit != null
    var title by remember { mutableStateOf(eventToEdit?.title ?: "") }
    var description by remember { mutableStateOf(eventToEdit?.description ?: "") }
    var dateStr by remember { mutableStateOf(eventToEdit?.date?.format(DateTimeFormatter.ISO_LOCAL_DATE) ?: initialDate.format(DateTimeFormatter.ISO_LOCAL_DATE)) }
    var startTime by remember { mutableStateOf(eventToEdit?.startTime ?: "09:00 AM") }
    var isAllDay by remember { mutableStateOf(eventToEdit?.isAllDay ?: true) }

    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) "Edit Calendar Event" else "Add Calendar Event",
                color = NexoraTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Event Title *") },
                    modifier = Modifier.fillMaxWidth().testTag("calendar_event_title_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NexoraCyan,
                        unfocusedBorderColor = NexoraBorder,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = NexoraSurfaceVariant
                    )
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description / Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    shape = RoundedCornerShape(16.dp)
                )

                OutlinedTextField(
                    value = dateStr,
                    onValueChange = { dateStr = it },
                    label = { Text("Date (YYYY-MM-DD) *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )

                OutlinedTextField(
                    value = startTime,
                    onValueChange = { startTime = it },
                    label = { Text("Time (e.g. 10:00 AM)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )

                if (showError) {
                    Text(text = errorMessage, color = NexoraError, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isEditing && onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = NexoraError)
                    ) {
                        Text("Delete", fontWeight = FontWeight.Bold)
                    }
                }
                NexoraPrimaryButton(
                    text = if (isEditing) "Save" else "Add Event",
                    onClick = {
                        val trimmed = title.trim()
                        if (trimmed.isEmpty()) {
                            showError = true
                            errorMessage = "Please enter an event title."
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

                        val event = CalendarEvent(
                            id = eventToEdit?.id ?: java.util.UUID.randomUUID().toString(),
                            title = trimmed,
                            description = description.trim(),
                            date = parsedDate,
                            startTime = startTime.ifBlank { null },
                            isAllDay = isAllDay,
                            taskId = eventToEdit?.taskId
                        )
                        onSave(event)
                    },
                    modifier = Modifier.testTag("calendar_event_save_button")
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
