package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.model.Chapter
import com.example.core.model.Subject
import com.example.core.model.SyllabusScope
import com.example.core.repository.SyllabusRepository
import com.example.ui.animation.bounceClick
import com.example.ui.animation.staggeredEntrance
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun SubjectDetailScreen(
    subject: Subject,
    onBackClick: () -> Unit,
    onAddChapterTodo: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedScope by remember { mutableStateOf(SyllabusScope.BOARD_EXAM) }
    val completedChapterIds by SyllabusRepository.completedChapterIds.collectAsStateWithLifecycle()

    val chapters = remember(subject.id, selectedScope) {
        SyllabusRepository.getChaptersByScope(subject.id, selectedScope)
    }

    val completedCount = chapters.count { completedChapterIds.contains(it.id) }
    val totalCount = chapters.size
    val progressFraction = if (totalCount > 0) completedCount.toFloat() / totalCount else 0.0f

    val hasParts = chapters.any { it.part != null }
    val groupedByPart = remember(chapters) {
        if (hasParts) chapters.groupBy { it.partName } else emptyMap()
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(NexoraBackground)
                .statusBarsPadding()
                .testTag("subject_detail_screen")
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
                        text = subject.name,
                        color = NexoraTextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Class 12 • Code ${subject.code}",
                        color = NexoraTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Scope Selector Tabs
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(NexoraSurfaceVariant, RoundedCornerShape(20.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ScopeTab(
                            title = "Full Board",
                            isSelected = selectedScope == SyllabusScope.BOARD_EXAM,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedScope = SyllabusScope.BOARD_EXAM }
                        )
                        ScopeTab(
                            title = "School Exam",
                            isSelected = selectedScope == SyllabusScope.SCHOOL_EXAM,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedScope = SyllabusScope.SCHOOL_EXAM }
                        )
                    }
                }
                // Progress Summary Card
                item {
                    NexoraCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 24.dp,
                        contentPadding = 16.dp
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    progress = { progressFraction },
                                    modifier = Modifier.fillMaxSize(),
                                    color = NexoraCyan,
                                    trackColor = NexoraSurfaceVariant,
                                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                                )
                                Text(
                                    text = "${(progressFraction * 100).toInt()}%",
                                    color = NexoraTextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Column {
                                Text(text = "Course Progress", color = NexoraTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Text(text = "$completedCount of $totalCount Chapters Done", color = NexoraTextSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                }
                // Chapter List
                if (hasParts) {
                    groupedByPart.forEach { (partName, partChapters) ->
                        item {
                            Text(
                                text = partName ?: "Chapters",
                                color = NexoraTextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        itemsIndexed(partChapters) { index, chapter ->
                            ChapterListItem(
                                chapter = chapter,
                                isCompleted = completedChapterIds.contains(chapter.id),
                                onToggle = { SyllabusRepository.toggleChapterCompletion(chapter.id) }
                            )
                        }
                    }
                } else {
                    itemsIndexed(chapters) { index, chapter ->
                        ChapterListItem(
                            chapter = chapter,
                            isCompleted = completedChapterIds.contains(chapter.id),
                            onToggle = { SyllabusRepository.toggleChapterCompletion(chapter.id) }
                        )
                    }
                }
                item {
                    NexoraCreatorCredit()
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
        
        FloatingActionButton(
            onClick = onAddChapterTodo,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = NexoraCyan,
            contentColor = Color.White
        ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = "Add Task")
        }
    }
}

@Composable
private fun ScopeTab(
    title: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) Color.White else Color.Transparent)
            .clickable(onClick = onClick)
            .then(if (isSelected) Modifier.border(1.dp, NexoraBorder, RoundedCornerShape(16.dp)) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (isSelected) NexoraCyan else NexoraTextMuted,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
        )
    }
}

@Composable
private fun ChapterListItem(
    chapter: Chapter,
    isCompleted: Boolean,
    onToggle: () -> Unit
) {
    NexoraCard(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick(onClick = onToggle),
        cornerRadius = 20.dp,
        contentPadding = 12.dp,
        borderColor = if (isCompleted) NexoraCyan.copy(alpha = 0.2f) else NexoraBorder
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(if (isCompleted) NexoraCyan else NexoraSurfaceVariant, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                } else {
                    Text(text = chapter.id.takeLast(2), color = NexoraTextMuted, fontSize = 12.sp, fontWeight = FontWeight.Black)
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chapter.title,
                    color = NexoraTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                if (chapter.englishTitle != null) {
                    Text(text = chapter.englishTitle, color = NexoraTextSecondary, fontSize = 11.sp)
                }
            }
            
            Checkbox(
                checked = isCompleted,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = NexoraCyan,
                    uncheckedColor = NexoraBorder
                )
            )
        }
    }
}
