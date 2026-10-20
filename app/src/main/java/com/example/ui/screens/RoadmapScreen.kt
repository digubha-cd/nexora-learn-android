package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.model.Subject
import com.example.core.repository.SyllabusRepository
import com.example.ui.animation.bounceClick
import com.example.ui.components.NexoraCard
import com.example.ui.theme.*

@Composable
fun RoadmapScreen(
    onBackClick: () -> Unit,
    onSubjectClick: (Subject) -> Unit,
    modifier: Modifier = Modifier
) {
    val completedChapterIds by SyllabusRepository.completedChapterIds.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NexoraBackground)
            .statusBarsPadding()
            .testTag("roadmap_screen")
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = NexoraTextPrimary)
            }
            Text(
                text = "Syllabus Roadmap",
                color = NexoraTextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(Subject.OFFICIAL_SUBJECTS) { subject ->
                val chapters = SyllabusRepository.getChapters(subject.id)
                val completedCount = chapters.count { completedChapterIds.contains(it.id) }
                val totalCount = chapters.size
                val progress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f

                NexoraCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .bounceClick { onSubjectClick(subject) },
                    cornerRadius = 20.dp,
                    contentPadding = 16.dp
                ) {
                    Column {
                        Text(
                            text = subject.name,
                            color = NexoraTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = NexoraCyan
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$completedCount / $totalCount chapters completed",
                            color = NexoraTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
