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

data class SideBarMenuItem(
    val icon: String,
    val label: String,
    val route: String
)

@Composable
fun SideBarContent(
    navController: NavController,
    drawerState: DrawerState,
    scope: CoroutineScope
) {
    val menuItems = listOf(
        SideBarMenuItem("🖥",  "POS",                  "pos"),
        SideBarMenuItem("🗑",  "Waste Management",     "waste_management"),
        SideBarMenuItem("📦",  "Inventory Adjustment", "inventory_adjustment"),
        SideBarMenuItem("🔃",  "Restock",              "restock"),
        SideBarMenuItem("📈",  "Sales Summary",        "sales_summary"),
        SideBarMenuItem("🧾",  "Transaction History",  "transacHistory"),
        SideBarMenuItem("🔔",  "Notification",         "notification"),
    )

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(270.dp)
            .background(Color(0xFF2E7D32))
    ) {

        // ── Header ────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 48.dp, bottom = 20.dp)
        ) {
            // Hamburger
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

            // User info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        scope.launch { drawerState.close() }
                        navController.navigate("staff_dashboard") {
                            popUpTo("home") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
            ) {
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
                        text = "Mariz Tuliao",
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

        // ── Menu Items ────────────────────────────────────────
        menuItems.forEach { item ->
            val isActive = item.label == "POS"
            SideBarItem(
                icon = item.icon,
                label = item.label,
                isActive = isActive,
                onClick = {
                    if (item.route.isNotEmpty()) {
                        scope.launch { drawerState.close() }
                        navController.navigate(item.route) {
                            popUpTo("home") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // ── Footer ────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Fix 2 — replace named padding with explicit padding calls
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
    icon: String,
    label: String,
    isActive: Boolean = false,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isActive) Color(0xFF1B5E20) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = icon, fontSize = 20.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = label,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}