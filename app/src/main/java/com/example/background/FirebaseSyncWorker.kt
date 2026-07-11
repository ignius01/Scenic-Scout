package com.example.background

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.AppContainer
import kotlinx.coroutines.flow.first

class FirebaseSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("FirebaseSyncWorker", "Starting offline-first backup sync to Firebase")
        return try {
            val container = AppContainer(applicationContext)
            val backupManager = container.firebaseBackupManager
            
            if (!backupManager.isLoggedIn) {
                Log.d("FirebaseSyncWorker", "User not logged in; skipping backup.")
                return Result.success()
            }
            
            val pins = container.scenicRepository.getAllPins().first()
            val result = backupManager.backupPins(pins)
            
            if (result.isSuccess) {
                Log.d("FirebaseSyncWorker", "Backup sync completed successfully.")
                Result.success()
            } else {
                Log.e("FirebaseSyncWorker", "Backup failed: ${result.exceptionOrNull()?.localizedMessage}")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e("FirebaseSyncWorker", "Error in sync job: ${e.localizedMessage}")
            Result.retry()
        }
    }
}

object FirebaseSyncManager {
    private const val SYNC_WORK_NAME = "firebase_backup_sync_work"

    /**
     * Enqueues a one-time Firebase backup work request to run when connected to the network.
     * 
     * Uses KEEP or REPLACE depending on if we want to overwrite pending queues. Replacing is safer
     * because it includes the newest local changes.
     */
    fun enqueueSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncWorkRequest = OneTimeWorkRequestBuilder<FirebaseSyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            SYNC_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            syncWorkRequest
        )
    }
}
