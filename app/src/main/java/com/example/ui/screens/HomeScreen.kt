package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.model.StudentProfile
import com.example.core.model.Subject
import com.example.core.repository.StudyPlannerRepository
import com.example.core.repository.SyllabusRepository
import com.example.ui.animation.bounceClick
import com.example.ui.animation.popIn
import com.example.ui.components.NexoraLogo
import com.example.ui.components.NexoraCard
import com.example.ui.components.NexoraTargetBadge
import com.example.ui.components.NexoraStatCard
import com.example.ui.components.StudyStreakCard
import com.example.ui.components.SmartStudyRemindersCard
import com.example.ui.components.DailyStudyTipsCard
import com.example.ui.components.YearlyStudyHeatmapCard
import com.example.ui.components.Past7DaysActivityCard
import com.example.ui.components.WeeklyInsightsCard
import com.example.ui.components.SyllabusProgressTrackerCard
import com.example.ui.components.NexoraCreatorCredit
import com.example.ui.components.DailyStudyDetailsBottomSheet
import com.example.ui.components.CompactWeeklyStreakCalendar
import com.example.ui.components.DailyMotivationCard
import com.example.ui.components.NexoraAvatar
import com.example.ui.components.WeeklySubjectHoursCard
import com.example.ui.components.WeeklyStudyGoalCard
import com.example.ui.theme.*
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    studentProfile: StudentProfile,
    onSubjectClick: (Subject) -> Unit = {},
    onNavigateToSubjects: () -> Unit = {},
    onNavigateToPlanner: () -> Unit = {},
    onOpenMarksCalculator: () -> Unit = {},
    onOpenNexoraAi: () -> Unit = {},
    onOpenFriends: () -> Unit = {},
    onOpenChat: () -> Unit = {},
    onOpenLeaderboard: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToRoadmap: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val tasks by StudyPlannerRepository.tasks.collectAsStateWithLifecycle()
    val sessionHistory by com.example.core.timer.StudyTimerManager.sessionHistory.collectAsStateWithLifecycle(initialValue = emptyList())
    val completedChapterIds by SyllabusRepository.completedChapterIds.collectAsStateWithLifecycle()
    val displayName = studentProfile.studentName.ifBlank { "Student" }
    val activityList = remember(tasks, sessionHistory) { StudyPlannerRepository.getPast7DaysActivity() }
    val streak = remember(tasks, sessionHistory) { StudyPlannerRepository.getStudyStreak() }
    val heatmapActivity = remember(tasks, sessionHistory) { StudyPlannerRepository.get365DaysHeatmapData() }
    val weeklySummary = remember(tasks, sessionHistory) { StudyPlannerRepository.getWeeklySummaryStats() }
    var selectedDayForDetails by remember { mutableStateOf<com.example.core.model.DayStudyActivity?>(null) }
    var showChatComingSoonDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("home_screen_root")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Official Logo + NEXORA LEARN and Subtitle
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NexoraLogo(emblemSize = 40.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "NEXORA LEARN",
                                color = NexoraTextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.8.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Premium Study Suite",
                                color = NexoraCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.4.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Right: Actions (Avatar, Streak, Settings)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Profile DP Avatar
                        val avatarUrl = studentProfile.photoUrl
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .border(1.dp, NexoraCyan, CircleShape)
                                .clickable { onNavigateToProfile() }
                                .testTag("header_profile_avatar")
                        ) {
                            NexoraAvatar(
                                photoUrl = avatarUrl,
                                name = studentProfile.studentName,
                                avatarColorHex = studentProfile.avatarColorHex,
                                size = 36.dp
                            )
                        }

                        Surface(
                            color = NexoraGold.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NexoraGold.copy(alpha = 0.4f)),
                            modifier = Modifier.testTag("header_streak_badge")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("🔥", fontSize = 12.sp)
                                Text(
                                    text = "${streak.currentStreak}d",
                                    color = NexoraTextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        val haptic = LocalHapticFeedback.current
                        var rotationDegrees by remember { mutableStateOf(0f) }
                        val animatedRotation by animateFloatAsState(
                            targetValue = rotationDegrees,
                            animationSpec = spring(stiffness = Spring.StiffnessLow),
                            label = "SettingsRotation"
                        )

                        IconButton(
                            onClick = {
                                rotationDegrees += 90f
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                onOpenSettings()
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .background(NexoraSurfaceVariant, CircleShape)
                                .border(1.dp, NexoraBorder, CircleShape)
                                .graphicsLayer(rotationZ = animatedRotation)
                        ) {
                            Icon(
                                Icons.Outlined.Settings,
                                contentDescription = "Settings",
                                tint = NexoraTextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // 2. Welcome
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Good day,",
                            color = NexoraTextSecondary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (studentProfile.studentName.isBlank()) "Hello, Student!" else "Hello, ${studentProfile.studentName}",
                                color = NexoraTextPrimary,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("🎓", fontSize = 18.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(NexoraCyan.copy(alpha = 0.12f))
                            .border(1.5.dp, NexoraCyan.copy(alpha = 0.35f), CircleShape)
                            .clickable { onNavigateToProfile() },
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = "Profile Avatar",
                            tint = NexoraCyan,
                            modifier = Modifier
                                .size(40.dp)
                                .padding(top = 4.dp)
                        )
                    }
                }
            }


            // Statistics Card (Stats only)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val totalChapters = SyllabusRepository.getTotalChaptersCount()
                    val completedChapters = completedChapterIds.size
                    val chaptersLeft = (totalChapters - completedChapters).coerceAtLeast(0)
                    val overallProgress = if (totalChapters > 0) {
                        (completedChapters.toFloat() / totalChapters.toFloat() * 100f).toInt()
                    } else 0

                    NexoraStatCard(Icons.Filled.MenuBook, "7", "Subjects", NexoraCyan, NexoraCyan.copy(alpha = 0.1f), Modifier.weight(1f))
                    NexoraStatCard(Icons.Filled.Whatshot, chaptersLeft.toString(), "Left", NexoraGold, NexoraGold.copy(alpha = 0.1f), Modifier.weight(1f))
                    NexoraStatCard(Icons.Filled.Stars, "$overallProgress%", "Done", NexoraSuccess, NexoraSuccess.copy(alpha = 0.1f), Modifier.weight(1f))
                }
            }

            // 4. Compact 7-Day Streak Calendar
            item {
                NexoraCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .popIn(delayMs = 40L, initialScale = 0.92f),
                    cornerRadius = 24.dp,
                    contentPadding = 16.dp,
                    borderColor = NexoraCyan.copy(alpha = 0.2f)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🔥", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "WEEKLY STREAK",
                                color = NexoraTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                        }
                        CompactWeeklyStreakCalendar(streak = streak)
                    }
                }
            }


            // 5. Daily Motivational Quote
            item {
                DailyMotivationCard()
            }

            // 6. Weekly Target Study Goal Card & Progress
            item {
                WeeklyStudyGoalCard()
            }

            // 6. Quick Access
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Quick Access",
                            color = NexoraTextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            HomeQuickAccessCard(Icons.Filled.CalendarMonth, "Planner", NexoraCyan, NexoraCyan.copy(alpha = 0.1f), Modifier.weight(1f), onNavigateToPlanner)
                            HomeQuickAccessCard(Icons.Filled.MenuBook, "Subjects", NexoraPurple, NexoraPurple.copy(alpha = 0.1f), Modifier.weight(1f), onNavigateToSubjects)
                            HomeQuickAccessCard(Icons.Filled.Calculate, "Marks Calc", NexoraElectricBlue, NexoraElectricBlue.copy(alpha = 0.1f), Modifier.weight(1f), onOpenMarksCalculator)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            HomeQuickAccessCard(Icons.Filled.People, "Friends", NexoraEmerald, NexoraEmerald.copy(alpha = 0.1f), Modifier.weight(1f), onOpenFriends)
                            HomeQuickAccessCard(Icons.Filled.Forum, "Chat", NexoraCyan, NexoraCyan.copy(alpha = 0.1f), Modifier.weight(1f)) { showChatComingSoonDialog = true }
                            HomeQuickAccessCard(Icons.Filled.EmojiEvents, "Leaderboard", NexoraPurple, NexoraPurple.copy(alpha = 0.1f), Modifier.weight(1f), onOpenLeaderboard)
                            HomeQuickAccessCard(Icons.Filled.Person, "Profile", NexoraElectricBlue, NexoraElectricBlue.copy(alpha = 0.1f), Modifier.weight(1f), onNavigateToProfile)
                        }
                    }
                }
            }

            // 7. Todo Preview
            item {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Upcoming Tasks",
                            color = NexoraTextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        TextButton(onClick = onNavigateToPlanner) {
                            Text("View All")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val pendingTasks = tasks.filter { !it.isCompleted }.take(3)
                    if (pendingTasks.isEmpty()) {
                        Text("No tasks yet. Add your first task in Planner.", color = NexoraTextSecondary, fontSize = 14.sp)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            pendingTasks.forEach { task ->
                                NexoraCard(modifier = Modifier.fillMaxWidth()) {
                                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(task.title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                        if (task.scheduledDateStr.isNotBlank()) {
                                            Text(task.scheduledDateFormatted, fontSize = 12.sp, color = NexoraTextSecondary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }



            // 5. Daily Study Tips
            item {
                DailyStudyTipsCard()
            }


            // 6. Heatmap
            item {
                YearlyStudyHeatmapCard(
                    activityList = heatmapActivity,
                    onDayClick = { day -> selectedDayForDetails = day }
                )
            }

            // 9. Syllabus
            item {
                SyllabusProgressTrackerCard(
                    onNavigateToSubjects = onNavigateToRoadmap,
                    onSubjectClick = onSubjectClick
                )
            }

            // Moved Weekly Study Breakdown to the bottom
            item {
                WeeklySubjectHoursCard(weeklySummary = weeklySummary)
            }

            // Footer at the absolute bottom
            item { NexoraCreatorCredit() }

            item { Spacer(modifier = Modifier.height(100.dp)) }
        }

        selectedDayForDetails?.let { day ->
            DailyStudyDetailsBottomSheet(
                dayActivity = day,
                onDismissRequest = { selectedDayForDetails = null }
            )
        }

        if (showChatComingSoonDialog) {
            AlertDialog(
                onDismissRequest = { showChatComingSoonDialog = false },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .wrapContentHeight(),
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    color = NexoraSurface,
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, NexoraBorder.copy(alpha = 0.75f)),
                    shadowElevation = 16.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(NexoraCyan.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Forum,
                                contentDescription = null,
                                tint = NexoraCyan,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "💬 Chat",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = NexoraTextPrimary
                            )
                            Text(
                                text = "✨ Coming Soon...",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = NexoraCyan
                            )
                        }

                        Text(
                            text = "Chat feature will be available in our next update!",
                            fontSize = 13.sp,
                            color = NexoraTextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = { showChatComingSoonDialog = false },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NexoraElectricBlue,
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = "Got it",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeQuickAccessCard(
    icon: ImageVector,
    label: String,
    iconColor: Color,
    bgColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    NexoraCard(
        modifier = modifier.bounceClick { onClick() },
        cornerRadius = 14.dp,
        contentPadding = 6.dp,
        elevation = 1.dp
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.size(36.dp).background(bgColor, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, label, tint = iconColor, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                color = NexoraTextPrimary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}
