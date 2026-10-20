package com.example.ui.ai

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.repository.AiMessage
import com.example.core.repository.NexoraAiRepository
import com.example.ui.animation.bounceClick
import com.example.ui.components.NexoraAiAvatar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * NexoraAiScreen: White Premium AI Chat Control Room.
 * Modern, clean white aesthetic with subtle gradients, distinct message bubbles,
 * refined typography, and full AI functionality preservation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NexoraAiScreen(
    subjectContext: String? = null,
    chapterContext: String? = null,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val messages by NexoraAiRepository.messages.collectAsStateWithLifecycle()
    val isLoading by NexoraAiRepository.isLoading.collectAsStateWithLifecycle()
    val errorMessage by NexoraAiRepository.errorMessage.collectAsStateWithLifecycle()
    val currentSessionTitle by NexoraAiRepository.currentSessionTitle.collectAsStateWithLifecycle()
    val currentSessionId by NexoraAiRepository.currentSessionId.collectAsStateWithLifecycle()

    val sessionsFlow = remember(context) { NexoraAiRepository.getAllSessions(context) }
    val pastSessions by sessionsFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    var inputPrompt by remember { mutableStateOf("") }
    var showHistorySheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = rememberLazyListState()

    // Speech-to-text launcher using RecognizerIntent
    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                inputPrompt = if (inputPrompt.isBlank()) spokenText else "$inputPrompt $spokenText"
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Dictate your question for NEXORA AI...")
            }
            try {
                speechRecognizerLauncher.launch(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Voice search is not supported on this device", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Microphone permission is required for voice dictation", Toast.LENGTH_SHORT).show()
        }
    }

    fun startVoiceDictation() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Dictate your question for NEXORA AI...")
            }
            try {
                speechRecognizerLauncher.launch(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Voice recognition is unavailable on this device", Toast.LENGTH_SHORT).show()
            }
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun handleSendPrompt(promptToSend: String) {
        if (promptToSend.isNotBlank() && !isLoading) {
            inputPrompt = ""
            coroutineScope.launch {
                NexoraAiRepository.sendMessage(promptToSend, context)
            }
        }
    }

    LaunchedEffect(subjectContext, chapterContext) {
        NexoraAiRepository.setContext(subjectContext, chapterContext)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC)) // Clean Off-White Canvas
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("nexora_ai_screen")
    ) {
        // Premium Light Background Pattern
        LightPremiumAiBackground(modifier = Modifier.fillMaxSize())

        Column(modifier = Modifier.fillMaxSize()) {
            // 1. TOP HEADER BAR
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFFFFFFF),
                shadowElevation = 2.dp
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = onBackClick,
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(Color(0xFFF1F5F9), CircleShape)
                                    .bounceClick { onBackClick() }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color(0xFF0F172A),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                NexoraAiAvatar(size = 38.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "NEXORA AI",
                                            color = Color(0xFF0F172A),
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 0.5.sp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFFE0F2FE))
                                                .border(1.dp, Color(0xFFBAE6FD), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "SMART AI",
                                                color = Color(0xFF0284C7),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Text(
                                        text = currentSessionTitle,
                                        color = Color(0xFF64748B),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            IconButton(
                                onClick = { showHistorySheet = true },
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(Color(0xFFE0F2FE), CircleShape)
                                    .bounceClick { showHistorySheet = true }
                                    .testTag("ai_history_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.History,
                                    contentDescription = "Chat History",
                                    tint = Color(0xFF0284C7),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        NexoraAiRepository.createNewSession()
                                    }
                                },
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(Color(0xFFF1F5F9), CircleShape)
                                    .bounceClick {
                                        coroutineScope.launch {
                                            NexoraAiRepository.createNewSession()
                                        }
                                    }
                                    .testTag("ai_new_chat_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Add,
                                    contentDescription = "New Chat",
                                    tint = Color(0xFF334155),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0xFFE2E8F0))
                    )
                }
            }

            // 2. CHAT MESSAGES LIST
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    NexoraAiWelcomeGraphic(
                        onChipClick = { chipPrompt ->
                            handleSendPrompt(chipPrompt)
                        }
                    )
                }

                items(
                    items = messages,
                    key = { it.id }
                ) { message ->
                    AiMessageBubble(message = message)
                }

                if (isLoading) {
                    item {
                        TypingLoadingIndicator()
                    }
                }
            }

            // ERROR DISPLAY
            errorMessage?.let { err ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFFFE4E6),
                    border = BorderStroke(1.dp, Color(0xFFFECDD3))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Error: $err",
                            color = Color(0xFF991B1B),
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = {
                            coroutineScope.launch {
                                NexoraAiRepository.retryLastMessage(context)
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = "Retry",
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Retry", color = Color(0xFFDC2626), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 3. ELEGANT INPUT BAR
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFFFFFFF),
                shadowElevation = 8.dp
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0xFFE2E8F0))
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .imePadding()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        OutlinedTextField(
                            value = inputPrompt,
                            onValueChange = { inputPrompt = it },
                            placeholder = {
                                Text(
                                    text = "Ask in English, Gujarati...",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 13.5.sp
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { startVoiceDictation() }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Mic,
                                        contentDescription = "Voice Dictation",
                                        tint = Color(0xFF0284C7)
                                    )
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ai_prompt_input"),
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF0284C7),
                                unfocusedBorderColor = Color(0xFFE2E8F0),
                                focusedContainerColor = Color(0xFFF8FAFC),
                                unfocusedContainerColor = Color(0xFFF1F5F9),
                                cursorColor = Color(0xFF0284C7)
                            ),
                            maxLines = 4,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = Color(0xFF0F172A))
                        )

                        IconButton(
                            onClick = { handleSendPrompt(inputPrompt) },
                            enabled = inputPrompt.isNotBlank() && !isLoading,
                            modifier = Modifier
                                .size(50.dp)
                                .background(
                                    brush = if (inputPrompt.isNotBlank() && !isLoading) {
                                        Brush.linearGradient(listOf(Color(0xFF0284C7), Color(0xFF2563EB), Color(0xFF7C4DFF)))
                                    } else {
                                        Brush.linearGradient(listOf(Color(0xFFE2E8F0), Color(0xFFE2E8F0)))
                                    },
                                    shape = CircleShape
                                )
                                .bounceClick { handleSendPrompt(inputPrompt) }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (inputPrompt.isNotBlank() && !isLoading) Color.White else Color(0xFF94A3B8),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // CHAT HISTORY BOTTOM SHEET
    if (showHistorySheet) {
        ModalBottomSheet(
            onDismissRequest = { showHistorySheet = false },
            sheetState = sheetState,
            containerColor = Color(0xFFFFFFFF)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.History,
                            contentDescription = null,
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Chat History",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                NexoraAiRepository.createNewSession()
                                sheetState.hide()
                                showHistorySheet = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(text = "New Chat", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (pastSessions.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(text = "No history yet", color = Color(0xFF64748B))
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(pastSessions) { session ->
                            val isSelected = session.sessionId == currentSessionId
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        coroutineScope.launch {
                                            NexoraAiRepository.loadSession(session.sessionId, context)
                                            sheetState.hide()
                                            showHistorySheet = false
                                        }
                                    },
                                color = if (isSelected) Color(0xFFE0F2FE) else Color(0xFFF8FAFC),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) Color(0xFF0284C7) else Color(0xFFE2E8F0)
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = session.title,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF0F172A),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${session.messageCount} messages",
                                            fontSize = 11.sp,
                                            color = Color(0xFF0284C7)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                NexoraAiRepository.deleteSession(session.sessionId, context)
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Delete,
                                            contentDescription = "Delete",
                                            tint = Color(0xFFEF4444)
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
}

@Composable
fun LightPremiumAiBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFFFFFFF), Color(0xFFF8FAFC), Color(0xFFF1F5F9))
            )
        )

        // Subtle decorative grid
        val gridSpacing = 48.dp.toPx()
        val gridColor = Color(0xFF0284C7).copy(alpha = 0.025f)
        for (x in 0..(width / gridSpacing).toInt()) {
            drawLine(gridColor, Offset(x * gridSpacing, 0f), Offset(x * gridSpacing, height))
        }
        for (y in 0..(height / gridSpacing).toInt()) {
            drawLine(gridColor, Offset(0f, y * gridSpacing), Offset(width, y * gridSpacing))
        }
    }
}

