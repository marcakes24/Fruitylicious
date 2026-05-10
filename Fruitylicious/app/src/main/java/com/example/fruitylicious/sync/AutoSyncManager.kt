package com.example.fruitylicious.sync

import android.util.Log
import com.example.fruitylicious.data.repository.SyncRepository
import com.example.fruitylicious.di.ApplicationScope
import com.example.fruitylicious.util.NetworkMonitor
import com.example.fruitylicious.util.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutoSyncManager @Inject constructor(
    private val syncRepository: SyncRepository,
    private val sessionManager: SessionManager,
    private val networkMonitor: NetworkMonitor,
    @ApplicationScope private val applicationScope: CoroutineScope
) {
    private var syncJob: Job? = null
    private var periodicSyncJob: Job? = null
    private val mutex = Mutex()
    private var isSyncing = false

    fun requestSync(reason: String) {
        Log.d("AutoSyncManager", "Sync requested: $reason")
        sessionManager.setPendingSync(true)
        
        if (!networkMonitor.isOnline()) {
            Log.d("AutoSyncManager", "Sync deferred: offline")
            return
        }

        scheduleSync()
    }

    private fun scheduleSync() {
        applicationScope.launch {
            mutex.withLock {
                if (isSyncing) return@launch
                
                syncJob?.cancel()
                syncJob = launch {
                    delay(2000) // Debounce 2 seconds
                    performSync()
                }
            }
        }
    }

    private suspend fun performSync() {
        if (isSyncing) return
        
        try {
            isSyncing = true
            Log.d("AutoSyncManager", "Auto-sync starting...")
            
            val lastPulledAt = sessionManager.getLastPulledAt()
            val result = syncRepository.smartSync(lastPulledAt)
            
            if (result.success) {
                Log.d("AutoSyncManager", "Auto-sync success: ${result.message}")
                sessionManager.setPendingSync(false)
            } else {
                Log.w("AutoSyncManager", "Auto-sync failed: ${result.message}")
            }
        } catch (e: Exception) {
            Log.e("AutoSyncManager", "Auto-sync error", e)
        } finally {
            isSyncing = false
        }
    }

    fun startNetworkObserver() {
        applicationScope.launch {
            networkMonitor.observeNetworkStatus().collectLatest { isOnline ->
                if (isOnline && (sessionManager.isPendingSync() || syncRepository.getUnsyncedCount() > 0)) {
                    Log.d("AutoSyncManager", "Network online, triggering pending sync")
                    performSync()
                }
            }
        }
    }

    fun startPeriodicSync() {
        periodicSyncJob?.cancel()
        periodicSyncJob = applicationScope.launch {
            while (true) {
                delay(5 * 60 * 1000) // 5 minutes
                if (networkMonitor.isOnline()) {
                    Log.d("AutoSyncManager", "Periodic sync triggered (5m interval)")
                    performSync()
                }
            }
        }
    }

    fun stopPeriodicSync() {
        periodicSyncJob?.cancel()
        periodicSyncJob = null
    }
}
