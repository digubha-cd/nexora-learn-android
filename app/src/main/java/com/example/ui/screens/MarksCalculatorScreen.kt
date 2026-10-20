package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.model.Subject
import com.example.core.repository.SyllabusRepository
import com.example.ui.animation.NexoraMotion
import com.example.ui.animation.bounceClick
import com.example.ui.animation.staggeredEntrance
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun MarksCalculatorScreen(
    onBackClick: () -> Unit
) {
    val completedChapterIds by SyllabusRepository.completedChapterIds.collectAsStateWithLifecycle()
    val chapterMarks by SyllabusRepository.chapterMarks.collectAsStateWithLifecycle()
    
    var selectedSubjectId by remember { mutableStateOf<String?>(null) }
    
    BackHandler {
        if (selectedSubjectId != null) {
            selectedSubjectId = null
        } else {
            onBackClick()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .testTag("marks_calculator_screen")
    ) {
        // Premium Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (selectedSubjectId != null) selectedSubjectId = null else onBackClick()
                },
                modifier = Modifier
                    .size(44.dp)
                    .background(NexoraSurfaceVariant, CircleShape)
                    .bounceClick {
                        if (selectedSubjectId != null) selectedSubjectId = null else onBackClick()
                    }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = NexoraTextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (selectedSubjectId == null) "Marks Hub" else {
                        Subject.OFFICIAL_SUBJECTS.find { it.id == selectedSubjectId }?.name ?: "Subject Marks"
                    },
                    color = NexoraTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = if (selectedSubjectId == null) "Syllabus Weightage" else "Chapter-wise Targets",
                    color = NexoraCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            NexoraLogo(emblemSize = 32.dp)
        }

        AnimatedContent(
            targetState = selectedSubjectId,
            transitionSpec = {
                if (targetState != null) {
                    slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                } else {
                    slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                }
            },
            label = "CalculatorNavigation"
        ) { subjectId ->
            if (subjectId == null) {
                CalculatorDashboard(
                    onSubjectClick = { selectedSubjectId = it }
                )
            } else {
                SubjectMarksConfig(
                    subjectId = subjectId,
                    onBack = { selectedSubjectId = null }
                )
            }
        }
    }
}