@Composable
private fun NexoraAiWelcomeGraphic(onChipClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        NexoraAiAvatar(size = 72.dp)
        Spacer(modifier = Modifier.height(14.dp))
        Text(text = "NEXORA AI", color = Color(0xFF0F172A), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        Text(text = "Your AI Study Assistant", color = Color(0xFF0284C7), fontSize = 13.sp, fontWeight = FontWeight.Medium)
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Suggestion Chips
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val suggestions = listOf(
                "Explain GSEB Class 12 Commerce Accounts Chapter 1",
                "Give me top revision tips for Economics board exam",
                "How to calculate percentage in Marks Calculator?"
            )
            for (suggestion in suggestions) {
                Surface(
                    onClick = { onChipClick(suggestion) },
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = suggestion,
                            color = Color(0xFF1E293B),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AiMessageBubble(message: AiMessage) {
    val isUser = message.isFromUser
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            NexoraAiAvatar(size = 32.dp)
            Spacer(modifier = Modifier.width(8.dp))
        }
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = if (isUser) Color(0xFF0284C7) else Color(0xFFFFFFFF),
            border = if (isUser) null else BorderStroke(1.dp, Color(0xFFE2E8F0)),
            shadowElevation = if (isUser) 2.dp else 1.dp,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message.text,
                color = if (isUser) Color.White else Color(0xFF0F172A),
                modifier = Modifier.padding(14.dp),
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun TypingLoadingIndicator() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    ) {
        NexoraAiAvatar(size = 28.dp)
        Spacer(modifier = Modifier.width(10.dp))
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    color = Color(0xFF0284C7),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "NEXORA AI is thinking...",
                    color = Color(0xFF0284C7),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))
}
