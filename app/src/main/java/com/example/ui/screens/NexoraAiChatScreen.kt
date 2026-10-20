package com.example.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.ui.ai.NexoraAiScreen

/**
 * NexoraAiChatScreen delegates directly to NexoraAiScreen, providing a single, unified
 * NEXORA AI chat experience with Room database persistence, history sheet, copy-to-clipboard,
 * and message fade-in animations.
 */
@Composable
fun NexoraAiChatScreen(
    subjectContext: String? = null,
    chapterContext: String? = null,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    NexoraAiScreen(
        subjectContext = subjectContext,
        chapterContext = chapterContext,
        onBackClick = onBackClick,
        modifier = modifier
    )
}
