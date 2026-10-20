package com.example.core.supabase.model

import com.example.core.model.StudentProfile
import com.example.core.model.StudyTask
import com.example.core.model.TaskPriority
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * Custom KSerializer to seamlessly handle avatar_color_hex whether Supabase PostgreSQL returns
 * a hex String (e.g., "#00E5FF") or a BIGINT (e.g., 4278248959).
 */
object FlexibleColorStringSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("FlexibleColorString", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String {
        return try {
            val jsonDecoder = decoder as? JsonDecoder
            if (jsonDecoder != null) {
                val element = jsonDecoder.decodeJsonElement()
                if (element is JsonPrimitive) {
                    if (element.isString) {
                        val str = element.content
                        if (str.startsWith("#")) str else "#00BCD4"
                    } else {
                        val num = element.longOrNull
                        if (num != null) {
                            String.format("#%06X", (num and 0xFFFFFFL))
                        } else {
                            "#00BCD4"
                        }
                    }
                } else {
                    "#00BCD4"
                }
            } else {
                decoder.decodeString()
            }
        } catch (e: Exception) {
            "#00BCD4"
        }
    }

    override fun serialize(encoder: Encoder, value: String) {
        val safeColor = if (value.startsWith("#")) value else "#00BCD4"
        encoder.encodeString(safeColor)
    }
}

/**
 * Supabase PostgreSQL DTO for student_profiles table.
 */
@Serializable
data class SupabaseStudentProfile(
    val id: String = "",
    @SerialName("student_id") val studentId: String? = "",
    @SerialName("student_name") val studentName: String? = "",
    val email: String? = null,
    @SerialName("phone_number") val phoneNumber: String? = null,
    @SerialName("photo_url") val photoUrl: String? = "",
    val medium: String? = "Gujarati Medium",
    @SerialName("class_level") val classLevel: String? = "Class 12 Commerce",
    @SerialName("target_marks") val targetMarks: String? = "90+ Marks",
    @Serializable(with = FlexibleColorStringSerializer::class)
    @SerialName("avatar_color_hex") val avatarColorHex: String? = "#00BCD4",
    @SerialName("completed_todo_count") val completedTodoCount: Int? = 0,
    @SerialName("study_streak_days") val studyStreakDays: Int? = 0,
    @SerialName("competition_points") val competitionPoints: Int? = 0,
    @SerialName("completed_chapter_ids") val completedChapterIds: List<String>? = emptyList(),
    @SerialName("total_focused_minutes") val totalFocusedMinutes: Int? = 0,
    @SerialName("completed_sessions_count") val completedSessionsCount: Int? = 0
) {
    fun toDomain(localUserId: String = id): StudentProfile {
        val effectiveUserId = if (localUserId.isNotBlank()) localUserId else id
        return StudentProfile(
            userId = effectiveUserId,
            studentId = studentId.orEmpty(),
            studentName = studentName.orEmpty(),
            email = email.orEmpty(),
            phoneNumber = phoneNumber,
            photoUrl = photoUrl.orEmpty(),
            medium = medium ?: "Gujarati Medium",
            classLevel = classLevel ?: "Class 12 Commerce",
            targetMarks = targetMarks ?: "90+ Marks",
            avatarColorHex = avatarColorHex?.ifBlank { "#00BCD4" } ?: "#00BCD4",
            completedTodoCount = completedTodoCount ?: 0,
            studyStreakDays = studyStreakDays ?: 0,
            competitionPoints = competitionPoints ?: 0,
            completedChapterIds = completedChapterIds ?: emptyList(),
            totalFocusedMinutes = totalFocusedMinutes ?: 0,
            completedSessionsCount = completedSessionsCount ?: 0
        )
    }

    companion object {
        fun fromDomain(domain: StudentProfile, authUserId: String): SupabaseStudentProfile {
            val safeColor = if (domain.avatarColorHex.startsWith("#")) domain.avatarColorHex else "#00BCD4"
            return SupabaseStudentProfile(
                id = authUserId,
                studentId = domain.studentId,
                studentName = domain.studentName,
                email = domain.email.ifBlank { null },
                phoneNumber = domain.phoneNumber,
                photoUrl = domain.photoUrl ?: "",
                medium = domain.medium,
                classLevel = domain.classLevel,
                targetMarks = domain.targetMarks,
                avatarColorHex = safeColor,
                completedTodoCount = domain.completedTodoCount,
                studyStreakDays = domain.studyStreakDays,
                competitionPoints = domain.competitionPoints,
                completedChapterIds = domain.completedChapterIds,
                totalFocusedMinutes = domain.totalFocusedMinutes,
                completedSessionsCount = domain.completedSessionsCount
            )
        }
    }
}

