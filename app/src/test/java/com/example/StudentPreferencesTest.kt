package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.storage.StudentPreferences
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
@Config(sdk = [33])
class StudentPreferencesTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        // Clear preferences
        context.getSharedPreferences("nexora_student_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun testInitialStateHasNoSavedName() {
        assertFalse(StudentPreferences.hasSavedName(context))
        assertEquals("", StudentPreferences.getSavedStudentName(context))
    }

    @Test
    fun testSaveStudentNamePersistsLocally() {
        val studentName = "Aarav Shah"
        val savedProfile = StudentPreferences.saveStudentName(context, studentName)

        assertTrue(StudentPreferences.hasSavedName(context))
        assertEquals(studentName, StudentPreferences.getSavedStudentName(context))
        assertEquals(studentName, savedProfile.studentName)
        assertEquals("Gujarati Medium", savedProfile.medium)
        assertEquals("Class 12 Commerce", savedProfile.classLevel)
        assertEquals("90+ Marks", savedProfile.targetMarks)
        assertNotNull(savedProfile.userId)
    }

    @Test
    fun testUpdateStudentName() {
        StudentPreferences.saveStudentName(context, "Initial Name")
        assertEquals("Initial Name", StudentPreferences.getSavedStudentName(context))

        val updated = StudentPreferences.saveStudentName(context, "Updated Name")
        assertEquals("Updated Name", StudentPreferences.getSavedStudentName(context))
        assertEquals("Updated Name", updated.studentName)
    }

    @Test
    fun testStudentProfilePersistenceAcrossCalls() {
        StudentPreferences.saveStudentName(context, "Diya Patel")
        val profile1 = StudentPreferences.getStudentProfile(context)
        val profile2 = StudentPreferences.getStudentProfile(context)

        assertEquals(profile1.userId, profile2.userId)
        assertEquals(profile1.studentId, profile2.studentId)
        assertEquals("Diya Patel", profile1.studentName)
    }
}
