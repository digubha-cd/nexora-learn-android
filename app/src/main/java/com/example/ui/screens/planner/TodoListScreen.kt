package com.example.ui.screens.planner

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.model.StudyTask
import com.example.core.model.Subject
import com.example.core.model.TaskPriority
import com.example.core.repository.StudyPlannerRepository
import com.example.core.repository.SyllabusRepository
import com.example.ui.animation.bounceClick
import com.example.ui.components.EmptyStateView
import com.example.ui.components.NexoraCard
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class TodoHubCategory(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accentColor: Color
) {
    TODAY("Today's To-Do", "Due Today", Icons.Outlined.WbSunny, NexoraCyan),
    ALL("All Tasks", "Everything", Icons.Outlined.FormatListBulleted, Color(0xFF64748B)),
    CHAPTER("Chapter Study", "Syllabus Linked", Icons.Outlined.MenuBook, NexoraPurple),
    CUSTOM("Custom Tasks", "Freeform Tasks", Icons.Outlined.EditNote, NexoraPink),
    PERSONAL("Personal Schedule", "Timed & Events", Icons.Outlined.AccessTime, NexoraGold),
    UPCOMING("Upcoming Tasks", "Scheduled Ahead", Icons.Outlined.Schedule, NexoraElectricBlue),
    OVERDUE("Overdue Tasks", "Needs Attention", Icons.Outlined.WarningAmber, NexoraError),
    HISTORY("Completed Tasks", "Done & Archive", Icons.Outlined.CheckCircle, NexoraSuccess)
}

enum class TaskSortOrder(
    val label: String,
    val icon: ImageVector,
    val description: String
) {
    DATE("Due Date", Icons.Outlined.CalendarToday, "Soonest & Overdue first"),
    PRIORITY("Priority", Icons.Outlined.Flag, "High -> Medium -> Low"),
    ALPHABETICAL("Alphabetical", Icons.Outlined.SortByAlpha, "A to Z by Task Title")
}

