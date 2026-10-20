package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.timer.StudyTimerManager
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material.icons.outlined.HourglassBottom
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.Subject
import com.example.core.timer.StudyTimerPreset
import com.example.core.timer.StudyTimerState
import com.example.ui.theme.NexoraBorder
import com.example.ui.theme.NexoraCyan
import com.example.ui.theme.NexoraElectricBlue
import com.example.ui.theme.NexoraGold
import com.example.ui.theme.NexoraMagenta
import com.example.ui.theme.NexoraPink
import com.example.ui.theme.NexoraPurple
import com.example.ui.theme.NexoraSuccess
import com.example.ui.theme.NexoraSurface
import com.example.ui.theme.NexoraSurfaceElevated
import com.example.ui.theme.NexoraSurfaceVariant
import com.example.ui.theme.NexoraTextMuted
import com.example.ui.theme.NexoraTextPrimary
import com.example.ui.theme.NexoraTextSecondary

private fun getTimerSubjectColor(subjectId: String): Color {
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
 * Persistent Study Timer Component.
 * Allows students to track focus sessions for specific subjects,
 * displaying time elapsed and remaining in an interactive circular progress bar.
 */
@Composable
fun PersistentStudyTimerCard(
    timerState: StudyTimerState,
    onStartTimer: () -> Unit,
    onPauseTimer: () -> Unit,
    onResumeTimer: () -> Unit,
    onResetTimer: () -> Unit,
    onCompleteEarly: () -> Unit,
    onSelectSubject: (Subject) -> Unit,
    modifier: Modifier = Modifier
) {
    val subjectColor: Color = getTimerSubjectColor(timerState.subjectId)

    var isFocusModeEnabled by remember { mutableStateOf(false) }

    if (isFocusModeEnabled) {
        FocusModeOverlay(
            timerState = timerState,
            subjectColor = subjectColor,
            onStartTimer = onStartTimer,
            onPauseTimer = onPauseTimer,
            onResumeTimer = onResumeTimer,
            onResetTimer = onResetTimer,
            onCompleteEarly = onCompleteEarly,
            onExitFocusMode = { isFocusModeEnabled = false }
        )
        return
    }

    // Animated Sweep Angle for Circular Progress
    // Since there's no fixed limit, we show a constant progress or hide it.
    // We'll use a pulsing effect instead of a filling bar.
    val animatedProgress = 1.0f // Always full track

    val infiniteTransition = rememberInfiniteTransition(label = "timer_pulse")
    val pulsingAlpha by infiniteTransition.animateFloat(
        initialValue = if (timerState.isRunning) 0.4f else 1.0f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    NexoraCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("persistent_study_timer_card"),
        containerColor = Color.White,
        borderColor = NexoraCyan.copy(alpha = 0.2f),
        cornerRadius = 18.dp,
        contentPadding = 18.dp,
        elevation = 0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Header: Icon + Title + Completed Sessions Metric
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(subjectColor.copy(alpha = 0.12f), CircleShape)
                            .border(1.dp, subjectColor.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Timer,
                            contentDescription = "Study Timer",
                            tint = subjectColor,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Focus Study Timer",
                            color = NexoraTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("timer_card_title")
                        )
                        Text(
                            text = "Track subject focus & master chapters",
                            color = NexoraTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Focus Mode & Stats badges
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(subjectColor.copy(alpha = 0.15f))
                            .border(1.dp, subjectColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .clickable { isFocusModeEnabled = true }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                            .testTag("enable_focus_mode_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Fullscreen,
                                contentDescription = "Focus Mode",
                                tint = subjectColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Focus",
                                color = subjectColor,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .background(NexoraSurfaceVariant, RoundedCornerShape(10.dp))
                            .border(1.dp, NexoraBorder, RoundedCornerShape(10.dp))
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                            .testTag("timer_today_stats_badge")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "⚡ ${timerState.completedSessionsCount} Done",
                                color = NexoraTextPrimary,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Subject Picker Pills (Disabled while timer is running)
            Text(
                text = "SELECT SUBJECT",
                color = NexoraTextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 2.dp, bottom = 6.dp)
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(Subject.OFFICIAL_SUBJECTS) { subject ->
                    val isSelected = (subject.id == timerState.subjectId)
                    val sColor = getTimerSubjectColor(subject.id)

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) sColor.copy(alpha = 0.15f)
                                else NexoraSurfaceVariant
                            )
                            .border(
                                1.dp,
                                if (isSelected) sColor else NexoraBorder,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable(enabled = !timerState.isRunning) {
                                onSelectSubject(subject)
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("timer_subject_chip_${subject.id}")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(sColor, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = subject.name,
                                color = if (isSelected) NexoraTextPrimary else NexoraTextSecondary,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Preset Duration Chips
            // Removed fixed duration presets as timer is now open-ended.
            // Users can study as long as they need.

            Spacer(modifier = Modifier.height(16.dp))

            // 4. CIRCULAR PROGRESS BAR
            Box(
                modifier = Modifier
                    .size(210.dp)
                    .testTag("timer_circular_progress_container"),
                contentAlignment = Alignment.Center
            ) {
                // Canvas Circular Ring
                Canvas(modifier = Modifier.size(190.dp)) {
                    val strokeWidth = 13.dp.toPx()
                    val arcPadding = strokeWidth / 2f
                    val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                    val topLeft = Offset(arcPadding, arcPadding)

                    // 1. Background Track
                    drawArc(
                        color = Color(0xFFF1F5F9),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // 2. Active Gradient Arc (Elapsed Time)
                    val sweepAngle = (animatedProgress * 360f).coerceIn(0f, 360f)
                    if (sweepAngle > 0f) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                listOf(
                                    subjectColor,
                                    NexoraCyan,
                                    NexoraElectricBlue,
                                    subjectColor
                                )
                            ),
                            startAngle = -90f,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                }

                // Inside Circular Area: Subject + Countdown + Elapsed Stats
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    // Subject Name Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(subjectColor.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = timerState.subjectName,
                            color = subjectColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Big Elapsed Display (HH:MM:SS)
                    Text(
                        text = timerState.formattedElapsedTime,
                        color = NexoraTextPrimary,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.testTag("timer_elapsed_display")
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Time Session Label
                    Text(
                        text = "Total Focus Time",
                        color = NexoraTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Status Pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(
                                    when {
                                        timerState.isRunning -> NexoraCyan.copy(alpha = pulsingAlpha)
                                        timerState.isPaused -> NexoraGold
                                        timerState.isCompleted -> NexoraSuccess
                                        else -> NexoraTextMuted
                                    },
                                    CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = when {
                                timerState.isRunning -> "FOCUSING"
                                timerState.isPaused -> "PAUSED"
                                timerState.isCompleted -> "COMPLETED"
                                else -> "READY"
                            },
                            color = when {
                                timerState.isRunning -> NexoraCyan
                                timerState.isPaused -> NexoraGold
                                timerState.isCompleted -> NexoraSuccess
                                else -> NexoraTextMuted
                            },
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. Completion Celebration Banner
            AnimatedVisibility(
                visible = timerState.isCompleted,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(NexoraSuccess.copy(alpha = 0.12f))
                        .border(1.dp, NexoraSuccess.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                        .testTag("timer_session_completed_banner")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = NexoraSuccess,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "🎉 Focus session logged to your Daily Planner!",
                            color = NexoraSuccess,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Scrollable Completed Focus Sessions History List with Subject Filtering
            val sessionHistory by StudyTimerManager.sessionHistory.collectAsStateWithLifecycle(initialValue = emptyList())
            var selectedFilterSubjectId by remember { mutableStateOf<String?>(null) }

            val filteredHistory = remember(sessionHistory, selectedFilterSubjectId) {
                if (selectedFilterSubjectId == null) sessionHistory
                else sessionHistory.filter { it.subjectId == selectedFilterSubjectId }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(NexoraSurfaceVariant.copy(alpha = 0.5f))
                    .border(1.dp, NexoraBorder, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Completed Focus History (${filteredHistory.size}/${sessionHistory.size})",
                        color = NexoraTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Filter by Subject",
                        color = NexoraTextSecondary,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Subject Filter Chips
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    val isAllSelected = selectedFilterSubjectId == null
                    item {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isAllSelected) NexoraCyan.copy(alpha = 0.2f) else NexoraSurface)
                                .border(1.dp, if (isAllSelected) NexoraCyan else NexoraBorder, RoundedCornerShape(8.dp))
                                .clickable { selectedFilterSubjectId = null }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                .testTag("filter_chip_all"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "All (${sessionHistory.size})",
                                color = if (isAllSelected) NexoraCyan else NexoraTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }

                    items(Subject.OFFICIAL_SUBJECTS) { subject ->
                        val isSelected = selectedFilterSubjectId == subject.id
                        val count = sessionHistory.count { it.subjectId == subject.id }
                        val sCol = getTimerSubjectColor(subject.id)

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) sCol.copy(alpha = 0.2f) else NexoraSurface)
                                .border(1.dp, if (isSelected) sCol else NexoraBorder, RoundedCornerShape(8.dp))
                                .clickable { selectedFilterSubjectId = subject.id }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                .testTag("filter_chip_${subject.id}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${subject.name} ($count)",
                                color = if (isSelected) sCol else NexoraTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (filteredHistory.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (sessionHistory.isEmpty()) "No completed focus sessions yet. Start a session above!"
                                   else "No completed sessions for this subject.",
                            color = NexoraTextMuted,
                            fontSize = 11.5.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp)
                            .testTag("focus_session_history_list"),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(filteredHistory, key = { it.id }) { session ->
                            val sColor = getTimerSubjectColor(session.subjectId)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NexoraSurfaceElevated)
                                    .border(0.8.dp, sColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                                    .testTag("session_history_item_${session.id}")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .background(sColor.copy(alpha = 0.15f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.CheckCircle,
                                                contentDescription = null,
                                                tint = sColor,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = session.subjectName,
                                                color = NexoraTextPrimary,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = session.formattedTimestamp,
                                                color = NexoraTextSecondary,
                                                fontSize = 10.5.sp
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .background(sColor.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "${session.durationMinutes} mins",
                                            color = sColor,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 6. Interactive Controls (Play / Pause / Resume / Reset / Done)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Primary Action Button (Start / Pause / Resume)
                Button(
                    onClick = {
                        when {
                            timerState.isRunning -> onPauseTimer()
                            timerState.isPaused -> onResumeTimer()
                            else -> onStartTimer()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("timer_primary_action_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (timerState.isRunning) NexoraGold else subjectColor
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = when {
                                timerState.isRunning -> Icons.Filled.Pause
                                else -> Icons.Filled.PlayArrow
                            },
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when {
                                timerState.isRunning -> "Pause Focus"
                                timerState.isPaused -> "Resume Focus"
                                timerState.isCompleted -> "Start New Session"
                                else -> "Start Focus"
                            },
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Reset Button
                OutlinedButton(
                    onClick = onResetTimer,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("timer_reset_btn"),
                    shape = RoundedCornerShape(12.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.linearGradient(listOf(NexoraBorder, NexoraBorder))
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Replay,
                        contentDescription = "Reset Timer",
                        tint = NexoraTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Complete Early / Log Session Button (Visible when timer is running or paused)
                if (timerState.isRunning || timerState.isPaused) {
                    OutlinedButton(
                        onClick = onCompleteEarly,
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("timer_complete_early_btn"),
                        shape = RoundedCornerShape(12.dp),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.linearGradient(listOf(NexoraSuccess, NexoraSuccess))
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = "Log Session",
                            tint = NexoraSuccess,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FocusModeOverlay(
    timerState: StudyTimerState,
    subjectColor: Color,
    onStartTimer: () -> Unit,
    onPauseTimer: () -> Unit,
    onResumeTimer: () -> Unit,
    onResetTimer: () -> Unit,
    onCompleteEarly: () -> Unit,
    onExitFocusMode: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .testTag("focus_mode_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            // Top Bar: Exit button & Focus Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(subjectColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "FOCUS MODE ACTIVE",
                        color = subjectColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(NexoraSurfaceVariant)
                        .border(1.dp, NexoraBorder, RoundedCornerShape(12.dp))
                        .clickable { onExitFocusMode() }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .testTag("exit_focus_mode_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.FullscreenExit,
                            contentDescription = "Exit Focus Mode",
                            tint = NexoraCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Exit Focus",
                            color = NexoraCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Subject Title
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = timerState.subjectName,
                    color = NexoraTextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "GSEB Class 12 Commerce Deep Study",
                    color = NexoraTextSecondary,
                    fontSize = 13.sp
                )
            }

            // Giant Countdown Display
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .background(subjectColor.copy(alpha = 0.1f), CircleShape)
                    .border(2.dp, subjectColor.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = timerState.formattedElapsedTime,
                        color = NexoraTextPrimary,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (timerState.isRunning) "FOCUSED" else if (timerState.isPaused) "PAUSED" else "READY",
                        color = subjectColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                }
            }

            // Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!timerState.isRunning && !timerState.isPaused && !timerState.isCompleted) {
                    Button(
                        onClick = { onStartTimer() },
                        colors = ButtonDefaults.buttonColors(containerColor = subjectColor),
                        modifier = Modifier.height(50.dp).testTag("focus_start_button")
                    ) {
                        Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Focus Session", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                } else if (timerState.isRunning) {
                    Button(
                        onClick = { onPauseTimer() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                        modifier = Modifier.height(50.dp).testTag("focus_pause_button")
                    ) {
                        Icon(imageVector = Icons.Filled.Pause, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pause", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                } else if (timerState.isPaused) {
                    Button(
                        onClick = { onResumeTimer() },
                        colors = ButtonDefaults.buttonColors(containerColor = subjectColor),
                        modifier = Modifier.height(50.dp).testTag("focus_resume_button")
                    ) {
                        Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Resume", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                OutlinedButton(
                    onClick = { onCompleteEarly() },
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                    modifier = Modifier.height(50.dp).testTag("focus_done_button")
                ) {
                    Icon(imageVector = Icons.Filled.Check, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Mark Done", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
