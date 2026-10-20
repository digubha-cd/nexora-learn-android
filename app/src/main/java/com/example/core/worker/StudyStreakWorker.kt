package com.example.core.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.core.repository.StudyPlannerRepository
import com.example.core.storage.StudentPreferences
import java.util.concurrent.TimeUnit

class StudyStreakWorker(
    appContext: Context,
    workerParams: androidx.work.WorkerParameters
) : androidx.work.CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "StudyStreakWorker"
        const val WORK_NAME = "StudyStreakDailySyncWork"

        fun schedule(context: Context) {
            try {
                val constraints = Constraints.Builder().build()

                val request = PeriodicWorkRequestBuilder<StudyStreakWorker>(
                    24, TimeUnit.HOURS
                )
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
                Log.d(TAG, "StudyStreakWorker scheduled successfully.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to schedule StudyStreakWorker: ${e.message}", e)
            }
        }
    }

    override suspend fun doWork(): Result {
        return try {
            val appContext = applicationContext
            val streak = StudyPlannerRepository.getStudyStreak()
            val currentProfile = StudentPreferences.getStudentProfile(appContext)
            val updated = currentProfile.copy(
                studyStreakDays = streak.currentStreak
            )
            StudentPreferences.updateStudentProfile(appContext, updated)
            Log.d(TAG, "StudyStreakWorker: Locally updated study streak. Current: ${streak.currentStreak} days")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "StudyStreakWorker: Error updating study streak: ${e.message}", e)
            Result.retry()
        }
    }
}
