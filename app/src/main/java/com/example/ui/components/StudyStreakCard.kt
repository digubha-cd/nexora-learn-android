package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.StudyStreak
import com.example.ui.theme.*

/**
 * Responsive Study Streak Card with rolling number animation, study points indicator,
 * and celebratory badge for daily study goal completion on the Home Dashboard.
 * Fully theme-aware across Light, Deep Navy, and Navy Night.
 */
@Composable
fun StudyStreakCard(
    streak: StudyStreak,
    modifier: Modifier = Modifier,
    points: Int = 0
) {
    var targetStreak by remember { mutableStateOf(0) }
    LaunchedEffect(streak.currentStreak) {
        targetStreak = streak.currentStreak
    }
    val animatedStreak by animateIntAsState(
        targetValue = targetStreak,
        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
        label = "StreakRollingAnimation"
    )

    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    NexoraCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("study_streak_card"),
        containerColor = if (streak.isCompletedToday) {
            if (isDark) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else Color(0xFFFFF7ED)
        } else {
            MaterialTheme.colorScheme.surface
        },
        borderColor = if (streak.isCompletedToday) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        } else {
            MaterialTheme.colorScheme.outline
        },
        cornerRadius = 20.dp,
        contentPadding = 16.dp,
        elevation = 1.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFFF97316),
                                        Color(0xFFEF4444)
                                    )
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Whatshot,
                            contentDescription = "Streak",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "$animatedStreak",
                                color = if (streak.isCompletedToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Black,
                                lineHeight = 24.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (animatedStreak == 1) "Day Streak" else "Days Streak",
                                color = if (streak.isCompletedToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 18.sp
                            )
                        }
                        Text(
                            text = if (streak.isCompletedToday) "Daily Goal Achieved • On Fire 🔥" else "Daily Target Pending",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (points > 0) {
                        Box(
                            modifier = Modifier
                                .background(
                                    NexoraGold.copy(alpha = 0.15f),
                                    RoundedCornerShape(10.dp)
                                )
                                .border(
                                    1.dp,
                                    NexoraGold.copy(alpha = 0.35f),
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "🏆 $points pts",
                                color = NexoraGold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .background(
                                color = if (streak.isCompletedToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .border(
                                1.dp,
                                if (streak.isCompletedToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (streak.isCompletedToday) "🔥 ON FIRE" else "⚡ ACTIVE",
                            color = if (streak.isCompletedToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .border(
                        0.5.dp,
                        MaterialTheme.colorScheme.outline,
                        RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (streak.isCompletedToday) "🎉" else "🎯",
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (streak.isCompletedToday)
                            "Great job! Your streak is active and your Class 12 study momentum is blazing."
                        else
                            "Complete at least one study task or focus session today to keep your streak alive!",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            CompactWeeklyStreakCalendar(streak = streak)
        }
    }
}
