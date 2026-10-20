package com.example.ui.screens.planner

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.Subject
import com.example.core.model.TaskPriority
import com.example.core.repository.StudyPlannerRepository
import com.example.core.repository.SyllabusRepository
import com.example.ui.components.NexoraCard
import com.example.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Premium Today's To-Do Task Creation Screen.
 * Fully features all 7 Gujarat Board Class 12 Commerce subjects + Personal Task,
 * dynamic chapter loading from SyllabusRepository, chapter search, multi-selection,
 * priority, start/end time presets, and automatic assignment to today's date.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TodayTodoScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedSubjectId by remember { mutableStateOf<String?>(null) } // null = Personal Task
    var selectedChapterIds by remember { mutableStateOf(emptySet<String>()) }
    var chapterSearch by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(TaskPriority.MEDIUM) }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    var durationMinutes by remember { mutableIntStateOf(45) }
    var isLoading by remember { mutableStateOf(false) }

    val todayDateFormatted = remember { StudyPlannerRepository.TODAY_FORMATTED }

    // Dynamic chapters loaded from SyllabusRepository
    val availableChapters = remember(selectedSubjectId) {
        if (selectedSubjectId != null) {
            SyllabusRepository.getChapters(selectedSubjectId!!)
        } else {
            emptyList()
        }
    }

    val filteredChapters = remember(availableChapters, chapterSearch) {
        if (chapterSearch.isBlank()) availableChapters
        else availableChapters.filter {
            it.title.contains(chapterSearch, ignoreCase = true) ||
            (it.englishTitle?.contains(chapterSearch, ignoreCase = true) == true)
        }
    }

    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = NexoraBackground,
        modifier = Modifier.testTag("today_todo_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Today's To-Do", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = NexoraTextPrimary)
                        Text(todayDateFormatted, fontSize = 12.sp, color = NexoraCyan, fontWeight = FontWeight.SemiBold)
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
            // 1. Today Badge Card (Glass aesthetic)
            NexoraCard(
                containerColor = NexoraCyan.copy(alpha = 0.08f),
                borderColor = NexoraCyan.copy(alpha = 0.35f),
                cornerRadius = 20.dp,
                contentPadding = 14.dp
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(NexoraCyan.copy(alpha = 0.18f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.WbSunny, contentDescription = null, tint = NexoraCyan, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Scheduled For Today", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = NexoraTextPrimary)
                        Text("Date automatically set to $todayDateFormatted", fontSize = 11.sp, color = NexoraTextSecondary)
                    }
                }
            }

            // 2. Subject Selection (All 7 Official + Personal Task)
            NexoraCard(
                containerColor = NexoraSurface,
                borderColor = NexoraBorder.copy(alpha = 0.75f),
                cornerRadius = 20.dp,
                contentPadding = 16.dp
            ) {
                Text(
                    text = "1. Select Subject or Personal Task",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = NexoraTextPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Personal Task option
                    val isPersonal = selectedSubjectId == null
                    Surface(
                        modifier = Modifier
                            .clickable {
                                selectedSubjectId = null
                                selectedChapterIds = emptySet()
                            }
                            .clip(RoundedCornerShape(12.dp)),
                        color = if (isPersonal) NexoraCyan.copy(alpha = 0.15f) else NexoraSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isPersonal) NexoraCyan else Color.Transparent
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isPersonal) {
                                Icon(Icons.Default.Check, null, tint = NexoraCyan, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                "Personal Task",
                                fontSize = 12.sp,
                                fontWeight = if (isPersonal) FontWeight.Bold else FontWeight.Medium,
                                color = if (isPersonal) NexoraCyan else NexoraTextSecondary
                            )
                        }
                    }

                    // 7 Official Subjects
                    Subject.OFFICIAL_SUBJECTS.forEach { subject ->
                        val isSelected = selectedSubjectId == subject.id
                        val subjectColor = getSubjectAccentColor(subject.id, subject.name)

                        Surface(
                            modifier = Modifier
                                .clickable {
                                    selectedSubjectId = subject.id
                                    selectedChapterIds = emptySet()
                                }
                                .clip(RoundedCornerShape(12.dp)),
                            color = if (isSelected) subjectColor.copy(alpha = 0.15f) else NexoraSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) subjectColor else Color.Transparent
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isSelected) {
                                    Icon(Icons.Default.Check, null, tint = subjectColor, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    subject.name,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) subjectColor else NexoraTextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // 3. Dynamic Chapter Selection (when subject is selected)
            if (selectedSubjectId != null) {
                NexoraCard(
                    containerColor = NexoraSurface,
                    borderColor = NexoraBorder.copy(alpha = 0.75f),
                    cornerRadius = 20.dp,
                    contentPadding = 16.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "2. Select Chapter(s)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = NexoraTextPrimary
                        )
                        if (selectedChapterIds.isNotEmpty()) {
                            Text(
                                "${selectedChapterIds.size} selected",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = NexoraCyan
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = chapterSearch,
                        onValueChange = { chapterSearch = it },
                        placeholder = { Text("Search chapters by name or number...", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = NexoraTextMuted, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (chapterSearch.isNotBlank()) {
                                IconButton(onClick = { chapterSearch = "" }) {
                                    Icon(Icons.Outlined.Close, null, tint = NexoraTextMuted, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = NexoraSurfaceVariant,
                            focusedContainerColor = NexoraSurfaceVariant,
                            unfocusedBorderColor = NexoraBorder.copy(alpha = 0.5f),
                            focusedBorderColor = NexoraCyan
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (filteredChapters.isEmpty()) {
                            Text(
                                "No chapters match your search.",
                                fontSize = 12.sp,
                                color = NexoraTextMuted,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            filteredChapters.forEach { chapter ->
                                val isChecked = selectedChapterIds.contains(chapter.id)
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedChapterIds = if (isChecked) {
                                                selectedChapterIds - chapter.id
                                            } else {
                                                selectedChapterIds + chapter.id
                                            }
                                            // Auto-suggest title if blank
                                            if (title.isBlank()) {
                                                title = "Study: ${chapter.title}"
                                            }
                                        }
                                        .clip(RoundedCornerShape(10.dp)),
                                    color = if (isChecked) NexoraCyan.copy(alpha = 0.08f) else NexoraSurfaceVariant.copy(alpha = 0.5f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        0.5.dp,
                                        if (isChecked) NexoraCyan.copy(alpha = 0.5f) else Color.Transparent
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = null,
                                            colors = CheckboxDefaults.colors(checkedColor = NexoraCyan)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                chapter.title,
                                                fontSize = 13.sp,
                                                fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal,
                                                color = NexoraTextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (!chapter.englishTitle.isNullOrBlank()) {
                                                Text(
                                                    chapter.englishTitle,
                                                    fontSize = 11.sp,
                                                    color = NexoraTextMuted,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Task Details (Title & Notes)
            NexoraCard(
                containerColor = NexoraSurface,
                borderColor = NexoraBorder.copy(alpha = 0.75f),
                cornerRadius = 20.dp,
                contentPadding = 16.dp
            ) {
                Text(
                    text = if (selectedSubjectId != null) "3. Task Details" else "2. Task Details",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = NexoraTextPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title *") },
                    placeholder = { Text("e.g., Solve Illustration 1-5 or Chapter 2 Revision") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = NexoraSurfaceVariant,
                        focusedContainerColor = NexoraSurfaceVariant,
                        unfocusedBorderColor = NexoraBorder.copy(alpha = 0.5f),
                        focusedBorderColor = NexoraCyan
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Optional Notes / Page Numbers") },
                    placeholder = { Text("e.g., Focus on Board Exam questions from Textbook p. 45-48") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = NexoraSurfaceVariant,
                        focusedContainerColor = NexoraSurfaceVariant,
                        unfocusedBorderColor = NexoraBorder.copy(alpha = 0.5f),
                        focusedBorderColor = NexoraCyan
                    )
                )
            }

            // 5. Priority & Times
            NexoraCard(
                containerColor = NexoraSurface,
                borderColor = NexoraBorder.copy(alpha = 0.75f),
                cornerRadius = 20.dp,
                contentPadding = 16.dp
            ) {
                Text("Priority Level", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = NexoraTextPrimary)
                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TaskPriority.values().forEach { p ->
                        val isPrioSelected = priority == p
                        val pColor = when (p) {
                            TaskPriority.HIGH -> NexoraError
                            TaskPriority.MEDIUM -> NexoraCyan
                            TaskPriority.LOW -> NexoraSuccess
                        }

                        FilterChip(
                            selected = isPrioSelected,
                            onClick = { priority = p },
                            label = { Text(p.name, fontWeight = if (isPrioSelected) FontWeight.Bold else FontWeight.Medium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = pColor.copy(alpha = 0.15f),
                                selectedLabelColor = pColor
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isPrioSelected,
                                borderColor = if (isPrioSelected) pColor else NexoraBorder
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Optional Study Time (Start & End)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = NexoraTextPrimary)
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Start Time") },
                        placeholder = { Text("09:00 AM") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = NexoraSurfaceVariant,
                            focusedContainerColor = NexoraSurfaceVariant,
                            unfocusedBorderColor = NexoraBorder.copy(alpha = 0.5f),
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
                            unfocusedContainerColor = NexoraSurfaceVariant,
                            focusedContainerColor = NexoraSurfaceVariant,
                            unfocusedBorderColor = NexoraBorder.copy(alpha = 0.5f),
                            focusedBorderColor = NexoraCyan
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                // Quick time slot pills
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("08:00 AM - 09:30 AM", "10:00 AM - 11:30 AM", "04:00 PM - 05:30 PM", "07:00 PM - 08:30 PM").forEach { slot ->
                        Surface(
                            modifier = Modifier
                                .clickable {
                                    val parts = slot.split(" - ")
                                    startTime = parts.getOrNull(0) ?: ""
                                    endTime = parts.getOrNull(1) ?: ""
                                }
                                .clip(RoundedCornerShape(8.dp)),
                            color = NexoraSurfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(slot, fontSize = 10.sp, color = NexoraTextSecondary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                }
            }

            // 6. Save Button
            Button(
                onClick = {
                    if (title.isBlank()) {
                        scope.launch { snackbarHostState.showSnackbar("Please enter a task title.") }
                        return@Button
                    }
                    if (isLoading) return@Button
                    isLoading = true

                    val formattedReminderTime = if (startTime.isNotBlank() && endTime.isNotBlank()) {
                        "$startTime - $endTime"
                    } else if (startTime.isNotBlank()) {
                        startTime
                    } else null

                    StudyPlannerRepository.addTask(
                        title = title.trim(),
                        description = notes.trim(),
                        subjectId = selectedSubjectId ?: "",
                        chapterIds = selectedChapterIds.toList(),
                        scheduledDate = StudyPlannerRepository.TODAY,
                        priority = priority,
                        reminderTime = formattedReminderTime,
                        context = context
                    )
                    keyboardController?.hide()
                    onBackClick()
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_today_task_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NexoraCyan,
                    contentColor = Color.White
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Saving Today's Task...", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                } else {
                    Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save To-Do For Today", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
