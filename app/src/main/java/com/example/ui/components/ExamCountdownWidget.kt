package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Stars
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.constants.AppConstants
import com.example.core.util.ExamCountdownTime
import com.example.core.util.IstTimeUtil
import com.example.ui.animation.staggeredEntrance
import com.example.ui.theme.NexoraBorder
import com.example.ui.theme.NexoraCyan
import com.example.ui.theme.NexoraGold
import com.example.ui.theme.NexoraMagenta
import com.example.ui.theme.NexoraSuccess
import com.example.ui.theme.NexoraSurface
import com.example.ui.theme.NexoraSurfaceElevated
import com.example.ui.theme.NexoraSurfaceVariant
import com.example.ui.theme.NexoraTextMuted
import com.example.ui.theme.NexoraTextPrimary
import com.example.ui.theme.NexoraTextSecondary
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.util.Locale

/**
 * Premium, prominent Exam Countdown Widget for the Home Dashboard.
 *
 * Features:
 * 1. Live IST Clock (Asia/Kolkata UTC+05:30) with real-time seconds tick & pulsing live indicator.
 * 2. Dual Real-Time Exam Countdowns:
 *    - School Exam (22 October 2026) — Phase 1 Priority (66 chapters)
 *    - Board Exam (25 February 2027) — Phase 2 Full Syllabus (94 chapters)
 * 3. DAYS | HOURS | MINUTES | SECONDS breakdown synchronized with the Exam screen.
 * 4. Responsive design with premium light/off-white + navy + cyan/magenta styling.
 * 5. Direct navigation to the full Exam Dashboard.
 */
@Composable
fun ExamCountdownWidget(
    onNavigateToExam: () -> Unit,
    modifier: Modifier = Modifier
) {
    val schoolDate = remember { LocalDate.of(2026, 10, 22) }
    val boardDate = remember { LocalDate.of(2027, 2, 25) }

    var currentTimeStr by remember { mutableStateOf(IstTimeUtil.getFormattedTime12h()) }
    var currentDateStr by remember { mutableStateOf(IstTimeUtil.getFormattedDate()) }
    var schoolCountdown by remember { mutableStateOf(IstTimeUtil.calculateExamCountdown(schoolDate)) }
    var boardCountdown by remember { mutableStateOf(IstTimeUtil.calculateExamCountdown(boardDate)) }

    LaunchedEffect(Unit) {
        while (true) {
            val zdt = IstTimeUtil.getCurrentZonedDateTime()
            currentTimeStr = IstTimeUtil.getFormattedTime12h(zdt)
            currentDateStr = IstTimeUtil.getFormattedDate(zdt)
            schoolCountdown = IstTimeUtil.calculateExamCountdown(schoolDate, zdt)
            boardCountdown = IstTimeUtil.calculateExamCountdown(boardDate, zdt)
            delay(1000L)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "home_ist_pulse")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "home_ist_dot_alpha"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .staggeredEntrance(index = 2)
            .testTag("home_exam_countdown_section"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Section Header with Shortcut
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(NexoraCyan.copy(alpha = 0.15f), CircleShape)
                        .border(1.dp, NexoraCyan.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Timer,
                        contentDescription = "Exam Countdown",
                        tint = NexoraCyan,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Exam Countdowns",
                        color = NexoraTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.3.sp
                    )
                    Text(
                        text = "Real-Time Gujarat Board Countdown",
                        color = NexoraTextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onNavigateToExam() }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
                    .testTag("home_view_exam_dashboard_btn")
            ) {
                Text(
                    text = "Exam Dashboard",
                    color = NexoraCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(3.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = NexoraCyan,
                    modifier = Modifier.size(13.dp)
                )
            }
        }

        // 1. Live IST Clock Card
        NexoraCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToExam() }
                .testTag("home_live_ist_card"),
            cornerRadius = 14.dp,
            contentPadding = 12.dp,
            borderColor = NexoraCyan.copy(alpha = 0.3f),
            containerColor = NexoraSurfaceElevated
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
                            .size(32.dp)
                            .background(NexoraCyan.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Schedule,
                            contentDescription = "Live IST",
                            tint = NexoraCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(NexoraSuccess.copy(alpha = dotAlpha), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "LIVE IST",
                                color = NexoraCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "• Asia/Kolkata",
                                color = NexoraTextMuted,
                                fontSize = 10.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = currentDateStr,
                            color = NexoraTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(NexoraSurfaceVariant)
                        .border(1.dp, NexoraBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = currentTimeStr,
                        color = NexoraTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.testTag("home_live_ist_time_display")
                    )
                }
            }
        }

        // 2. Dual Countdown Cards: School Exam (Phase 1) & Board Exam (Phase 2)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // School Exam Countdown Card (Phase 1)
            CountdownCard(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateToExam() }
                    .testTag("home_school_exam_countdown_card"),
                title = AppConstants.SCHOOL_EXAM_NAME,
                dateText = AppConstants.SCHOOL_EXAM_DATE,
                phaseLabel = "Phase 1",
                syllabusNote = "66 Ch • Oct 2026",
                countdown = schoolCountdown,
                themeColor = NexoraCyan,
                icon = Icons.Outlined.School
            )

            // Board Exam Countdown Card (Phase 2)
            CountdownCard(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateToExam() }
                    .testTag("home_board_exam_countdown_card"),
                title = AppConstants.BOARD_EXAM_NAME,
                dateText = AppConstants.BOARD_EXAM_DATE,
                phaseLabel = "Phase 2",
                syllabusNote = "94 Ch • Feb 2027",
                countdown = boardCountdown,
                themeColor = NexoraMagenta,
                icon = Icons.Outlined.Stars
            )
        }
    }
}

@Composable
private fun CountdownCard(
    title: String,
    dateText: String,
    phaseLabel: String,
    syllabusNote: String,
    countdown: ExamCountdownTime,
    themeColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    NexoraCard(
        modifier = modifier,
        cornerRadius = 16.dp,
        contentPadding = 12.dp,
        borderColor = themeColor.copy(alpha = 0.35f),
        containerColor = NexoraSurface
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(themeColor.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = themeColor,
                    modifier = Modifier.size(15.dp)
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(themeColor.copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = phaseLabel,
                    color = themeColor,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = title,
            color = NexoraTextSecondary,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = dateText,
            color = themeColor,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (countdown.isExamDay) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(themeColor.copy(alpha = 0.15f))
                    .border(1.dp, themeColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "EXAM DAY 🎯",
                    color = themeColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                HomeCountdownUnitBox(
                    value = String.format(Locale.ENGLISH, "%02d", countdown.days),
                    label = "DAYS",
                    color = themeColor,
                    modifier = Modifier.weight(1f)
                )
                HomeCountdownUnitBox(
                    value = String.format(Locale.ENGLISH, "%02d", countdown.hours),
                    label = "HRS",
                    color = themeColor,
                    modifier = Modifier.weight(1f)
                )
                HomeCountdownUnitBox(
                    value = String.format(Locale.ENGLISH, "%02d", countdown.minutes),
                    label = "MIN",
                    color = themeColor,
                    modifier = Modifier.weight(1f)
                )
                HomeCountdownUnitBox(
                    value = String.format(Locale.ENGLISH, "%02d", countdown.seconds),
                    label = "SEC",
                    color = themeColor,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = syllabusNote,
            color = NexoraTextMuted,
            fontSize = 9.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HomeCountdownUnitBox(
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.08f))
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
            .padding(vertical = 4.dp, horizontal = 1.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = NexoraTextPrimary,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(1.dp))
        Text(
            text = label,
            color = color,
            fontSize = 7.5.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1
        )
    }
}
