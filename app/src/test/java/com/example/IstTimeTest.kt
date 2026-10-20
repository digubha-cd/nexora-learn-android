package com.example

import com.example.core.util.IstTimeUtil
import com.example.core.repository.ExamRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * Unit tests for India Standard Time (IST) utility and synchronized exam countdowns.
 */
class IstTimeTest {

    @Before
    fun setUp() {
        ExamRepository.resetForTesting()
    }

    @Test
    fun testIstTimezoneIsAsiaKolkata() {
        val zoneId = IstTimeUtil.IST_ZONE_ID
        assertEquals("Asia/Kolkata", zoneId.id)
    }

    @Test
    fun testIstZonedDateTimeAndFormatting() {
        val zdt = IstTimeUtil.getCurrentZonedDateTime()
        assertNotNull(zdt)
        assertEquals("Asia/Kolkata", zdt.zone.id)

        val time12h = IstTimeUtil.getFormattedTime12h(zdt)
        assertNotNull(time12h)
        assertTrue("12h time format must contain AM or PM", time12h.contains("AM") || time12h.contains("PM"))

        val time24h = IstTimeUtil.getFormattedTime24h(zdt)
        assertNotNull(time24h)
        assertTrue("24h time format must contain colon separator", time24h.contains(":"))

        val formattedWithPref = IstTimeUtil.getFormattedTime(zdt, is24Hour = true)
        assertEquals(time24h, formattedWithPref)

        val dateStr = IstTimeUtil.getFormattedDate(zdt)
        assertNotNull(dateStr)
        assertTrue("Date format must not be blank", dateStr.isNotBlank())
    }

    @Test
    fun testExamCountdownsSynchronizedWithIstDate() {
        // Set fixed IST reference date for deterministic test
        val testDate = LocalDate.of(2026, 9, 27)
        ExamRepository.currentDate = testDate

        val schoolDays = ExamRepository.getSchoolExamDaysRemaining(testDate)
        // 27 Sep 2026 to 22 Oct 2026 -> 25 days
        assertEquals(25L, schoolDays)

        val boardDays = ExamRepository.getBoardExamDaysRemaining(testDate)
        // 27 Sep 2026 to 25 Feb 2027 -> 151 days
        assertEquals(151L, boardDays)

        val schoolCountdown = ExamRepository.formatSchoolExamCountdown(testDate)
        assertEquals("School Exam — 25 days", schoolCountdown)

        val boardCountdown = ExamRepository.formatBoardExamCountdown(testDate)
        assertEquals("Board Exam — 151 days", boardCountdown)
    }

    @Test
    fun testCalculateExamCountdownPrecision() {
        // Reference ZonedDateTime in IST: 2026-09-27 10:00:00 IST
        val refZdt = ZonedDateTime.of(2026, 9, 27, 10, 0, 0, 0, IstTimeUtil.IST_ZONE_ID)
        val examDate = LocalDate.of(2026, 9, 28) // 24 hours later (midnight 28 Sep)
        val countdown = IstTimeUtil.calculateExamCountdown(examDate, refZdt)

        assertEquals(0L, countdown.days)
        assertEquals(14L, countdown.hours) // 10:00 AM to midnight = 14 hours remaining
        assertEquals(0L, countdown.minutes)
        assertEquals(0L, countdown.seconds)
        assertFalse(countdown.isExamDay)
        assertFalse(countdown.isCompleted)
    }

    @Test
    fun testExamDayAndCompletedStatusInIst() {
        // Test Exam Day status
        val examDate = LocalDate.of(2026, 10, 22)
        val refZdtExamDay = ZonedDateTime.of(2026, 10, 22, 11, 0, 0, 0, IstTimeUtil.IST_ZONE_ID)
        val examTime = LocalTime.of(10, 30)
        val examEndTime = LocalTime.of(13, 30)

        val examDayCountdown = IstTimeUtil.calculateExamCountdown(
            targetDate = examDate,
            currentZdt = refZdtExamDay,
            targetTime = examTime,
            targetEndTime = examEndTime
        )
        assertTrue("Must mark as Exam Day during active exam day", examDayCountdown.isExamDay)
        assertFalse("Must not mark as completed during exam day before end time", examDayCountdown.isCompleted)

        // Test Past Exam Completed status
        val refZdtPast = ZonedDateTime.of(2026, 10, 23, 9, 0, 0, 0, IstTimeUtil.IST_ZONE_ID)
        val pastCountdown = IstTimeUtil.calculateExamCountdown(
            targetDate = examDate,
            currentZdt = refZdtPast,
            targetTime = examTime
        )
        assertTrue("Must mark as completed for past exam dates", pastCountdown.isCompleted)
        assertFalse("Past date is not exam day", pastCountdown.isExamDay)
    }

    @Test
    fun testParseExamTimeDefaulting() {
        val parsedDefault = IstTimeUtil.parseExamTime(null)
        assertEquals(null, parsedDefault)

        val parsedSpecified = IstTimeUtil.parseExamTime("10:30 AM")
        assertEquals(LocalTime.of(10, 30), parsedSpecified)
    }
}
