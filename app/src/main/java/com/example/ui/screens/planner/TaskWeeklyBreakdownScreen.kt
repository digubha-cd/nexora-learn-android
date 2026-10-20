package com.example.ui.screens.planner

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.model.StudyTask
import com.example.core.model.Subject
import com.example.core.model.TaskPriority
import com.example.core.repository.StudyPlannerRepository
import com.example.ui.animation.bounceClick
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Data model representing completed task statistics for a specific subject over the weekly window.
 */
data class SubjectWeeklyStat(
    val subjectId: String,
    val subjectName: String,
    val count: Int,
    val percentage: Float,
    val color: Color
)

/**
 * Data model representing daily completed task activity for a specific day in the past 7 days.
 */
data class DayActivityStat(
    val date: LocalDate,
    val dayLabel: String,
    val isToday: Boolean,
    val count: Int
)

/**
 * Detailed breakdown screen that shows completed tasks by subject over the last week using a simple bar chart.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskWeeklyBreakdownScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tasks by StudyPlannerRepository.tasks.collectAsStateWithLifecycle()
    val today = remember { StudyPlannerRepository.TODAY }
    val weekStartDate = remember(today) { today.minusDays(6) }

    val shortDateFormatter = remember { DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH) }
    val dayNameFormatter = remember { DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH) }
    val fullDateFormatter = remember { DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH) }

    val dateRangeText = remember(weekStartDate, today) {
        "${weekStartDate.format(shortDateFormatter)} – ${today.format(fullDateFormatter)}"
    }

    // Helper to resolve completion date for each task
    fun getTaskCompletionDate(task: StudyTask): LocalDate? {
        if (!task.isCompleted) return null
        if (task.completedAt != null && task.completedAt > 0L) {
            return try {
                Instant.ofEpochMilli(task.completedAt)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
            } catch (_: Exception) { null }
        }
        if (task.scheduledDateStr.isNotBlank()) {
            return try {
                LocalDate.parse(task.scheduledDateStr, StudyPlannerRepository.ISO_FORMATTER)
            } catch (_: Exception) { null }
        }
        if (task.updatedAt > 0L) {
            return try {
                Instant.ofEpochMilli(task.updatedAt)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
            } catch (_: Exception) { null }
        }
        return null
    }

    // Filter tasks completed in the last 7 days: [today - 6 days, today]
    val weeklyCompletedTasks = remember(tasks, weekStartDate, today) {
        tasks.filter { task ->
            if (!task.isCompleted) return@filter false
            val compDate = getTaskCompletionDate(task) ?: return@filter false
            !compDate.isBefore(weekStartDate) && !compDate.isAfter(today)
        }.sortedByDescending { it.completedAt ?: it.updatedAt }
    }

    val totalCompletedCount = weeklyCompletedTasks.size

    // Calculate subject-wise breakdown for all 7 official subjects + Personal/Custom
    val subjectStats = remember(weeklyCompletedTasks) {
        val subjectMap = mutableMapOf<String, Int>()

        // Initialize all 7 official subjects with 0
        Subject.OFFICIAL_SUBJECTS.forEach { subject ->
            subjectMap[subject.id] = 0
        }
        subjectMap["personal"] = 0

        weeklyCompletedTasks.forEach { task ->
            val key = if (task.subjectId.isBlank()) "personal" else task.subjectId.lowercase()
            subjectMap[key] = (subjectMap[key] ?: 0) + 1
        }

        val list = mutableListOf<SubjectWeeklyStat>()
        Subject.OFFICIAL_SUBJECTS.forEach { subject ->
            val count = subjectMap[subject.id] ?: 0
            val pct = if (totalCompletedCount > 0) (count.toFloat() / totalCompletedCount) * 100f else 0f
            list.add(
                SubjectWeeklyStat(
                    subjectId = subject.id,
                    subjectName = subject.name,
                    count = count,
                    percentage = pct,
                    color = getSubjectAccentColor(subject.id, subject.name)
                )
            )
        }

        // Add Personal / Custom tasks if any
        val personalCount = subjectMap["personal"] ?: 0
        if (personalCount > 0) {
            val pct = if (totalCompletedCount > 0) (personalCount.toFloat() / totalCompletedCount) * 100f else 0f
            list.add(
                SubjectWeeklyStat(
                    subjectId = "personal",
                    subjectName = "Personal & Custom",
                    count = personalCount,
                    percentage = pct,
                    color = NexoraPink
                )
            )
        }

        // Sort: subjects with completed tasks first, then by count descending
        list.sortedWith(compareByDescending<SubjectWeeklyStat> { it.count }.thenBy { it.subjectName })
    }

    // Daily activity stat for each of the 7 days
    val dailyStats = remember(weeklyCompletedTasks, weekStartDate, today) {
        (0..6).map { dayOffset ->
            val d = weekStartDate.plusDays(dayOffset.toLong())
            val isToday = d == today
            val count = weeklyCompletedTasks.count { task -> getTaskCompletionDate(task) == d }
            val label = if (isToday) "Today" else d.format(dayNameFormatter)
            DayActivityStat(
                date = d,
                dayLabel = label,
                isToday = isToday,
                count = count
            )
        }
    }

    val maxDailyCount = remember(dailyStats) {
        val max = dailyStats.maxOfOrNull { it.count } ?: 1
        if (max <= 0) 1 else max
    }

    val maxSubjectCount = remember(subjectStats) {
        val max = subjectStats.maxOfOrNull { it.count } ?: 1
        if (max <= 0) 1 else max
    }

    val topSubject = remember(subjectStats) {
        subjectStats.firstOrNull { it.count > 0 }
    }

    val dailyAverage = remember(totalCompletedCount) {
        String.format(Locale.ENGLISH, "%.1f", totalCompletedCount.toFloat() / 7f)
    }

    var selectedSubjectFilter by remember { mutableStateOf<String?>(null) }

    val filteredList = remember(weeklyCompletedTasks, selectedSubjectFilter) {
        if (selectedSubjectFilter == null) weeklyCompletedTasks
        else if (selectedSubjectFilter == "personal") weeklyCompletedTasks.filter { it.subjectId.isBlank() }
        else weeklyCompletedTasks.filter { it.subjectId.equals(selectedSubjectFilter, ignoreCase = true) }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("task_weekly_breakdown_screen"),
        containerColor = NexoraBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Weekly Breakdown",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = NexoraTextPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = NexoraCyan.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Last 7 Days",
                                    color = NexoraCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = dateRangeText,
                            fontSize = 11.sp,
                            color = NexoraTextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("weekly_breakdown_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = NexoraTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NexoraBackground)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. KPI Stats Summary Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Total Completed
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp)),
                        color = NexoraSurface.copy(alpha = 0.95f),
                        border = BorderStroke(1.dp, NexoraCyan.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Total Done", fontSize = 11.5.sp, color = NexoraTextSecondary)
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = NexoraCyan, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$totalCompletedCount",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = NexoraCyan
                            )
                            Text("tasks finished", fontSize = 10.sp, color = NexoraTextMuted)
                        }
                    }

                    // Top Subject
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp)),
                        color = NexoraSurface.copy(alpha = 0.95f),
                        border = BorderStroke(1.dp, (topSubject?.color ?: NexoraPurple).copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Top Subject", fontSize = 11.5.sp, color = NexoraTextSecondary)
                                Icon(Icons.Outlined.EmojiEvents, contentDescription = null, tint = topSubject?.color ?: NexoraPurple, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = topSubject?.let { if (it.count > 0) it.subjectName else "None yet" } ?: "None yet",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = NexoraTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = topSubject?.let { if (it.count > 0) "${it.count} completed" else "0 tasks" } ?: "Start a task",
                                fontSize = 10.sp,
                                color = topSubject?.color ?: NexoraTextMuted
                            )
                        }
                    }

                    // Daily Average
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp)),
                        color = NexoraSurface.copy(alpha = 0.95f),
                        border = BorderStroke(1.dp, NexoraSuccess.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Daily Avg", fontSize = 11.5.sp, color = NexoraTextSecondary)
                                Icon(Icons.Outlined.TrendingUp, contentDescription = null, tint = NexoraSuccess, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = dailyAverage,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = NexoraSuccess
                            )
                            Text("tasks / day", fontSize = 10.sp, color = NexoraTextMuted)
                        }
                    }
                }
            }

            // 2. MAIN BAR CHART: Completed Tasks by Subject
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = NexoraSurface.copy(alpha = 0.95f),
                    border = BorderStroke(1.dp, NexoraBorder.copy(alpha = 0.75f)),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.BarChart,
                                    contentDescription = null,
                                    tint = NexoraCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Completed by Subject",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NexoraTextPrimary
                                )
                            }
                            Text(
                                text = "$totalCompletedCount Total",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NexoraTextSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (totalCompletedCount == 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Outlined.Assessment,
                                        contentDescription = null,
                                        tint = NexoraTextMuted,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "No completed tasks this week yet",
                                        fontSize = 13.sp,
                                        color = NexoraTextSecondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Check off tasks in To-Do to populate your subject chart",
                                        fontSize = 11.sp,
                                        color = NexoraTextMuted
                                    )
                                }
                            }
                        } else {
                            // Subject Horizontal Bar Rows
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                subjectStats.forEach { stat ->
                                    val isFilterActive = selectedSubjectFilter == stat.subjectId
                                    val progressFraction = if (maxSubjectCount > 0) stat.count.toFloat() / maxSubjectCount else 0f
                                    val animatedProgress by animateFloatAsState(
                                        targetValue = progressFraction,
                                        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                                        label = "bar_${stat.subjectId}"
                                    )

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                selectedSubjectFilter = if (isFilterActive) null else stat.subjectId
                                            }
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .background(stat.color, CircleShape)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = stat.subjectName,
                                                    fontSize = 12.5.sp,
                                                    fontWeight = if (stat.count > 0) FontWeight.SemiBold else FontWeight.Normal,
                                                    color = if (stat.count > 0) NexoraTextPrimary else NexoraTextMuted
                                                )
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "${stat.count} task${if (stat.count == 1) "" else "s"}",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (stat.count > 0) stat.color else NexoraTextMuted
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    color = stat.color.copy(alpha = 0.14f),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = "${stat.percentage.toInt()}%",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = stat.color,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        // Bar progress track
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(NexoraSurfaceVariant.copy(alpha = 0.45f))
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(animatedProgress)
                                                    .fillMaxHeight()
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(stat.color)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. SECONDARY BAR CHART: Daily Completion Activity (Mon - Sun / Last 7 Days)
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = NexoraSurface.copy(alpha = 0.95f),
                    border = BorderStroke(1.dp, NexoraBorder.copy(alpha = 0.75f)),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.CalendarMonth,
                                    contentDescription = null,
                                    tint = NexoraPurple,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Daily Completion Activity",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NexoraTextPrimary
                                )
                            }
                            Text(
                                text = "7-Day Trend",
                                fontSize = 11.5.sp,
                                color = NexoraTextMuted
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Vertical Bars Row for 7 Days
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            dailyStats.forEach { dayStat ->
                                val heightFraction = if (maxDailyCount > 0) dayStat.count.toFloat() / maxDailyCount else 0f
                                val animatedHeight by animateFloatAsState(
                                    targetValue = heightFraction.coerceIn(0.08f, 1f),
                                    animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
                                    label = "day_bar_${dayStat.date}"
                                )

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Count label above bar
                                    Text(
                                        text = "${dayStat.count}",
                                        fontSize = 11.sp,
                                        fontWeight = if (dayStat.count > 0) FontWeight.Bold else FontWeight.Normal,
                                        color = if (dayStat.isToday) NexoraCyan else if (dayStat.count > 0) NexoraTextPrimary else NexoraTextMuted
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Vertical Bar
                                    Box(
                                        modifier = Modifier
                                            .width(20.dp)
                                            .fillMaxHeight(animatedHeight)
                                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                                            .background(
                                                if (dayStat.isToday) NexoraCyan
                                                else if (dayStat.count > 0) NexoraPurple.copy(alpha = 0.85f)
                                                else NexoraSurfaceVariant.copy(alpha = 0.45f)
                                            )
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Day of week label
                                    Text(
                                        text = dayStat.dayLabel,
                                        fontSize = 10.5.sp,
                                        fontWeight = if (dayStat.isToday) FontWeight.Bold else FontWeight.Medium,
                                        color = if (dayStat.isToday) NexoraCyan else NexoraTextSecondary
                                    )
                                    Text(
                                        text = "${dayStat.date.dayOfMonth}",
                                        fontSize = 9.5.sp,
                                        color = NexoraTextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. SUBJECT FILTER ROW
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Completed Tasks List",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = NexoraTextPrimary
                        )
                        if (selectedSubjectFilter != null) {
                            TextButton(
                                onClick = { selectedSubjectFilter = null },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("Show All", fontSize = 11.5.sp, color = NexoraCyan)
                            }
                        }
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 2.dp)
                    ) {
                        item {
                            val isAll = selectedSubjectFilter == null
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { selectedSubjectFilter = null },
                                color = if (isAll) NexoraCyan.copy(alpha = 0.2f) else NexoraSurfaceVariant.copy(alpha = 0.45f),
                                border = BorderStroke(1.dp, if (isAll) NexoraCyan else NexoraBorder.copy(alpha = 0.35f)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    text = "All ($totalCompletedCount)",
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isAll) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isAll) NexoraCyan else NexoraTextSecondary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

                        items(subjectStats.filter { it.count > 0 }) { stat ->
                            val isSelected = selectedSubjectFilter == stat.subjectId
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { selectedSubjectFilter = if (isSelected) null else stat.subjectId },
                                color = if (isSelected) stat.color.copy(alpha = 0.22f) else NexoraSurfaceVariant.copy(alpha = 0.45f),
                                border = BorderStroke(1.dp, if (isSelected) stat.color else NexoraBorder.copy(alpha = 0.35f)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Box(modifier = Modifier.size(6.dp).background(stat.color, CircleShape))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${stat.subjectName} (${stat.count})",
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) stat.color else NexoraTextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. LIST OF COMPLETED TASKS
            if (filteredList.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        color = NexoraSurfaceVariant.copy(alpha = 0.7f),
                        border = BorderStroke(1.dp, NexoraBorder.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Box(modifier = Modifier.padding(28.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = NexoraTextMuted, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (totalCompletedCount == 0) "No tasks completed in the last 7 days" else "No completed tasks for this subject",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = NexoraTextSecondary
                                )
                            }
                        }
                    }
                }
            } else {
                items(filteredList, key = { it.taskId }) { task ->
                    val subjectColor = remember(task.subjectId, task.subjectName) {
                        getSubjectAccentColor(task.subjectId, task.subjectName)
                    }

                    val completionDate = remember(task) { getTaskCompletionDate(task) }
                    val dateFormatted = remember(completionDate) {
                        completionDate?.format(shortDateFormatter) ?: task.scheduledDateStr
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp)),
                        color = NexoraSurface.copy(alpha = 0.95f),
                        border = BorderStroke(1.dp, NexoraBorder.copy(alpha = 0.75f)),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Completed icon indicator
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(NexoraSuccess.copy(alpha = 0.16f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Completed",
                                    tint = NexoraSuccess,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = task.title,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = NexoraTextPrimary,
                                    textDecoration = TextDecoration.LineThrough,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(3.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Subject Chip
                                    Surface(
                                        color = subjectColor.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = task.subjectName.ifBlank { "Personal" },
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = subjectColor,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    // Completion Date
                                    Text(
                                        text = "Done on $dateFormatted",
                                        fontSize = 11.sp,
                                        color = NexoraTextMuted
                                    )
                                }
                            }

                            // Priority badge
                            val priorityColor = when (task.priority) {
                                TaskPriority.HIGH -> NexoraError
                                TaskPriority.MEDIUM -> NexoraCyan
                                TaskPriority.LOW -> NexoraSuccess
                            }
                            Surface(
                                color = priorityColor.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = task.priority.name,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = priorityColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}
