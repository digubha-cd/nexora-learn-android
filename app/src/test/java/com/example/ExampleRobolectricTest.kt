package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.model.Subject
import com.example.core.repository.SyllabusRepository
import com.example.core.timer.StudyTimerManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        SyllabusRepository.initialize(context)
        StudyTimerManager.initialize(context)
        StudyTimerManager.resetForTesting()
    }

    @Test
    fun testSyllabusStructure() {
        val subjects = Subject.OFFICIAL_SUBJECTS
        assertEquals("Gujarat Board Class 12 Commerce must have 7 official subjects", 7, subjects.size)

        val accounts = subjects.first { it.id == "accounts" }
        assertEquals("Elements of Accounts", accounts.name)

        val stat = subjects.first { it.id == "stat" }
        assertEquals("Statistics", stat.name)
    }

    @Test
    fun testStudyTimerStateAndHistory() {
        val statSubject = Subject.OFFICIAL_SUBJECTS.first { it.id == "stat" }
        StudyTimerManager.selectSubject(statSubject)
        StudyTimerManager.startTimer()
        StudyTimerManager.pauseTimer()
        StudyTimerManager.resetTimer()

        assertFalse(StudyTimerManager.timerState.value.isRunning)
    }
}