/**
 * Supabase PostgreSQL DTO for friend_requests table.
 */
@Serializable
data class SupabaseFriendRequest(
    val id: String? = null,
    @SerialName("sender_id") val senderId: String,
    @SerialName("receiver_id") val receiverId: String,
    val status: String = "PENDING", // PENDING, ACCEPTED, REJECTED
    @SerialName("created_at") val createdAt: Long? = null
)

/**
 * Supabase PostgreSQL DTO for study_tasks table.
 */
@Serializable
data class SupabaseStudyTask(
    val id: String,
    @SerialName("user_id") val userId: String,
    val title: String,
    val description: String = "",
    @SerialName("subject_id") val subjectId: String = "",
    @SerialName("subject_name") val subjectName: String = "",
    @SerialName("chapter_id") val chapterId: String = "",
    @SerialName("chapter_title") val chapterTitle: String = "",
    @SerialName("exam_phase") val examPhase: String = "SCHOOL_EXAM",
    @SerialName("scheduled_date_str") val scheduledDateStr: String = "",
    @SerialName("scheduled_date_formatted") val scheduledDateFormatted: String = "",
    @SerialName("is_completed") val isCompleted: Boolean = false,
    @SerialName("completed_at") val completedAt: Long? = null,
    @SerialName("is_missed") val isMissed: Boolean = false,
    val priority: String = "MEDIUM",
    @SerialName("duration_minutes") val durationMinutes: Int = 30,
    val notes: String = "",
    @SerialName("reminder_time") val reminderTime: String? = null,
    @SerialName("reminder_date_str") val reminderDateStr: String? = null,
    @SerialName("reminder_epoch_millis") val reminderEpochMillis: Long? = null
) {
    fun toDomain(): StudyTask {
        return StudyTask(
            taskId = id,
            title = title,
            description = description,
            subjectId = subjectId,
            subjectName = subjectName,
            chapterId = chapterId,
            chapterTitle = chapterTitle,
            scheduledDateStr = scheduledDateStr,
            scheduledDateFormatted = scheduledDateFormatted,
            isCompleted = isCompleted,
            completedAt = completedAt,
            isMissed = isMissed,
            priority = try { TaskPriority.valueOf(priority) } catch (_: Exception) { TaskPriority.MEDIUM },
            durationMinutes = durationMinutes,
            notes = notes,
            reminderTime = reminderTime,
            reminderDateStr = reminderDateStr,
            reminderEpochMillis = reminderEpochMillis
        )
    }

    companion object {
        fun fromDomain(domain: StudyTask, authUserId: String): SupabaseStudyTask {
            return SupabaseStudyTask(
                id = domain.taskId,
                userId = authUserId,
                title = domain.title,
                description = domain.description,
                subjectId = domain.subjectId,
                subjectName = domain.subjectName,
                chapterId = domain.chapterId ?: "",
                chapterTitle = domain.chapterTitle ?: "",
                examPhase = domain.examPhase?.name ?: "SCHOOL_EXAM",
                scheduledDateStr = domain.scheduledDateStr,
                scheduledDateFormatted = domain.scheduledDateFormatted,
                isCompleted = domain.isCompleted,
                completedAt = domain.completedAt,
                isMissed = domain.isMissed,
                priority = domain.priority.name,
                durationMinutes = domain.durationMinutes,
                notes = domain.notes,
                reminderTime = domain.reminderTime,
                reminderDateStr = domain.reminderDateStr,
                reminderEpochMillis = domain.reminderEpochMillis
            )
        }
    }
}

/**
 * Supabase PostgreSQL DTO for student_milestones table.
 */
@Serializable
data class SupabaseStudentMilestone(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("subject_id") val subjectId: String,
    @SerialName("chapter_id") val chapterId: String,
    @SerialName("is_completed") val isCompleted: Boolean = false,
    @SerialName("completed_at") val completedAt: Long? = null
)

@Serializable
data class SupabaseMessage(
    val id: String,
    @SerialName("conversation_id") val conversationId: String,
    @SerialName("sender_id") val senderId: String,
    val content: String,
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("created_at") val createdAt: String
)
