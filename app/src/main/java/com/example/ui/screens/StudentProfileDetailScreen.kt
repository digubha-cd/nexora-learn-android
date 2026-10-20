package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.StudentProfile
import com.example.core.util.StudyBadgeProvider
import com.example.ui.animation.bounceClick
import com.example.ui.components.NexoraAvatar
import com.example.ui.theme.NexoraCyan
import java.text.NumberFormat
import java.util.Locale

@Composable
fun StudentProfileDetailScreen(
    profile: StudentProfile,
    rank: Int?,
    onBackClick: () -> Unit
) {
    BackHandler {
        onBackClick()
    }

    val rankAccentColor = when (rank) {
        1 -> Color(0xFFF59E0B)
        2 -> Color(0xFFCBD5E1)
        3 -> Color(0xFFD97706)
        in 4..10 -> NexoraCyan
        else -> Color(0xFF94A3B8)
    }

    val rankTitle = when (rank) {
        1 -> "👑 1st Rank"
        2 -> "🥈 2nd Rank"
        3 -> "🥉 3rd Rank"
        null -> "Class 12 Scholar"
        else -> "#$rank on Leaderboard"
    }

    val badges = StudyBadgeProvider.calculateBadges(
        sessionHistory = emptyList(),
        streakDays = profile.studyStreakDays,
        completedChaptersCount = profile.completedChapterIds.size,
        totalCompletedTasks = profile.completedTodoCount,
        highQuizScoreCount = if (profile.competitionPoints >= 100) 1 else 0
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070B18))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Luxury Ambient Glow Background
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        rankAccentColor.copy(alpha = 0.18f),
                        Color(0x0606B6D4),
                        Color.Transparent
                    ),
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.5f, size.height * 0.15f),
                    radius = size.width * 0.7f
                )
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color(0xFF0D1426), CircleShape)
                            .border(BorderStroke(1.dp, Color(0xFF1E293B)), CircleShape)
                            .bounceClick { onBackClick() }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Leaderboard",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Student Details",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Leaderboard Profile View",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = rankAccentColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, rankAccentColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = rankTitle,
                        color = rankAccentColor,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            // 2. Scrollable Profile Details
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Hero Header Card
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(8.dp, RoundedCornerShape(24.dp), ambientColor = rankAccentColor.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(24.dp),
                        color = Color(0xFF0D1426),
                        border = BorderStroke(1.dp, rankAccentColor.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(contentAlignment = Alignment.BottomEnd) {
                                NexoraAvatar(
                                    name = profile.studentName,
                                    photoUrl = profile.photoUrl,
                                    avatarColorHex = profile.avatarColorHex,
                                    size = 80.dp
                                )
                                if (rank != null) {
                                    Surface(
                                        shape = CircleShape,
                                        color = rankAccentColor,
                                        border = BorderStroke(2.dp, Color(0xFF0D1426)),
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "#$rank",
                                                color = Color(0xFF050814),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = profile.studentName.ifBlank { "Scholar" },
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )

                            if (profile.studentId.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF1E293B)
                                ) {
                                    Text(
                                        text = "ID: ${profile.studentId}",
                                        color = NexoraCyan,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = profile.statusText.ifBlank { "“Aiming for 90+ in GSEB Board Commerce Exams”" },
                                color = Color(0xFF94A3B8),
                                fontSize = 12.5.sp,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(100.dp),
                                    color = Color(0xFF16213A)
                                ) {
                                    Text(
                                        text = profile.classLevel.ifBlank { "Class 12 Commerce" },
                                        color = Color(0xFFCBD5E1),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(100.dp),
                                    color = Color(0xFF16213A)
                                ) {
                                    Text(
                                        text = profile.medium.ifBlank { "Gujarati Medium" },
                                        color = Color(0xFFCBD5E1),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Core Stats 2x2 Grid
                item {
                    Text(
                        text = "STUDY PERFORMANCE",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            icon = "⭐",
                            label = "Competition Points",
                            value = NumberFormat.getIntegerInstance(Locale.US).format(profile.competitionPoints),
                            valueColor = Color(0xFFF59E0B),
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            icon = "📚",
                            label = "Study Hours",
                            value = String.format(Locale.US, "%.1f hrs", profile.totalFocusedMinutes / 60.0),
                            valueColor = NexoraCyan,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            icon = "🔥",
                            label = "Study Streak",
                            value = "${profile.studyStreakDays} Days",
                            valueColor = Color(0xFFFF6D00),
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            icon = "🎯",
                            label = "Target Score",
                            value = profile.targetMarks.ifBlank { "${profile.targetScore}%" },
                            valueColor = Color(0xFF10B981),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Study Activity Breakdown
                item {
                    Text(
                        text = "LEARNING ACTIVITY",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                    )
                }

                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFF0D1426),
                        border = BorderStroke(1.dp, Color(0xFF1E293B))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ActivityRow(
                                icon = "⏱️",
                                label = "Total Focused Minutes",
                                value = "${profile.totalFocusedMinutes} mins"
                            )
                            HorizontalDivider(color = Color(0xFF1E293B))
                            ActivityRow(
                                icon = "🏆",
                                label = "Completed Focus Sessions",
                                value = "${profile.completedSessionsCount} sessions"
                            )
                            HorizontalDivider(color = Color(0xFF1E293B))
                            ActivityRow(
                                icon = "✅",
                                label = "Completed Tasks & To-Dos",
                                value = "${profile.completedTodoCount} tasks"
                            )
                            HorizontalDivider(color = Color(0xFF1E293B))
                            ActivityRow(
                                icon = "📖",
                                label = "Chapters Mastered",
                                value = "${profile.completedChapterIds.size} chapters"
                            )
                            HorizontalDivider(color = Color(0xFF1E293B))
                            ActivityRow(
                                icon = "🎯",
                                label = "Weekly Study Goal",
                                value = "${profile.weeklyStudyGoalHours} hrs / week"
                            )
                        }
                    }
                }

                // GSEB Commerce Subjects Enrolled
                item {
                    Text(
                        text = "COMMERCE SUBJECTS",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                    )
                }

                item {
                    val subjectsList = if (profile.subjects.isNotEmpty()) {
                        profile.subjects
                    } else {
                        listOf(
                            "Accountancy",
                            "Statistics",
                            "Economics",
                            "Organization of Commerce (O.C.M.)",
                            "English",
                            "Gujarati",
                            "Commercial Correspondence (S.P.C.C.)"
                        )
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFF0D1426),
                        border = BorderStroke(1.dp, Color(0xFF1E293B))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            subjectsList.forEachIndexed { index, subject ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("📘", fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = subject,
                                        color = Color.White,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                if (index < subjectsList.lastIndex) {
                                    HorizontalDivider(
                                        color = Color(0xFF1E293B).copy(alpha = 0.5f),
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Badges & Honors
                item {
                    Text(
                        text = "ACHIEVEMENTS & BADGES",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                    )
                }

                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFF0D1426),
                        border = BorderStroke(1.dp, Color(0xFF1E293B))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            badges.forEach { badge ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (badge.isUnlocked) Color(0x2200E5FF) else Color(0xFF16213A),
                                        border = BorderStroke(
                                            1.dp,
                                            if (badge.isUnlocked) NexoraCyan else Color(0xFF334155)
                                        ),
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(badge.iconEmoji, fontSize = 18.sp)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = badge.title,
                                            color = if (badge.isUnlocked) Color.White else Color(0xFF94A3B8),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = badge.description,
                                            color = Color(0xFF64748B),
                                            fontSize = 11.5.sp
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(100.dp),
                                        color = if (badge.isUnlocked) Color(0x2210B981) else Color(0xFF1E293B)
                                    ) {
                                        Text(
                                            text = if (badge.isUnlocked) "Unlocked" else "In Progress",
                                            color = if (badge.isUnlocked) Color(0xFF10B981) else Color(0xFF64748B),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    icon: String,
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0D1426),
        border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Text(icon, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                color = valueColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ActivityRow(
    icon: String,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 16.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                color = Color(0xFFCBD5E1),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            text = value,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
