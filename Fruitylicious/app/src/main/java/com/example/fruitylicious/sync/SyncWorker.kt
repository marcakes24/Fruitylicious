package com.example.fruitylicious.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.fruitylicious.data.repository.SyncRepository
import com.example.fruitylicious.util.NetworkMonitor
import com.example.fruitylicious.util.SessionManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncRepository: SyncRepository,
    private val sessionManager: SessionManager,
    private val networkMonitor: NetworkMonitor
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        if (!networkMonitor.isOnline()) {
            SyncManager.scheduleNextSync(applicationContext)
            return Result.retry()
        }

        val lastPulledAt = sessionManager.getLastPulledAt()
        val syncResult = syncRepository.sync(lastPulledAt)
        val completedAt = System.currentTimeMillis()

        sessionManager.saveSyncStatus(
            syncedAt = completedAt,
            success = syncResult.success,
            message = syncResult.message
        )

        SyncManager.scheduleNextSync(applicationContext)

        return if (syncResult.success) {
            Result.success()
        } else {
            Result.retry()
        }
    }
}