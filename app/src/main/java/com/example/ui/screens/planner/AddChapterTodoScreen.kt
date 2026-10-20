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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Close
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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddChapterTodoScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedSubjectId by remember { mutableStateOf(Subject.OFFICIAL_SUBJECTS.first().id) }
    var selectedChapterIds by remember { mutableStateOf(emptySet<String>()) }
    var chapterSearch by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(TaskPriority.MEDIUM) }

    val today = remember { StudyPlannerRepository.TODAY }
    var selectedDateOption by remember { mutableStateOf("today") }
    var customDate by remember { mutableStateOf<LocalDate?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    val resolvedDate: LocalDate? = when (selectedDateOption) {
        "today" -> today
        "tomorrow" -> today.plusDays(1)
        "custom" -> customDate ?: today
        else -> null
    }

    val availableChapters = remember(selectedSubjectId) { SyllabusRepository.getChapters(selectedSubjectId) }
    val filteredChapters = remember(availableChapters, chapterSearch) {
        if (chapterSearch.isBlank()) availableChapters
        else availableChapters.filter { it.title.contains(chapterSearch, ignoreCase = true) || (it.englishTitle?.contains(chapterSearch, ignoreCase = true) == true) }
    }

    var isLoading by remember { mutableStateOf(false) }
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = NexoraBackground,
        modifier = Modifier.testTag("add_chapter_todo_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Add Chapter Study Task", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = NexoraTextPrimary)
                        Text("Syllabus & Textbook Chapters", fontSize = 11.sp, color = NexoraTextSecondary)
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
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            NexoraCard(
                containerColor = Color(0xFF0F172A).copy(alpha = 0.75f),
                borderColor = NexoraBorder.copy(alpha = 0.45f),
                cornerRadius = 20.dp,
                contentPadding = 16.dp
            ) {
                Text("1. Select Subject", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = NexoraTextPrimary)
                Spacer(modifier = Modifier.height(10.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Subject.OFFICIAL_SUBJECTS.forEach { subject ->
                        val isSelected = selectedSubjectId == subject.id
                        val sColor = getSubjectAccentColor(subject.id, subject.name)

                        Surface(
                            modifier = Modifier.clickable { selectedSubjectId = subject.id; selectedChapterIds = emptySet() }.clip(RoundedCornerShape(12.dp)),
                            color = if (isSelected) sColor.copy(alpha = 0.15f) else NexoraSurfaceVariant,
                            border = BorderStroke(1.dp, if (isSelected) sColor else Color.Transparent)
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (isSelected) {
                                    Icon(Icons.Default.Check, null, tint = sColor, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(subject.name, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, color = if (isSelected) sColor else NexoraTextSecondary)
                            }
                        }
                    }
                }
            }

            NexoraCard(
                containerColor = Color(0xFF0F172A).copy(alpha = 0.75f),
                borderColor = NexoraBorder.copy(alpha = 0.45f),
                cornerRadius = 20.dp,
                contentPadding = 16.dp
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("2. Select Chapter(s)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = NexoraTextPrimary)
                    if (selectedChapterIds.isNotEmpty()) {
                        Text("${selectedChapterIds.size} Selected", color = NexoraCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = chapterSearch,
                    onValueChange = { chapterSearch = it },
                    placeholder = { Text("Search chapters in syllabus...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = NexoraTextMuted, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (chapterSearch.isNotBlank()) {
                            IconButton(onClick = { chapterSearch = "" }) { Icon(Icons.Outlined.Close, null, tint = NexoraTextMuted, modifier = Modifier.size(16.dp)) }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = NexoraSurfaceVariant,
                        focusedContainerColor = NexoraSurfaceVariant,
                        focusedBorderColor = NexoraCyan
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (filteredChapters.isEmpty()) {
                        Text("No chapters found.", fontSize = 12.sp, color = NexoraTextMuted, modifier = Modifier.padding(12.dp))
                    } else {
                        filteredChapters.forEach { chapter ->
                            val isChecked = selectedChapterIds.contains(chapter.id)
                            Surface(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    selectedChapterIds = if (isChecked) selectedChapterIds - chapter.id else selectedChapterIds + chapter.id
                                    if (title.isBlank()) title = "Study: ${chapter.title}"
                                }.clip(RoundedCornerShape(10.dp)),
                                color = if (isChecked) NexoraCyan.copy(alpha = 0.08f) else NexoraSurfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(0.5.dp, if (isChecked) NexoraCyan else Color.Transparent)
                            ) {
                                Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = isChecked, onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = NexoraCyan))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(chapter.title, fontSize = 13.sp, fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal, color = NexoraTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        if (!chapter.englishTitle.isNullOrBlank()) {
                                            Text(chapter.englishTitle, fontSize = 11.sp, color = NexoraTextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            NexoraCard(
                containerColor = Color(0xFF0F172A).copy(alpha = 0.75f),
                borderColor = NexoraBorder.copy(alpha = 0.45f),
                cornerRadius = 20.dp,
                contentPadding = 16.dp
            ) {
                Text("3. Task Details & Schedule", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = NexoraTextPrimary)
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title *") },
                    placeholder = { Text("e.g. Accounts Ch 1 - Complete Examples") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = NexoraSurfaceVariant)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Revision Goals") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = NexoraSurfaceVariant)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Schedule Date", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = NexoraTextSecondary)
                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("today" to "Today", "tomorrow" to "Tomorrow", "custom" to (customDate?.format(DateTimeFormatter.ofPattern("d MMM")) ?: "Pick Date")).forEach { (opt, label) ->
                        val isOptSelected = selectedDateOption == opt
                        Surface(
                            modifier = Modifier.weight(1f).clickable { if (opt == "custom") showDatePicker = true else selectedDateOption = opt }.clip(RoundedCornerShape(10.dp)),
                            color = if (isOptSelected) NexoraCyan.copy(alpha = 0.15f) else NexoraSurfaceVariant,
                            border = BorderStroke(1.dp, if (isOptSelected) NexoraCyan else Color.Transparent)
                        ) {
                            Text(label, fontSize = 11.sp, fontWeight = if (isOptSelected) FontWeight.Bold else FontWeight.Medium, color = if (isOptSelected) NexoraCyan else NexoraTextSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(vertical = 8.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Priority", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = NexoraTextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TaskPriority.values().forEach { p ->
                        FilterChip(selected = priority == p, onClick = { priority = p }, label = { Text(p.name, fontSize = 11.sp) })
                    }
                }
            }

            Button(
                onClick = {
                    val finalTitle = title.ifBlank {
                        val subName = Subject.OFFICIAL_SUBJECTS.firstOrNull { it.id == selectedSubjectId }?.name ?: "Subject"
                        "Study $subName Chapter(s)"
                    }
                    if (isLoading) return@Button
                    isLoading = true

                    scope.launch {
                        val result = StudyPlannerRepository.addTaskAsync(
                            title = finalTitle.trim(),
                            description = notes.trim(),
                            subjectId = selectedSubjectId,
                            chapterIds = selectedChapterIds.toList(),
                            scheduledDate = resolvedDate,
                            priority = priority,
                            context = context
                        )
                        keyboardController?.hide()
                        if (result.isSuccess) {
                            onBackClick()
                        } else {
                            isLoading = false
                            snackbarHostState.showSnackbar("Failed to save task. Please try again.")
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NexoraCyan)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                } else {
                    Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Chapter Task", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        customDate = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                        selectedDateOption = "custom"
                    }
                    showDatePicker = false
                }) { Text("OK", color = NexoraCyan) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
