package com.example.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.core.repository.AiMessage
import java.util.UUID

/**
 * Room Entity representing a message within a NEXORA AI chat session.
 */
@Entity(
    tableName = "ai_chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = AiChatSessionEntity::class,
            parentColumns = ["sessionId"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sessionId"])]
)
data class AiChatMessageEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val text: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toAiMessage(): AiMessage {
        return AiMessage(
            id = id,
            text = text,
            isFromUser = isFromUser,
            timestamp = timestamp
        )
    }

    companion object {
        fun fromAiMessage(sessionId: String, message: AiMessage): AiChatMessageEntity {
            return AiChatMessageEntity(
                id = message.id,
                sessionId = sessionId,
                text = message.text,
                isFromUser = message.isFromUser,
                timestamp = message.timestamp
            )
        }
    }
}
