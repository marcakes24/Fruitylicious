package com.example.fruitylicious

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable // Addition: Added clickable
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
import androidx.navigation.NavController // Addition: Added NavController import

@Composable
fun SideBarContent(navController: NavController) { // Addition: Accept navController
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
        HorizontalDivider(color = Color.White.copy(alpha = 0.2f)) // Addition: Updated to Material3 HorizontalDivider
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
            "🏪" to "Inventory" to "",
            "📊" to "Dashboard" to "home",
            "🧾" to "Purchased Orders" to "",
            "🚚" to "Suppliers" to "",
            "📋" to "Stocks Reports" to "stocks_report",
            "🔔" to "Alerts" to "stock_alerts",
            "🔄" to "Movements" to "",
            "⚙️" to "Settings" to ""
        )

        menuItems.forEach { (data, route) ->
            val (emoji, label) = data
            SideBarItem(
                emoji = emoji,
                label = label,
                onClick = {
                    if (route.isNotEmpty()) {
                        navController.navigate(route) {
                            popUpTo("home") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // ── Footer ─────────────────────────────────────────────
        HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "All rights reserved 2025",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 11.sp
        )
    }
}

@Composable
fun SideBarItem(emoji: String, label: String, onClick: () -> Unit = {}) { // Addition: Added onClick
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() } // Addition: Made clickable
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