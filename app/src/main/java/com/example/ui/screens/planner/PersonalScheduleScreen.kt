package com.example.ui.screens.planner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.AccessTime
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
import com.example.core.model.CalendarEvent
import com.example.core.repository.CalendarRepository
import com.example.core.repository.StudyPlannerRepository
import com.example.ui.components.NexoraCard
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Premium Personal Schedule Screen.
 * Allows students to schedule timed study blocks, classes, or revision sessions
 * with start and end times, and synchronizes them to the To-Do planner and Calendar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalScheduleScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("09:00 AM") }
    var endTime by remember { mutableStateOf("10:30 AM") }

    val today = remember { StudyPlannerRepository.TODAY }
    var selectedDateOption by remember { mutableStateOf("today") }
    var customDate by remember { mutableStateOf<LocalDate?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    val resolvedDate: LocalDate = when (selectedDateOption) {
        "today" -> today
        "tomorrow" -> today.plusDays(1)
        "custom" -> customDate ?: today
        else -> today
    }

    var isLoading by remember { mutableStateOf(false) }
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = NexoraBackground,
        modifier = Modifier.testTag("personal_schedule_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Schedule Personal Task", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = NexoraTextPrimary)
                        Text("Time-Blocked Study Sessions & Events", fontSize = 11.sp, color = NexoraTextSecondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        keyboardController?.hide()
                        onBackClick()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NexoraTextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NexoraBackground)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            NexoraCard(
                containerColor = NexoraSurface,
                borderColor = NexoraBorder.copy(alpha = 0.75f),
                cornerRadius = 20.dp,
                contentPadding = 16.dp
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.AccessTime, null, tint = NexoraGold, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Session Details", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = NexoraTextPrimary)
                        Text("Automatically syncs with Study Planner & Calendar", fontSize = 11.sp, color = NexoraTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Session / Task Title *") },
                    placeholder = { Text("e.g. Accounts Paper Solving Block") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = NexoraSurfaceVariant,
                        focusedContainerColor = NexoraSurfaceVariant,
                        focusedBorderColor = NexoraCyan
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Details / Location / Notes") },
                    placeholder = { Text("e.g. Self-study at library desk, silence phone") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = NexoraSurfaceVariant,
                        focusedContainerColor = NexoraSurfaceVariant,
                        focusedBorderColor = NexoraCyan
                    )
                )
            }

            // Date & Time Block Card
            NexoraCard(
                containerColor = NexoraSurface,
                borderColor = NexoraBorder.copy(alpha = 0.75f),
                cornerRadius = 20.dp,
                contentPadding = 16.dp
            ) {
                Text("Scheduled Date", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NexoraTextPrimary)
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("today" to "Today", "tomorrow" to "Tomorrow", "custom" to (customDate?.format(DateTimeFormatter.ofPattern("d MMM")) ?: "Pick Date")).forEach { (opt, label) ->
                        val isOptSelected = selectedDateOption == opt
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    if (opt == "custom") showDatePicker = true
                                    else selectedDateOption = opt
                                }
                                .clip(RoundedCornerShape(10.dp)),
                            color = if (isOptSelected) NexoraGold.copy(alpha = 0.15f) else NexoraSurfaceVariant,
                            border = BorderStroke(1.dp, if (isOptSelected) NexoraGold else Color.Transparent)
                        ) {
                            Text(
                                label,
                                fontSize = 11.sp,
                                fontWeight = if (isOptSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isOptSelected) Color(0xFFB45309) else NexoraTextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Study Window (Start & End)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NexoraTextPrimary)
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Start Time") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("End Time") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("08:00 AM - 09:30 AM", "10:00 AM - 12:00 PM", "03:00 PM - 05:00 PM", "07:30 PM - 09:30 PM").forEach { preset ->
                        Surface(
                            modifier = Modifier
                                .clickable {
                                    val parts = preset.split(" - ")
                                    startTime = parts.getOrNull(0) ?: ""
                                    endTime = parts.getOrNull(1) ?: ""
                                }
                                .clip(RoundedCornerShape(8.dp)),
                            color = NexoraSurfaceVariant
                        ) {
                            Text(preset, fontSize = 10.sp, color = NexoraTextSecondary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                }
            }

            // Save Button
            Button(
                onClick = {
                    if (title.isBlank()) {
                        scope.launch { snackbarHostState.showSnackbar("Please enter a title for the session.") }
                        return@Button
                    }
                    if (isLoading) return@Button
                    isLoading = true

                    scope.launch {
                        val event = CalendarEvent(
                            title = title.trim(),
                            description = description.trim(),
                            date = resolvedDate,
                            startTime = startTime.trim(),
                            endTime = endTime.trim(),
                            isAllDay = false
                        )

                        // Synchronize both to Calendar and to Study Planner
                        val saved = CalendarRepository.addEvent(
                            event = event,
                            syncToPlanner = true,
                            context = context
                        )

                        keyboardController?.hide()
                        if (saved != null) {
                            onBackClick()
                        } else {
                            isLoading = false
                            snackbarHostState.showSnackbar("Failed to save schedule.")
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NexoraCyan)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                } else {
                    Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Personal Schedule", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        customDate = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.of("UTC"))
                            .toLocalDate()
                        selectedDateOption = "custom"
                    }
                    showDatePicker = false
                }) {
                    Text("OK", color = NexoraCyan)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
