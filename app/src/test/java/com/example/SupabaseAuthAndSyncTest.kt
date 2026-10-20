package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.model.StudentProfile
import com.example.core.model.StudyTask
import com.example.core.repository.SyllabusRepository
import com.example.core.supabase.SupabaseClientManager
import com.example.core.supabase.model.SupabaseStudentProfile
import com.example.core.supabase.model.SupabaseStudyTask
import com.example.core.timer.StudyTimerManager
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SupabaseAuthAndSyncTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        SyllabusRepository.initialize(context)
        StudyTimerManager.initialize(context)
    }

    @Test
    fun testSupabaseStudentProfileDtoMapping() {
        val original = StudentProfile(
            userId = "auth_uuid_999",
            studentId = "STU-9999",
            studentName = "Digvijay Chauhan",
            email = "digvijay@example.com",
            avatarColorHex = "#00E5FF",
            medium = "Gujarati Medium",
            classLevel = "Class 12 Commerce",
            targetMarks = "90+ Marks",
            completedChapterIds = listOf("acc_ch1", "stat_ch1"),
            totalFocusedMinutes = 120,
            completedSessionsCount = 4
        )

        val dto = SupabaseStudentProfile.fromDomain(original, "auth_uuid_999")
        assertEquals("auth_uuid_999", dto.id)
        assertEquals("STU-9999", dto.studentId)
        assertEquals("Digvijay Chauhan", dto.studentName)
        assertEquals("digvijay@example.com", dto.email)
        assertEquals("#00E5FF", dto.avatarColorHex)
        assertEquals("90+ Marks", dto.targetMarks)
        assertEquals(120, dto.totalFocusedMinutes)
        assertEquals(4, dto.completedSessionsCount)
        assertEquals(listOf("acc_ch1", "stat_ch1"), dto.completedChapterIds)

        val restored = dto.toDomain("auth_uuid_999")
        assertEquals(original.userId, restored.userId)
        assertEquals(original.studentId, restored.studentId)
        assertEquals(original.studentName, restored.studentName)
        assertEquals(original.email, restored.email)
        assertEquals("#00E5FF", restored.avatarColorHex)
        assertEquals(original.totalFocusedMinutes, restored.totalFocusedMinutes)
        assertEquals(original.completedSessionsCount, restored.completedSessionsCount)
        assertEquals(original.completedChapterIds, restored.completedChapterIds)
    }

    @Test
    fun testAvatarColorHexSerializationAsString() {
        val json = Json { ignoreUnknownKeys = true }
        val profile = SupabaseStudentProfile(
            id = "550e8400-e29b-41d4-a716-446655440000",
            studentId = "NX-88888888",
            studentName = "Test Student",
            email = "test@example.com",
            avatarColorHex = "#00E5FF"
        )

        val serialized = json.encodeToString(SupabaseStudentProfile.serializer(), profile)
        assertTrue(serialized.contains("\"avatar_color_hex\":\"#00E5FF\""))
        assertFalse(serialized.contains("\"avatar_color_hex\":42")) // Confirms no raw numeric bigint is sent

        val deserialized = json.decodeFromString(SupabaseStudentProfile.serializer(), serialized)
        assertEquals("#00E5FF", deserialized.avatarColorHex)
    }

    @Test
    fun testFlexibleColorStringSerializerHandlesLegacyNumericValue() {
        val json = Json { ignoreUnknownKeys = true }
        val legacyJson = """
            {
                "id": "550e8400-e29b-41d4-a716-446655440000",
                "student_id": "NX-88888888",
                "student_name": "Legacy Student",
                "avatar_color_hex": 4278248959
            }
        """.trimIndent()

        val decoded = json.decodeFromString(SupabaseStudentProfile.serializer(), legacyJson)
        assertNotNull(decoded.avatarColorHex)
        assertTrue(decoded.avatarColorHex!!.startsWith("#"))
    }

    @Test
    fun testSupabaseStudyTaskDtoMapping() {
        val task = StudyTask(
            taskId = "task_uuid_123",
            title = "Complete Accounts Partnership Chapter 1",
            description = "Revise illustration 1 to 5",
            subjectId = "accounts",
            subjectName = "Elements of Accounts",
            scheduledDateStr = "2026-10-22",
            scheduledDateFormatted = "22 Oct 2026",
            isCompleted = true,
            durationMinutes = 45
        )

        val dto = SupabaseStudyTask.fromDomain(task, "auth_uuid_999")
        assertEquals("task_uuid_123", dto.id)
        assertEquals("auth_uuid_999", dto.userId)
        assertEquals("Complete Accounts Partnership Chapter 1", dto.title)
        assertEquals("accounts", dto.subjectId)
        assertEquals(45, dto.durationMinutes)
        assertTrue(dto.isCompleted)

        val domain = dto.toDomain()
        assertEquals(task.taskId, domain.taskId)
        assertEquals(task.title, domain.title)
        assertEquals(task.durationMinutes, domain.durationMinutes)
        assertTrue(domain.isCompleted)
    }

    @Test
    fun testSupabaseUrlConfiguration() {
        val url = SupabaseClientManager.getSupabaseUrl()
        assertEquals("https://mysnralzdpusuuquhxlf.supabase.co", url)
    }

    @Test
    fun testSyllabusRepositorySetCompletedChapterIds() {
        val chapters = setOf("guj_ch1", "guj_ch2", "eco_ch1")
        SyllabusRepository.setCompletedChapterIds(chapters)

        assertTrue(SyllabusRepository.isChapterCompleted("guj_ch1"))
        assertTrue(SyllabusRepository.isChapterCompleted("guj_ch2"))
        assertTrue(SyllabusRepository.isChapterCompleted("eco_ch1"))
        assertFalse(SyllabusRepository.isChapterCompleted("ba_ch1"))

        assertEquals(chapters, SyllabusRepository.completedChapterIds.value)
    }

    @Test
    fun testStudyTimerManagerUpdateSessionStats() {
        StudyTimerManager.resetForTesting()
        assertEquals(0, StudyTimerManager.timerState.value.completedSessionsCount)
        assertEquals(0, StudyTimerManager.timerState.value.totalFocusedMinutes)

        // Merge incoming higher stats from cloud
        StudyTimerManager.updateSessionStats(sessions = 5, focusedMins = 125)
        assertEquals(5, StudyTimerManager.timerState.value.completedSessionsCount)
        assertEquals(125, StudyTimerManager.timerState.value.totalFocusedMinutes)

        // Lower numbers do not regress local progress
        StudyTimerManager.updateSessionStats(sessions = 2, focusedMins = 50)
        assertEquals(5, StudyTimerManager.timerState.value.completedSessionsCount)
        assertEquals(125, StudyTimerManager.timerState.value.totalFocusedMinutes)
    }
}
