package com.example.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Room Entity representing a persisted NEXORA AI chat session.
 */
@Entity(tableName = "ai_chat_sessions")
data class AiChatSessionEntity(
    @PrimaryKey
    val sessionId: String = UUID.randomUUID().toString(),
    val title: String,
    val subjectContext: String? = null,
    val chapterContext: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val messageCount: Int = 0
)
