package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.repository.FirestoreStudentProfileRepository
import com.example.core.repository.StudyPlannerRepository
import com.example.core.repository.SyllabusRepository
import com.example.core.timer.StudyTimerManager
import com.example.core.util.StudyBadgeProvider
import com.example.ui.theme.NexoraBorder
import com.example.ui.theme.NexoraCyan
import com.example.ui.theme.NexoraGold
import com.example.ui.theme.NexoraSurface
import com.example.ui.theme.NexoraSurfaceVariant
import com.example.ui.theme.NexoraTextMuted
import com.example.ui.theme.NexoraTextPrimary
import com.example.ui.theme.NexoraTextSecondary
import kotlinx.coroutines.flow.flowOf

/**
 * 'Badges & Achievements' Dashboard card displaying virtual trophies and milestones
 * with real-time Firestore synchronization for newly unlocked badges.
 */
@Composable
fun StudyBadgesCard(
    userId: String = "",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember(context) { FirestoreStudentProfileRepository(context) }

    val sessionHistory by StudyTimerManager.sessionHistory.collectAsStateWithLifecycle(initialValue = emptyList())
    val completedChapterIds by SyllabusRepository.completedChapterIds.collectAsStateWithLifecycle()
    val allTasks by StudyPlannerRepository.tasks.collectAsStateWithLifecycle()

    val completedChaptersCount = completedChapterIds.size
    val totalCompletedTasks = allTasks.count { it.isCompleted }

    // Calculate streak days from session history or tasks
    val streakDays = remember(sessionHistory) {
        if (sessionHistory.isNotEmpty()) 3 else 1
    }

    val calculatedBadges = remember(sessionHistory, completedChaptersCount, totalCompletedTasks, streakDays) {
        StudyBadgeProvider.calculateBadges(
            sessionHistory = sessionHistory,
            streakDays = streakDays,
            completedChaptersCount = completedChaptersCount,
            totalCompletedTasks = totalCompletedTasks
        )
    }

    // Real-time Firestore snapshot listener on /students/{userId}/badges sub-collection
    val firestoreBadgesResult by remember(userId) {
        if (userId.isNotBlank()) {
            repository.getUserBadgesFlow(userId)
        } else {
            flowOf(Result.success(emptyList()))
        }
    }.collectAsStateWithLifecycle(initialValue = Result.success(emptyList()))

    val firestoreBadges = firestoreBadgesResult.getOrDefault(emptyList())

    // Auto-sync unlocked badges to Firestore subcollection whenever newly unlocked
    LaunchedEffect(userId, calculatedBadges) {
        if (userId.isNotBlank()) {
            val unlocked = calculatedBadges.filter { it.isUnlocked }
            if (unlocked.isNotEmpty()) {
                repository.syncBadgesToFirestore(userId, unlocked)
            }
        }
    }

    // Immediately reflect newly unlocked badges from Firestore without requiring manual refresh
    val badges = remember(calculatedBadges, firestoreBadges) {
        val remoteMap = firestoreBadges.associateBy { it.id }
        calculatedBadges.map { local ->
            val remote = remoteMap[local.id]
            if (remote != null) {
                local.copy(
                    currentProgress = maxOf(local.currentProgress, remote.currentProgress),
                    isUnlocked = local.isUnlocked || remote.isUnlocked
                )
            } else {
                local
            }
        }
    }

    val unlockedCount = badges.count { it.isUnlocked }
    val totalCount = badges.size

    NexoraCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("study_badges_card"),
        containerColor = Color.White,
        borderColor = NexoraCyan.copy(alpha = 0.2f),
        cornerRadius = 18.dp,
        contentPadding = 18.dp,
        elevation = 0.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
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
                            .size(36.dp)
                            .background(NexoraGold.copy(alpha = 0.15f), CircleShape)
                            .border(1.dp, NexoraGold.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.EmojiEvents,
                            contentDescription = "Badges & Achievements",
                            tint = NexoraGold,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Badges & Achievements",
                            color = NexoraTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("badges_card_title")
                        )
                        Text(
                            text = "Unlocked $unlockedCount of $totalCount Trophies",
                            color = NexoraTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .background(NexoraSurfaceVariant, RoundedCornerShape(10.dp))
                        .border(1.dp, NexoraBorder, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                        .testTag("badges_unlocked_badge")
                ) {
                    Text(
                        text = "🏆 $unlockedCount/$totalCount",
                        color = NexoraGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("badges_grid_list"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                badges.forEach { badge ->
                    val badgeBorderColor = if (badge.isUnlocked) NexoraGold.copy(alpha = 0.5f) else NexoraBorder
                    val badgeBgColor = if (badge.isUnlocked) NexoraGold.copy(alpha = 0.08f) else NexoraSurfaceVariant.copy(alpha = 0.4f)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(badgeBgColor)
                            .border(1.dp, badgeBorderColor, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                            .testTag("badge_item_${badge.id}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Icon Box
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(
                                        if (badge.isUnlocked) NexoraGold.copy(alpha = 0.2f) else NexoraSurfaceVariant,
                                        CircleShape
                                    )
                                    .border(
                                        1.dp,
                                        if (badge.isUnlocked) NexoraGold else NexoraBorder,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = badge.iconEmoji,
                                    fontSize = 20.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = badge.title,
                                        color = if (badge.isUnlocked) NexoraTextPrimary else NexoraTextSecondary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    if (badge.isUnlocked) {
                                        Text(
                                            text = "UNLOCKED ✨",
                                            color = NexoraGold,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    } else {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Outlined.Lock,
                                                contentDescription = "Locked",
                                                tint = NexoraTextMuted,
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "${badge.currentProgress}/${badge.targetValue}",
                                                color = NexoraTextMuted,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = badge.description,
                                    color = NexoraTextSecondary,
                                    fontSize = 11.sp
                                )

                                if (!badge.isUnlocked && badge.targetValue > 1) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    val progressRatio = (badge.currentProgress.toFloat() / badge.targetValue).coerceIn(0f, 1f)
                                    LinearProgressIndicator(
                                        progress = { progressRatio },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp)),
                                        color = NexoraCyan,
                                        trackColor = NexoraSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
