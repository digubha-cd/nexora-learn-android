package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.storage.StudentPreferences
import com.example.core.util.StudentIdGenerator
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
class StudentEntryTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("nexora_student_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun testStudentIdValidation() {
        assertTrue(StudentIdGenerator.isValidStudentId("NX-7K4P9288"))
        assertTrue(StudentIdGenerator.isValidStudentId("nx-7k4p9288"))
        assertTrue(StudentIdGenerator.isValidStudentId("7K4P9288"))
        assertFalse(StudentIdGenerator.isValidStudentId("NX-123"))
        assertFalse(StudentIdGenerator.isValidStudentId(""))
        assertFalse(StudentIdGenerator.isValidStudentId(null))

        assertEquals("NX-7K4P9288", StudentIdGenerator.normalizeStudentId("NX-7K4P9288"))
        assertEquals("NX-7K4P9288", StudentIdGenerator.normalizeStudentId("nx-7k4p9288"))
        assertEquals("NX-7K4P9288", StudentIdGenerator.normalizeStudentId("7k4p9288"))
    }

    @Test
    fun testSaveStudentProfileWithId() {
        val name = "Aarav Patel"
        val id = "NX-8M3X9K"

        val profile = StudentPreferences.saveStudentProfileWithId(context, name, id)

        assertTrue(StudentPreferences.hasSavedName(context))
        assertEquals(name, profile.studentName)
        assertEquals(id, profile.studentId)
        assertEquals(name, StudentPreferences.getSavedStudentName(context))

        val loaded = StudentPreferences.getStudentProfile(context)
        assertEquals(name, loaded.studentName)
        assertEquals(id, loaded.studentId)
    }

    @Test
    fun testCandidateIdGeneration() {
        val candidate = StudentIdGenerator.generateCandidateId()
        assertNotNull(candidate)
        assertTrue(candidate.startsWith("NX-"))
        assertTrue(StudentIdGenerator.isValidStudentId(candidate))
    }

    @Test
    fun testFirstLaunchSetupFlowAndImmutability() {
        // 1. Initial first-launch state
        assertFalse(StudentPreferences.hasCompletedSetup(context))
        assertFalse(StudentPreferences.isStudentIdImmutable(context))

        // 2. Complete student identity setup
        val studentName = "Priya Sharma"
        val candidateId = StudentIdGenerator.generateCandidateId()
        val completedProfile = StudentPreferences.completeIdentitySetup(context, studentName, candidateId)

        // 3. Setup is completed and ID is immutable
        assertTrue(StudentPreferences.hasCompletedSetup(context))
        assertTrue(StudentPreferences.isStudentIdImmutable(context))
        assertEquals(studentName, completedProfile.studentName)
        assertEquals(candidateId, completedProfile.studentId)

        // 4. Future launches retain completed setup and immutable ID
        val reloaded = StudentPreferences.getStudentProfile(context)
        assertEquals(studentName, reloaded.studentName)
        assertEquals(candidateId, reloaded.studentId)
        assertTrue(StudentPreferences.hasCompletedSetup(context))

        // 5. Updating student name in profile does NOT change student ID
        StudentPreferences.saveStudentName(context, "Priya S. Patel")
        val updatedNameProfile = StudentPreferences.getStudentProfile(context)
        assertEquals("Priya S. Patel", updatedNameProfile.studentName)
        assertEquals(candidateId, updatedNameProfile.studentId)
    }

    @Test
    fun testMultipleUniqueCandidateGenerations() {
        val generatedIds = mutableSetOf<String>()
        for (i in 1..100) {
            val id = StudentIdGenerator.generateCandidateId()
            assertTrue(StudentIdGenerator.isValidStudentId(id))
            generatedIds.add(id)
        }
        // Collision resistance across 100 generations
        assertEquals(100, generatedIds.size)
    }
}
