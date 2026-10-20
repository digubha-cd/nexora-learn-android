package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.model.Subject
import com.example.core.repository.StudyPlannerRepository
import com.example.core.timer.StudyTimerManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit and Robolectric tests for the persistent Study Focus Timer component.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StudyTimerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        StudyPlannerRepository.resetForTesting()
        StudyTimerManager.initialize(context)
        StudyTimerManager.resetForTesting()
    }

    @Test
    fun testInitialTimerStateDefaults() {
        val state = StudyTimerManager.timerState.value
        assertEquals("accounts", state.subjectId)
        assertEquals("Elements of Accounts", state.subjectName)
        assertEquals(0L, state.elapsedSeconds)
        assertFalse(state.isRunning)
        assertFalse(state.isPaused)
        assertFalse(state.isCompleted)
        assertEquals("00:00", state.formattedElapsedTime)
    }

    @Test
    fun testSelectOfficialSubjectUpdatesTimer() {
        val statSubject = Subject.OFFICIAL_SUBJECTS.find { it.id == "stat" }!!
        StudyTimerManager.selectSubject(statSubject)

        val state = StudyTimerManager.timerState.value
        assertEquals("stat", state.subjectId)
        assertEquals("Statistics", state.subjectName)

        val ecoSubject = Subject.OFFICIAL_SUBJECTS.find { it.id == "economics" }!!
        StudyTimerManager.selectSubject(ecoSubject)
        assertEquals("economics", StudyTimerManager.timerState.value.subjectId)
    }

    @Test
    fun testStartPauseResumeAndResetFlow() {
        // 1. Start timer
        StudyTimerManager.startTimer()

        assertTrue(StudyTimerManager.timerState.value.isRunning)
        assertFalse(StudyTimerManager.timerState.value.isPaused)

        // 2. Pause timer
        StudyTimerManager.pauseTimer()
        assertTrue(StudyTimerManager.timerState.value.isPaused)

        // 3. Resume timer
        StudyTimerManager.resumeTimer()
        assertTrue(StudyTimerManager.timerState.value.isRunning)
        assertFalse(StudyTimerManager.timerState.value.isPaused)

        // 4. Reset timer
        StudyTimerManager.resetTimer()
        assertFalse(StudyTimerManager.timerState.value.isRunning)
        assertFalse(StudyTimerManager.timerState.value.isPaused)
    }

    @Test
    fun testSessionStats() {
        assertEquals(0, StudyTimerManager.timerState.value.completedSessionsCount)

        val baSubject = Subject.OFFICIAL_SUBJECTS.find { it.id == "ba" }!!
        StudyTimerManager.selectSubject(baSubject)
        StudyTimerManager.startTimer()
        StudyTimerManager.resetTimer()

        val state = StudyTimerManager.timerState.value
        assertFalse(state.isRunning)
    }
}
