package com.example.fruitylicious

import kotlinx.coroutines.launch
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.CoroutineScope

@Composable
fun AdminSideBarContent(
    navController: NavController,
    drawerState: DrawerState,
    scope: CoroutineScope
) {
    // 1. Observe the current navigation backstack
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    // 2. Extract the current route string
    val currentRoute = navBackStackEntry?.destination?.route
    var reportsExpanded by remember { mutableStateOf(false) }

    val menuItems = listOf(
        Triple("🏠", "Dashboard",             "admin_home"),
        Triple("🖥️", "POS",                   "admin_pos"),
        Triple("📦", "Manage Products",        ""),
        Triple("🥣", "Manage Ingredients",     ""),
        Triple("📖", "Recipe Management",      ""),
        Triple("🔍", "Inventory Monitoring",   ""),
        Triple("📋", "Inventory Adjustment",   "admin_inventory_adjustment"),
        Triple("🗑️", "Waste Management",       "admin_waste_management"),
        Triple("🔃", "Restock",                "admin_restock"),
    )

    val reportSubItems = listOf(
        "Waste Report",
        "Restock Report",
        "Inventory Report",
        "Sales Report"
    )

    val bottomItems = listOf(
        Triple("🧾", "Transaction History", "admin_transacHistory"),
        Triple("👥", "User Management",     ""),
        Triple("🔔", "Notification",        "admin_notification"),
    )

    fun navigate(route: String) {
        if (route.isNotEmpty()) {
            scope.launch { drawerState.close() }
            navController.navigate(route) {
                popUpTo("admin_home") { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(270.dp)
            .background(Color(0xFF2E7D32))
    ) {

        // ── Header (NOT scrollable — stays fixed) ────────────────────────────
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
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                        text = "Mianne Navarro",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Owner",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 12.sp
                    )
                }
            }
        }

        // ── Scrollable menu area ─────────────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Main Menu Items ───────────────────────────────────────────────
            menuItems.forEach { (emoji, label, route) ->
                val isActive = currentRoute == route
                AdminSideBarItem(
                    emoji = emoji,
                    label = label,
                    isActive = isActive,
                    onClick = {
                        if (route.isNotEmpty() && currentRoute != route) {
                            scope.launch { drawerState.close() }
                            navController.navigate(route) {
                                popUpTo("admin_home") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        } else {
                            scope.launch { drawerState.close() }
                        }
                    }
                )
            }

            // ── Reports (expandable) ──────────────────────────────────────────
            AdminSideBarItem(
                emoji = "📊",
                label = "Reports",
                isActive = false,
                trailingContent = {
                    Text(
                        text = if (reportsExpanded) "∧" else "∨",
                        color = Color.White,
                        fontSize = 13.sp
                    )
                },
                onClick = { reportsExpanded = !reportsExpanded }
            )

            // Report sub-items dropdown
            AnimatedVisibility(
                visible = reportsExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    reportSubItems.forEach { subLabel ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1B5E20))
                                .clickable {
                                    scope.launch { drawerState.close() }
                                }
                                .padding(start = 56.dp, end = 20.dp, top = 13.dp, bottom = 13.dp)
                        ) {
                            Text(
                                text = subLabel,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // ── Bottom Menu Items ─────────────────────────────────────────────
            bottomItems.forEach { (emoji, label, route) ->
                val isActive = currentRoute == route   // ← add this
                AdminSideBarItem(
                    emoji = emoji,
                    label = label,
                    isActive = isActive,               // ← change from false
                    onClick = { navigate(route) }
                )
            }
        }

        // ── Footer (NOT scrollable — stays fixed) ────────────────────────────
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

// ── Admin Sidebar Item ────────────────────────────────────────────────────────

@Composable
fun AdminSideBarItem(
    emoji: String,
    label: String,
    isActive: Boolean = false,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isActive) Color(0xFF1B5E20) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 13.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = emoji, fontSize = 20.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = label,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            trailingContent?.invoke()
        }
    }
}
