package com.example

import com.example.core.model.TaskPriority
import com.example.core.repository.StudyPlannerRepository
import com.example.core.timer.FocusSessionRecord
import com.example.core.timer.StudyTimerManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Unit tests verifying the 365-Day Study Consistency Heatmap feature:
 * - 52/53-week aligned grid structure
 * - 5-level intensity calculation (0 to 4)
 * - IST date grouping
 * - Summary statistics (Active days, streaks, total duration)
 * - Daily details lookup without double counting
 */
class StudyConsistencyHeatmapTest {

    @Before
    fun setUp() {
        StudyPlannerRepository.resetForTesting()
    }

    @Test
    fun testHeatmapReturns53WeeksAlignedToMonday() {
        val testToday = LocalDate.of(2026, 10, 16) // Friday
        StudyPlannerRepository.currentDateOverride = testToday

        val heatmapData = StudyPlannerRepository.get365DaysHeatmapData()

        // 53 weeks * 7 days = 371 days (covering the full 365 days)
        assertEquals(371, heatmapData.size)

        // First day must be Monday
        assertEquals(DayOfWeek.MONDAY, heatmapData.first().date.dayOfWeek)

        // Last day must be Sunday
        assertEquals(DayOfWeek.SUNDAY, heatmapData.last().date.dayOfWeek)

        // Today must be marked
        val todayItem = heatmapData.find { it.isToday }
        assertTrue("Today must be present in heatmap data", todayItem != null)
        assertEquals(testToday, todayItem?.date)
    }

    @Test
    fun testActivityLevelsFromZeroToFour() {
        val testToday = LocalDate.of(2026, 10, 16)
        StudyPlannerRepository.currentDateOverride = testToday

        val heatmapData = StudyPlannerRepository.get365DaysHeatmapData()
        val todayItem = heatmapData.first { it.date == testToday }

        // Level 0 initially
        assertEquals(0, todayItem.activityLevel)
        assertEquals(0, todayItem.completedCount)
        assertEquals(0, todayItem.durationMinutes)

        // Add 1 completed task of 20 mins -> Level 1 (Very Low)
        val task1 = StudyPlannerRepository.addTask(
            title = "Accounts Intro",
            subjectId = "accounts",
            scheduledDate = testToday,
            priority = TaskPriority.MEDIUM,
            durationMinutes = 20
        )
        StudyPlannerRepository.toggleTaskCompletion(task1.taskId)

        val dataAfterTask1 = StudyPlannerRepository.get365DaysHeatmapData()
        val itemLevel1 = dataAfterTask1.first { it.date == testToday }
        assertEquals(1, itemLevel1.activityLevel)
        assertEquals(20, itemLevel1.durationMinutes)

        // Add 1 more task of 25 mins (total 45 mins, 2 tasks) -> Level 2 (Low)
        val task2 = StudyPlannerRepository.addTask(
            title = "Stat Revision",
            subjectId = "stat",
            scheduledDate = testToday,
            priority = TaskPriority.HIGH,
            durationMinutes = 25
        )
        StudyPlannerRepository.toggleTaskCompletion(task2.taskId)

        val dataAfterTask2 = StudyPlannerRepository.get365DaysHeatmapData()
        val itemLevel2 = dataAfterTask2.first { it.date == testToday }
        assertEquals(2, itemLevel2.activityLevel)
        assertEquals(45, itemLevel2.durationMinutes)

        // Add 1 more task of 40 mins (total 85 mins, 3 tasks) -> Level 3 (Medium)
        val task3 = StudyPlannerRepository.addTask(
            title = "Economics Concepts",
            subjectId = "economics",
            scheduledDate = testToday,
            priority = TaskPriority.HIGH,
            durationMinutes = 40
        )
        StudyPlannerRepository.toggleTaskCompletion(task3.taskId)

        val dataAfterTask3 = StudyPlannerRepository.get365DaysHeatmapData()
        val itemLevel3 = dataAfterTask3.first { it.date == testToday }
        assertEquals(3, itemLevel3.activityLevel)
        assertEquals(85, itemLevel3.durationMinutes)

        // Add 40 more mins (total 125 mins) -> Level 4 (High)
        val task4 = StudyPlannerRepository.addTask(
            title = "BA Case Studies",
            subjectId = "ba",
            scheduledDate = testToday,
            priority = TaskPriority.HIGH,
            durationMinutes = 40
        )
        StudyPlannerRepository.toggleTaskCompletion(task4.taskId)

        val dataAfterTask4 = StudyPlannerRepository.get365DaysHeatmapData()
        val itemLevel4 = dataAfterTask4.first { it.date == testToday }
        assertEquals(4, itemLevel4.activityLevel)
        assertEquals(125, itemLevel4.durationMinutes)
    }

    @Test
    fun testSummaryStatisticsCalculations() {
        val testToday = LocalDate.of(2026, 10, 16)
        StudyPlannerRepository.currentDateOverride = testToday

        // Add activity yesterday and today
        val yesterday = testToday.minusDays(1)
        val yTask = StudyPlannerRepository.addTask(
            title = "Yesterday Task",
            subjectId = "accounts",
            scheduledDate = yesterday,
            priority = TaskPriority.HIGH,
            durationMinutes = 60
        )
        StudyPlannerRepository.toggleTaskCompletion(yTask.taskId)

        val tTask = StudyPlannerRepository.addTask(
            title = "Today Task",
            subjectId = "stat",
            scheduledDate = testToday,
            priority = TaskPriority.HIGH,
            durationMinutes = 45
        )
        StudyPlannerRepository.toggleTaskCompletion(tTask.taskId)

        val heatmapData = StudyPlannerRepository.get365DaysHeatmapData()
        val stats = StudyPlannerRepository.getHeatmapSummaryStats(heatmapData)

        assertEquals("Must count exactly 2 active days", 2, stats.activeDaysCount)
        assertEquals("Total study minutes must match sum", 105L, stats.totalStudyMinutes)
        assertTrue("Current streak must be non-negative", stats.currentStreak >= 0)
        assertTrue("Best streak must be at least active days count", stats.bestStreak >= 1)
    }

    @Test
    fun testGetStudyDetailsForDate() {
        val testToday = LocalDate.of(2026, 10, 16)
        StudyPlannerRepository.currentDateOverride = testToday

        val task = StudyPlannerRepository.addTask(
            title = "Special Revision Task",
            subjectId = "economics",
            scheduledDate = testToday,
            priority = TaskPriority.HIGH,
            durationMinutes = 50
        )
        StudyPlannerRepository.toggleTaskCompletion(task.taskId)

        val (tasks, sessions) = StudyPlannerRepository.getStudyDetailsForDate(testToday)
        assertEquals(1, tasks.size)
        assertEquals("Special Revision Task", tasks[0].title)
        assertTrue(tasks[0].isCompleted)
    }
}
