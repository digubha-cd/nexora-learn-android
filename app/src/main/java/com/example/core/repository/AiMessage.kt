package com.example.core.repository

/**
 * Model representing a single chat message in the NEXORA AI ecosystem.
 */
data class AiMessage(
    val id: String,
    val text: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
