package com.example.ui.screens

import android.app.TimePickerDialog
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.StudentProfile
import com.example.core.storage.StudentPreferences
import com.example.ui.animation.bounceClick
import com.example.ui.animation.staggeredEntrance
import com.example.ui.components.*
import com.example.ui.theme.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    studentProfile: StudentProfile,
    onBackClick: () -> Unit,
    onSignOut: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isGuest = remember { StudentPreferences.isGuestMode(context) }

    val themeMode by com.example.core.theme.NexoraThemeManager.themeModeFlow.collectAsStateWithLifecycle(
        initialValue = com.example.core.theme.NexoraThemeManager.getThemeMode(context)
    )
    var studyReminders by remember { mutableStateOf(StudentPreferences.isStudyRemindersEnabled(context)) }
    var motivationAlerts by remember { mutableStateOf(StudentPreferences.isMotivationAlertsEnabled(context)) }
    var hapticEnabled by remember { mutableStateOf(StudentPreferences.isHapticEnabled(context)) }
    var showThemePreview by remember { mutableStateOf(false) }
    
    var reminderHour by remember { mutableIntStateOf(StudentPreferences.getReminderTimeHour(context)) }
    var reminderMinute by remember { mutableIntStateOf(StudentPreferences.getReminderTimeMinute(context)) }

    val timePickerDialog = remember {
        TimePickerDialog(context, { _, h, m ->
            reminderHour = h
            reminderMinute = m
            StudentPreferences.setReminderTime(context, h, m)
        }, reminderHour, reminderMinute, false)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .testTag("settings_screen")
    ) {
        // Premium Header
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
                    text = "Settings",
                    color = NexoraTextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "Preferences & App Config",
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
            // Theme Section
            item {
                val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
                SettingsSection(title = "Appearance", icon = Icons.Outlined.Palette, color = NexoraPurple) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ThemeOptionItem(
                            title = "Light (Focus Mode)",
                            subtitle = "Clean white design for daytime study",
                            isSelected = themeMode == "LIGHT",
                            onClick = {
                                if (themeMode != "LIGHT") {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    com.example.core.theme.NexoraThemeManager.setThemeMode(context, "LIGHT")
                                }
                            }
                        )
                        ThemeOptionItem(
                            title = "Deep Navy",
                            subtitle = "Rich midnight navy theme for evening",
                            isSelected = themeMode == "DEEP_NAVY",
                            onClick = {
                                if (themeMode != "DEEP_NAVY") {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    com.example.core.theme.NexoraThemeManager.setThemeMode(context, "DEEP_NAVY")
                                }
                            }
                        )
                        ThemeOptionItem(
                            title = "Navy Night",
                            subtitle = "Pitch dark OLED navy theme for night",
                            isSelected = themeMode == "NAVY_NIGHT",
                            onClick = {
                                if (themeMode != "NAVY_NIGHT") {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    com.example.core.theme.NexoraThemeManager.setThemeMode(context, "NAVY_NIGHT")
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = { showThemePreview = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NexoraPurple),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NexoraPurple.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Outlined.Visibility, contentDescription = "Preview", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Preview All Themes", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }

            // General Preferences (Haptic Feedback) Section
            item {
                SettingsSection(title = "General Preferences", icon = Icons.Outlined.Vibration, color = NexoraCyan) {
                    ToggleItem(
                        title = "Haptic Feedback",
                        checked = hapticEnabled,
                        onCheckedChange = {
                            hapticEnabled = it
                            StudentPreferences.setHapticEnabled(context, it)
                        }
                    )
                }
            }

            // Notifications Section
            item {
                SettingsSection(title = "Notifications", icon = Icons.Outlined.Notifications, color = NexoraGold) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(NexoraSurfaceVariant)
                                .clickable { onNavigateToNotifications() }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Alerts & Reminders",
                                    color = NexoraTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Study slots, summary & motivation",
                                    color = NexoraTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowForwardIos,
                                contentDescription = null,
                                tint = NexoraTextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // Account Section
            item {
                val supabaseRepo = remember { com.example.core.supabase.SupabaseAuthRepository(context) }
                val isSupabaseSignedIn = remember { supabaseRepo.isUserSignedIn() }
                var syncFeedback by remember { mutableStateOf<String?>(null) }
                var isSyncing by remember { mutableStateOf(false) }

                SettingsSection(title = "Account & Cloud Sync", icon = Icons.Outlined.Person, color = NexoraCyan) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(text = "Student Profile", color = NexoraTextSecondary, fontSize = 12.sp)
                        Text(
                            text = if (isGuest) "Guest Student" else studentProfile.studentName.ifBlank { "Class 12 Student" },
                            color = NexoraTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (!isGuest && studentProfile.studentId.isNotBlank()) {
                            Text(
                                text = "Student ID: ${studentProfile.studentId}",
                                color = NexoraCyan,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (studentProfile.email != null && studentProfile.email.isNotBlank()) {
                            Text(
                                text = "Email: ${studentProfile.email}",
                                color = NexoraTextSecondary,
                                fontSize = 12.5.sp
                            )
                        }

                        // Cloud status pill
                        Surface(
                            color = if (isSupabaseSignedIn) Color(0xFFECFDF5) else Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isSupabaseSignedIn) Icons.Outlined.CloudDone else Icons.Outlined.CloudOff,
                                    contentDescription = null,
                                    tint = if (isSupabaseSignedIn) Color(0xFF059669) else Color(0xFF64748B),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isSupabaseSignedIn) "Supabase Account Active (RLS Protected)" else "Offline / Guest Profile",
                                    fontSize = 11.5.sp,
                                    color = if (isSupabaseSignedIn) Color(0xFF065F46) else Color(0xFF475569),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        if (syncFeedback != null) {
                            Text(
                                text = syncFeedback!!,
                                color = NexoraCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (isSupabaseSignedIn) {
                            OutlinedButton(
                                onClick = {
                                    isSyncing = true
                                    syncFeedback = null
                                    coroutineScope.launch {
                                        try {
                                            com.example.core.repository.StudyPlannerRepository.syncWithSupabase(context)
                                            supabaseRepo.syncProfileToCloud(studentProfile)
                                            syncFeedback = "Study data synchronized with Supabase!"
                                        } catch (e: Exception) {
                                            syncFeedback = "Sync: ${e.message}"
                                        } finally {
                                            isSyncing = false
                                        }
                                    }
                                },
                                enabled = !isSyncing,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Outlined.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Sync Now with Cloud", fontSize = 13.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        NexoraSecondaryButton(
                            text = if (isGuest) "Sign In with Supabase" else "Sign Out from Supabase",
                            onClick = {
                                coroutineScope.launch {
                                    supabaseRepo.signOut()
                                    StudentPreferences.setGuestMode(context, false)
                                    onSignOut()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = if (isGuest) NexoraSurfaceVariant else NexoraRose,
                            contentColor = if (isGuest) NexoraTextPrimary else NexoraError
                        )
                    }
                }
            }

            item {
                NexoraCreatorCredit()
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        if (showThemePreview) {
            ThemePreviewDialog(
                currentThemeMode = themeMode,
                onSelectTheme = { mode ->
                    StudentPreferences.setThemeMode(context, mode)
                },
                onDismiss = { showThemePreview = false }
            )
        }
    }
}

@Composable
private fun SettingsSection(
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
private fun ThemeOptionItem(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) NexoraCyan.copy(alpha = 0.1f) else NexoraSurfaceVariant)
            .border(1.dp, if (isSelected) NexoraCyan else Color.Transparent, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = title, color = if (isSelected) NexoraCyan else NexoraTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(text = subtitle, color = NexoraTextSecondary, fontSize = 11.sp)
        }
        if (isSelected) {
            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = NexoraCyan, modifier = Modifier.size(20.dp))
        } else {
            Box(modifier = Modifier.size(20.dp).border(1.5.dp, NexoraBorder, CircleShape))
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

@Composable
private fun ThemePreviewDialog(
    currentThemeMode: String,
    onSelectTheme: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var previewTheme by remember { mutableStateOf(currentThemeMode) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Theme Live Preview",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "See how cards, buttons, and text fields render in each theme before applying.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("LIGHT" to "Light", "DEEP_NAVY" to "Deep Navy", "NAVY_NIGHT" to "Night").forEach { (mode, label) ->
                        FilterChip(
                            selected = previewTheme == mode,
                            onClick = { previewTheme = mode },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }

                NexoraTheme(themeMode = previewTheme) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Sample Card Title",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Card Content with M3 ColorScheme", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                                }
                            }
                            Button(
                                onClick = {},
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Primary Action Button", color = MaterialTheme.colorScheme.onPrimary, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSelectTheme(previewTheme)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = NexoraPurple)
            ) {
                Text("Apply Theme", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