@Composable
private fun CalculatorDashboard(
    onSubjectClick: (String) -> Unit
) {
    val totalMarks = SyllabusRepository.getOverallTotalMarks()
    val completedMarks = SyllabusRepository.getOverallCompletedMarks()
    val completionPercentage = if (totalMarks > 0) (completedMarks / totalMarks * 100).toInt() else 0

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Overall Summary
        item {
            NexoraCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .staggeredEntrance(index = 0),
                cornerRadius = 32.dp,
                contentPadding = 24.dp
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(140.dp)) {
                        CircularProgressIndicator(
                            progress = { 1f },
                            modifier = Modifier.fillMaxSize(),
                            color = NexoraSurfaceVariant,
                            strokeWidth = 10.dp,
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                        
                        val animatedProgress by animateFloatAsState(
                            targetValue = completionPercentage / 100f,
                            animationSpec = NexoraMotion.SoftSpringFloat,
                            label = "OverallProgress"
                        )
                        
                        CircularProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.fillMaxSize(),
                            color = NexoraCyan,
                            strokeWidth = 10.dp,
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$completionPercentage%",
                                color = NexoraTextPrimary,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "Overall Done",
                                color = NexoraTextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        MarksStatItem(label = "Total Marks", value = totalMarks.toInt(), color = NexoraTextPrimary)
                        MarksStatItem(label = "Marks Done", value = completedMarks.toInt(), color = NexoraSuccess)
                        MarksStatItem(label = "Remaining", value = (totalMarks - completedMarks).toInt(), color = NexoraPink)
                    }
                }
            }
        }

        item {
            Text(
                text = "Subject Breakdown",
                color = NexoraTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        items(Subject.OFFICIAL_SUBJECTS, key = { it.id }) { subject ->
            val subTotal = SyllabusRepository.getSubjectTotalMarks(subject.id)
            val subCompleted = SyllabusRepository.getSubjectCompletedMarks(subject.id)
            val subPercentage = if (subTotal > 0) (subCompleted / subTotal * 100).toInt() else 0
            val style = getSubjectStyle(subject.id)

            NexoraCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .bounceClick { onSubjectClick(subject.id) },
                cornerRadius = 24.dp,
                contentPadding = 16.dp
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(style.pastelBg.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = style.icon,
                            contentDescription = null,
                            tint = style.accentColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = subject.name,
                                color = NexoraTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "$subPercentage%",
                                color = style.accentColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        LinearProgressIndicator(
                            progress = { subPercentage / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = style.accentColor,
                            trackColor = NexoraSurfaceVariant
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = NexoraTextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        item {
            NexoraCreatorCredit()
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SubjectMarksConfig(
    subjectId: String,
    onBack: () -> Unit
) {
    val chapters = remember(subjectId) { SyllabusRepository.getChapters(subjectId) }
    val chapterMarks by SyllabusRepository.chapterMarks.collectAsStateWithLifecycle()
    val completedIds by SyllabusRepository.completedChapterIds.collectAsStateWithLifecycle()

    val subTotal = SyllabusRepository.getSubjectTotalMarks(subjectId)
    val subCompleted = SyllabusRepository.getSubjectCompletedMarks(subjectId)
    
    var editingChapterId by remember { mutableStateOf<String?>(null) }
    var markInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            NexoraCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = NexoraSurfaceVariant,
                cornerRadius = 24.dp,
                contentPadding = 20.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Subject Summary", color = NexoraTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(text = "${subCompleted.toInt()} / ${subTotal.toInt()} Marks", color = NexoraTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    }
                    
                    val style = remember(subjectId) { getSubjectStyle(subjectId) }
                    CircularProgressIndicator(
                        progress = { if (subTotal > 0) (subCompleted / subTotal).toFloat() else 0f },
                        modifier = Modifier.size(48.dp),
                        color = style.accentColor,
                        strokeWidth = 6.dp,
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            }
        }

        items(chapters, key = { it.id }) { chapter ->
            val isCompleted = completedIds.contains(chapter.id)
            val weightage = chapterMarks[chapter.id] ?: 0.0
            
            NexoraCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = if (isCompleted) NexoraSuccess.copy(alpha = 0.5f) else NexoraBorder,
                cornerRadius = 20.dp,
                contentPadding = 12.dp
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { SyllabusRepository.toggleChapterCompletion(chapter.id) }
                    ) {
                        Icon(
                            imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = "Toggle",
                            tint = if (isCompleted) NexoraSuccess else NexoraTextMuted
                        )
                    }
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = chapter.title,
                            color = NexoraTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isCompleted) "Completed (+${weightage.toInt()})" else "Pending",
                            color = if (isCompleted) NexoraSuccess else NexoraTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(NexoraSurfaceVariant)
                            .clickable {
                                editingChapterId = chapter.id
                                markInput = weightage.toInt().toString()
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "${weightage.toInt()}",
                            color = NexoraTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
        
        item {
            NexoraSecondaryButton(
                text = "Reset All Marks",
                onClick = { SyllabusRepository.resetAllChapterMarks() },
                modifier = Modifier.fillMaxWidth(),
                containerColor = NexoraRose,
                contentColor = NexoraError
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (editingChapterId != null) {
        val chapter = chapters.find { it.id == editingChapterId }
        AlertDialog(
            onDismissRequest = { editingChapterId = null },
            title = { Text(text = "Chapter Weightage", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(text = chapter?.title ?: "", fontSize = 14.sp, color = NexoraTextSecondary)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = markInput,
                        onValueChange = { if (it.length <= 3) markInput = it.filter { char -> char.isDigit() } },
                        label = { Text("Max Marks") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            },
            confirmButton = {
                NexoraPrimaryButton(
                    text = "Save",
                    onClick = {
                        val marks = markInput.toDoubleOrNull() ?: 0.0
                        SyllabusRepository.updateChapterMark(editingChapterId!!, marks)
                        editingChapterId = null
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { editingChapterId = null }) {
                    Text("Cancel", color = NexoraTextSecondary)
                }
            },
            containerColor = NexoraSurface,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
private fun MarksStatItem(
    label: String,
    value: Int,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value.toString(),
            color = color,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            text = label,
            color = NexoraTextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
