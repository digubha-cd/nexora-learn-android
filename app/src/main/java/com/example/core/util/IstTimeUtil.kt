package com.example.core.util

import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class ExamCountdownTime(
    val days: Long = 0L,
    val hours: Long = 0L,
    val minutes: Long = 0L,
    val seconds: Long = 0L,
    val isExamDay: Boolean = false,
    val isCompleted: Boolean = false
)

object IstTimeUtil {
    val IST_ZONE_ID: ZoneId = ZoneId.of("Asia/Kolkata")

    fun getCurrentLocalDate(): LocalDate {
        return LocalDate.now(IST_ZONE_ID)
    }

    fun getCurrentZonedDateTime(): ZonedDateTime {
        return ZonedDateTime.now(IST_ZONE_ID)
    }

    fun getFormattedTime12h(zdt: ZonedDateTime = getCurrentZonedDateTime()): String {
        return zdt.format(DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH))
    }

    fun getFormattedTime24h(zdt: ZonedDateTime = getCurrentZonedDateTime()): String {
        return zdt.format(DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH))
    }

    fun getFormattedTime(zdt: ZonedDateTime = getCurrentZonedDateTime(), is24Hour: Boolean = false): String {
        return if (is24Hour) getFormattedTime24h(zdt) else getFormattedTime12h(zdt)
    }

    fun getFormattedDate(zdt: ZonedDateTime = getCurrentZonedDateTime()): String {
        return zdt.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH))
    }

    fun getFormattedFullDate(zdt: ZonedDateTime = getCurrentZonedDateTime()): String {
        return zdt.format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy", Locale.ENGLISH))
    }

    fun parseExamTime(timeStr: String?): LocalTime? {
        if (timeStr.isNullOrBlank()) return null
        return try {
            LocalTime.parse(timeStr.trim())
        } catch (_: Exception) {
            try {
                LocalTime.parse(timeStr.trim(), DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH))
            } catch (_: Exception) {
                null
            }
        }
    }

    fun parseExamEndTime(timeStr: String?): LocalTime? {
        return parseExamTime(timeStr)
    }

    fun calculateExamCountdown(
        targetDate: LocalDate,
        currentZdt: ZonedDateTime = getCurrentZonedDateTime(),
        targetTime: LocalTime? = null,
        targetEndTime: LocalTime? = null
    ): ExamCountdownTime {
        val today = currentZdt.toLocalDate()
        val examStartTime = targetTime ?: LocalTime.of(10, 30)
        val examStartZdt = targetDate.atTime(examStartTime).atZone(IST_ZONE_ID)
        val examEndTime = targetEndTime ?: LocalTime.of(13, 30)
        val examEndZdt = targetDate.atTime(examEndTime).atZone(IST_ZONE_ID)

        val isCompleted = currentZdt.isAfter(examEndZdt)
        val isExamDay = today == targetDate && !isCompleted

        if (isCompleted) {
            return ExamCountdownTime(
                days = 0,
                hours = 0,
                minutes = 0,
                seconds = 0,
                isExamDay = false,
                isCompleted = true
            )
        }

        val targetZdt = if (targetTime != null) examStartZdt else targetDate.atStartOfDay(IST_ZONE_ID)
        val duration = Duration.between(currentZdt, targetZdt)
        val totalSeconds = duration.seconds

        if (totalSeconds <= 0) {
            return ExamCountdownTime(
                days = 0,
                hours = 0,
                minutes = 0,
                seconds = 0,
                isExamDay = isExamDay,
                isCompleted = false
            )
        }

        val days = totalSeconds / (24 * 3600)
        val hours = (totalSeconds % (24 * 3600)) / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return ExamCountdownTime(
            days = days,
            hours = hours,
            minutes = minutes,
            seconds = seconds,
            isExamDay = isExamDay,
            isCompleted = false
        )
    }
}
