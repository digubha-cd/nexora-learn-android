package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.model.Subject
import com.example.core.repository.SyllabusRepository
import com.example.ui.animation.NexoraMotion
import com.example.ui.animation.bounceClick
import com.example.ui.animation.staggeredEntrance
import com.example.ui.components.NexoraCard
import com.example.ui.components.NexoraCreatorCredit
import com.example.ui.components.NexoraLogo
import com.example.ui.theme.*

/**
 * Visual metadata for each of the 7 Class 12 Commerce subjects.
 * Redesigned with richer, deeper accents for the Premium Rich aesthetic.
 */
data class SubjectStyle(
    val code: String,
    val icon: ImageVector,
    val accentColor: Color,
    val subtitle: String,
    val pastelBg: Color
)

fun getSubjectStyle(subjectId: String): SubjectStyle {
    return when (subjectId) {
        "gujarati" -> SubjectStyle(
            code = "901",
            icon = Icons.Outlined.MenuBook,
            accentColor = NexoraRoyalBlue,
            subtitle = "ગુજરાતી • First Language",
            pastelBg = NexoraSky
        )
        "english" -> SubjectStyle(
            code = "023",
            icon = Icons.Outlined.Language,
            accentColor = NexoraDeepViolet,
            subtitle = "English • Second Language",
            pastelBg = NexoraLavender
        )
        "sp_cc" -> SubjectStyle(
            code = "337",
            icon = Icons.Outlined.Description,
            accentColor = NexoraBurntAmber,
            subtitle = "Secretarial Practice & CC",
            pastelBg = NexoraAmber
        )
        "ba" -> SubjectStyle(
            code = "144",
            icon = Icons.Outlined.BusinessCenter,
            accentColor = NexoraEmerald,
            subtitle = "Business Administration",
            pastelBg = NexoraMint
        )
        "stat" -> SubjectStyle(
            code = "135",
            icon = Icons.Outlined.BarChart,
            accentColor = NexoraIndigo,
            subtitle = "Statistics • આંકડાશાસ્ત્ર",
            pastelBg = NexoraViolet
        )
        "accounts" -> SubjectStyle(
            code = "151",
            icon = Icons.Outlined.AccountBalance,
            accentColor = NexoraDeepTeal,
            subtitle = "Elements of Accounts",
            pastelBg = NexoraMint
        )
        "economics" -> SubjectStyle(
            code = "022",
            icon = Icons.Outlined.TrendingUp,
            accentColor = NexoraBurgundy,
            subtitle = "Economics • અર્થશાસ્ત્ર",
            pastelBg = NexoraRose
        )
        else -> SubjectStyle(
            code = "000",
            icon = Icons.Outlined.MenuBook,
            accentColor = NexoraIndigo,
            subtitle = "Class 12 Commerce",
            pastelBg = NexoraSky
        )
    }
}

@Composable
fun SubjectsScreen(
    onSubjectClick: (Subject) -> Unit,
    onOpenMarksCalculator: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val completedChapterIds by SyllabusRepository.completedChapterIds.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .testTag("subjects_screen")
    ) {
        // 1. Premium Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NexoraLogo(emblemSize = 38.dp)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Study Subjects",
                        color = NexoraTextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Class 12 Commerce Curriculum",
                        color = CategorySubjects,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            IconButton(
                onClick = onOpenMarksCalculator,
                modifier = Modifier
                    .size(48.dp)
                    .background(NexoraSurfaceVariant, CircleShape)
                    .border(1.dp, NexoraBorder, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Calculate,
                    contentDescription = "Calculator",
                    tint = CategoryStats,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Board Banner
            item {
                NexoraCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .staggeredEntrance(index = 0),
                    containerColor = NexoraSurfaceVariant,
                    borderColor = NexoraBorder.copy(alpha = 0.8f),
                    cornerRadius = 24.dp,
                    contentPadding = 20.dp,
                    elevation = 0.dp
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(NexoraDeepTeal.copy(alpha = 0.1f), CircleShape)
                                .border(1.dp, NexoraDeepTeal.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.School,
                                contentDescription = null,
                                tint = NexoraDeepTeal,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "GSHSEB Board",
                                color = NexoraTextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "Class 12 Commerce • GSHSEB Gujarat",
                                color = NexoraTextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Subject List
            itemsIndexed(Subject.OFFICIAL_SUBJECTS, key = { _, item -> item.id }) { index, subject ->
                val chapters = SyllabusRepository.getChapters(subject.id)
                val completedCount = chapters.count { completedChapterIds.contains(it.id) }
                val totalCount = chapters.size
                val progress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
                val animatedProgress by animateFloatAsState(
                    targetValue = progress,
                    animationSpec = NexoraMotion.SoftSpringFloat,
                    label = "SubjectProgress_$index"
                )
                val style = getSubjectStyle(subject.id)

                SubjectListItem(
                    subject = subject,
                    style = style,
                    completedCount = completedCount,
                    totalCount = totalCount,
                    progress = animatedProgress,
                    onClick = { onSubjectClick(subject) },
                    index = index + 1
                )
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
                NexoraCreatorCredit()
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SubjectListItem(
    subject: Subject,
    style: SubjectStyle,
    completedCount: Int,
    totalCount: Int,
    progress: Float,
    onClick: () -> Unit,
    index: Int,
    modifier: Modifier = Modifier
) {
    NexoraCard(
        modifier = modifier
            .fillMaxWidth()
            .staggeredEntrance(index = index)
            .bounceClick(onClick = onClick)
            .testTag("subject_card_${subject.id}"),
        containerColor = NexoraSurface,
        borderColor = NexoraBorder.copy(alpha = 0.7f),
        cornerRadius = 28.dp,
        contentPadding = 20.dp,
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular Rich Icon with Border
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(style.accentColor.copy(alpha = 0.1f), CircleShape)
                    .border(1.5.dp, style.accentColor.copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = style.icon,
                    contentDescription = null,
                    tint = style.accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = subject.name,
                            color = NexoraTextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = style.subtitle,
                            color = NexoraTextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = style.accentColor.copy(alpha = 0.5f),
                        modifier = Modifier.size(14.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Progress Bar
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .weight(1f)
                            .height(10.dp)
                            .clip(CircleShape),
                        color = style.accentColor,
                        trackColor = NexoraSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        color = NexoraTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}
