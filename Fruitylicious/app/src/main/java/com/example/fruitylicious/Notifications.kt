package com.example.fruitylicious

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// ── DATA MODEL ──────────────────────────────────────────────────────────────

data class NotificationData(
    val name: String,
    val sku: String,
    val current: String,
    val min: String,
    val status: String, // "Critical" or "Warning"
    val emoji: String,
    val branch: String, // "B1" or "B2"
    val progress: Float
)

@Composable
fun NotificationsScreen(
    navController: NavController,
    drawerState: DrawerState,
    scope: CoroutineScope
) {
    var selectedBranch by remember { mutableStateOf("All") }

    // Mock Notification Data
    val allNotifications = listOf(
        NotificationData("Mango", "SKU-0041 - Mangoes", "7 kg", "50 kg", "Critical", "🥭", "B1", 0.14f),
        NotificationData("Watermelon", "SKU-0018 - Melons", "12 kg", "40 kg", "Critical", "🍉", "B2", 0.3f),
        NotificationData("Avocado", "SKU-0012 - Avocados", "15 kg", "N/A", "Warning", "🥑", "B1", 0.45f),
        NotificationData("Mango", "SKU-0041 - Mangoes", "7 kg", "50 kg", "Warning", "🥭", "B2", 0.6f),
        NotificationData("Mango", "SKU-0041 - Mangoes", "7 kg", "50 kg", "Critical", "🥭", "B1", 0.1f),
        NotificationData("Mango", "SKU-0041 - Mangoes", "7 kg", "50 kg", "Warning", "🥭", "B1", 0.55f),
    )

    val filteredNotifications = if (selectedBranch == "All") {
        allNotifications
    } else {
        allNotifications.filter { it.branch == selectedBranch }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFEAA0)) // Pale yellow background
    ) {

        // ── TOP GREEN HEADER ──────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2E7D32)) // Brand Green
                .padding(top = 40.dp, bottom = 12.dp, start = 16.dp, end = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                }

                Text(
                    text = "NOTIFICATION",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                // Branch Selector Toggle
                Surface(
                    color = Color(0xFF1B5E20), // Darker green
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("B1", "B2", "All").forEach { branch ->
                            val isSelected = selectedBranch == branch
                            Surface(
                                color = if (isSelected) Color.White else Color.Transparent,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .clickable { selectedBranch = branch }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 10.dp)
                                ) {
                                    Text(
                                        text = branch,
                                        color = if (isSelected) Color(0xFF2E7D32) else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── NOTIFICATION LIST ────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            filteredNotifications.forEach { notification ->
                NotificationItem(
                    name = notification.name,
                    sku = notification.sku,
                    current = notification.current,
                    min = notification.min,
                    status = notification.status,
                    emoji = notification.emoji,
                    progress = notification.progress,
                    onViewDetails = { 
                        // Using route based on context
                        val route = if (navController.currentBackStackEntry?.destination?.route?.startsWith("admin") == true) 
                            "admin_inventory_adjustment" else "inventory_adjustment"
                        navController.navigate(route)
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun NotificationItem(
    name: String,
    sku: String,
    current: String,
    min: String,
    status: String,
    emoji: String,
    progress: Float,
    onViewDetails: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {

                // Icon Box
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(50.dp))
                        .background(Color(0xFFF5F5F5)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = emoji, fontSize = 24.sp)
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(text = name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1E293B))
                    Text(text = sku, color = Color.Gray, fontSize = 12.sp)
                }

                // Badge
                val badgeColor = if (status == "Critical") Color(0xFFFFEBEE) else Color(0xFFFFF9C4)
                val textColor = if (status == "Critical") Color(0xFFEF5350) else Color(0xFFFBC02D)

                Surface(color = badgeColor, shape = RoundedCornerShape(12.dp)) {
                    Text(
                        text = status,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = textColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Current: $current",
                    color = Color(0xFF795548),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                if (min != "N/A") {
                    Text(
                        text = "Min: $min",
                        color = Color(0xFF795548),
                        fontSize = 14.sp
                    )
                }
            }

            // Progress Bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (status == "Critical") Color(0xFFEF5350) else Color(0xFFFBC02D),
                trackColor = Color(0xFFEEEEEE)
            )

            Button(
                onClick = onViewDetails,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(text = "View Details →", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}
