package com.example.core.model

/**
 * Virtual trophy / badge earned by students for achieving study milestones.
 */
data class StudyBadge(
    val id: String,
    val title: String,
    val description: String,
    val iconEmoji: String,
    val targetValue: Int,
    val currentProgress: Int,
    val isUnlocked: Boolean,
    val unlockedAt: Long = System.currentTimeMillis()
) {
    fun toFirestoreMap(): Map<String, Any> {
        return mapOf(
            "badgeId" to id,
            "title" to title,
            "description" to description,
            "iconEmoji" to iconEmoji,
            "targetValue" to targetValue,
            "currentProgress" to currentProgress,
            "isUnlocked" to isUnlocked,
            "unlockedAt" to unlockedAt
        )
    }

    companion object {
        fun fromFirestoreMap(map: Map<String, Any>): StudyBadge? {
            val badgeId = map["badgeId"] as? String ?: map["id"] as? String ?: return null
            val title = map["title"] as? String ?: "Achievement Badge"
            val description = map["description"] as? String ?: ""
            val iconEmoji = map["iconEmoji"] as? String ?: "🏆"
            val targetValue = (map["targetValue"] as? Long)?.toInt() ?: 1
            val currentProgress = (map["currentProgress"] as? Long)?.toInt() ?: 0
            val isUnlocked = map["isUnlocked"] as? Boolean ?: false
            val unlockedAt = (map["unlockedAt"] as? Long) ?: System.currentTimeMillis()

            return StudyBadge(
                id = badgeId,
                title = title,
                description = description,
                iconEmoji = iconEmoji,
                targetValue = targetValue,
                currentProgress = currentProgress,
                isUnlocked = isUnlocked,
                unlockedAt = unlockedAt
            )
        }
    }
}
