package com.example.fruitylicious

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import com.example.fruitylicious.sync.SyncManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var syncManager: SyncManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        syncManager.start(applicationContext)
        syncManager.syncNow(applicationContext)

        setContent {
            MaterialTheme {
                Surface(color = Color(0xFFFFFDF6)) {
                    FruityliciousNavGraph()
                }
            }
        }
    }
}