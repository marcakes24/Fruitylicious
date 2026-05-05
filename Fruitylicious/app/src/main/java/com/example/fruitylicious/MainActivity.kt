package com.example.fruitylicious

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import com.example.fruitylicious.sync.SyncManager
import com.example.fruitylicious.sync.AutoSyncManager
import com.example.fruitylicious.util.SessionManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var syncManager: SyncManager

    @Inject
    lateinit var autoSyncManager: AutoSyncManager

    @Inject
    lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        syncManager.start(applicationContext)
        syncManager.syncNow(applicationContext)
        autoSyncManager.startNetworkObserver()

        setContent {
            MaterialTheme {
                Surface(color = Color(0xFFFFFDF6)) {
                    FruityliciousNavGraph(sessionManager = sessionManager)
                }
            }
        }
    }
}