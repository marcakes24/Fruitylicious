package com.example.fruitylicious.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor() {

    fun start(context: Context) {
        scheduleNextSync(context)
    }

    fun syncNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(networkConstraints())
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_IMMEDIATE_SYNC_WORK,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun stop(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_SYNC_WORK)
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_IMMEDIATE_SYNC_WORK)
    }

    companion object {
        private const val UNIQUE_SYNC_WORK = "fruitylicious_background_sync"
        private const val UNIQUE_IMMEDIATE_SYNC_WORK = "fruitylicious_immediate_sync"
        private const val SYNC_INTERVAL_MINUTES = 5L

        fun scheduleNextSync(context: Context) {
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setInitialDelay(SYNC_INTERVAL_MINUTES, TimeUnit.MINUTES)
                .setConstraints(networkConstraints())
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_SYNC_WORK,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        private fun networkConstraints(): Constraints {
            return Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        }
    }
}