package com.example.core.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.core.model.StudyTask
import com.example.core.repository.StudyPlannerRepository
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class DailyReminderConfig(
    val enabled: Boolean = true,
    val hour: Int = 17,
    val minute: Int = 0
) {
    val formattedTime: String
        get() {
            val localTime = LocalTime.of(hour, minute)
            return localTime.format(DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH))
        }
}

enum class ReminderSlot(val hour: Int, val minute: Int) {
    MORNING(7, 0),
    EVENING(17, 0),
    NIGHT(21, 0)
}

object StudyReminderManager {
    private const val TAG = "StudyReminderManager"
    private const val PREFS_NAME = "nexora_daily_reminder_prefs"
    private const val KEY_ENABLED = "daily_reminder_enabled"
    private const val KEY_HOUR = "daily_reminder_hour"
    private const val KEY_MINUTE = "daily_reminder_minute"

    fun scheduleReminder(context: Context, task: StudyTask, timeMillis: Long) {
        scheduleReminder(context, task.taskId, timeMillis, task.title, task.subjectId, task.subjectName)
    }

    fun scheduleReminder(
        context: Context,
        taskId: String,
        timeMillis: Long,
        taskTitle: String = "",
        subjectId: String = "",
        subjectName: String = ""
    ) {
        if (timeMillis <= System.currentTimeMillis()) return
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, StudyReminderReceiver::class.java).apply {
                action = "com.example.ACTION_STUDY_REMINDER"
                putExtra(StudyReminderReceiver.EXTRA_TASK_ID, taskId)
                putExtra(StudyReminderReceiver.EXTRA_TASK_TITLE, taskTitle)
                putExtra(StudyReminderReceiver.EXTRA_SUBJECT_ID, subjectId)
                putExtra(StudyReminderReceiver.EXTRA_SUBJECT_NAME, subjectName)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                taskId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent)
            Log.d(TAG, "Scheduled reminder for task $taskId at epoch $timeMillis")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling reminder for $taskId: ${e.message}")
        }
    }

    fun cancelReminder(context: Context, taskId: String) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, StudyReminderReceiver::class.java).apply {
                action = "com.example.ACTION_STUDY_REMINDER"
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                taskId.hashCode(),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
            Log.d(TAG, "Cancelled reminder for task $taskId")
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling reminder for $taskId: ${e.message}")
        }
    }

    fun setDailyStudyReminderPreferences(context: Context, enabled: Boolean, hour: Int, minute: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_ENABLED, enabled)
            .putInt(KEY_HOUR, hour)
            .putInt(KEY_MINUTE, minute)
            .apply()

        if (enabled) {
            scheduleDailyStudySessionReminder(context, hour, minute)
        } else {
            cancelDailyStudySessionReminder(context)
        }
    }

    fun getDailyStudyReminderPreferences(context: Context): DailyReminderConfig {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return DailyReminderConfig(
            enabled = prefs.getBoolean(KEY_ENABLED, true),
            hour = prefs.getInt(KEY_HOUR, 17),
            minute = prefs.getInt(KEY_MINUTE, 0)
        )
    }

    fun scheduleDailyStudySessionReminder(context: Context, hour: Int, minute: Int) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, StudyReminderReceiver::class.java).apply {
                action = StudyReminderReceiver.ACTION_DAILY_STUDY_SESSION_REMINDER
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                StudyReminderReceiver.NOTIFICATION_ID_DAILY_PLAN,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val zone = ZoneId.of("Asia/Kolkata")
            val now = java.time.ZonedDateTime.now(zone)
            var scheduledZdt = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
            if (scheduledZdt.isBefore(now)) {
                scheduledZdt = scheduledZdt.plusDays(1)
            }
            val epochMillis = scheduledZdt.toInstant().toEpochMilli()
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, epochMillis, pendingIntent)
            Log.d(TAG, "Scheduled daily study reminder for $hour:$minute at $epochMillis")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling daily study reminder: ${e.message}")
        }
    }

    fun cancelDailyStudySessionReminder(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, StudyReminderReceiver::class.java).apply {
                action = StudyReminderReceiver.ACTION_DAILY_STUDY_SESSION_REMINDER
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                StudyReminderReceiver.NOTIFICATION_ID_DAILY_PLAN,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling daily study reminder: ${e.message}")
        }
    }

    fun rescheduleAllPlannedStudyReminders(context: Context) {
        val tasks = StudyPlannerRepository.tasks.value
        val now = System.currentTimeMillis()
        for (task in tasks) {
            val epoch = task.reminderEpochMillis
            if (epoch != null && epoch > now && !task.isCompleted) {
                scheduleReminder(context, task, epoch)
            }
        }
        val config = getDailyStudyReminderPreferences(context)
        if (config.enabled) {
            scheduleDailyStudySessionReminder(context, config.hour, config.minute)
        }
    }

    fun sendImmediateDailyStudySessionNotification(context: Context) {
        val intent = Intent(context, StudyReminderReceiver::class.java).apply {
            action = StudyReminderReceiver.ACTION_DAILY_STUDY_SESSION_REMINDER
        }
        context.sendBroadcast(intent)
    }

    fun computeEpochMillis(date: LocalDate, time: LocalTime): Long {
        val zone = ZoneId.of("Asia/Kolkata")
        return date.atTime(time).atZone(zone).toInstant().toEpochMilli()
    }

    fun scheduleRemainingChaptersReminder(
        context: Context,
        subjectId: String,
        slot: ReminderSlot,
        date: LocalDate
    ) {
        val time = LocalTime.of(slot.hour, slot.minute)
        val epoch = computeEpochMillis(date, time)
        val finalEpoch = if (epoch < System.currentTimeMillis()) epoch + 24 * 60 * 60 * 1000L else epoch
        scheduleReminder(context, "remaining_$subjectId", finalEpoch, "Remaining Chapters Reminder", subjectId)
    }
}
