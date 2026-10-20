package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.supabase.SupabaseClientManager
import com.example.core.supabase.model.SupabaseMessage
import com.example.ui.animation.bounceClick
import com.example.ui.components.NexoraAvatar
import com.example.ui.theme.*
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

@Composable
fun ConversationDetailScreen(
    conversationId: String,
    recipientName: String,
    recipientId: String,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentUserId = remember { SupabaseClientManager.getCurrentUserId() ?: "" }

    var messageList by remember { mutableStateOf<List<SupabaseMessage>>(emptyList()) }
    var inputMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isSending by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Load conversation messages from Supabase
    LaunchedEffect(conversationId) {
        isLoading = true
        try {
            val client = SupabaseClientManager.client
            if (client != null && SupabaseClientManager.isConfigured(context)) {
                val remote = client.from("messages")
                    .select {
                        filter {
                            eq("conversation_id", conversationId)
                        }
                        order("created_at", Order.ASCENDING)
                    }
                    .decodeList<SupabaseMessage>()
                messageList = remote
            }
        } catch (e: Exception) {
            errorMessage = "Error loading messages: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    BackHandler {
        onBackClick()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NexoraBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .testTag("conversation_detail_screen")
    ) {
        // WhatsApp-inspired Header
        Surface(
            color = NexoraSurfaceVariant,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(40.dp)
                        .background(NexoraBackground, CircleShape)
                        .bounceClick { onBackClick() }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = NexoraTextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                NexoraAvatar(photoUrl = null, name = recipientName, size = 38.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recipientName,
                        color = NexoraTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Online • Study Partner",
                        color = NexoraCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Messages List
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(NexoraBackground)
                .padding(horizontal = 16.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = NexoraCyan
                )
            } else if (messageList.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "No messages yet",
                        color = NexoraTextSecondary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Send a message to start studying together.",
                        color = NexoraTextMuted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    items(messageList) { msg ->
                        val isMe = msg.senderId == currentUserId
                        MessageBubble(message = msg, isMe = isMe)
                    }
                }
            }
        }

        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = NexoraError,
                fontSize = 11.5.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        // Message Input Bar
        Surface(
            color = NexoraSurfaceVariant,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputMessage,
                    onValueChange = { inputMessage = it },
                    placeholder = { Text("Type a study message...") },
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NexoraCyan,
                        unfocusedBorderColor = NexoraBorder,
                        focusedContainerColor = NexoraBackground,
                        unfocusedContainerColor = NexoraBackground
                    ),
                    maxLines = 4
                )

                IconButton(
                    onClick = {
                        val text = inputMessage.trim()
                        if (text.isNotBlank() && currentUserId.isNotBlank()) {
                            isSending = true
                            errorMessage = null
                            scope.launch {
                                try {
                                    val client = SupabaseClientManager.client
                                    if (client != null && SupabaseClientManager.isConfigured(context)) {
                                        val newMsg = SupabaseMessage(
                                            id = UUID.randomUUID().toString(),
                                            conversationId = conversationId,
                                            senderId = currentUserId,
                                            content = text,
                                            isRead = false,
                                            createdAt = System.currentTimeMillis().toString()
                                        )
                                        client.from("messages").insert(newMsg)
                                        messageList = messageList + newMsg
                                        inputMessage = ""
                                    } else {
                                        errorMessage = "Supabase not configured for messaging."
                                    }
                                } catch (e: Exception) {
                                    errorMessage = "Failed to send: ${e.message}"
                                } finally {
                                    isSending = false
                                }
                            }
                        }
                    },
                    enabled = !isSending && inputMessage.isNotBlank(),
                    modifier = Modifier
                        .size(48.dp)
                        .background(NexoraCyan, CircleShape)
                        .bounceClick { }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: SupabaseMessage,
    isMe: Boolean
) {
    val bubbleColor = if (isMe) NexoraCyan else NexoraSurfaceVariant
    val textColor = if (isMe) Color.White else NexoraTextPrimary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMe) 16.dp else 4.dp,
                bottomEnd = if (isMe) 4.dp else 16.dp
            ),
            modifier = Modifier.widthIn(max = 280.dp),
            shadowElevation = 1.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = message.content,
                    color = textColor,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Just now",
                        color = if (isMe) Color.White.copy(alpha = 0.7f) else NexoraTextMuted,
                        fontSize = 9.5.sp
                    )
                    if (isMe) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}
