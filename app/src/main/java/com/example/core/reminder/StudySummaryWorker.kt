package com.example.core.reminder

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.R
import com.example.core.model.Subject
import com.example.core.repository.StudyPlannerRepository
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

import android.annotation.SuppressLint

class StudySummaryWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        if (!com.example.core.storage.StudentPreferences.isWeeklySummaryEnabled(context)) {
            return Result.success()
        }
        
        val summary = StudyPlannerRepository.getWeeklySummaryStats()
        
        val totalHours = summary.totalMinutes / 60
        val totalMins = summary.totalMinutes % 60
        
        val subjectBreakdown = summary.subjectBreakdown.entries
            .sortedByDescending { it.value }
            .take(2)
            .joinToString(", ") { entry ->
                val name = Subject.OFFICIAL_SUBJECTS.find { it.id == entry.key }?.name ?: entry.key
                "$name (${entry.value / 60}h)"
            }
        
        val contentText = "You studied ${totalHours}h ${totalMins}m this week! " +
                "Top subjects: $subjectBreakdown. " +
                "Current Streak: ${summary.currentStreak} days. " +
                "Keep going for that 90+ goal!"
                
        showNotification(contentText)
        
        return Result.success()
    }

    @SuppressLint("MissingPermission")
    private fun showNotification(text: String) {
        val channelId = "weekly_study_summary"
        val notificationManager = NotificationManagerCompat.from(applicationContext)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Weekly Study Summary",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }
        
        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Placeholder
            .setContentTitle("Weekly Study Summary 🎯")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            
        // Assuming MainActivity is the correct entry point
        // val pendingIntent = PendingIntent.getActivity(...) 
        
        try {
            notificationManager.notify(999, builder.build())
        } catch (_: SecurityException) {
        } catch (_: Exception) {
        }
    }
}
