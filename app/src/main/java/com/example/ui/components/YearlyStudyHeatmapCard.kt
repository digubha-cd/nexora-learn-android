package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.DayStudyActivity
import com.example.core.model.HeatmapSummaryStats
import com.example.core.repository.StudyPlannerRepository
import com.example.ui.theme.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * GitHub-style 365-day Study Consistency Heatmap Card.
 * Displays a 52-week horizontal grid showing 5 activity levels based on real
 * study-session history and completed tasks in Asia/Kolkata (IST).
 */
@Composable
fun YearlyStudyHeatmapCard(
    activityList: List<DayStudyActivity>,
    onDayClick: (DayStudyActivity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Calculate summary statistics
    val summaryStats: HeatmapSummaryStats = remember(activityList) {
        StudyPlannerRepository.getHeatmapSummaryStats(activityList)
    }

    // Auto-scroll to the end (today) on initial appearance
    LaunchedEffect(activityList.size) {
        if (activityList.isNotEmpty()) {
            // Scroll to end so latest week is immediately visible
            scrollState.scrollTo(scrollState.maxValue)
        }
    }

    // Group days into columns of 7 days (Monday -> Sunday)
    val weekColumns: List<List<DayStudyActivity>> = remember(activityList) {
        activityList.chunked(7)
    }

    // Month headers mapped by week column index
    val monthHeaders = remember(weekColumns) {
        val monthFormatter = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH)
        val headers = mutableListOf<Pair<Int, String>>()
        var lastMonth: String? = null

        weekColumns.forEachIndexed { index: Int, days: List<DayStudyActivity> ->
            // Use the first day of the week to detect month transitions
            val firstDay = days.firstOrNull()?.date
            if (firstDay != null) {
                val currentMonth = firstDay.format(monthFormatter)
                if (currentMonth != lastMonth) {
                    headers.add(Pair(index, currentMonth))
                    lastMonth = currentMonth
                }
            }
        }
        headers
    }

    NexoraCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("yearly_study_heatmap_card"),
        cornerRadius = 20.dp,
        contentPadding = 12.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 1. Header with title & active days badge
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
                            .clip(CircleShape)
                            .background(NexoraCyan.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = NexoraCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "365-Day Consistency Heatmap",
                            color = NexoraTextPrimary,
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "GitHub-style daily activity tracker & 90+ momentum",
                            color = NexoraTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .background(NexoraCyan.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 9.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "52 WEEKS",
                        color = NexoraCyan,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Summary Statistics Row: 4 metric pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val totalHours = summaryStats.totalStudyMinutes / 60
                val totalMins = summaryStats.totalStudyMinutes % 60
                val formattedTime = if (totalHours > 0L) "${totalHours}h ${totalMins}m" else "${totalMins}m"

                SummaryStatPill(
                    label = "Active Days",
                    value = "${summaryStats.activeDaysCount}",
                    color = NexoraCyan,
                    modifier = Modifier.weight(1f)
                )
                SummaryStatPill(
                    label = "Streak",
                    value = "${summaryStats.currentStreak}d 🔥",
                    color = NexoraGold,
                    modifier = Modifier.weight(1f)
                )
                SummaryStatPill(
                    label = "Best Streak",
                    value = "${summaryStats.bestStreak}d 🏆",
                    color = NexoraElectricBlue,
                    modifier = Modifier.weight(1f)
                )
                SummaryStatPill(
                    label = "Total Time",
                    value = formattedTime,
                    color = NexoraSuccess,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Grid Container: Weekday labels on left + Horizontally scrollable 52-week matrix
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Fixed Weekday Labels Column (Mon, Wed, Fri)
                Column(
                    modifier = Modifier
                        .padding(top = 22.dp, end = 6.dp)
                        .width(26.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    val weekdayLabels = listOf("M", "", "W", "", "F", "", "S")
                    weekdayLabels.forEach { label ->
                        Box(
                            modifier = Modifier.size(width = 26.dp, height = 12.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            if (label.isNotBlank()) {
                                Text(
                                    text = label,
                                    color = NexoraTextMuted,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Scrollable 52-week grid with month labels
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(scrollState)
                        .padding(bottom = 6.dp)
                ) {
                    Column {
                        // Month Labels Row
                        Row(
                            modifier = Modifier.height(18.dp)
                        ) {
                            var currentMonthIndex = 0
                            val colWidth = 15.dp // 12dp cell + 3dp spacing

                            weekColumns.forEachIndexed { colIdx, _ ->
                                val header = monthHeaders.getOrNull(currentMonthIndex)
                                if (header != null && header.first == colIdx) {
                                    Box(
                                        modifier = Modifier.width(colWidth * 4),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Text(
                                            text = header.second,
                                            color = NexoraTextSecondary,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    currentMonthIndex++
                                } else if (header == null || colIdx > header.first + 3) {
                                    // Spacing cell
                                    Spacer(modifier = Modifier.width(colWidth))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Heatmap Matrix: Week Columns
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            val today = StudyPlannerRepository.TODAY

                            weekColumns.forEach { weekDays ->
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    weekDays.forEach { day ->
                                        val isFuture = day.date > today
                                        val cellColor = when {
                                            isFuture -> NexoraSurfaceVariant.copy(alpha = 0.35f)
                                            day.activityLevel == 4 -> NexoraGold
                                            day.activityLevel == 3 -> NexoraElectricBlue
                                            day.activityLevel == 2 -> NexoraCyan
                                            day.activityLevel == 1 -> Color(0xFFBAE6FD) // Pastel Light Cyan
                                            else -> NexoraSurfaceVariant
                                        }

                                        val isToday = day.isToday

                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(cellColor)
                                                .border(
                                                    width = if (isToday) 1.5.dp else 0.dp,
                                                    color = if (isToday) NexoraGold else Color.Transparent,
                                                    shape = RoundedCornerShape(3.dp)
                                                )
                                                .clickable(enabled = !isFuture) {
                                                    onDayClick(day)
                                                }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Legend & Hint Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tap any date to inspect details",
                    color = NexoraTextSecondary,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Less", color = NexoraTextMuted, fontSize = 9.sp)
                    Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.5.dp)).background(NexoraSurfaceVariant))
                    Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.5.dp)).background(Color(0xFFBAE6FD)))
                    Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.5.dp)).background(NexoraCyan))
                    Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.5.dp)).background(NexoraElectricBlue))
                    Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.5.dp)).background(NexoraGold))
                    Text("More", color = NexoraTextMuted, fontSize = 9.sp)
                }
            }
        }
    }
}

@Composable
private fun SummaryStatPill(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.08f))
            .border(0.5.dp, color.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .padding(horizontal = 6.dp, vertical = 7.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                color = color,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
            Text(
                text = label,
                color = NexoraTextSecondary,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}
