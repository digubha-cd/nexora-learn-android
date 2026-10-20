package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.StudyStreak
import com.example.ui.animation.popIn
import com.example.ui.theme.NexoraCyan
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * A compact row of 7 days representing the study streak for the current week.
 * Fully theme-aware across Light, Deep Navy, and Navy Night.
 */
@Composable
fun CompactWeeklyStreakCalendar(
    streak: StudyStreak,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val startOfWeek = today.minusDays(today.dayOfWeek.value.toLong() - 1)
    val days = (0..6).map { startOfWeek.plusDays(it.toLong()) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        days.forEachIndexed { index, date ->
            val isToday = date == today
            val isCompleted = streak.streakDays.contains(date) || (isToday && streak.isCompletedToday)
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.popIn(delayMs = (index * 40L) + 60L, initialScale = 0.78f)
            ) {
                Text(
                    text = date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    fontSize = 11.sp,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                    color = if (isToday) NexoraCyan else MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(
                            color = if (isCompleted) NexoraCyan else Color.Transparent,
                            shape = CircleShape
                        )
                        .border(
                            width = 1.dp,
                            color = if (isToday && !isCompleted) NexoraCyan else if (isCompleted) NexoraCyan else MaterialTheme.colorScheme.outline,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Text(
                            text = date.dayOfMonth.toString(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isToday) NexoraCyan else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
