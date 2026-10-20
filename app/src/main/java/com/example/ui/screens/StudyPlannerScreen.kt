package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.model.StudyTask
import com.example.core.model.Subject
import com.example.core.repository.StudyPlannerRepository
import com.example.core.timer.StudyTimerManager
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.example.ui.animation.bounceClick
import com.example.ui.components.EmptyStateView
import com.example.ui.components.NexoraCard
import com.example.ui.components.NexoraTargetBadge
import com.example.ui.components.NexoraCreatorCredit
import com.example.ui.components.PersistentStudyTimerCard
import com.example.ui.screens.planner.AddEditTaskDialog
import com.example.ui.screens.planner.PlannerCalendarView
import com.example.ui.theme.*
import java.time.LocalDate

enum class PlannerTab(val label: String) {
    TODAY("Today"),
    UPCOMING("Upcoming"),
    MISSED("Missed"),
    HISTORY("History"),
    ALL("All")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyPlannerScreen(
    modifier: Modifier = Modifier,
    onNavigateToSubjects: () -> Unit = {},
    onOpenTodoList: () -> Unit = {}
) {
    val tasks by StudyPlannerRepository.tasks.collectAsStateWithLifecycle()
    val timerState by StudyTimerManager.timerState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    val today = StudyPlannerRepository.TODAY
    val schoolDays = StudyPlannerRepository.getSchoolExamDaysRemaining(today)
    val boardDays = StudyPlannerRepository.getBoardExamDaysRemaining(today)

    var showAddTaskDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<StudyTask?>(null) }
    var showCalendar by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .testTag("study_planner_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Official Logo + Title and Subtitle aligned together
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    com.example.ui.components.NexoraLogo(emblemSize = 40.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Study Planner",
                            color = NexoraTextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Class 12 Commerce System",
                            color = NexoraCyan,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.4.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Right: Calendar toggle button
                Button(
                    onClick = { showCalendar = !showCalendar },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (showCalendar) NexoraCyan else NexoraSurfaceVariant,
                        contentColor = if (showCalendar) MaterialTheme.colorScheme.onPrimary else NexoraTextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("calendar_toggle_button")
                ) {
                    Icon(
                        imageVector = if (showCalendar) Icons.Outlined.FormatListBulleted else Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (showCalendar) "List View" else "Calendar",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (showCalendar) {
                PlannerCalendarView(
                    tasks = tasks,
                    onToggleTaskCompletion = { taskId -> StudyPlannerRepository.toggleTaskCompletion(taskId) },
                    onSelectTask = { task -> taskToEdit = task },
                    onAddTaskForDate = { _ -> showAddTaskDialog = true },
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 2. 90+ MARKS TARGET Card
                    item {
                        NexoraTargetBadge()
                    }

                    // 3. Focus Timer
                    item {
                        PersistentStudyTimerCard(
                            timerState = timerState,
                            onStartTimer = { StudyTimerManager.startTimer() },
                            onPauseTimer = { StudyTimerManager.pauseTimer() },
                            onResumeTimer = { StudyTimerManager.resumeTimer() },
                            onResetTimer = { StudyTimerManager.resetTimer() },
                            onCompleteEarly = { StudyTimerManager.finishSession() },
                            onSelectSubject = { subject -> StudyTimerManager.selectSubject(subject) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // 5. To-Do Hub Entry Card (Premium Glass)
                    item {
                        val todayCount = remember(tasks) { tasks.count { it.scheduledDateStr == StudyPlannerRepository.TODAY_STR } }
                        val pendingCount = remember(tasks) { tasks.count { !it.isCompleted } }

                        NexoraCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .bounceClick(onClick = onOpenTodoList)
                                .testTag("open_todo_hub_card"),
                            cornerRadius = 22.dp,
                            contentPadding = 18.dp,
                            borderColor = NexoraCyan.copy(alpha = 0.5f),
                            containerColor = MaterialTheme.colorScheme.surface,
                            elevation = 3.dp
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .background(NexoraCyan.copy(alpha = 0.14f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Outlined.Assignment, contentDescription = null, tint = NexoraCyan, modifier = Modifier.size(24.dp))
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "To-Do Hub",
                                                color = MaterialTheme.colorScheme.onBackground,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                            if (todayCount > 0) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Surface(
                                                    color = NexoraCyan,
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text(
                                                        "$todayCount Today",
                                                        color = MaterialTheme.colorScheme.onPrimary,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Today's Tasks • Chapters • Schedule • History ($pendingCount pending)",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = NexoraCyan)
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        NexoraCreatorCredit()
                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
            }
        }

    }

    // Dialogs
    if (showAddTaskDialog) {
        AddEditTaskDialog(
            onDismiss = { showAddTaskDialog = false },
            onSave = { title, desc, subId, date, prio, remT, remD, remE, chIds ->
                StudyPlannerRepository.addTask(
                    title = title,
                    description = desc,
                    subjectId = subId,
                    scheduledDate = date,
                    priority = prio,
                    reminderTime = remT,
                    reminderDateStr = remD,
                    reminderEpochMillis = remE,
                    chapterIds = chIds,
                    context = context
                )
                showAddTaskDialog = false
            }
        )
    }

    if (taskToEdit != null) {
        AddEditTaskDialog(
            taskToEdit = taskToEdit,
            onDismiss = { taskToEdit = null },
            onSave = { title, desc, subId, date, prio, remT, remD, remE, chIds ->
                StudyPlannerRepository.updateTask(
                    taskId = taskToEdit!!.taskId,
                    title = title,
                    description = desc,
                    subjectId = subId,
                    scheduledDate = date,
                    priority = prio,
                    reminderTime = remT,
                    reminderDateStr = remD,
                    reminderEpochMillis = remE,
                    chapterIds = chIds,
                    context = context
                )
                taskToEdit = null
            },
            onDelete = {
                StudyPlannerRepository.deleteTask(taskToEdit!!.taskId, context)
                taskToEdit = null
            }
        )
    }
}

@Composable
private fun TaskListItem(
    task: StudyTask,
    onClick: () -> Unit
) {
    val subjectColor = remember(task.subjectId, task.subjectName) {
        getSubjectAccentColor(task.subjectId, task.subjectName)
    }

    NexoraCard(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick(onClick = onClick)
            .testTag("task_item_${task.taskId}"),
        containerColor = if (task.isCompleted) Color(0xFF0A0F1D).copy(alpha = 0.5f) else Color(0xFF0F172A).copy(alpha = 0.78f),
        cornerRadius = 20.dp,
        contentPadding = 14.dp,
        borderColor = if (task.isCompleted) NexoraSuccess.copy(alpha = 0.35f) else NexoraBorder.copy(alpha = 0.45f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (task.isCompleted) subjectColor.copy(alpha = 0.35f) else subjectColor)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { StudyPlannerRepository.toggleTaskCompletion(task.taskId) },
                colors = CheckboxDefaults.colors(
                    checkedColor = NexoraSuccess,
                    uncheckedColor = NexoraBorder.copy(alpha = 0.7f)
                ),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    color = if (task.isCompleted) NexoraTextMuted else NexoraTextPrimary,
                    fontSize = 14.5.sp,
                    fontWeight = if (task.isCompleted) FontWeight.Medium else FontWeight.Bold,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = task.subjectName.ifBlank { "Personal Task" },
                        color = subjectColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (task.scheduledDateStr.isNotBlank()) {
                        Text(
                            text = " • ${task.scheduledDateFormatted}",
                            color = NexoraTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            if (task.priority == com.example.core.model.TaskPriority.HIGH) {
                Surface(
                    color = NexoraError.copy(alpha = 0.15f),
                    border = BorderStroke(0.8.dp, NexoraError.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "URGENT",
                        color = NexoraError,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
