package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.model.Subject
import com.example.core.repository.SyllabusRepository
import com.example.core.storage.StudentPreferences
import com.example.ui.theme.*
import com.example.ui.animation.bounceClick

/**
 * Modes for the Progress Tracking Chart.
 */
enum class ProgressChartMode {
    SUBJECT_BARS,
    TIMELINE_TREND
}

/**
 * Visual Progress Tracking Card on the main dashboard with a premium rich aesthetic.
 */
@Composable
fun SyllabusProgressTrackerCard(
    onNavigateToSubjects: () -> Unit,
    onSubjectClick: (Subject) -> Unit,
    modifier: Modifier = Modifier
) {
    val completedChapterIds by SyllabusRepository.completedChapterIds.collectAsStateWithLifecycle()
    var selectedMode by remember { mutableStateOf(ProgressChartMode.SUBJECT_BARS) }
    var selectedSubjectId by remember { mutableStateOf<String?>(null) }

    val totalChapters = remember(completedChapterIds) { SyllabusRepository.getTotalChaptersCount() }
    val totalCompleted = remember(completedChapterIds) { SyllabusRepository.getTotalCompletedChaptersCount(completedChapterIds) }
    val overallProgress = remember(completedChapterIds) { SyllabusRepository.getOverallProgress(completedChapterIds) }
    val overallPercentInt = (overallProgress * 100).toInt()

    // 7 Official Subjects with their color mappings
    val subjectsData = remember(completedChapterIds) {
        Subject.OFFICIAL_SUBJECTS.map { subject ->
            val chapters = SyllabusRepository.getChapters(subject.id)
            val completed = chapters.count { completedChapterIds.contains(it.id) }
            val progress = if (chapters.isNotEmpty()) completed.toFloat() / chapters.size else 0f
            val color = getSubjectColor(subject.id)
            SubjectProgressItem(
                subject = subject,
                totalChapters = chapters.size,
                completedChapters = completed,
                progress = progress,
                color = color
            )
        }
    }

    NexoraCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("syllabus_progress_tracker_card"),
        borderColor = CategorySubjects.copy(alpha = 0.15f),
        cornerRadius = 32.dp,
        contentPadding = 24.dp,
        elevation = 2.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header: Title, Total Progress Badge & 90+ Target
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(CategorySubjects.copy(alpha = 0.08f), CircleShape)
                            .border(1.5.dp, CategorySubjects.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.TrendingUp,
                            contentDescription = "Progress Tracker",
                            tint = CategorySubjects,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Course Syllabus",
                                color = NexoraTextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.testTag("progress_tracker_title")
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            // 90+ Target Pill
                            Box(
                                modifier = Modifier
                                    .background(NexoraBurntAmber.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "90+ PACE",
                                    color = NexoraBurntAmber,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                        Text(
                            text = "$totalCompleted / $totalChapters Chapters Mastered",
                            color = NexoraTextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Overall Percentage Ring / Pill
                Box(
                    modifier = Modifier
                        .background(NexoraSurfaceVariant, RoundedCornerShape(14.dp))
                        .border(1.5.dp, CategorySubjects.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .testTag("overall_progress_badge")
                ) {
                    Text(
                        text = "$overallPercentInt%",
                        color = CategorySubjects,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            val context = LocalContext.current
            var showGoalsDialog by remember { mutableStateOf(false) }

            val subjectGoals = remember(completedChapterIds, showGoalsDialog) {
                Subject.OFFICIAL_SUBJECTS.associate { s ->
                    s.id to StudentPreferences.getSubjectGoal(context, s.id, 90)
                }
            }
            val avgGoal = if (subjectGoals.isNotEmpty()) subjectGoals.values.average().toInt() else 90
            val combinedActual = overallPercentInt
            val combinedGoal = avgGoal

            // Combined Study Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(NexoraSurfaceVariant.copy(alpha = 0.5f))
                    .border(1.dp, NexoraDeepTeal.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    .bounceClick { showGoalsDialog = true }
                    .padding(16.dp)
                    .testTag("combined_study_progress_bar"),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Combined Mastery",
                                color = NexoraTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .background(NexoraDeepTeal.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Target: $combinedGoal%",
                                    color = NexoraDeepTeal,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "$combinedActual% / $combinedGoal%",
                                color = if (combinedActual >= combinedGoal) NexoraSuccess else NexoraTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Edit Goals",
                                tint = NexoraDeepTeal,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    LinearProgressIndicator(
                        progress = { (combinedActual.toFloat() / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(CircleShape),
                        color = if (combinedActual >= combinedGoal) NexoraSuccess else NexoraDeepTeal,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Subject Percentage Goals Dialog
            if (showGoalsDialog) {
                AlertDialog(
                    onDismissRequest = { showGoalsDialog = false },
                    title = {
                        Text(text = "Assign Subject Goals", color = NexoraTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 400.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Set your target completion percentage for each subject to maintain a 90+ pace.",
                                color = NexoraTextSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Subject.OFFICIAL_SUBJECTS.forEach { subject ->
                                val currentGoal = StudentPreferences.getSubjectGoal(context, subject.id, 90)
                                var goalState by remember { mutableStateOf(currentGoal.toFloat()) }

                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = subject.name,
                                            color = NexoraTextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                        Text(
                                            text = "${goalState.toInt()}%",
                                            color = getSubjectColor(subject.id),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                    Slider(
                                        value = goalState,
                                        onValueChange = { newVal ->
                                            goalState = newVal
                                            StudentPreferences.setSubjectGoal(context, subject.id, newVal.toInt())
                                        },
                                        valueRange = 50f..100f,
                                        steps = 9,
                                        colors = SliderDefaults.colors(
                                            thumbColor = getSubjectColor(subject.id),
                                            activeTrackColor = getSubjectColor(subject.id),
                                            inactiveTrackColor = NexoraSurfaceVariant
                                        )
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showGoalsDialog = false }) {
                            Text(text = "Done", color = NexoraDeepTeal, fontWeight = FontWeight.Black)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(28.dp)
                )
            }

            // Chart Mode Switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NexoraSurfaceVariant, RoundedCornerShape(14.dp))
                    .border(1.dp, NexoraBorder, RoundedCornerShape(14.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Tab 1: Subject Bars
                val barsSelected = selectedMode == ProgressChartMode.SUBJECT_BARS
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (barsSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
                        .border(if (barsSelected) 1.dp else 0.dp, if (barsSelected) NexoraBorder else Color.Transparent, RoundedCornerShape(10.dp))
                        .clickable { selectedMode = ProgressChartMode.SUBJECT_BARS }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.BarChart,
                            contentDescription = null,
                            tint = if (barsSelected) CategorySubjects else NexoraTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Subjects",
                            color = if (barsSelected) NexoraTextPrimary else NexoraTextMuted,
                            fontSize = 12.sp,
                            fontWeight = if (barsSelected) FontWeight.Black else FontWeight.Bold
                        )
                    }
                }

                // Tab 2: Timeline Trend
                val trendSelected = selectedMode == ProgressChartMode.TIMELINE_TREND
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (trendSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
                        .border(if (trendSelected) 1.dp else 0.dp, if (trendSelected) NexoraBorder else Color.Transparent, RoundedCornerShape(10.dp))
                        .clickable { selectedMode = ProgressChartMode.TIMELINE_TREND }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.ShowChart,
                            contentDescription = null,
                            tint = if (trendSelected) NexoraDeepTeal else NexoraTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Trajectory",
                            color = if (trendSelected) NexoraTextPrimary else NexoraTextMuted,
                            fontSize = 12.sp,
                            fontWeight = if (trendSelected) FontWeight.Black else FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Animated Chart View
            AnimatedContent(
                targetState = selectedMode,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(180)) },
                label = "progress_chart_content"
            ) { mode ->
                when (mode) {
                    ProgressChartMode.SUBJECT_BARS -> {
                        SubjectBarsChartView(
                            subjects = subjectsData,
                            selectedSubjectId = selectedSubjectId,
                            onSelectSubject = { id ->
                                selectedSubjectId = if (selectedSubjectId == id) null else id
                            },
                            onSubjectClick = onSubjectClick
                        )
                    }
                    ProgressChartMode.TIMELINE_TREND -> {
                        TimelineTrendChartView(
                            overallProgress = overallProgress,
                            subjects = subjectsData
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "GSEB Official Curriculum",
                    color = NexoraTextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = onNavigateToSubjects,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "View Full Syllabus",
                        color = CategorySubjects,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = CategorySubjects,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

/**
 * Subject Bars Chart View
 */
@Composable
private fun SubjectBarsChartView(
    subjects: List<SubjectProgressItem>,
    selectedSubjectId: String?,
    onSelectSubject: (String) -> Unit,
    onSubjectClick: (Subject) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(NexoraSurfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            .border(1.dp, NexoraBorder, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        subjects.forEach { item ->
            val isSelected = selectedSubjectId == item.subject.id
            val animatedProgress by animateFloatAsState(
                targetValue = item.progress,
                animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                label = "bar_anim_${item.subject.id}"
            )
            val percent = (item.progress * 100).toInt()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .bounceClick { onSelectSubject(item.subject.id) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(item.color, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = item.subject.name,
                            color = NexoraTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = "$percent%",
                        color = item.color,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        .border(0.5.dp, NexoraBorder, CircleShape)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress.coerceIn(0f, 1f))
                            .height(8.dp)
                            .background(
                                Brush.horizontalGradient(listOf(item.color.copy(alpha = 0.7f), item.color)),
                                CircleShape
                            )
                    )
                }
            }
        }
    }
}

/**
 * Timeline Trend Chart View
 */
@Composable
private fun TimelineTrendChartView(
    overallProgress: Float,
    subjects: List<SubjectProgressItem>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(NexoraSurfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            .border(1.dp, NexoraBorder, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Growth Trajectory",
                    color = NexoraTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Weekly completion pace",
                    color = NexoraTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).background(NexoraBurntAmber, CircleShape))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "90% Goal",
                    color = NexoraTextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val dataPoints = remember(overallProgress) {
            listOf(0.05f, (overallProgress * 0.3f), (overallProgress * 0.6f), (overallProgress * 0.85f), overallProgress)
        }

        val borderColor = MaterialTheme.colorScheme.outline
        val surfaceColor = MaterialTheme.colorScheme.surface

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            val width = size.width
            val height = size.height
            val chartHeight = height - 20.dp.toPx()

            val y90 = chartHeight * 0.1f
            val y0 = chartHeight

            // Grid
            drawLine(color = borderColor, start = Offset(0f, y0), end = Offset(width, y0), strokeWidth = 1.dp.toPx())
            
            // 90% Goal Line
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            drawLine(
                color = NexoraBurntAmber.copy(alpha = 0.5f),
                start = Offset(0f, y90),
                end = Offset(width, y90),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = dashEffect
            )

            // Curve
            val stepX = width / (dataPoints.size - 1)
            val points = dataPoints.mapIndexed { index, value ->
                Offset(index * stepX, chartHeight * (1f - value.coerceIn(0f, 1f)))
            }

            val path = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 0 until points.size - 1) {
                    val p1 = points[i]
                    val p2 = points[i + 1]
                    val control1 = Offset((p1.x + p2.x) / 2, p1.y)
                    val control2 = Offset((p1.x + p2.x) / 2, p2.y)
                    cubicTo(control1.x, control1.y, control2.x, control2.y, p2.x, p2.y)
                }
            }

            drawPath(
                path = path,
                color = NexoraDeepTeal,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )

            points.forEachIndexed { index, pt ->
                drawCircle(color = NexoraDeepTeal, radius = 5.dp.toPx(), center = pt)
                drawCircle(color = surfaceColor, radius = 2.5.dp.toPx(), center = pt)
            }
        }
    }
}

/**
 * Subject Progress Data Model
 */
data class SubjectProgressItem(
    val subject: Subject,
    val totalChapters: Int,
    val completedChapters: Int,
    val progress: Float,
    val color: Color
)

/**
 * Helper to get subject color
 */
fun getSubjectColor(subjectId: String): Color {
    return when (subjectId) {
        "gujarati" -> SubjectGujaratiColor
        "english" -> SubjectEnglishColor
        "sp_cc" -> SubjectSpCcColor
        "ba" -> SubjectBaColor
        "stat" -> SubjectStatsColor
        "accounts" -> SubjectAccountsColor
        "economics" -> SubjectEconColor
        else -> NexoraDeepTeal
    }
}
