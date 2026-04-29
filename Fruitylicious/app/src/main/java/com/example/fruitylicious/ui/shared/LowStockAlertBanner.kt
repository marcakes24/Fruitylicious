package com.example.fruitylicious.ui.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun LowStockAlertBanner(
    lowStockCount: Int,
    modifier: Modifier = Modifier
) {
    if (lowStockCount <= 0) {
        return
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFFFF8E1))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Low stock warning",
            tint = Color(0xFFF57F17)
        )

        Text(
            text = "$lowStockCount ingredient${if (lowStockCount == 1) "" else "s"} below low stock threshold",
            color = Color(0xFFF57F17),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
