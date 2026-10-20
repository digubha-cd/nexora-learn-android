package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.DayStudyActivity
import com.example.core.model.Subject
import com.example.core.timer.FocusSessionRecord
import com.example.ui.theme.NexoraBorder
import com.example.ui.theme.NexoraCyan
import com.example.ui.theme.NexoraElectricBlue
import com.example.ui.theme.NexoraGold
import com.example.ui.theme.NexoraMagenta
import com.example.ui.theme.NexoraPink
import com.example.ui.theme.NexoraPurple
import com.example.ui.theme.NexoraSuccess
import com.example.ui.theme.NexoraSurfaceElevated
import com.example.ui.theme.NexoraSurfaceVariant
import com.example.ui.theme.NexoraTextMuted
import com.example.ui.theme.NexoraTextPrimary
import com.example.ui.theme.NexoraTextSecondary
import com.example.ui.animation.bounceClick

/**
 * Complete Study Activity & Insights Card for the Home screen.
 * Displays:
 * 1. 7-day horizontal bar chart of actual task completion and focus duration.
 * 2. 7-day summary metrics (tasks completed, active streak days, consistency percentage).
 * 3. Real completed focus study sessions from StudyTimerManager.
 * 4. Direct actionable navigation to Focus Timer & Planner.
 */
