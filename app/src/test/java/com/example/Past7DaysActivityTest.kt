package com.example

import com.example.core.model.TaskPriority
import com.example.core.repository.StudyPlannerRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests for 7-Day Study Activity bar chart data and consistency calculations.
 */
class Past7DaysActivityTest {

    @Before
    fun setUp() {
        StudyPlannerRepository.resetForTesting()
    }

    @Test
    fun testPast7DaysActivityReturnsExactly7Days() {
        val testToday = LocalDate.of(2026, 9, 27)
        StudyPlannerRepository.currentDateOverride = testToday

        val activities = StudyPlannerRepository.getPast7DaysActivity()
        assertEquals(7, activities.size)

        // Oldest day is Today - 6 days (21 Sep 2026)
        assertEquals(LocalDate.of(2026, 9, 21), activities.first().date)
        assertEquals("2026-09-21", activities.first().dateStr)
        assertFalse(activities.first().isToday)

        // Newest day is Today (27 Sep 2026)
        assertEquals(testToday, activities.last().date)
        assertEquals("2026-09-27", activities.last().dateStr)
        assertTrue(activities.last().isToday)
    }

    @Test
    fun testPast7DaysActivityComputesCompletedTasksAndStreakAchieved() {
        val testToday = LocalDate.of(2026, 9, 27)
        StudyPlannerRepository.currentDateOverride = testToday

        // Add 2 tasks for today (1 completed, 1 pending)
        val todayTask1 = StudyPlannerRepository.addTask(
            title = "Accounts Revision",
            subjectId = "accounts",
            scheduledDate = testToday,
            priority = TaskPriority.HIGH,
            durationMinutes = 45
        )
        StudyPlannerRepository.addTask(
            title = "Stat Practice",
            subjectId = "stat",
            scheduledDate = testToday,
            priority = TaskPriority.MEDIUM,
            durationMinutes = 30
        )
        StudyPlannerRepository.toggleTaskCompletion(todayTask1.taskId)

        // Add 1 task for 2 days ago (completed)
        val pastDate = testToday.minusDays(2)
        val pastTask = StudyPlannerRepository.addTask(
            title = "Economics Graph",
            subjectId = "economics",
            scheduledDate = pastDate,
            priority = TaskPriority.LOW,
            durationMinutes = 40
        )
        StudyPlannerRepository.toggleTaskCompletion(pastTask.taskId)

        val activities = StudyPlannerRepository.getPast7DaysActivity()
        assertEquals(7, activities.size)

        val todayActivity = activities.last()
        assertEquals(1, todayActivity.completedCount)
        assertEquals(2, todayActivity.totalCount)
        assertTrue(todayActivity.isStreakAchieved)
        assertEquals(45, todayActivity.durationMinutes)

        val twoDaysAgoActivity = activities[4] // index 4 is 2 days ago (6 - 2 = 4)
        assertEquals(pastDate, twoDaysAgoActivity.date)
        assertEquals(1, twoDaysAgoActivity.completedCount)
        assertTrue(twoDaysAgoActivity.isStreakAchieved)
        assertEquals(40, twoDaysAgoActivity.durationMinutes)

        val emptyDayActivity = activities[0]
        assertEquals(0, emptyDayActivity.completedCount)
        assertFalse(emptyDayActivity.isStreakAchieved)
    }
}
