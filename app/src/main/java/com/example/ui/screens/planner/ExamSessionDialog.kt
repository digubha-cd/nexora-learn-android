package com.example.ui.screens.planner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
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
import com.example.core.model.ExamItem
import com.example.core.model.ExamStatus
import com.example.ui.components.NexoraCard
import com.example.ui.components.NexoraPrimaryButton
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Exam Session Dialog providing:
 * 1. REAL-TIME EXAM COUNTDOWN (DAYS | HOURS | MINUTES | SECONDS) with live second ticks.
 * 2. LIVE NORMAL CLOCK card displaying current local time, date, and timezone.
 */
@Composable
fun ExamSessionDialog(
    exam: ExamItem,
    onDismiss: () -> Unit,
    onEdit: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var is24Hour by remember { mutableStateOf(com.example.core.storage.StudentPreferences.is24HourFormat(context)) }
    var now by remember { mutableStateOf(com.example.core.util.IstTimeUtil.getCurrentZonedDateTime()) }

    LaunchedEffect(Unit) {
        while (true) {
            now = com.example.core.util.IstTimeUtil.getCurrentZonedDateTime()
            delay(1000L)
        }
    }

    val examTime = com.example.core.util.IstTimeUtil.parseExamTime(exam.startTime)
    val examEndTime = com.example.core.util.IstTimeUtil.parseExamEndTime(exam.endTime)

    val countdownTime = com.example.core.util.IstTimeUtil.calculateExamCountdown(
        targetDate = exam.date,
        currentZdt = now,
        targetTime = examTime,
        targetEndTime = examEndTime
    )

    val isCompleted = countdownTime.isCompleted
    val isExamDay = countdownTime.isExamDay

    val days = countdownTime.days
    val hours = countdownTime.hours
    val minutes = countdownTime.minutes
    val seconds = countdownTime.seconds

    val liveTimeString = com.example.core.util.IstTimeUtil.getFormattedTime(now, is24Hour)
    val liveDateString = com.example.core.util.IstTimeUtil.getFormattedFullDate(now)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = exam.name,
                        color = NexoraTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${exam.type.label} • ${exam.dateFormatted}",
                        color = NexoraCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.Close, contentDescription = "Close", tint = NexoraTextSecondary)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. EXAM COUNTDOWN HEADING & CARDS
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "EXAM COUNTDOWN",
                        color = NexoraTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (exam.startTime.isNullOrBlank()) "Default time: 12:00 AM IST (Asia/Kolkata)" else "Exam time: ${exam.startTime} IST (Asia/Kolkata)",
                        color = NexoraTextSecondary,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (isCompleted) {
                        NexoraCard(
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = NexoraSuccess.copy(alpha = 0.15f),
                            borderColor = NexoraSuccess
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "EXAM COMPLETED",
                                    color = NexoraSuccess,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 2.sp
                                )
                            }
                        }
                    } else if (isExamDay) {
                        NexoraCard(
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = NexoraGold.copy(alpha = 0.15f),
                            borderColor = NexoraGold
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "EXAM DAY / EXAM STARTED",
                                    color = NexoraGold,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.5.sp
                                )
                            }
                        }
                    } else {
                        // Four live countdown values: DAYS | HOURS | MINUTES | SECONDS
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CountdownBox(value = String.format(Locale.ENGLISH, "%02d", days), label = "DAYS", modifier = Modifier.weight(1f))
                            CountdownBox(value = String.format(Locale.ENGLISH, "%02d", hours), label = "HOURS", modifier = Modifier.weight(1f))
                            CountdownBox(value = String.format(Locale.ENGLISH, "%02d", minutes), label = "MINS", modifier = Modifier.weight(1f))
                            CountdownBox(value = String.format(Locale.ENGLISH, "%02d", seconds), label = "SECS", modifier = Modifier.weight(1f))
                        }
                    }
                }

                // 2. LIVE NORMAL CLOCK CARD
                NexoraCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = NexoraSurfaceVariant,
                    borderColor = NexoraBorder,
                    cornerRadius = 20.dp,
                    contentPadding = 16.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Schedule, null, tint = NexoraCyan, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "CURRENT TIME",
                                    color = NexoraCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                            }
                            TextButton(
                                onClick = {
                                    val newFormat = !is24Hour
                                    is24Hour = newFormat
                                    com.example.core.storage.StudentPreferences.set24HourFormat(context, newFormat)
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (is24Hour) "24H" else "12H AM/PM",
                                    color = NexoraCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = liveTimeString,
                            color = NexoraTextPrimary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = liveDateString,
                            color = NexoraTextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Indian Standard Time (IST) • Asia/Kolkata",
                            color = NexoraTextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onEdit) {
                    Text("Edit Exam Details", color = NexoraCyan, fontWeight = FontWeight.Bold)
                }
                NexoraPrimaryButton(
                    text = "Done",
                    onClick = onDismiss
                )
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun CountdownBox(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    NexoraCard(
        modifier = modifier,
        containerColor = NexoraSurfaceElevated,
        cornerRadius = 14.dp,
        contentPadding = 10.dp,
        borderColor = NexoraBorder
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                color = NexoraTextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                color = NexoraTextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}