@Composable
fun Past7DaysActivityCard(
    activityList: List<DayStudyActivity>,
    modifier: Modifier = Modifier,
    recentSessions: List<FocusSessionRecord> = emptyList(),
    onNavigateToPlanner: () -> Unit = {}
) {
    val maxCompleted = maxOf(1, activityList.maxOfOrNull { it.completedCount } ?: 1)
    // Scale reference: max completed tasks or minimum of 3 for nice proportional visual bars
    val maxScale = maxOf(maxCompleted, 3).toFloat()

    val totalCompleted7Days = activityList.sumOf { it.completedCount }
    val activeDaysCount = activityList.count { it.isStreakAchieved }
    val consistencyPercentage = if (activityList.isNotEmpty()) {
        (activeDaysCount * 100) / activityList.size
    } else 0

    NexoraCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_past_7_days_activity_card"),
        cornerRadius = 20.dp,
        contentPadding = 16.dp,
        elevation = 1.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Header: Icon + Title + Active Days Pill
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
                            .background(NexoraPurple.copy(alpha = 0.12f), CircleShape)
                            .border(1.dp, NexoraPurple.copy(alpha = 0.28f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.BarChart,
                            contentDescription = "7-Day Activity",
                            tint = NexoraPurple,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "7-Day Study Activity",
                            color = NexoraTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("activity_chart_title")
                        )
                        Text(
                            text = "Daily task completion & streak consistency",
                            color = NexoraTextSecondary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Active Days Pill
                Box(
                    modifier = Modifier
                        .background(
                            if (activeDaysCount > 0) NexoraSuccess.copy(alpha = 0.12f) else NexoraSurfaceVariant,
                            RoundedCornerShape(10.dp)
                        )
                        .border(
                            1.dp,
                            if (activeDaysCount > 0) NexoraSuccess.copy(alpha = 0.35f) else NexoraBorder,
                            RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 9.dp, vertical = 5.dp)
                        .testTag("active_days_pill")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (activeDaysCount >= 5) "🔥" else "📊",
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$activeDaysCount/7 Days Active",
                            color = if (activeDaysCount > 0) NexoraSuccess else NexoraTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 2. Horizontal Bar Chart for the past 7 days
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("activity_bars_container"),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                activityList.forEach { dayActivity ->
                    val progressRatio = (dayActivity.completedCount / maxScale).coerceIn(0f, 1f)
                    val animatedProgress by animateFloatAsState(
                        targetValue = progressRatio,
                        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
                        label = "bar_anim_${dayActivity.dateStr}"
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("activity_bar_day_${dayActivity.dayLabel.lowercase()}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Day Label & Date
                        Column(
                            modifier = Modifier.width(62.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (dayActivity.isToday) "Today" else dayActivity.dayLabel,
                                    color = if (dayActivity.isToday) NexoraCyan else NexoraTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = if (dayActivity.isToday || dayActivity.isStreakAchieved) FontWeight.Bold else FontWeight.SemiBold
                                )
                                if (dayActivity.isToday) {
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .background(NexoraCyan, CircleShape)
                                    )
                                }
                            }
                            Text(
                                text = dayActivity.dayNumberFormatted,
                                color = NexoraTextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Horizontal Bar Track
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(18.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(NexoraSurfaceVariant)
                                .border(
                                    0.5.dp,
                                    if (dayActivity.isToday) NexoraCyan.copy(alpha = 0.3f) else NexoraBorder.copy(alpha = 0.5f),
                                    RoundedCornerShape(9.dp)
                                )
                        ) {
                            if (animatedProgress > 0f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(animatedProgress)
                                        .clip(RoundedCornerShape(9.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = when {
                                                    dayActivity.isToday -> listOf(
                                                        NexoraCyan,
                                                        NexoraElectricBlue
                                                    )
                                                    dayActivity.isStreakAchieved -> listOf(
                                                        NexoraPurple,
                                                        NexoraCyan
                                                    )
                                                    else -> listOf(
                                                        NexoraSurfaceVariant,
                                                        NexoraBorder
                                                    )
                                                }
                                            )
                                        )
                                )
                            } else {
                                // Subtle empty placeholder dot
                                Box(
                                    modifier = Modifier
                                        .padding(start = 6.dp)
                                        .size(6.dp)
                                        .align(Alignment.CenterStart)
                                        .background(NexoraTextMuted.copy(alpha = 0.3f), CircleShape)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Right Status / Count Badge
                        Box(
                            modifier = Modifier
                                .width(72.dp)
                                .align(Alignment.CenterVertically),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            if (dayActivity.completedCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (dayActivity.isToday) NexoraCyan.copy(alpha = 0.12f)
                                            else NexoraPurple.copy(alpha = 0.1f),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                    ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            tint = if (dayActivity.isToday) NexoraCyan else NexoraPurple,
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "${dayActivity.completedCount} done",
                                            color = if (dayActivity.isToday) NexoraCyan else NexoraPurple,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = if (dayActivity.isToday) "Pending" else "0 done",
                                    color = if (dayActivity.isToday) NexoraGold else NexoraTextMuted,
                                    fontSize = 10.5.sp,
                                    fontWeight = if (dayActivity.isToday) FontWeight.SemiBold else FontWeight.Normal,
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                    }
                }
            }

            // 3. Mini Summary Stats Footer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(NexoraSurfaceElevated)
                    .border(1.dp, NexoraBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Stat 1: Total Tasks
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$totalCompleted7Days",
                            color = NexoraCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tasks Done (7d)",
                            color = NexoraTextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Divider dot
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .background(NexoraBorder, CircleShape)
                    )

                    // Stat 2: Active Days
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$activeDaysCount / 7",
                            color = NexoraPurple,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Streak Days",
                            color = NexoraTextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Divider dot
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .background(NexoraBorder, CircleShape)
                    )

                    // Stat 3: Consistency
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$consistencyPercentage%",
                            color = if (consistencyPercentage >= 70) NexoraSuccess else NexoraGold,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Consistency",
                            color = NexoraTextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // 4. Real Focus Study Sessions Section
            if (recentSessions.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Timer,
                                contentDescription = null,
                                tint = NexoraCyan,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Recent Focus Sessions (${recentSessions.size})",
                                color = NexoraTextPrimary,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    recentSessions.take(3).forEach { session ->
                        val subColor = when (session.subjectId) {
                            "accounts" -> NexoraGold
                            "economics" -> NexoraPink
                            "ba" -> NexoraMagenta
                            "stat" -> NexoraCyan
                            "sp_cc" -> NexoraPurple
                            "english" -> NexoraElectricBlue
                            "gujarati" -> NexoraCyan
                            else -> NexoraCyan
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(NexoraSurfaceElevated)
                                .border(0.5.dp, NexoraBorder, RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f, fill = false),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(subColor, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = session.subjectName,
                                        color = NexoraTextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = session.formattedTimestamp,
                                        color = NexoraTextMuted,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(subColor.copy(alpha = 0.12f))
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "${session.durationMinutes} min",
                                    color = subColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            } else {
                // Helpful guidance when no timer sessions have been recorded yet
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(NexoraCyan.copy(alpha = 0.06f))
                        .border(1.dp, NexoraCyan.copy(alpha = 0.18f), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⏱️", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Start a focus timer or complete tasks in the Planner to record real session activity here!",
                            color = NexoraTextSecondary,
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // 5. Quick Planner & Timer shortcut
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .bounceClick { onNavigateToPlanner() }
                    .padding(vertical = 4.dp)
                    .testTag("activity_card_planner_shortcut"),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Open Focus Timer & Planner",
                    color = NexoraCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = NexoraCyan,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}
