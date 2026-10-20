package com.example.ui.screens.planner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.model.CalendarEvent
import com.example.core.model.StudyTask
import com.example.core.model.TaskPriority
import com.example.core.repository.CalendarRepository
import com.example.core.repository.ExamRepository
import com.example.core.repository.StudyPlannerRepository
import com.example.ui.components.NexoraCard
import com.example.ui.theme.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

enum class CalendarViewMode {
    MONTH, WEEK, DAY
}

private fun getSubjectColor(subjectId: String): Color {
    return when (subjectId) {
        "gujarati" -> NexoraCyan
        "english" -> NexoraElectricBlue
        "sp_cc" -> NexoraPurple
        "ba" -> NexoraMagenta
        "stat" -> NexoraCyan
        "accounts" -> NexoraGold
        "economics" -> NexoraPink
        else -> NexoraCyan
    }
}

/**
 * Full Calendar View supporting Month, Week, and Day views, previous/next navigation,
 * Today jump, tasks, exams, and custom calendar events persistence & synchronization.
 */
@Composable
fun PlannerCalendarView(
    tasks: List<StudyTask>,
    onToggleTaskCompletion: (String) -> Unit,
    onSelectTask: (StudyTask) -> Unit,
    onAddTaskForDate: ((LocalDate) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val calendarEvents by CalendarRepository.events.collectAsStateWithLifecycle()
    val examsList by ExamRepository.exams.collectAsStateWithLifecycle()

    var viewMode by remember { mutableStateOf(CalendarViewMode.MONTH) }
    var currentYearMonth by remember {
        val today = StudyPlannerRepository.TODAY
        mutableStateOf(YearMonth.of(today.year, today.month))
    }
    var selectedDate by remember { mutableStateOf(StudyPlannerRepository.TODAY) }

    var showAddEventDialog by remember { mutableStateOf(false) }
    var eventToEdit by remember { mutableStateOf<CalendarEvent?>(null) }

    val today = StudyPlannerRepository.TODAY
    val selectedDateStr = selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE)

    // Student-created tasks scheduled on this selected date
    val selectedDateTasks = tasks.filter { it.scheduledDateStr == selectedDateStr }
    val selectedDateEvents = calendarEvents.filter { it.date == selectedDate }
    val selectedDateExams = examsList.filter { it.dateIsoStr == selectedDateStr }

    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("planner_calendar_view")
    ) {
        // --- View Mode Tabs & Month Navigator Header ---
        NexoraCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            containerColor = Color.White,
            borderColor = NexoraCyan.copy(alpha = 0.2f),
            elevation = 0.dp
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Mode switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NexoraSurfaceVariant, RoundedCornerShape(12.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        CalendarViewMode.MONTH to "Month",
                        CalendarViewMode.WEEK to "Week",
                        CalendarViewMode.DAY to "Day"
                    ).forEach { (mode, label) ->
                        val isSelected = viewMode == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color.White else Color.Transparent)
                                .clickable { viewMode = mode },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) NexoraCyan else NexoraTextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Navigation Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            when (viewMode) {
                                CalendarViewMode.MONTH -> currentYearMonth = currentYearMonth.minusMonths(1)
                                CalendarViewMode.WEEK -> selectedDate = selectedDate.minusWeeks(1)
                                CalendarViewMode.DAY -> selectedDate = selectedDate.minusDays(1)
                            }
                        },
                        modifier = Modifier.size(36.dp).testTag("calendar_prev_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Previous",
                            tint = NexoraCyan
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = when (viewMode) {
                                CalendarViewMode.MONTH -> "${currentYearMonth.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${currentYearMonth.year}"
                                CalendarViewMode.WEEK -> "Week of ${selectedDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH))}"
                                CalendarViewMode.DAY -> selectedDate.format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy", Locale.ENGLISH))
                            },
                            color = NexoraTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("calendar_header_label")
                        )
                        Text(
                            text = "Class 12 Commerce Academic Calendar",
                            color = NexoraTextMuted,
                            fontSize = 10.sp
                        )
                    }

                    IconButton(
                        onClick = {
                            when (viewMode) {
                                CalendarViewMode.MONTH -> currentYearMonth = currentYearMonth.plusMonths(1)
                                CalendarViewMode.WEEK -> selectedDate = selectedDate.plusWeeks(1)
                                CalendarViewMode.DAY -> selectedDate = selectedDate.plusDays(1)
                            }
                        },
                        modifier = Modifier.size(36.dp).testTag("calendar_next_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                            contentDescription = "Next",
                            tint = NexoraCyan
                        )
                    }
                }

                // Quick jump to Today
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            currentYearMonth = YearMonth.of(today.year, today.month)
                            selectedDate = today
                        },
                        modifier = Modifier.testTag("calendar_today_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DateRange,
                            contentDescription = null,
                            tint = NexoraCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Today (${today.format(StudyPlannerRepository.DATE_FORMATTER)})",
                            color = NexoraCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // --- Grid or List based on ViewMode ---
                when (viewMode) {
                    CalendarViewMode.MONTH -> {
                        // Day-of-week Headers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            listOf(
                                DayOfWeek.MONDAY,
                                DayOfWeek.TUESDAY,
                                DayOfWeek.WEDNESDAY,
                                DayOfWeek.THURSDAY,
                                DayOfWeek.FRIDAY,
                                DayOfWeek.SATURDAY,
                                DayOfWeek.SUNDAY
                            ).forEach { day ->
                                Text(
                                    text = day.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).take(2),
                                    color = if (day == DayOfWeek.SUNDAY) NexoraGold else NexoraTextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        val firstDayOfMonth = currentYearMonth.atDay(1)
                        val daysInMonth = currentYearMonth.lengthOfMonth()
                        val dayOfWeekOffset = (firstDayOfMonth.dayOfWeek.value - 1)
                        val totalCells = ((dayOfWeekOffset + daysInMonth + 6) / 7) * 7

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            for (row in 0 until (totalCells / 7)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    for (col in 0 until 7) {
                                        val cellIndex = row * 7 + col
                                        val dayNumber = cellIndex - dayOfWeekOffset + 1

                                        if (dayNumber in 1..daysInMonth) {
                                            val cellDate = currentYearMonth.atDay(dayNumber)
                                            val cellDateStr = cellDate.format(DateTimeFormatter.ISO_LOCAL_DATE)

                                            val isSelected = cellDate == selectedDate
                                            val isToday = cellDate == today
                                            val hasExam = examsList.any { it.dateIsoStr == cellDateStr }
                                            val hasTasks = tasks.any { it.scheduledDateStr == cellDateStr }
                                            val hasEvents = calendarEvents.any { it.date == cellDate }

                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .aspectRatio(1f)
                                                    .padding(2.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(
                                                        when {
                                                            isSelected -> NexoraCyan.copy(alpha = 0.25f)
                                                            hasExam -> NexoraGold.copy(alpha = 0.15f)
                                                            isToday -> NexoraSurfaceElevated
                                                            else -> Color.Transparent
                                                        }
                                                    )
                                                    .border(
                                                        width = if (isSelected || hasExam || isToday) 1.5.dp else 0.dp,
                                                        color = when {
                                                            isSelected -> NexoraCyan
                                                            hasExam -> NexoraGold
                                                            isToday -> NexoraBorder
                                                            else -> Color.Transparent
                                                        },
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .clickable { selectedDate = cellDate },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center
                                                ) {
                                                    Text(
                                                        text = "$dayNumber",
                                                        color = when {
                                                            isSelected -> NexoraCyan
                                                            hasExam -> NexoraGold
                                                            isToday -> NexoraCyan
                                                            else -> NexoraTextPrimary
                                                        },
                                                        fontSize = 12.sp,
                                                        fontWeight = if (isSelected || isToday || hasExam) FontWeight.Bold else FontWeight.Normal
                                                    )

                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                        modifier = Modifier.height(5.dp)
                                                    ) {
                                                        if (hasExam) Box(modifier = Modifier.size(4.dp).background(NexoraGold, CircleShape))
                                                        if (hasTasks) Box(modifier = Modifier.size(4.dp).background(NexoraCyan, CircleShape))
                                                        if (hasEvents) Box(modifier = Modifier.size(4.dp).background(NexoraPurple, CircleShape))
                                                    }
                                                }
                                            }
                                        } else {
                                            Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    CalendarViewMode.WEEK -> {
                        // Show 7 days of the current week around selectedDate
                        val startOfWeek = selectedDate.minusDays(((selectedDate.dayOfWeek.value - 1).toLong()))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            for (i in 0 until 7) {
                                val d = startOfWeek.plusDays(i.toLong())
                                val isSelected = d == selectedDate
                                val isToday = d == today
                                val dStr = d.format(DateTimeFormatter.ISO_LOCAL_DATE)
                                val hasExam = examsList.any { it.dateIsoStr == dStr }

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) NexoraCyan.copy(alpha = 0.2f) else if (isToday) NexoraSurfaceVariant else Color.Transparent)
                                        .clickable { selectedDate = d }
                                        .padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).take(2),
                                        color = NexoraTextMuted,
                                        fontSize = 10.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${d.dayOfMonth}",
                                        color = if (isSelected) NexoraCyan else NexoraTextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (hasExam) {
                                        Box(modifier = Modifier.size(4.dp).background(NexoraGold, CircleShape))
                                    }
                                }
                            }
                        }
                    }
                    CalendarViewMode.DAY -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Viewing Schedule for ${selectedDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH))}",
                                color = NexoraCyan,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // --- Selected Date Header & Actions ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Schedule for ${selectedDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH))}",
                    color = NexoraTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${selectedDateExams.size} Exams • ${selectedDateEvents.size} Events • ${selectedDateTasks.size} To-Dos",
                    color = NexoraTextSecondary,
                    fontSize = 11.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { showAddEventDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = NexoraPurple, contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("calendar_add_event_button")
                ) {
                    Icon(imageVector = Icons.Outlined.Event, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Event", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- Exams, Events & Tasks for Selected Date ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Exams
            selectedDateExams.forEach { exam ->
                NexoraCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = Color.White,
                    borderColor = NexoraCyan.copy(alpha = 0.2f),
                    elevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(NexoraGold.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.School, null, tint = NexoraGold, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = exam.name, color = NexoraGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(text = "${exam.type.label} • ${exam.startTime ?: "All Day"}", color = NexoraTextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }

            // Custom Calendar Events
            selectedDateEvents.forEach { event ->
                NexoraCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { eventToEdit = event },
                    containerColor = Color.White,
                    borderColor = NexoraCyan.copy(alpha = 0.2f),
                    elevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(NexoraPurple.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Event, null, tint = NexoraPurple, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = event.title, color = NexoraTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            if (event.description.isNotBlank()) {
                                Text(text = event.description, color = NexoraTextSecondary, fontSize = 11.sp, maxLines = 1)
                            }
                        }
                        Text(text = event.startTime ?: "All Day", color = NexoraPurple, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Study Tasks
            if (selectedDateTasks.isEmpty() && selectedDateEvents.isEmpty() && selectedDateExams.isEmpty()) {
                NexoraCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = Color.White,
                    borderColor = NexoraCyan.copy(alpha = 0.2f),
                    elevation = 0.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Outlined.Event, null, tint = NexoraTextMuted, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No items scheduled on this date.",
                                color = NexoraTextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            } else {
                selectedDateTasks.forEach { task ->
                    CalendarTaskItem(
                        task = task,
                        onToggle = { onToggleTaskCompletion(task.taskId) },
                        onClick = { onSelectTask(task) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Add Event Dialog
    if (showAddEventDialog) {
        AddEditCalendarEventDialog(
            initialDate = selectedDate,
            onDismiss = { showAddEventDialog = false },
            onSave = { newEvent ->
                CalendarRepository.addEvent(newEvent, syncToPlanner = true, context = context)
                showAddEventDialog = false
            }
        )
    }

    // Edit Event Dialog
    if (eventToEdit != null) {
        AddEditCalendarEventDialog(
            initialDate = selectedDate,
            eventToEdit = eventToEdit,
            onDismiss = { eventToEdit = null },
            onSave = { updatedEvent ->
                CalendarRepository.updateEvent(updatedEvent, context)
                eventToEdit = null
            },
            onDelete = {
                CalendarRepository.deleteEvent(eventToEdit!!.id, context)
                eventToEdit = null
            }
        )
    }
}

@Composable
private fun CalendarTaskItem(
    task: StudyTask,
    onToggle: () -> Unit,
    onClick: () -> Unit
) {
    val subjectColor = getSubjectColor(task.subjectId)
    val priorityColor = when (task.priority) {
        TaskPriority.HIGH -> NexoraError
        TaskPriority.MEDIUM -> NexoraGold
        TaskPriority.LOW -> NexoraCyan
    }

    NexoraCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("calendar_task_item_${task.taskId}"),
        containerColor = Color.White,
        borderColor = NexoraCyan.copy(alpha = 0.2f),
        elevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = NexoraSuccess,
                    uncheckedColor = NexoraTextMuted,
                    checkmarkColor = NexoraBackground
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (task.subjectName.isNotBlank() && task.subjectName != "General") {
                        Box(
                            modifier = Modifier
                                .background(subjectColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = task.subjectName,
                                color = subjectColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .background(priorityColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = task.priority.label,
                            color = priorityColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = task.title,
                    color = if (task.isCompleted) NexoraTextMuted else NexoraTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = if (task.isCompleted) FontWeight.Normal else FontWeight.SemiBold,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .background(
                        if (task.isCompleted) NexoraSuccess.copy(alpha = 0.15f) else NexoraCyan.copy(alpha = 0.15f),
                        RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = if (task.isCompleted) "Completed" else "Pending",
                    color = if (task.isCompleted) NexoraSuccess else NexoraCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