enum class WorkloadTab(
    val title: String,
    val icon: ImageVector,
    val description: String,
    val accentColor: Color
) {
    ALL("All Workload", Icons.Outlined.Dashboard, "All tasks", NexoraElectricBlue),
    ACADEMIC("Academic", Icons.Outlined.School, "Syllabus & Subjects", NexoraCyan),
    REVISION("Revision", Icons.Outlined.Loop, "Review & Practice", NexoraPurple),
    EXTRACURRICULAR("Extracurricular", Icons.Outlined.SportsSoccer, "Activities & Personal", NexoraPink)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TodoListScreen(
    onBackClick: () -> Unit,
    onNavigateToTodayTodo: () -> Unit,
    onNavigateToAddChapterTodo: () -> Unit,
    onNavigateToCustomTodo: () -> Unit,
    onNavigateToPersonalSchedule: () -> Unit,
    onNavigateToBreakdown: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val tasks by StudyPlannerRepository.tasks.collectAsStateWithLifecycle()
    val today = remember { StudyPlannerRepository.TODAY }
    val todayStr = remember { StudyPlannerRepository.TODAY_STR }
    val todayFormatted = remember { StudyPlannerRepository.TODAY_FORMATTED }

    var selectedCategory by remember { mutableStateOf(TodoHubCategory.TODAY) }
    var selectedSubjectFilter by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var searchInAllCategories by remember { mutableStateOf(true) }
    val searchFocusRequester = remember { FocusRequester() }

    var taskSortOrder by remember { mutableStateOf(TaskSortOrder.DATE) }
    var showSortMenu by remember { mutableStateOf(false) }
    var selectedWorkloadTab by remember { mutableStateOf(WorkloadTab.ALL) }

    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<String>() }

    var showCreateDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<StudyTask?>(null) }

    val todayTasks = remember(tasks, todayStr) { tasks.filter { it.scheduledDateStr == todayStr } }
    val upcomingTasks = remember(tasks, today) {
        tasks.filter { task ->
            if (task.scheduledDateStr.isBlank() || task.isCompleted) false
            else {
                try { LocalDate.parse(task.scheduledDateStr, StudyPlannerRepository.ISO_FORMATTER).isAfter(today) }
                catch (_: Exception) { false }
            }
        }.sortedBy { it.scheduledDateStr }
    }
    val overdueTasks = remember(tasks, today) {
        tasks.filter { task ->
            if (task.scheduledDateStr.isBlank() || task.isCompleted) false
            else {
                try { LocalDate.parse(task.scheduledDateStr, StudyPlannerRepository.ISO_FORMATTER).isBefore(today) }
                catch (_: Exception) { false }
            }
        }.sortedBy { it.scheduledDateStr }
    }
    val completedTasks = remember(tasks) { tasks.filter { it.isCompleted } }
    val chapterTasks = remember(tasks) { tasks.filter { it.subjectId.isNotBlank() || it.chapterIds.isNotEmpty() || !it.chapterId.isNullOrBlank() } }
    val customTasks = remember(tasks) { tasks.filter { it.subjectId.isBlank() && it.chapterIds.isEmpty() && it.chapterId.isNullOrBlank() } }
    val personalTasks = remember(tasks) { tasks.filter { !it.reminderTime.isNullOrBlank() || it.reminderEpochMillis != null } }
    val allTasks = tasks

    fun getCategoryCount(cat: TodoHubCategory): Int = when (cat) {
        TodoHubCategory.TODAY -> todayTasks.size
        TodoHubCategory.ALL -> allTasks.size
        TodoHubCategory.CHAPTER -> chapterTasks.size
        TodoHubCategory.CUSTOM -> customTasks.size
        TodoHubCategory.PERSONAL -> personalTasks.size
        TodoHubCategory.UPCOMING -> upcomingTasks.size
        TodoHubCategory.OVERDUE -> overdueTasks.size
        TodoHubCategory.HISTORY -> completedTasks.size
    }

    val categoryTasks = remember(selectedCategory, tasks, todayTasks, upcomingTasks, overdueTasks, completedTasks, chapterTasks, customTasks, personalTasks) {
        when (selectedCategory) {
            TodoHubCategory.TODAY -> todayTasks
            TodoHubCategory.ALL -> allTasks
            TodoHubCategory.CHAPTER -> chapterTasks
            TodoHubCategory.CUSTOM -> customTasks
            TodoHubCategory.PERSONAL -> personalTasks
            TodoHubCategory.UPCOMING -> upcomingTasks
            TodoHubCategory.OVERDUE -> overdueTasks
            TodoHubCategory.HISTORY -> completedTasks
        }
    }

    val allMatchesCount = remember(allTasks, selectedSubjectFilter, searchQuery) {
        if (searchQuery.isBlank()) 0
        else {
            val q = searchQuery.trim().lowercase()
            var list = allTasks
            if (selectedSubjectFilter != null) {
                list = if (selectedSubjectFilter == "personal") list.filter { it.subjectId.isBlank() }
                else list.filter { it.subjectId == selectedSubjectFilter }
            }
            list.count {
                it.title.lowercase().contains(q) ||
                it.notes.lowercase().contains(q) ||
                it.description.lowercase().contains(q) ||
                it.subjectName.lowercase().contains(q) ||
                (it.chapterTitle ?: "").lowercase().contains(q) ||
                it.priority.name.lowercase().contains(q)
            }
        }
    }

    val categoryMatchesCount = remember(categoryTasks, selectedSubjectFilter, searchQuery) {
        if (searchQuery.isBlank()) 0
        else {
            val q = searchQuery.trim().lowercase()
            var list = categoryTasks
            if (selectedSubjectFilter != null) {
                list = if (selectedSubjectFilter == "personal") list.filter { it.subjectId.isBlank() }
                else list.filter { it.subjectId == selectedSubjectFilter }
            }
            list.count {
                it.title.lowercase().contains(q) ||
                it.notes.lowercase().contains(q) ||
                it.description.lowercase().contains(q) ||
                it.subjectName.lowercase().contains(q) ||
                (it.chapterTitle ?: "").lowercase().contains(q) ||
                it.priority.name.lowercase().contains(q)
            }
        }
    }

    val displayTasks = remember(categoryTasks, allTasks, selectedSubjectFilter, searchQuery, searchInAllCategories, taskSortOrder, selectedWorkloadTab) {
        val baseList = if (searchQuery.isNotBlank() && searchInAllCategories) allTasks else categoryTasks
        var list = baseList
        if (selectedSubjectFilter != null) {
            list = if (selectedSubjectFilter == "personal") list.filter { it.subjectId.isBlank() }
            else list.filter { it.subjectId == selectedSubjectFilter }
        }
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase()
            list = list.filter {
                it.title.lowercase().contains(q) ||
                it.notes.lowercase().contains(q) ||
                it.description.lowercase().contains(q) ||
                it.subjectName.lowercase().contains(q) ||
                (it.chapterTitle ?: "").lowercase().contains(q) ||
                it.priority.name.lowercase().contains(q)
            }
        }
        list = when (selectedWorkloadTab) {
            WorkloadTab.ALL -> list
            WorkloadTab.ACADEMIC -> list.filter { task ->
                val isRev = task.title.lowercase().contains("revision") || task.title.lowercase().contains("revise") || task.title.lowercase().contains("practice") || task.title.lowercase().contains("test") || task.title.lowercase().contains("mock") || task.description.lowercase().contains("revision") || task.description.lowercase().contains("revise")
                !isRev && (task.subjectId.isNotBlank() || !task.chapterId.isNullOrBlank() || task.chapterIds.isNotEmpty() || task.subjectName.isNotBlank())
            }
            WorkloadTab.REVISION -> list.filter { task ->
                task.title.lowercase().contains("revision") || task.title.lowercase().contains("revise") || task.title.lowercase().contains("practice") || task.title.lowercase().contains("test") || task.title.lowercase().contains("mock") || task.description.lowercase().contains("revision") || task.description.lowercase().contains("revise")
            }
            WorkloadTab.EXTRACURRICULAR -> list.filter { task ->
                val isRev = task.title.lowercase().contains("revision") || task.title.lowercase().contains("revise") || task.title.lowercase().contains("practice") || task.title.lowercase().contains("test") || task.title.lowercase().contains("mock") || task.description.lowercase().contains("revision") || task.description.lowercase().contains("revise")
                val isAcad = !isRev && (task.subjectId.isNotBlank() || !task.chapterId.isNullOrBlank() || task.chapterIds.isNotEmpty() || task.subjectName.isNotBlank())
                !isRev && !isAcad
            }
        }
        when (taskSortOrder) {
            TaskSortOrder.DATE -> {
                list.sortedWith(
                    compareBy<StudyTask> { it.isCompleted } // Pending first
                        .thenBy { if (it.scheduledDateStr.isBlank()) "9999-99-99" else it.scheduledDateStr }
                        .thenBy { it.priority.ordinal }
                        .thenBy { it.title.lowercase() }
                )
            }
            TaskSortOrder.PRIORITY -> {
                list.sortedWith(
                    compareBy<StudyTask> { it.isCompleted } // Pending first
                        .thenBy { it.priority.ordinal } // High (0) -> Med (1) -> Low (2)
                        .thenBy { if (it.scheduledDateStr.isBlank()) "9999-99-99" else it.scheduledDateStr }
                        .thenBy { it.title.lowercase() }
                )
            }
            TaskSortOrder.ALPHABETICAL -> {
                list.sortedWith(
                    compareBy<StudyTask> { it.isCompleted } // Pending first
                        .thenBy { it.title.lowercase().trim() }
                        .thenBy { it.priority.ordinal }
                        .thenBy { if (it.scheduledDateStr.isBlank()) "9999-99-99" else it.scheduledDateStr }
                )
            }
        }
    }

    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    Scaffold(
        modifier = Modifier.testTag("todo_hub_screen"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = NexoraBackground,
        topBar = {
            TopAppBar(
                title = {
                    if (isSelectionMode) {
                        Text("${selectedIds.size} Selected", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = NexoraTextPrimary)
                    } else {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("My Tasks", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = NexoraTextPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(color = NexoraCyan.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp)) {
                                    Text("Class 12", color = NexoraCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                            Text(todayFormatted, fontSize = 11.sp, color = NexoraTextSecondary)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        keyboardController?.hide()
                        if (isSelectionMode) { isSelectionMode = false; selectedIds.clear() }
                        else onBackClick()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NexoraTextPrimary)
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        TextButton(onClick = {
                            if (selectedIds.size == displayTasks.size) selectedIds.clear()
                            else { selectedIds.clear(); selectedIds.addAll(displayTasks.map { it.taskId }) }
                        }) {
                            Text(if (selectedIds.size == displayTasks.size) "Deselect All" else "Select All", fontWeight = FontWeight.Bold, color = NexoraCyan)
                        }
                        TextButton(onClick = { isSelectionMode = false; selectedIds.clear() }) {
                            Text("Done", fontWeight = FontWeight.SemiBold, color = NexoraTextSecondary)
                        }
                    } else {
                        // Weekly Breakdown Button
                        IconButton(
                            onClick = onNavigateToBreakdown,
                            modifier = Modifier.testTag("weekly_breakdown_top_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.BarChart,
                                contentDescription = "Weekly Breakdown",
                                tint = NexoraCyan
                            )
                        }

                        // Sort Menu Button
                        Box {
                            IconButton(
                                onClick = { showSortMenu = true },
                                modifier = Modifier.testTag("sort_tasks_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Sort,
                                    contentDescription = "Sort tasks",
                                    tint = NexoraTextSecondary
                                )
                            }

                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false },
                                modifier = Modifier.background(NexoraSurface)
                            ) {
                                Text(
                                    text = "SORT TASKS BY",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NexoraCyan,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                                TaskSortOrder.values().forEach { order ->
                                    val isSelected = taskSortOrder == order
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = order.icon,
                                                    contentDescription = null,
                                                    tint = if (isSelected) NexoraCyan else NexoraTextSecondary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(
                                                        text = order.label,
                                                        fontSize = 13.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (isSelected) NexoraCyan else NexoraTextPrimary
                                                    )
                                                    Text(
                                                        text = order.description,
                                                        fontSize = 10.sp,
                                                        color = NexoraTextMuted
                                                    )
                                                }
                                            }
                                        },
                                        trailingIcon = {
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = NexoraCyan,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        },
                                        onClick = {
                                            taskSortOrder = order
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        IconButton(onClick = {
                            if (searchQuery.isNotBlank()) {
                                searchQuery = ""
                                keyboardController?.hide()
                            } else {
                                searchFocusRequester.requestFocus()
                                keyboardController?.show()
                            }
                        }) {
                            Icon(
                                imageVector = if (searchQuery.isNotBlank()) Icons.Outlined.SearchOff else Icons.Outlined.Search,
                                contentDescription = if (searchQuery.isNotBlank()) "Clear Search" else "Search",
                                tint = if (searchQuery.isNotBlank()) NexoraCyan else NexoraTextSecondary
                            )
                        }
                        IconButton(onClick = { isSelectionMode = true }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Multi-select", tint = NexoraTextSecondary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NexoraBackground)
            )
        },
        bottomBar = {
            AnimatedVisibility(
                visible = isSelectionMode && selectedIds.isNotEmpty(),
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Surface(
                    color = Color(0xFF0A0F1D).copy(alpha = 0.96f),
                    shadowElevation = 16.dp,
                    border = BorderStroke(1.dp, NexoraBorder.copy(alpha = 0.45f)),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("${selectedIds.size} tasks selected", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NexoraTextPrimary)
                            Text("Batch actions", fontSize = 11.sp, color = NexoraTextMuted)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = {
                                    StudyPlannerRepository.batchSetCompleted(selectedIds.toList(), true)
                                    scope.launch { snackbarHostState.showSnackbar("Marked ${selectedIds.size} tasks complete.") }
                                    selectedIds.clear()
                                    isSelectionMode = false
                                },
                                colors = ButtonDefaults.filledTonalButtonColors(containerColor = NexoraSuccess.copy(alpha = 0.15f), contentColor = NexoraSuccess),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Done", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            FilledTonalButton(
                                onClick = {
                                    StudyPlannerRepository.batchReschedule(selectedIds.toList(), StudyPlannerRepository.TODAY)
                                    scope.launch { snackbarHostState.showSnackbar("Rescheduled ${selectedIds.size} tasks to Today.") }
                                    selectedIds.clear()
                                    isSelectionMode = false
                                },
                                colors = ButtonDefaults.filledTonalButtonColors(containerColor = NexoraCyan.copy(alpha = 0.15f), contentColor = NexoraCyan),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Outlined.Today, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Today", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    val count = selectedIds.size
                                    StudyPlannerRepository.deleteTasks(selectedIds.toList(), context)
                                    scope.launch { snackbarHostState.showSnackbar("Deleted $count tasks.") }
                                    selectedIds.clear()
                                    isSelectionMode = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NexoraError, contentColor = Color.White),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Delete", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = NexoraElectricBlue,
                    contentColor = Color.White,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Task", modifier = Modifier.size(24.dp))
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Top Search Bar (Always visible for fast navigation)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "Search tasks, notes, subjects, chapters...",
                        fontSize = 13.sp,
                        color = NexoraTextMuted
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = if (searchQuery.isNotBlank()) NexoraCyan else NexoraTextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        if (searchQuery.isNotBlank()) {
                            Surface(
                                color = NexoraCyan.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.padding(end = 6.dp)
                            ) {
                                Text(
                                    text = "${displayTasks.size} found",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NexoraCyan,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    searchQuery = ""
                                    keyboardController?.hide()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Clear search",
                                    tint = NexoraTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .focusRequester(searchFocusRequester)
                    .testTag("todo_search_bar"),
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = NexoraSurfaceVariant.copy(alpha = 0.85f),
                    focusedContainerColor = NexoraSurface.copy(alpha = 0.98f),
                    unfocusedBorderColor = NexoraBorder.copy(alpha = 0.65f),
                    focusedBorderColor = NexoraElectricBlue,
                    unfocusedTextColor = NexoraTextPrimary,
                    focusedTextColor = NexoraTextPrimary,
                    cursorColor = NexoraElectricBlue
                )
            )

            // Search filter scope pill indicator when query is active
            AnimatedVisibility(visible = searchQuery.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { searchInAllCategories = true },
                            color = if (searchInAllCategories) NexoraCyan.copy(alpha = 0.2f) else NexoraSurfaceVariant.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, if (searchInAllCategories) NexoraCyan else NexoraBorder.copy(alpha = 0.35f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "All Tasks ($allMatchesCount)",
                                fontSize = 11.sp,
                                fontWeight = if (searchInAllCategories) FontWeight.Bold else FontWeight.Medium,
                                color = if (searchInAllCategories) NexoraCyan else NexoraTextSecondary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { searchInAllCategories = false },
                            color = if (!searchInAllCategories) selectedCategory.accentColor.copy(alpha = 0.2f) else NexoraSurfaceVariant.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, if (!searchInAllCategories) selectedCategory.accentColor else NexoraBorder.copy(alpha = 0.35f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "${selectedCategory.title} ($categoryMatchesCount)",
                                fontSize = 11.sp,
                                fontWeight = if (!searchInAllCategories) FontWeight.Bold else FontWeight.Medium,
                                color = if (!searchInAllCategories) selectedCategory.accentColor else NexoraTextSecondary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    TextButton(
                        onClick = {
                            searchQuery = ""
                            keyboardController?.hide()
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Reset", fontSize = 11.sp, color = NexoraTextMuted)
                    }
                }
            }

            // 1. Daily Progress Dashboard Card (Polished Circular Indicator)
            val todayTotalCount = todayTasks.size
            val todayCompletedCount = todayTasks.count { it.isCompleted }
            val dailyProgressFraction = if (todayTotalCount > 0) todayCompletedCount.toFloat() / todayTotalCount else 0f
            val animatedDailyProgress by animateFloatAsState(
                targetValue = dailyProgressFraction,
                animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
                label = "daily_progress_anim"
            )
            val todayPercentInt = (dailyProgressFraction * 100).toInt()

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                color = NexoraSurface.copy(alpha = 0.95f),
                border = BorderStroke(1.2.dp, NexoraBorder.copy(alpha = 0.8f)),
                shape = RoundedCornerShape(22.dp),
                shadowElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = NexoraCyan,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Daily Progress",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.5.sp,
                                color = NexoraTextPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = NexoraCyan.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "$todayPercentInt%",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = NexoraCyan,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (todayTotalCount == 0) "No tasks scheduled for today yet"
                            else "$todayCompletedCount of $todayTotalCount tasks completed",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NexoraTextSecondary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = when {
                                todayTotalCount == 0 -> "Tap '+' below to plan your study session"
                                todayCompletedCount == todayTotalCount -> "Outstanding! All daily tasks finished! 🎉"
                                todayPercentInt >= 50 -> "Great momentum! You're halfway through! 🚀"
                                todayCompletedCount > 0 -> "Good start! Keep learning and pushing forward."
                                else -> "Ready to begin? Check off your first task today!"
                            },
                            fontSize = 11.sp,
                            color = NexoraTextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(onClick = onNavigateToBreakdown),
                            color = NexoraCyan.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, NexoraCyan.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Outlined.BarChart, contentDescription = null, tint = NexoraCyan, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("Weekly Breakdown & Chart", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NexoraCyan)
                            }
                        }
                    }

                    // Polished Circular Progress Indicator Ring
                    Box(
                        modifier = Modifier.size(58.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { 1f },
                            modifier = Modifier.fillMaxSize(),
                            color = NexoraSurfaceVariant,
                            strokeWidth = 6.dp,
                            trackColor = Color.Transparent
                        )
                        CircularProgressIndicator(
                            progress = { animatedDailyProgress },
                            modifier = Modifier.fillMaxSize(),
                            color = NexoraCyan,
                            strokeWidth = 6.dp,
                            trackColor = Color.Transparent,
                            strokeCap = StrokeCap.Round
                        )
                        Text(
                            text = "$todayPercentInt%",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = NexoraTextPrimary
                        )
                    }
                }
            }

            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(TodoHubCategory.values()) { category ->
                    val isSelected = selectedCategory == category
                    val count = getCategoryCount(category)

                    Surface(
                        modifier = Modifier
                            .bounceClick { selectedCategory = category }
                            .width(138.dp)
                            .clip(RoundedCornerShape(18.dp)),
                        color = if (isSelected) category.accentColor.copy(alpha = 0.16f) else NexoraSurface.copy(alpha = 0.95f),
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 0.8.dp,
                            color = if (isSelected) category.accentColor else NexoraBorder.copy(alpha = 0.65f)
                        ),
                        shape = RoundedCornerShape(18.dp),
                        shadowElevation = if (isSelected) 3.dp else 1.dp
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(32.dp).background(if (isSelected) category.accentColor.copy(alpha = 0.22f) else NexoraSurfaceVariant, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(category.icon, contentDescription = null, tint = if (isSelected) category.accentColor else NexoraTextSecondary, modifier = Modifier.size(16.dp))
                                }
                                Surface(
                                    color = if (isSelected) category.accentColor else NexoraSurfaceVariant,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(text = "$count", fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, color = if (isSelected) Color.White else NexoraTextPrimary, modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp))
                                }
                            }
                            Text(text = category.title, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold, fontSize = 12.sp, color = if (isSelected) NexoraTextPrimary else NexoraTextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(text = category.subtitle, fontSize = 10.sp, color = NexoraTextMuted, maxLines = 1)
                        }
                    }
                }
            }

            // Workload Category Tabs using Material 3 ScrollableTabRow
            ScrollableTabRow(
                selectedTabIndex = WorkloadTab.values().indexOf(selectedWorkloadTab),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(16.dp)),
                containerColor = NexoraSurface,
                contentColor = NexoraTextPrimary,
                edgePadding = 0.dp,
                indicator = { tabPositions ->
                    if (WorkloadTab.values().indices.contains(WorkloadTab.values().indexOf(selectedWorkloadTab))) {
                        TabRowDefaults.Indicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[WorkloadTab.values().indexOf(selectedWorkloadTab)]),
                            height = 3.dp,
                            color = selectedWorkloadTab.accentColor
                        )
                    }
                },
                divider = {}
            ) {
                WorkloadTab.values().forEach { tab ->
                    val isSelected = selectedWorkloadTab == tab
                    val tabCount = when (tab) {
                        WorkloadTab.ALL -> categoryTasks.size
                        WorkloadTab.ACADEMIC -> categoryTasks.count { task ->
                            val isRev = task.title.lowercase().contains("revision") || task.title.lowercase().contains("revise") || task.title.lowercase().contains("practice") || task.title.lowercase().contains("test") || task.title.lowercase().contains("mock")
                            !isRev && (task.subjectId.isNotBlank() || !task.chapterId.isNullOrBlank() || task.chapterIds.isNotEmpty() || task.subjectName.isNotBlank())
                        }
                        WorkloadTab.REVISION -> categoryTasks.count { task ->
                            task.title.lowercase().contains("revision") || task.title.lowercase().contains("revise") || task.title.lowercase().contains("practice") || task.title.lowercase().contains("test") || task.title.lowercase().contains("mock")
                        }
                        WorkloadTab.EXTRACURRICULAR -> categoryTasks.count { task ->
                            val isRev = task.title.lowercase().contains("revision") || task.title.lowercase().contains("revise") || task.title.lowercase().contains("practice") || task.title.lowercase().contains("test") || task.title.lowercase().contains("mock")
                            val isAcad = !isRev && (task.subjectId.isNotBlank() || !task.chapterId.isNullOrBlank() || task.chapterIds.isNotEmpty() || task.subjectName.isNotBlank())
                            !isRev && !isAcad
                        }
                    }

                    Tab(
                        selected = isSelected,
                        onClick = { selectedWorkloadTab = tab },
                        modifier = Modifier
                            .padding(horizontal = 4.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) tab.accentColor else NexoraTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = tab.title,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) NexoraTextPrimary else NexoraTextSecondary
                                )
                                Surface(
                                    color = if (isSelected) tab.accentColor.copy(alpha = 0.2f) else NexoraSurfaceVariant,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "$tabCount",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) tab.accentColor else NexoraTextPrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    )
                }
            }

            if (overdueTasks.isNotEmpty() && selectedCategory != TodoHubCategory.HISTORY) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    color = NexoraError.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, NexoraError.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Outlined.WarningAmber, null, tint = NexoraError, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("${overdueTasks.size} Overdue Task${if (overdueTasks.size > 1) "s" else ""}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = NexoraError)
                                Text("Unfinished tasks from past days", fontSize = 10.sp, color = NexoraTextSecondary)
                            }
                        }
                        TextButton(onClick = {
                            StudyPlannerRepository.rescheduleAllMissedToToday()
                            scope.launch { snackbarHostState.showSnackbar("All overdue tasks rescheduled to Today.") }
                        }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                            Text("Reschedule All", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = NexoraError)
                        }
                    }
                }
            }

            // Subject Filter Row (Glass rounded chips)
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    val isAllSelected = selectedSubjectFilter == null
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { selectedSubjectFilter = null },
                        color = if (isAllSelected) NexoraCyan.copy(alpha = 0.2f) else NexoraSurfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, if (isAllSelected) NexoraCyan else NexoraBorder.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            if (isAllSelected) {
                                Box(modifier = Modifier.size(6.dp).background(NexoraCyan, CircleShape))
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                "All Subjects",
                                fontSize = 11.5.sp,
                                fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isAllSelected) NexoraCyan else NexoraTextSecondary
                            )
                        }
                    }
                }
                item {
                    val isPersonalSelected = selectedSubjectFilter == "personal"
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { selectedSubjectFilter = if (isPersonalSelected) null else "personal" },
                        color = if (isPersonalSelected) NexoraPink.copy(alpha = 0.2f) else NexoraSurfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, if (isPersonalSelected) NexoraPink else NexoraBorder.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Box(modifier = Modifier.size(6.dp).background(NexoraPink, CircleShape))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Personal",
                                fontSize = 11.5.sp,
                                fontWeight = if (isPersonalSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isPersonalSelected) NexoraPink else NexoraTextSecondary
                            )
                        }
                    }
                }
                items(Subject.OFFICIAL_SUBJECTS) { subject ->
                    val isSubjectSelected = selectedSubjectFilter == subject.id
                    val sColor = getSubjectAccentColor(subject.id, subject.name)
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { selectedSubjectFilter = if (isSubjectSelected) null else subject.id },
                        color = if (isSubjectSelected) sColor.copy(alpha = 0.2f) else NexoraSurfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, if (isSubjectSelected) sColor else NexoraBorder.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Box(modifier = Modifier.size(6.dp).background(sColor, CircleShape))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                subject.name,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSubjectSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSubjectSelected) sColor else NexoraTextSecondary
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val listHeader = if (searchQuery.isNotBlank()) {
                    "Search Results (${displayTasks.size})"
                } else {
                    "${selectedCategory.title} (${displayTasks.size})"
                }
                Text(text = listHeader, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = NexoraTextPrimary)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Interactive Sort Pill
                    Box {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showSortMenu = true }
                                .testTag("sort_pill_button"),
                            color = NexoraCyan.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, NexoraCyan.copy(alpha = 0.35f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = taskSortOrder.icon,
                                    contentDescription = null,
                                    tint = NexoraCyan,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = taskSortOrder.label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NexoraCyan
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = NexoraCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    if (searchQuery.isNotBlank()) {
                        TextButton(
                            onClick = {
                                searchQuery = ""
                                keyboardController?.hide()
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Clear", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = NexoraCyan)
                        }
                    } else if (selectedCategory == TodoHubCategory.TODAY) {
                        TextButton(onClick = onNavigateToTodayTodo, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp), tint = NexoraCyan)
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Add", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = NexoraCyan)
                        }
                    }
                }
            }

            if (displayTasks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    if (searchQuery.isNotBlank()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .background(NexoraCyan.copy(alpha = 0.12f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.SearchOff, contentDescription = null, tint = NexoraCyan, modifier = Modifier.size(30.dp))
                            }
                            Text(
                                text = "No tasks found",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = NexoraTextPrimary
                            )
                            Text(
                                text = "No tasks matching \"$searchQuery\" in ${if (searchInAllCategories) "all categories" else selectedCategory.title}. Try searching with another keyword or subject.",
                                fontSize = 12.5.sp,
                                color = NexoraTextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            FilledTonalButton(
                                onClick = {
                                    searchQuery = ""
                                    keyboardController?.hide()
                                },
                                colors = ButtonDefaults.filledTonalButtonColors(containerColor = NexoraCyan.copy(alpha = 0.15f), contentColor = NexoraCyan),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Outlined.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Clear Search", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    } else {
                        EmptyStateView(
                            title = "No ${selectedCategory.title}",
                            subtitle = when (selectedCategory) {
                                TodoHubCategory.TODAY -> "You have no tasks scheduled for today. Tap below to create one!"
                                TodoHubCategory.OVERDUE -> "Great job! You have zero overdue tasks."
                                TodoHubCategory.HISTORY -> "Completed tasks will be saved here for your study records."
                                else -> "No tasks found in this view. Tap '+' to create a new task."
                            },
                            icon = selectedCategory.icon
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(displayTasks, key = { it.taskId }) { task ->
                        val isSelected = selectedIds.contains(task.taskId)
                        TodoTaskGlassItem(
                            task = task,
                            today = today,
                            isSelectionMode = isSelectionMode,
                            isSelected = isSelected,
                            onToggleSelection = {
                                if (isSelected) selectedIds.remove(task.taskId)
                                else selectedIds.add(task.taskId)
                            },
                            onToggleComplete = { StudyPlannerRepository.toggleTaskCompletion(task.taskId) },
                            onClick = {
                                if (isSelectionMode) {
                                    if (isSelected) selectedIds.remove(task.taskId)
                                    else selectedIds.add(task.taskId)
                                } else {
                                    taskToEdit = task
                                }
                            },
                            onDelete = {
                                StudyPlannerRepository.deleteTask(task.taskId, context)
                                scope.launch { snackbarHostState.showSnackbar("Task deleted.") }
                            },
                            onRescheduleToToday = {
                                StudyPlannerRepository.rescheduleTask(task.taskId, StudyPlannerRepository.TODAY)
                                scope.launch { snackbarHostState.showSnackbar("Rescheduled to Today.") }
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateTaskUnifiedDialog(
            initialCategory = selectedCategory,
            onDismiss = { showCreateDialog = false },
            onSaved = {
                showCreateDialog = false
                scope.launch { snackbarHostState.showSnackbar("Task saved successfully!") }
            },
            onOpenTodayScreen = { showCreateDialog = false; onNavigateToTodayTodo() },
            onOpenChapterScreen = { showCreateDialog = false; onNavigateToAddChapterTodo() },
            onOpenCustomScreen = { showCreateDialog = false; onNavigateToCustomTodo() },
            onOpenPersonalScreen = { showCreateDialog = false; onNavigateToPersonalSchedule() }
        )
    }

    if (taskToEdit != null) {
        EditTaskDialog(
            task = taskToEdit!!,
            onDismiss = { taskToEdit = null },
            onSave = { updatedTitle, updatedNotes, updatedPriority, updatedDate, startTime, endTime ->
                val timeStr = if (startTime.isNotBlank() && endTime.isNotBlank()) "$startTime - $endTime" else startTime.ifBlank { null }
                StudyPlannerRepository.updateTask(
                    taskId = taskToEdit!!.taskId,
                    title = updatedTitle,
                    notes = updatedNotes,
                    priority = updatedPriority,
                    scheduledDate = updatedDate,
                    reminderTime = timeStr,
                    context = context
                )
                taskToEdit = null
                scope.launch { snackbarHostState.showSnackbar("Task updated.") }
            },
            onDelete = {
                StudyPlannerRepository.deleteTask(taskToEdit!!.taskId, context)
                taskToEdit = null
                scope.launch { snackbarHostState.showSnackbar("Task deleted.") }
            }
        )
    }
}

@Composable
fun TodoTaskGlassItem(
    task: StudyTask,
    today: LocalDate,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onToggleSelection: () -> Unit,
    onToggleComplete: () -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onRescheduleToToday: () -> Unit
) {
    val isOverdue = remember(task.scheduledDateStr, task.isCompleted, today) {
        if (task.scheduledDateStr.isBlank() || task.isCompleted) false
        else {
            try { LocalDate.parse(task.scheduledDateStr, StudyPlannerRepository.ISO_FORMATTER).isBefore(today) }
            catch (_: Exception) { false }
        }
    }

    val isDueToday = task.scheduledDateStr == StudyPlannerRepository.TODAY_STR

    val subjectColor = remember(task.subjectId, task.subjectName) {
        getSubjectAccentColor(task.subjectId, task.subjectName)
    }

    val priorityColor = remember(task.priority) {
        when (task.priority) {
            TaskPriority.HIGH -> NexoraError
            TaskPriority.MEDIUM -> NexoraCyan
            TaskPriority.LOW -> NexoraSuccess
        }
    }

    val displaySubjectName = remember(task.subjectName, task.subjectId) {
        if (task.subjectName.isNotBlank() && task.subjectName != "General") task.subjectName
        else when (task.subjectId.lowercase()) {
            "accounts" -> "Elements of Accounts"
            "stat" -> "Statistics"
            "economics" -> "Economics"
            "ba" -> "B.A."
            "sp_cc" -> "SP & CC"
            "english" -> "English"
            "gujarati" -> "Gujarati"
            else -> "Personal Task"
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag("task_item_${task.taskId}"),
        color = if (isSelected) NexoraElectricBlue.copy(alpha = 0.16f)
                else if (task.isCompleted) NexoraSurfaceVariant.copy(alpha = 0.7f)
                else NexoraSurface,
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) NexoraElectricBlue
                    else if (isOverdue) NexoraError.copy(alpha = 0.65f)
                    else if (task.isCompleted) NexoraSuccess.copy(alpha = 0.45f)
                    else NexoraBorder.copy(alpha = 0.75f)
        ),
        shape = RoundedCornerShape(20.dp),
        shadowElevation = if (isSelected) 3.dp else 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Subject vertical accent indicator line
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (task.isCompleted) subjectColor.copy(alpha = 0.35f) else subjectColor)
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Main Card Content Column
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSelectionMode) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onToggleSelection() },
                            colors = CheckboxDefaults.colors(
                                checkedColor = NexoraCyan,
                                uncheckedColor = NexoraBorder
                            ),
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Checkbox(
                            checked = task.isCompleted,
                            onCheckedChange = { onToggleComplete() },
                            colors = CheckboxDefaults.colors(
                                checkedColor = NexoraSuccess,
                                uncheckedColor = if (isOverdue) NexoraError else NexoraBorder.copy(alpha = 0.7f)
                            ),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = task.title,
                            fontWeight = if (task.isCompleted) FontWeight.Medium else FontWeight.Bold,
                            fontSize = 14.5.sp,
                            color = if (task.isCompleted) NexoraTextMuted else NexoraTextPrimary,
                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (task.notes.isNotBlank() || task.description.isNotBlank()) {
                            val noteText = task.notes.ifBlank { task.description }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = noteText,
                                fontSize = 11.5.sp,
                                color = NexoraTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (!isSelectionMode) {
                        var showDropdown by remember { mutableStateOf(false) }
                        Box {
                            IconButton(
                                onClick = { showDropdown = true },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = "Actions",
                                    tint = NexoraTextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = showDropdown,
                                onDismissRequest = { showDropdown = false },
                                modifier = Modifier.background(Color(0xFF0F172A))
                            ) {
                                if (isOverdue && !task.isCompleted) {
                                    DropdownMenuItem(
                                        text = { Text("Reschedule to Today", color = NexoraCyan) },
                                        leadingIcon = { Icon(Icons.Outlined.Today, null, tint = NexoraCyan) },
                                        onClick = { showDropdown = false; onRescheduleToToday() }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text(if (task.isCompleted) "Mark Pending" else "Mark Complete", color = NexoraTextPrimary) },
                                    leadingIcon = { Icon(Icons.Default.Check, null, tint = NexoraSuccess) },
                                    onClick = { showDropdown = false; onToggleComplete() }
                                )
                                DropdownMenuItem(
                                    text = { Text("Edit Task", color = NexoraTextPrimary) },
                                    leadingIcon = { Icon(Icons.Outlined.Edit, null, tint = NexoraCyan) },
                                    onClick = { showDropdown = false; onClick() }
                                )
                                HorizontalDivider(color = NexoraBorder.copy(alpha = 0.3f))
                                DropdownMenuItem(
                                    text = { Text("Delete", color = NexoraError) },
                                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = NexoraError) },
                                    onClick = { showDropdown = false; onDelete() }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Metadata Pill Badges Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Subject Badge with colored dot
                    Surface(
                        color = subjectColor.copy(alpha = 0.15f),
                        border = BorderStroke(0.8.dp, subjectColor.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(subjectColor, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = displaySubjectName,
                                color = subjectColor,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // 2. Chapters Badge
                    if (task.chapterIds.isNotEmpty()) {
                        Surface(
                            color = NexoraPurple.copy(alpha = 0.14f),
                            border = BorderStroke(0.8.dp, NexoraPurple.copy(alpha = 0.35f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Icon(Icons.Outlined.MenuBook, null, tint = NexoraPurple, modifier = Modifier.size(11.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (task.chapterIds.size == 1) "1 Chapter" else "${task.chapterIds.size} Chapters",
                                    color = NexoraPurple,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // 3. Date Badge
                    if (task.scheduledDateStr.isNotBlank()) {
                        val dateLabel = when {
                            isDueToday -> "Today"
                            task.scheduledDateStr == today.plusDays(1).format(StudyPlannerRepository.ISO_FORMATTER) -> "Tomorrow"
                            isOverdue -> "Overdue: ${task.scheduledDateFormatted}"
                            else -> task.scheduledDateFormatted
                        }
                        val (dateBg, dateBorder, dateColor) = when {
                            isOverdue -> Triple(NexoraError.copy(alpha = 0.14f), NexoraError.copy(alpha = 0.45f), NexoraError)
                            isDueToday -> Triple(NexoraCyan.copy(alpha = 0.15f), NexoraCyan.copy(alpha = 0.45f), NexoraCyan)
                            else -> Triple(NexoraSurfaceVariant, NexoraBorder.copy(alpha = 0.3f), NexoraTextSecondary)
                        }
                        Surface(
                            color = dateBg,
                            border = BorderStroke(0.8.dp, dateBorder),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Icon(
                                    if (isOverdue) Icons.Outlined.WarningAmber else if (isDueToday) Icons.Outlined.WbSunny else Icons.Outlined.CalendarToday,
                                    contentDescription = null,
                                    tint = dateColor,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    dateLabel,
                                    color = dateColor,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // 4. Time Badge
                    if (!task.reminderTime.isNullOrBlank()) {
                        Surface(
                            color = NexoraGold.copy(alpha = 0.14f),
                            border = BorderStroke(0.8.dp, NexoraGold.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Icon(Icons.Outlined.AccessTime, null, tint = NexoraGold, modifier = Modifier.size(11.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(task.reminderTime, color = NexoraGold, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // 5. Priority Badge
                    Surface(
                        color = priorityColor.copy(alpha = 0.14f),
                        border = BorderStroke(0.8.dp, priorityColor.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (task.priority == TaskPriority.HIGH) "URGENT" else task.priority.name,
                            color = priorityColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateTaskUnifiedDialog(
    initialCategory: TodoHubCategory,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    onOpenTodayScreen: () -> Unit,
    onOpenChapterScreen: () -> Unit,
    onOpenCustomScreen: () -> Unit,
    onOpenPersonalScreen: () -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedSubjectId by remember { mutableStateOf<String?>(null) }
    var selectedChapterIds by remember { mutableStateOf(emptySet<String>()) }
    var priority by remember { mutableStateOf(TaskPriority.MEDIUM) }
    var chapterSearch by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }

    val today = remember { StudyPlannerRepository.TODAY }
    var selectedDateOption by remember { mutableStateOf(if (initialCategory == TodoHubCategory.UPCOMING) "tomorrow" else "today") }
    var customSelectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var showDatePickerDialog by remember { mutableStateOf(false) }

    val resolvedScheduledDate: LocalDate? = when (selectedDateOption) {
        "today" -> today
        "tomorrow" -> today.plusDays(1)
        "custom" -> customSelectedDate ?: today
        else -> null
    }

    val availableChapters = remember(selectedSubjectId) {
        if (selectedSubjectId != null) SyllabusRepository.getChapters(selectedSubjectId!!) else emptyList()
    }
    val filteredChapters = remember(availableChapters, chapterSearch) {
        if (chapterSearch.isBlank()) availableChapters
        else availableChapters.filter { it.title.contains(chapterSearch, ignoreCase = true) || (it.englishTitle?.contains(chapterSearch, ignoreCase = true) == true) }
    }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { keyboardController?.hide(); onDismiss() },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 8.dp)
            .imePadding(),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .wrapContentHeight(),
            color = NexoraSurface,
            border = BorderStroke(1.dp, NexoraBorder.copy(alpha = 0.75f)),
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 20.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Create Task",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = NexoraTextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "Organize your study goals and stay on track",
                            fontSize = 11.5.sp,
                            color = NexoraTextSecondary
                        )
                    }
                    IconButton(
                        onClick = { keyboardController?.hide(); onDismiss() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close", tint = NexoraTextMuted, modifier = Modifier.size(20.dp))
                    }
                }

                // Quick Navigation Flow Badges
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onOpenTodayScreen() },
                        color = NexoraCyan.copy(alpha = 0.12f),
                        border = BorderStroke(0.8.dp, NexoraCyan.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Outlined.WbSunny, null, modifier = Modifier.size(13.dp), tint = NexoraCyan)
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Today Flow", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NexoraCyan)
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onOpenChapterScreen() },
                        color = NexoraPurple.copy(alpha = 0.12f),
                        border = BorderStroke(0.8.dp, NexoraPurple.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Outlined.MenuBook, null, modifier = Modifier.size(13.dp), tint = NexoraPurple)
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Chapter Flow", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NexoraPurple)
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onOpenPersonalScreen() },
                        color = NexoraGold.copy(alpha = 0.12f),
                        border = BorderStroke(0.8.dp, NexoraGold.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Outlined.AccessTime, null, modifier = Modifier.size(13.dp), tint = NexoraGold)
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Schedule Flow", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NexoraGold)
                        }
                    }
                }

                HorizontalDivider(color = NexoraBorder.copy(alpha = 0.3f))

                // Schedule For Date Chips
                Text("Schedule For", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = NexoraTextPrimary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "today" to "Today (${today.format(DateTimeFormatter.ofPattern("d MMM"))})",
                        "tomorrow" to "Tomorrow",
                        "custom" to (customSelectedDate?.format(DateTimeFormatter.ofPattern("d MMM")) ?: "Pick Date"),
                        "none" to "No Date"
                    ).forEach { (key, label) ->
                        val isSelected = selectedDateOption == key
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    if (key == "custom") showDatePickerDialog = true
                                    else selectedDateOption = key
                                },
                            color = if (isSelected) NexoraCyan.copy(alpha = 0.18f) else NexoraSurfaceVariant.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, if (isSelected) NexoraCyan else Color.Transparent),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) NexoraCyan else NexoraTextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                maxLines = 1
                            )
                        }
                    }
                }

                // Subject Selection Chips
                Text("Subject (or Personal)", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = NexoraTextPrimary)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val isPersonal = selectedSubjectId == null
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                selectedSubjectId = null
                                selectedChapterIds = emptySet()
                            },
                        color = if (isPersonal) NexoraPink.copy(alpha = 0.2f) else NexoraSurfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, if (isPersonal) NexoraPink else NexoraBorder.copy(alpha = 0.25f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                        ) {
                            Box(modifier = Modifier.size(6.dp).background(NexoraPink, CircleShape))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Personal",
                                fontSize = 11.5.sp,
                                fontWeight = if (isPersonal) FontWeight.Bold else FontWeight.Medium,
                                color = if (isPersonal) NexoraPink else NexoraTextSecondary
                            )
                        }
                    }

                    Subject.OFFICIAL_SUBJECTS.forEach { sub ->
                        val isSubSelected = selectedSubjectId == sub.id
                        val subColor = getSubjectAccentColor(sub.id, sub.name)
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    selectedSubjectId = sub.id
                                    selectedChapterIds = emptySet()
                                },
                            color = if (isSubSelected) subColor.copy(alpha = 0.2f) else NexoraSurfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, if (isSubSelected) subColor else NexoraBorder.copy(alpha = 0.25f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                            ) {
                                Box(modifier = Modifier.size(6.dp).background(subColor, CircleShape))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    sub.name,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSubSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSubSelected) subColor else NexoraTextSecondary
                                )
                            }
                        }
                    }
                }

                // Chapter Selector if subject is chosen
                if (selectedSubjectId != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(NexoraSurfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                            .border(BorderStroke(0.8.dp, NexoraBorder.copy(alpha = 0.35f)), RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Select Chapters", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = NexoraTextPrimary)
                            if (selectedChapterIds.isNotEmpty()) {
                                Text("${selectedChapterIds.size} selected", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NexoraCyan)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = chapterSearch,
                            onValueChange = { chapterSearch = it },
                            placeholder = { Text("Search chapters...", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = NexoraSurface.copy(alpha = 0.6f),
                                focusedContainerColor = NexoraSurfaceVariant,
                                unfocusedBorderColor = NexoraBorder.copy(alpha = 0.35f),
                                focusedBorderColor = NexoraCyan
                            )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 140.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            filteredChapters.forEach { ch ->
                                val isChecked = selectedChapterIds.contains(ch.id)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedChapterIds = if (isChecked) selectedChapterIds - ch.id else selectedChapterIds + ch.id
                                            if (title.isBlank()) title = "Study: ${ch.title}"
                                        }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = null,
                                        modifier = Modifier.size(24.dp),
                                        colors = CheckboxDefaults.colors(checkedColor = NexoraCyan)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(ch.title, fontSize = 12.sp, color = NexoraTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }

                // Task Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title *") },
                    placeholder = { Text("e.g. Chapter 3 Summary & Illustrations") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = NexoraSurfaceVariant.copy(alpha = 0.65f),
                        focusedContainerColor = NexoraSurfaceVariant,
                        unfocusedBorderColor = NexoraBorder.copy(alpha = 0.4f),
                        focusedBorderColor = NexoraCyan
                    )
                )

                // Notes / Details
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Details (Optional)") },
                    placeholder = { Text("Add specific page numbers, topics, or focus areas...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = NexoraSurfaceVariant.copy(alpha = 0.65f),
                        focusedContainerColor = NexoraSurfaceVariant,
                        unfocusedBorderColor = NexoraBorder.copy(alpha = 0.4f),
                        focusedBorderColor = NexoraCyan
                    )
                )

                // Priority Selector with Distinct Colors
                Column {
                    Text("Priority Level", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = NexoraTextPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple(TaskPriority.HIGH, "High", NexoraError),
                            Triple(TaskPriority.MEDIUM, "Medium", NexoraCyan),
                            Triple(TaskPriority.LOW, "Low", NexoraSuccess)
                        ).forEach { (p, label, color) ->
                            val isPrioSelected = priority == p
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { priority = p },
                                color = if (isPrioSelected) color.copy(alpha = 0.2f) else NexoraSurfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, if (isPrioSelected) color else NexoraBorder.copy(alpha = 0.25f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.size(6.dp).background(color, CircleShape))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        label,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isPrioSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isPrioSelected) color else NexoraTextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                // Time Slots (Start Time & End Time)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Start Time") },
                        placeholder = { Text("09:00 AM") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = NexoraSurfaceVariant.copy(alpha = 0.65f),
                            focusedContainerColor = NexoraSurfaceVariant,
                            unfocusedBorderColor = NexoraBorder.copy(alpha = 0.4f),
                            focusedBorderColor = NexoraCyan
                        )
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("End Time") },
                        placeholder = { Text("10:30 AM") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = NexoraSurfaceVariant.copy(alpha = 0.65f),
                            focusedContainerColor = NexoraSurfaceVariant,
                            unfocusedBorderColor = NexoraBorder.copy(alpha = 0.4f),
                            focusedBorderColor = NexoraCyan
                        )
                    )
                }

                if (errorMessage != null) {
                    Text(errorMessage!!, color = NexoraError, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }

                // Action Buttons: Cancel and Save Task
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { keyboardController?.hide(); onDismiss() },
                        modifier = Modifier.weight(0.8f).height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NexoraTextSecondary),
                        border = BorderStroke(1.dp, NexoraBorder.copy(alpha = 0.4f))
                    ) {
                        Text("Cancel", fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            if (title.isBlank()) {
                                errorMessage = "Please enter a task title."
                                return@Button
                            }
                            isLoading = true
                            val timeStr = if (startTime.isNotBlank() && endTime.isNotBlank()) "$startTime - $endTime" else startTime.ifBlank { null }
                            scope.launch {
                                val result = StudyPlannerRepository.addTaskAsync(
                                    title = title.trim(),
                                    description = notes.trim(),
                                    subjectId = selectedSubjectId ?: "",
                                    chapterIds = selectedChapterIds.toList(),
                                    scheduledDate = resolvedScheduledDate,
                                    priority = priority,
                                    reminderTime = timeStr,
                                    context = context
                                )
                                keyboardController?.hide()
                                if (result.isSuccess) {
                                    onSaved()
                                } else {
                                    isLoading = false
                                    errorMessage = "Failed to persist task. Please try again."
                                }
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier.weight(1.2f).height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NexoraCyan, contentColor = Color.White)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Task", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showDatePickerDialog) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        customSelectedDate = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                        selectedDateOption = "custom"
                    }
                    showDatePickerDialog = false
                }) {
                    Text("OK", fontWeight = FontWeight.Bold, color = NexoraCyan)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskDialog(
    task: StudyTask,
    onDismiss: () -> Unit,
    onSave: (title: String, notes: String, priority: TaskPriority, date: LocalDate?, startTime: String, endTime: String) -> Unit,
    onDelete: () -> Unit
) {
    var title by remember { mutableStateOf(task.title) }
    var notes by remember { mutableStateOf(task.notes.ifBlank { task.description }) }
    var priority by remember { mutableStateOf(task.priority) }

    var scheduledDate by remember {
        mutableStateOf(
            try {
                if (task.scheduledDateStr.isNotBlank()) LocalDate.parse(task.scheduledDateStr, StudyPlannerRepository.ISO_FORMATTER)
                else null
            } catch (_: Exception) { null }
        )
    }

    var startTime by remember { mutableStateOf(task.reminderTime?.split(" - ")?.getOrNull(0) ?: task.reminderTime ?: "") }
    var endTime by remember { mutableStateOf(task.reminderTime?.split(" - ")?.getOrNull(1) ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }

    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    AlertDialog(
        onDismissRequest = { keyboardController?.hide(); onDismiss() },
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .imePadding()
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F172A).copy(alpha = 0.96f),
            border = BorderStroke(1.dp, NexoraBorder.copy(alpha = 0.45f)),
            shadowElevation = 20.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Edit Task", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = NexoraTextPrimary)
                        Text("Update study target details", fontSize = 11.5.sp, color = NexoraTextSecondary)
                    }
                    IconButton(onClick = { keyboardController?.hide(); onDismiss() }) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close", tint = NexoraTextMuted)
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title *") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = NexoraSurfaceVariant.copy(alpha = 0.65f),
                        focusedContainerColor = NexoraSurfaceVariant,
                        unfocusedBorderColor = NexoraBorder.copy(alpha = 0.4f),
                        focusedBorderColor = NexoraCyan
                    )
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Details") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = NexoraSurfaceVariant.copy(alpha = 0.65f),
                        focusedContainerColor = NexoraSurfaceVariant,
                        unfocusedBorderColor = NexoraBorder.copy(alpha = 0.4f),
                        focusedBorderColor = NexoraCyan
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Scheduled Date", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = NexoraTextPrimary)
                        Text(scheduledDate?.format(DateTimeFormatter.ofPattern("d MMM yyyy")) ?: "No date assigned", fontSize = 11.sp, color = NexoraCyan)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TextButton(onClick = { scheduledDate = StudyPlannerRepository.TODAY }) { Text("Today", fontSize = 11.sp, color = NexoraCyan) }
                        TextButton(onClick = { showDatePicker = true }) { Text("Change Date", fontSize = 11.sp, color = NexoraTextPrimary) }
                    }
                }

                // Priority
                Column {
                    Text("Priority Level", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = NexoraTextPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple(TaskPriority.HIGH, "High", NexoraError),
                            Triple(TaskPriority.MEDIUM, "Medium", NexoraCyan),
                            Triple(TaskPriority.LOW, "Low", NexoraSuccess)
                        ).forEach { (p, label, color) ->
                            val isPrioSelected = priority == p
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { priority = p },
                                color = if (isPrioSelected) color.copy(alpha = 0.2f) else NexoraSurfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, if (isPrioSelected) color else NexoraBorder.copy(alpha = 0.25f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.size(6.dp).background(color, CircleShape))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        label,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isPrioSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isPrioSelected) color else NexoraTextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Start Time") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = NexoraSurfaceVariant.copy(alpha = 0.65f),
                            focusedContainerColor = NexoraSurfaceVariant,
                            unfocusedBorderColor = NexoraBorder.copy(alpha = 0.4f),
                            focusedBorderColor = NexoraCyan
                        )
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("End Time") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = NexoraSurfaceVariant.copy(alpha = 0.65f),
                            focusedContainerColor = NexoraSurfaceVariant,
                            unfocusedBorderColor = NexoraBorder.copy(alpha = 0.4f),
                            focusedBorderColor = NexoraCyan
                        )
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { keyboardController?.hide(); onDelete() },
                        modifier = Modifier.weight(0.9f).height(48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NexoraError),
                        border = BorderStroke(1.dp, NexoraError.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                keyboardController?.hide()
                                onSave(title, notes, priority, scheduledDate, startTime, endTime)
                            }
                        },
                        modifier = Modifier.weight(1.1f).height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NexoraCyan, contentColor = Color.White)
                    ) {
                        Text("Save Changes", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = scheduledDate?.atStartOfDay(ZoneId.of("UTC"))?.toInstant()?.toEpochMilli() ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        scheduledDate = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK", color = NexoraCyan, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
