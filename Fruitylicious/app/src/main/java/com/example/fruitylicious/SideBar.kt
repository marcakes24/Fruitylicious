package com.example.fruitylicious

import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope

@Composable
fun SideBarContent(
    navController: NavController,
    drawerState: DrawerState,
    scope: CoroutineScope
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(260.dp)
            .background(Color(0xFF2E7D32))
    ) {

        // ── Top Section ────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 32.dp)
        ) {

            // Hamburger icon
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

            // ── User Info ─────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Person icon circle
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1B5E20)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("👤", fontSize = 20.sp)
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
                        text = "Staff",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 12.sp
                    )
                }
            }
        }

        // ── Nav Items ──────────────────────────────────────────
        val menuItems = listOf(
            "🏪" to "POS"        to "inventory",
            "📊" to "Waste Management"        to "home",
            "🧾" to "Inventory Adjustment" to "",
            "🚚" to "Restock"        to "suppliers",
            "📋" to "Sales Summary"   to "stocks_report_main",
            "🔔" to "Transaction History"           to "stock_alerts",
            "🔄" to "Notification"        to "movements"
        )

        menuItems.forEach { (data, route) ->
            val (emoji, label) = data
            // Highlight first item as active (POS-style highlight)
            val isActive = route == "inventory"
            SideBarItem(
                emoji = emoji,
                label = label,
                isActive = isActive,
                onClick = {
                    if (route.isNotEmpty()) {
                        scope.launch { drawerState.close() }
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 32.dp)
        ) {
            HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "All rights reserved 2025",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun SideBarItem(
    emoji: String,
    label: String,
    isActive: Boolean = false,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isActive) Color(0xFF1B5E20) else Color.Transparent
            )
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Icon in dark circle bg for active, plain for others
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isActive) Color(0xFF2E7D32) else Color.Transparent
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.width(14.dp))

            Text(
                text = label,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}