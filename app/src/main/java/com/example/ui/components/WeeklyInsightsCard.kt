package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.model.Subject
import com.example.core.timer.StudyTimerManager
import com.example.ui.theme.NexoraBorder
import com.example.ui.theme.NexoraCyan
import com.example.ui.theme.NexoraElectricBlue
import com.example.ui.theme.NexoraGold
import com.example.ui.theme.NexoraMagenta
import com.example.ui.theme.NexoraPink
import com.example.ui.theme.NexoraPurple
import com.example.ui.theme.NexoraSurfaceVariant
import com.example.ui.theme.NexoraTextMuted
import com.example.ui.theme.NexoraTextPrimary
import com.example.ui.theme.NexoraTextSecondary

/**
 * 'Weekly Insights' Dashboard card visualizing study hours per subject over the last 7 days,
 * helping students identify their most productive times and subjects.
 */
@Composable
fun WeeklyInsightsCard(
    modifier: Modifier = Modifier
) {
    val sessionHistory by StudyTimerManager.sessionHistory.collectAsStateWithLifecycle(initialValue = emptyList())

    val sevenDaysAgoMillis = remember { System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000L) }
    val recentSessions = remember(sessionHistory, sevenDaysAgoMillis) {
        sessionHistory.filter { it.timestampEpochMillis >= sevenDaysAgoMillis }
    }

    val totalMinutes7Days = recentSessions.sumOf { it.durationMinutes }
    val totalHours7Days = totalMinutes7Days / 60f
    val formattedTotalHours = String.format("%.1f hrs", totalHours7Days)

    val subjectHoursMap = remember(recentSessions) {
        val map = mutableMapOf<String, Int>()
        recentSessions.forEach { session ->
            val current = map[session.subjectId] ?: 0
            map[session.subjectId] = current + session.durationMinutes
        }
        map
    }

    val maxSubjectMinutes = maxOf(60, subjectHoursMap.values.maxOrNull() ?: 60).toFloat()

    val topSubjectEntry = subjectHoursMap.maxByOrNull { it.value }
    val topSubjectName = topSubjectEntry?.let { entry ->
        Subject.OFFICIAL_SUBJECTS.find { it.id == entry.key }?.name
    } ?: "None yet"

    NexoraCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("weekly_insights_card"),
        cornerRadius = 18.dp,
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
                            .size(36.dp)
                            .background(NexoraCyan.copy(alpha = 0.12f), CircleShape)
                            .border(1.dp, NexoraCyan.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Insights,
                            contentDescription = "Weekly Insights",
                            tint = NexoraCyan,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Weekly Insights & Study Hours",
                            color = NexoraTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("weekly_insights_title")
                        )
                        Text(
                            text = "Past 7 Days • Top: $topSubjectName",
                            color = NexoraTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .background(NexoraSurfaceVariant, RoundedCornerShape(10.dp))
                        .border(1.dp, NexoraBorder, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                        .testTag("weekly_total_hours_badge")
                ) {
                    Text(
                        text = "⏱️ $formattedTotalHours",
                        color = NexoraCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Spacer(modifier = Modifier.height(20.dp))
            
            StudyTrendChart(sessions = recentSessions)

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Subject-wise Distribution (Last 7 Days):",
                color = NexoraTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("weekly_subject_hours_chart"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Subject.OFFICIAL_SUBJECTS.forEach { subject ->
                    val mins = subjectHoursMap[subject.id] ?: 0
                    val hours = mins / 60f
                    val progressRatio = (mins / maxSubjectMinutes).coerceIn(0f, 1f)
                    val animatedProgress by animateFloatAsState(
                        targetValue = progressRatio,
                        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                        label = "weekly_bar_${subject.id}"
                    )
                    val sColor = when (subject.id) {
                        "gujarati" -> NexoraCyan
                        "english" -> NexoraElectricBlue
                        "sp_cc" -> NexoraPurple
                        "ba" -> NexoraMagenta
                        "stat" -> NexoraCyan
                        "accounts" -> NexoraGold
                        "economics" -> NexoraPink
                        else -> NexoraCyan
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = subject.name,
                            color = NexoraTextPrimary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.width(95.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(NexoraSurfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(animatedProgress)
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(sColor)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = String.format("%.1f h", hours),
                            color = NexoraTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(42.dp),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(NexoraCyan.copy(alpha = 0.08f))
                    .border(1.dp, NexoraCyan.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "💡",
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (totalMinutes7Days > 0)
                            "Great focus consistency! Most productive time is tracked through your active study sessions."
                        else
                            "Start a focus session in the timer to populate your weekly productivity and study hours insights.",
                        color = NexoraTextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
