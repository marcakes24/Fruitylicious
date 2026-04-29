package com.example.fruitylicious.ui.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.fruitylicious.util.DateTimeUtil

@Composable
fun SyncStatusBar(
    isOnline: Boolean,
    lastSyncAt: Long,
    lastSyncSuccessful: Boolean,
    message: String,
    modifier: Modifier = Modifier
) {
    val background = when {
        !isOnline -> Color(0xFFFFF3E0)
        lastSyncSuccessful -> Color(0xFFE8F5E9)
        else -> Color(0xFFFFEBEE)
    }

    val contentColor = when {
        !isOnline -> Color(0xFFE65100)
        lastSyncSuccessful -> Color(0xFF1B5E20)
        else -> Color(0xFFB71C1C)
    }

    val icon = when {
        !isOnline -> Icons.Default.CloudOff
        lastSyncSuccessful -> Icons.Default.CloudDone
        else -> Icons.Default.Sync
    }

    val syncText = if (lastSyncAt > 0L) {
        "Last sync: ${DateTimeUtil.formatTime(lastSyncAt)}"
    } else {
        "Not synced yet"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(background)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Sync status",
            tint = contentColor
        )

        Text(
            text = if (message.isBlank()) syncText else "$syncText • $message",
            color = contentColor,
            style = MaterialTheme.typography.bodySmall
        )
    }
}