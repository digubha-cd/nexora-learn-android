package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.DayStudyActivity
import com.example.core.model.Subject
import com.example.core.repository.StudyPlannerRepository
import com.example.ui.theme.*
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyStudyDetailsBottomSheet(
    dayActivity: DayStudyActivity,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val details = remember(dayActivity.date) {
        StudyPlannerRepository.getStudyDetailsForDate(dayActivity.date)
    }
    val tasks = details.first
    val sessions = details.second

    val fullDateFormatted = remember(dayActivity.date) {
        val formatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH)
        dayActivity.date.format(formatter)
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = NexoraBackground,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = NexoraTextMuted.copy(alpha = 0.4f),
                width = 40.dp,
                height = 4.dp
            )
        },
        modifier = modifier.testTag("daily_study_details_bottom_sheet")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row: Date & Close Button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = fullDateFormatted,
                                color = NexoraTextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (dayActivity.isToday) {
                                Box(
                                    modifier = Modifier
                                        .background(NexoraGold, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "TODAY",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Activity Level Badge
                        val levelBadgeColor = when (dayActivity.activityLevel) {
                            4 -> NexoraGold
                            3 -> NexoraElectricBlue
                            2 -> NexoraCyan
                            1 -> Color(0xFF38BDF8)
                            else -> NexoraTextMuted
                        }
                        val levelText = when (dayActivity.activityLevel) {
                            4 -> "Level 4 • High Study Activity 🔥"
                            3 -> "Level 3 • Medium Study Activity ⚡"
                            2 -> "Level 2 • Low Study Activity 🎯"
                            1 -> "Level 1 • Very Low Activity 🌱"
                            else -> "Level 0 • No Study Activity Recorded"
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(levelBadgeColor)
                            )
                            Text(
                                text = levelText,
                                color = levelBadgeColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier
                            .size(36.dp)
                            .background(NexoraSurfaceVariant, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = NexoraTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Summary 3-Card Metrics
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val hours = dayActivity.durationMinutes / 60
                    val mins = dayActivity.durationMinutes % 60
                    val timeFormatted = when {
                        hours > 0 && mins > 0 -> "${hours}h ${mins}m"
                        hours > 0 -> "${hours}h"
                        else -> "${mins}m"
                    }

                    MetricMiniCard(
                        icon = Icons.Default.Timer,
                        label = "Study Time",
                        value = timeFormatted,
                        accentColor = NexoraCyan,
                        modifier = Modifier.weight(1f)
                    )
                    MetricMiniCard(
                        icon = Icons.Default.CheckCircle,
                        label = "Tasks Done",
                        value = "${dayActivity.completedTaskCount}",
                        accentColor = NexoraSuccess,
                        modifier = Modifier.weight(1f)
                    )
                    MetricMiniCard(
                        icon = Icons.Default.Whatshot,
                        label = "Sessions",
                        value = "${dayActivity.sessionCount}",
                        accentColor = NexoraGold,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Focus Sessions Section
            if (sessions.isNotEmpty()) {
                item {
                    Text(
                        text = "Recorded Focus Sessions (${sessions.size})",
                        color = NexoraTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                items(sessions) { session ->
                    val subjectName = Subject.OFFICIAL_SUBJECTS.find { it.id == session.subjectId }?.name
                        ?: session.subjectId.replaceFirstChar { it.uppercase() }
                    val subjectColor = getHeatmapSubjectColor(session.subjectId)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(NexoraSurface)
                            .border(1.dp, NexoraBorder, RoundedCornerShape(14.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(subjectColor)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = subjectName,
                                    color = NexoraTextPrimary,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Focus Session • ${session.durationMinutes} min",
                                    color = NexoraTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .background(subjectColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "+${session.durationMinutes}m",
                                color = subjectColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Completed Tasks Section
            val completedTasks = tasks.filter { it.isCompleted }
            if (completedTasks.isNotEmpty()) {
                item {
                    Text(
                        text = "Completed Tasks (${completedTasks.size})",
                        color = NexoraTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                items(completedTasks) { task ->
                    val subjectName = Subject.OFFICIAL_SUBJECTS.find { it.id == task.subjectId }?.name
                        ?: task.subjectId.replaceFirstChar { it.uppercase() }
                    val subjectColor = getHeatmapSubjectColor(task.subjectId)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(NexoraSurface)
                            .border(1.dp, NexoraBorder, RoundedCornerShape(14.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = NexoraSuccess,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = task.title,
                                    color = NexoraTextPrimary,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "$subjectName • ${task.durationMinutes}m",
                                    color = NexoraTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            // Zero State when neither sessions nor tasks exist
            if (sessions.isEmpty() && completedTasks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(NexoraSurfaceVariant.copy(alpha = 0.5f))
                            .border(1.dp, NexoraBorder, RoundedCornerShape(16.dp))
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("🌱", fontSize = 28.sp)
                            Text(
                                text = "No Study Activity Recorded",
                                color = NexoraTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Start a focus timer session or check off study tasks in the planner to build your daily streak!",
                                color = NexoraTextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onDismissRequest,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NexoraSurfaceElevated,
                        contentColor = NexoraTextPrimary
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .border(1.dp, NexoraBorder, RoundedCornerShape(14.dp))
                ) {
                    Text(
                        text = "Done",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricMiniCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(NexoraSurface)
            .border(1.dp, NexoraBorder, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = label,
                    color = NexoraTextSecondary,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = value,
                color = NexoraTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

private fun getHeatmapSubjectColor(subjectId: String): Color {
    return when (subjectId) {
        "accounts" -> NexoraGold
        "economics" -> NexoraPink
        "ba" -> NexoraMagenta
        "stat" -> NexoraCyan
        "sp_cc" -> NexoraPurple
        "english" -> NexoraElectricBlue
        "gujarati" -> NexoraCyan
        else -> NexoraCyan
    }
}
