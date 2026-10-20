package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.storage.StudentPreferences
import com.example.core.timer.StudyTimerManager
import com.example.ui.theme.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/**
 * Compact 7-day Weekly Streak Calendar Card for the Student Home screen:
 * Displays 7 days at a time (Mon to Sun) highlighting days where study goal was achieved,
 * taking up much less vertical space while allowing week-by-week navigation back in time.
 */
@Composable
fun MonthlyCalendarStreakCard(
    modifier: Modifier = Modifier
) {
    WeeklyCalendarStreakCard(modifier = modifier)
}

@Composable
fun WeeklyCalendarStreakCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sessionHistory by StudyTimerManager.sessionHistory.collectAsStateWithLifecycle(initialValue = emptyList())
    val weeklyGoalHours = remember { StudentPreferences.getWeeklyStudyGoalHours(context) }
    val dailyGoalMinutes = remember(weeklyGoalHours) {
        ((weeklyGoalHours * 60f) / 7f).coerceAtLeast(30f).toInt()
    }

    val today = remember { LocalDate.now() }
    val currentWeekStart = remember(today) {
        today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }

    var selectedWeekStart by remember { mutableStateOf(currentWeekStart) }
    var selectedDateInfo by remember { mutableStateOf<Pair<LocalDate, Int>?>(null) }

    // Map epoch day or date string to total study minutes on that day
    val dailyMinutesMap = remember(sessionHistory) {
        val map = mutableMapOf<LocalDate, Int>()
        for (session in sessionHistory) {
            val date = java.time.Instant.ofEpochMilli(session.timestampEpochMillis)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
            map[date] = (map[date] ?: 0) + session.durationMinutes
        }
        map
    }

    // 7 Days of the currently selected week (Monday to Sunday)
    val weekDays = remember(selectedWeekStart) {
        (0L..6L).map { selectedWeekStart.plusDays(it) }
    }

    val startDay = weekDays.first()
    val endDay = weekDays.last()
    val weekRangeLabel = remember(selectedWeekStart) {
        if (startDay.month == endDay.month) {
            "${startDay.dayOfMonth} - ${endDay.dayOfMonth} ${startDay.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)} ${startDay.year}"
        } else {
            "${startDay.dayOfMonth} ${startDay.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)} - ${endDay.dayOfMonth} ${endDay.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)} ${endDay.year}"
        }
    }

    val canGoNext = selectedWeekStart < currentWeekStart

    NexoraCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("monthly_calendar_streak_card"),
        cornerRadius = 20.dp,
        contentPadding = 16.dp,
        containerColor = Color.White,
        borderColor = NexoraCyan.copy(alpha = 0.35f),
        elevation = 2.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header: Title & Week Navigation
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
                            .background(NexoraGold.copy(alpha = 0.15f), CircleShape)
                            .border(1.dp, NexoraGold.copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Whatshot,
                            contentDescription = "Streak Calendar",
                            tint = NexoraGold,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Streak Calendar",
                            color = NexoraTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("streak_calendar_title")
                        )
                        Text(
                            text = weekRangeLabel,
                            color = NexoraTextSecondary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.testTag("current_month_label")
                        )
                    }
                }

                // Week Switcher Controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = { selectedWeekStart = selectedWeekStart.minusWeeks(1) },
                        modifier = Modifier
                            .size(32.dp)
                            .background(NexoraSurfaceVariant, CircleShape)
                            .testTag("prev_month_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Previous Week",
                            tint = NexoraTextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = {
                            if (canGoNext) {
                                selectedWeekStart = selectedWeekStart.plusWeeks(1)
                            }
                        },
                        enabled = canGoNext,
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                if (canGoNext) NexoraSurfaceVariant else NexoraSurfaceVariant.copy(alpha = 0.4f),
                                CircleShape
                            )
                            .testTag("next_month_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next Week",
                            tint = if (canGoNext) NexoraTextPrimary else NexoraTextMuted.copy(alpha = 0.4f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 7-day Weekly Calendar Row (Mon - Sun)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                weekDays.forEach { date ->
                    val minutesStudied = dailyMinutesMap[date] ?: 0
                    val goalAchieved = minutesStudied >= dailyGoalMinutes || minutesStudied > 0
                    val isToday = date == today
                    val isFuture = date > today
                    val dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                width = if (isToday) 1.5.dp else 0.dp,
                                color = if (isToday) NexoraCyan else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .background(
                                when {
                                    goalAchieved -> NexoraSuccess.copy(alpha = 0.18f)
                                    isToday -> NexoraCyan.copy(alpha = 0.12f)
                                    else -> NexoraSurfaceVariant.copy(alpha = 0.5f)
                                },
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                selectedDateInfo = date to minutesStudied
                            }
                            .padding(vertical = 8.dp, horizontal = 2.dp)
                            .testTag("calendar_day_${date.dayOfMonth}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = dayName,
                                color = if (isToday) NexoraCyan else NexoraTextMuted,
                                fontSize = 10.5.sp,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium
                            )
                            Text(
                                text = "${date.dayOfMonth}",
                                color = when {
                                    goalAchieved -> NexoraSuccess
                                    isToday -> NexoraCyan
                                    isFuture -> NexoraTextMuted.copy(alpha = 0.5f)
                                    else -> NexoraTextPrimary
                                },
                                fontSize = 13.sp,
                                fontWeight = if (goalAchieved || isToday) FontWeight.Bold else FontWeight.SemiBold
                            )
                            if (goalAchieved) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .background(NexoraSuccess, CircleShape)
                                )
                            } else {
                                Spacer(modifier = Modifier.size(5.dp))
                            }
                        }
                    }
                }
            }

            // Selected date study details info if clicked
            if (selectedDateInfo != null) {
                Spacer(modifier = Modifier.height(10.dp))
                val (date, mins) = selectedDateInfo!!
                val hours = mins / 60
                val remMins = mins % 60
                val timeStr = if (hours > 0) "${hours}h ${remMins}m" else "${remMins}m"

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NexoraSurfaceVariant, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📅 ${date.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH))}: $timeStr studied",
                            color = NexoraTextPrimary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        TextButton(
                            onClick = { selectedDateInfo = null },
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Text("Close", fontSize = 11.sp, color = NexoraCyan)
                        }
                    }
                }
            }
        }
    }
}
