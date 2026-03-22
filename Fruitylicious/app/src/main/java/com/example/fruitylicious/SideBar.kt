package com.example.fruitylicious

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SideBarContent() {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(260.dp)
            .background(Color(0xFF2E7D32))
            .padding(horizontal = 20.dp, vertical = 32.dp)
    ) {

        // ── Hamburger icon ─────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .width(22.dp)
                        .height(2.dp)
                        .background(Color.White, RoundedCornerShape(1.dp))
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── User Info ──────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(Color(0xFFC62828), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "E1",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = "Sean Andrei",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "seanandreemployee1@gmail.com",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
        Divider(color = Color.White.copy(alpha = 0.2f))
        Spacer(modifier = Modifier.height(16.dp))

        // ── Access Here Label ──────────────────────────────────
        Text(
            text = "ACCESS HERE",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ── Nav Items ──────────────────────────────────────────
        val menuItems = listOf(
            "🏪" to "Inventory",
            "📊" to "Dashboard",
            "🧾" to "Purchased Orders",
            "🚚" to "Suppliers",
            "📋" to "Stocks Reports",
            "🔔" to "Alerts",
            "🔄" to "Movements",
            "⚙️" to "Settings"
        )

        menuItems.forEach { (emoji, label) ->
            SideBarItem(emoji = emoji, label = label)
        }

        Spacer(modifier = Modifier.weight(1f))

        // ── Footer ─────────────────────────────────────────────
        Divider(color = Color.White.copy(alpha = 0.2f))
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "All rights reserved 2025",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 11.sp
        )
    }
}

@Composable
fun SideBarItem(emoji: String, label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = emoji, fontSize = 18.sp)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}