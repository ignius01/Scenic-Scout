package com.example.background

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.AppContainer

class ScoutSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val pinId = inputData.getLong("PIN_ID", -1L)
        if (pinId == -1L) return Result.failure()

        Log.d("ScoutSyncWorker", "Starting sync job for pin ID: $pinId")
        try {
            val container = AppContainer(applicationContext)
            container.scenicRepository.syncWeatherForPin(pinId)
            return Result.success()
        } catch (e: Exception) {
            Log.e("ScoutSyncWorker", "Error in sync job: ${e.localizedMessage}")
            return Result.retry()
        }
    }
}

object ScoutSyncManager {
    fun enqueueWeatherSync(context: Context, pinId: Long) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val inputData = Data.Builder()
            .putLong("PIN_ID", pinId)
            .build()

        val syncWorkRequest = OneTimeWorkRequestBuilder<ScoutSyncWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(context).enqueue(syncWorkRequest)
    }
}
