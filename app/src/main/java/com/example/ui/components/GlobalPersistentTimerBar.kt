package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.timer.StudyTimerState
import com.example.ui.theme.NexoraBorder
import com.example.ui.theme.NexoraCyan
import com.example.ui.theme.NexoraGold
import com.example.ui.theme.NexoraSuccess
import com.example.ui.theme.NexoraSurfaceElevated
import com.example.ui.theme.NexoraTextPrimary
import com.example.ui.theme.NexoraTextSecondary

/**
 * Compact Persistent Floating Timer Bar.
 * Shown when a study session is active (running or paused).
 */
@Composable
fun GlobalPersistentTimerBar(
    timerState: StudyTimerState,
    onPauseTimer: () -> Unit,
    onResumeTimer: () -> Unit,
    onClickBar: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = timerState.isRunning || timerState.isPaused,
        enter = fadeIn() + slideInVertically { it },
        exit = fadeOut() + slideOutVertically { it },
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(NexoraSurfaceElevated)
                .border(
                    1.dp,
                    if (timerState.isRunning) NexoraCyan.copy(alpha = 0.5f) else NexoraGold.copy(alpha = 0.5f),
                    RoundedCornerShape(16.dp)
                )
                .clickable { onClickBar() }
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .testTag("global_persistent_timer_bar")
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
                    // Mini Circular Progress
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { 1f },
                            modifier = Modifier.size(34.dp),
                            color = if (timerState.isRunning) NexoraCyan else NexoraGold,
                            trackColor = Color(0xFFE2E8F0),
                            strokeWidth = 3.5.dp
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    if (timerState.isRunning) NexoraCyan else NexoraGold,
                                    CircleShape
                                )
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = timerState.subjectName,
                                color = NexoraTextPrimary,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (timerState.isRunning) NexoraCyan.copy(alpha = 0.12f)
                                        else NexoraGold.copy(alpha = 0.12f),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = if (timerState.isRunning) "FOCUSING" else "PAUSED",
                                    color = if (timerState.isRunning) NexoraCyan else NexoraGold,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Text(
                            text = "Elapsed: ${timerState.formattedElapsedTime}",
                            color = NexoraTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Mini Play / Pause Action Button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (timerState.isRunning) NexoraGold else NexoraCyan)
                        .clickable {
                            if (timerState.isRunning) onPauseTimer() else onResumeTimer()
                        }
                        .testTag("global_timer_toggle_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (timerState.isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = "Toggle Timer",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
