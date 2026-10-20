package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Flag
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.storage.StudentPreferences
import com.example.core.timer.StudyTimerManager
import com.example.ui.animation.bounceClick
import com.example.ui.theme.*

/**
 * Weekly Study Goal Card:
 * Allows students to set and display weekly study hour goals, showing a progress
 * indicator that tracks actual study time against these goals to encourage students.
 */
@Composable
fun WeeklyStudyGoalCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sessionHistory by StudyTimerManager.sessionHistory.collectAsStateWithLifecycle(initialValue = emptyList())

    var weeklyGoalHours by remember {
        mutableStateOf(StudentPreferences.getWeeklyStudyGoalHours(context).toFloat())
    }
    var showEditDialog by remember { mutableStateOf(false) }

    // Calculate past 7 days actual study time
    val sevenDaysAgoMillis = remember { System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000L) }
    val recentSessions = remember(sessionHistory, sevenDaysAgoMillis) {
        sessionHistory.filter { it.timestampEpochMillis >= sevenDaysAgoMillis }
    }
    val totalMinutes7Days = recentSessions.sumOf { it.durationMinutes }
    val totalHours7Days = totalMinutes7Days / 60f

    val progressRatio = if (weeklyGoalHours > 0f) {
        (totalHours7Days / weeklyGoalHours).coerceIn(0f, 1f)
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progressRatio,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "WeeklyGoalProgress"
    )

    val percentageInt = (progressRatio * 100).toInt()

    NexoraCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("weekly_study_goal_card"),
        cornerRadius = 20.dp,
        contentPadding = 18.dp,
        elevation = 2.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
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
                            .size(38.dp)
                            .background(NexoraCyan.copy(alpha = 0.12f), CircleShape)
                            .border(1.dp, NexoraCyan.copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Flag,
                            contentDescription = "Weekly Study Goal",
                            tint = NexoraCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Weekly Study Goal",
                            color = NexoraTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("weekly_goal_title")
                        )
                        Text(
                            text = "Past 7 Days Progress",
                            color = NexoraTextSecondary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                IconButton(
                    onClick = { showEditDialog = true },
                    modifier = Modifier
                        .size(36.dp)
                        .background(NexoraSurfaceVariant, CircleShape)
                        .border(1.dp, NexoraBorder, CircleShape)
                        .testTag("edit_weekly_goal_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit Weekly Goal",
                        tint = NexoraCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = String.format("%.1f hrs", totalHours7Days),
                        color = NexoraTextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.testTag("weekly_actual_hours_text")
                    )
                    Text(
                        text = "Goal: ${weeklyGoalHours.toInt()} hrs / week",
                        color = NexoraTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Box(
                    modifier = Modifier
                        .background(
                            if (percentageInt >= 100) NexoraSuccess.copy(alpha = 0.15f) else NexoraCyan.copy(alpha = 0.12f),
                            RoundedCornerShape(10.dp)
                        )
                        .border(
                            1.dp,
                            if (percentageInt >= 100) NexoraSuccess.copy(alpha = 0.4f) else NexoraCyan.copy(alpha = 0.3f),
                            RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                        .testTag("weekly_goal_percentage_badge")
                ) {
                    Text(
                        text = "$percentageInt% Completed",
                        color = if (percentageInt >= 100) NexoraSuccess else NexoraCyan,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .testTag("weekly_goal_progress_indicator"),
                color = if (percentageInt >= 100) NexoraSuccess else NexoraCyan,
                trackColor = NexoraSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (percentageInt >= 100) {
                    "🎉 Incredible! You've reached or exceeded your weekly study goal!"
                } else {
                    val remaining = ((weeklyGoalHours - totalHours7Days).coerceAtLeast(0f))
                    String.format("💪 Keep going! %.1f hrs left to achieve your weekly target.", remaining)
                },
                color = NexoraTextSecondary,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }

    if (showEditDialog) {
        var sliderValue by remember { mutableStateOf(weeklyGoalHours) }
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Text("Set Weekly Study Goal", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Choose your target study hours per week:",
                        color = NexoraTextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "${sliderValue.toInt()} Hours / Week",
                        color = NexoraCyan,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = sliderValue,
                        onValueChange = { sliderValue = it },
                        valueRange = 5f..50f,
                        steps = 45,
                        modifier = Modifier.testTag("weekly_goal_slider")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("5 hrs", fontSize = 11.sp, color = NexoraTextMuted)
                        Text("25 hrs", fontSize = 11.sp, color = NexoraTextMuted)
                        Text("50 hrs", fontSize = 11.sp, color = NexoraTextMuted)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        weeklyGoalHours = sliderValue
                        StudentPreferences.setWeeklyStudyGoalHours(context, sliderValue.toInt())
                        showEditDialog = false
                    },
                    modifier = Modifier.testTag("save_weekly_goal_button")
                ) {
                    Text("Save Goal", fontWeight = FontWeight.Bold, color = NexoraCyan)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel", color = NexoraTextSecondary)
                }
            },
            containerColor = NexoraBackground,
            shape = RoundedCornerShape(24.dp)
        )
    }
}
