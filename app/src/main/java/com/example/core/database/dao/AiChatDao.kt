package com.example.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.core.database.entity.AiChatMessageEntity
import com.example.core.database.entity.AiChatSessionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for local Room operations on NEXORA AI chat sessions and messages.
 */
@Dao
interface AiChatDao {

    @Query("SELECT * FROM ai_chat_sessions ORDER BY updatedAt DESC")
    fun getAllSessions(): Flow<List<AiChatSessionEntity>>

    @Query("SELECT * FROM ai_chat_sessions WHERE sessionId = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: String): AiChatSessionEntity?

    @Query("SELECT * FROM ai_chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSession(sessionId: String): Flow<List<AiChatMessageEntity>>

    @Query("SELECT * FROM ai_chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getMessagesForSessionSync(sessionId: String): List<AiChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: AiChatSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: AiChatMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<AiChatMessageEntity>)

    @Query("UPDATE ai_chat_sessions SET updatedAt = :updatedAt, messageCount = :messageCount WHERE sessionId = :sessionId")
    suspend fun updateSessionStats(sessionId: String, updatedAt: Long, messageCount: Int)

    @Query("DELETE FROM ai_chat_sessions WHERE sessionId = :sessionId")
    suspend fun deleteSessionById(sessionId: String)

    @Query("DELETE FROM ai_chat_messages WHERE sessionId = :sessionId")
    suspend fun deleteMessagesForSession(sessionId: String)

    @Query("DELETE FROM ai_chat_messages")
    suspend fun deleteAllMessages()

    @Query("DELETE FROM ai_chat_sessions")
    suspend fun deleteAllSessions()

    @Transaction
    suspend fun deleteSessionAndMessages(sessionId: String) {
        deleteMessagesForSession(sessionId)
        deleteSessionById(sessionId)
    }

    @Transaction
    suspend fun clearAllChatSessions() {
        deleteAllMessages()
        deleteAllSessions()
    }
}
