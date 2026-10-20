package com.example.ui.screens

import android.app.TimePickerDialog
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
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
import com.example.core.storage.StudentPreferences
import com.example.ui.animation.bounceClick
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun NotificationSettingsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    var studyReminders by remember { mutableStateOf(StudentPreferences.isStudyRemindersEnabled(context)) }
    var morningEnabled by remember { mutableStateOf(StudentPreferences.isMorningReminderEnabled(context)) }
    var eveningEnabled by remember { mutableStateOf(StudentPreferences.isEveningReminderEnabled(context)) }
    var nightEnabled by remember { mutableStateOf(StudentPreferences.isNightReminderEnabled(context)) }
    
    var weeklySummary by remember { mutableStateOf(StudentPreferences.isWeeklySummaryEnabled(context)) }
    var motivationAlerts by remember { mutableStateOf(StudentPreferences.isMotivationAlertsEnabled(context)) }
    var examAlerts by remember { mutableStateOf(StudentPreferences.isExamAlertsEnabled(context)) }
    
    var summaryHour by remember { mutableIntStateOf(StudentPreferences.getSummaryTimeHour(context)) }
    var summaryMinute by remember { mutableIntStateOf(StudentPreferences.getSummaryTimeMinute(context)) }

    val summaryTimePickerDialog = remember {
        TimePickerDialog(context, { _, h, m ->
            summaryHour = h
            summaryMinute = m
            StudentPreferences.setSummaryTime(context, h, m)
        }, summaryHour, summaryMinute, false)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NexoraBackground)
            .statusBarsPadding()
            .testTag("notification_settings_screen")
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(44.dp)
                    .background(NexoraSurfaceVariant, CircleShape)
                    .bounceClick { onBackClick() }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = NexoraTextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = "Notifications",
                    color = NexoraTextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "Manage your reminders",
                    color = NexoraTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Study Reminder Slots
            item {
                NotificationSection(
                    title = "Study Session Reminders",
                    icon = Icons.Outlined.Timer,
                    color = NexoraCyan
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        ToggleItem(
                            title = "Enable Study Reminders",
                            checked = studyReminders,
                            onCheckedChange = {
                                studyReminders = it
                                StudentPreferences.setStudyRemindersEnabled(context, it)
                            }
                        )
                        
                        if (studyReminders) {
                            HorizontalDivider(color = NexoraBorder, thickness = 1.dp)
                            
                            ToggleItem(
                                title = "Morning (7:00 AM)",
                                checked = morningEnabled,
                                onCheckedChange = {
                                    morningEnabled = it
                                    StudentPreferences.setMorningReminderEnabled(context, it)
                                }
                            )
                            ToggleItem(
                                title = "Evening (5:00 PM)",
                                checked = eveningEnabled,
                                onCheckedChange = {
                                    eveningEnabled = it
                                    StudentPreferences.setEveningReminderEnabled(context, it)
                                }
                            )
                            ToggleItem(
                                title = "Night (9:00 PM)",
                                checked = nightEnabled,
                                onCheckedChange = {
                                    nightEnabled = it
                                    StudentPreferences.setNightReminderEnabled(context, it)
                                }
                            )
                        }
                    }
                }
            }

            // Weekly Summary
            item {
                NotificationSection(
                    title = "Weekly Performance Summary",
                    icon = Icons.Outlined.Assessment,
                    color = NexoraPurple
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        ToggleItem(
                            title = "Weekly Summary Report",
                            checked = weeklySummary,
                            onCheckedChange = {
                                weeklySummary = it
                                StudentPreferences.setWeeklySummaryEnabled(context, it)
                            }
                        )
                        
                        if (weeklySummary) {
                            val time = String.format("%02d:%02d", summaryHour, summaryMinute)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .bounceClick { summaryTimePickerDialog.show() }
                                    .background(NexoraSurfaceVariant, RoundedCornerShape(12.dp))
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = "Delivery Time", color = NexoraTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Text(text = "Sent every Sunday", color = NexoraTextSecondary, fontSize = 12.sp)
                                }
                                Text(text = time, color = NexoraPurple, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }

            // Other Alerts
            item {
                NotificationSection(
                    title = "Other Alerts",
                    icon = Icons.Outlined.Campaign,
                    color = NexoraGold
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        ToggleItem(
                            title = "Motivational Quotes",
                            checked = motivationAlerts,
                            onCheckedChange = {
                                motivationAlerts = it
                                StudentPreferences.setMotivationAlertsEnabled(context, it)
                            }
                        )
                        ToggleItem(
                            title = "Exam Deadlines",
                            checked = examAlerts,
                            onCheckedChange = {
                                examAlerts = it
                                StudentPreferences.setExamAlertsEnabled(context, it)
                            }
                        )
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun NotificationSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    content: @Composable () -> Unit
) {
    NexoraCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 28.dp,
        contentPadding = 20.dp
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(36.dp).background(color.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = title, color = NexoraTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(modifier = Modifier.height(20.dp))
            content()
        }
    }
}

@Composable
private fun ToggleItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, color = NexoraTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = NexoraCyan,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = NexoraBorder
            )
        )
    }
}
