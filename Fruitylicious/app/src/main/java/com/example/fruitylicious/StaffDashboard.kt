package com.example.fruitylicious

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun StaffDashboardScreen(
    navController: NavController,
    drawerState: DrawerState,
    scope: CoroutineScope
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFEAA0))
            .verticalScroll(rememberScrollState())
    ) {
        StaffHeader(onMenuClick = { scope.launch { drawerState.open() } })

            Column(modifier = Modifier.padding(16.dp)) {

                // 1. Big POS Button
                StartSellingCard(navController)

                Spacer(modifier = Modifier.height(24.dp))

                // 2. Quick Actions Section
                Text(
                    text = "Quick Actions",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B5E20)
                )
                Spacer(modifier = Modifier.height(12.dp))

                QuickActionRow(emoji = "🗑", label = "Waste Management", onClick = { /* TODO */ })

                Spacer(modifier = Modifier.height(12.dp))

                QuickActionRow(
                    emoji = "📦",
                    label = "Inventory Adjustment",
                    onClick = {
                        navController.navigate("inventory") {
                            popUpTo("home") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
                // 3. Sales Summary Preview
                Text(
                    text = "Sales Summary Preview",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B5E20)
                )
                Spacer(modifier = Modifier.height(12.dp))

                StaffSalesChart()
            }
        }
    }

@Composable
fun StaffHeader(onMenuClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(GreenPrimary)
            .padding(start = 20.dp, end = 20.dp, top = 48.dp, bottom = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        // Hamburger Menu
        Column(
            modifier = Modifier.clickable { onMenuClick() },
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            repeat(3) {
                Box(modifier = Modifier.width(22.dp).height(2.dp).background(Color.White))
            }
        }

        // Title Centered
        Text(
            text = "STAFF DASHBOARD",
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
fun StartSellingCard(navController: NavController) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable { navController.navigate("pos"){
                popUpTo("home") { saveState = true }
                launchSingleTop = true
                restoreState = true
            } },
        color = Color(0xFFFFFF55),
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "🖥", fontSize = 60.sp)

            Spacer(modifier = Modifier.width(20.dp))

            Column {
                Text(
                    text = "Start Selling",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Black
                )
                Text(
                    text = "Open POS",
                    fontSize = 18.sp,
                    color = Color.Black.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun QuickActionRow(emoji: String, label: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clickable { onClick() }, // Added clickable logic here
        color = Color(0xFF99FF66), // Bright Green
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = emoji, fontSize = 32.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = label,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B5E20)
            )
        }
    }
}

@Composable
fun StaffSalesChart() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Reusing your existing BarChart logic with specific colors
            // Note: Use your existing BarChart component here
            BarChart(
                bars = listOf(
                    "Feb 10" to 60,
                    "Feb 11" to 80,
                    "Feb 12" to 30, // Low bar (Red in your image)
                    "Feb 13" to 95,
                    "Feb 14" to 45  // Medium bar (Red in your image)
                ),
                barColor = Color(0xFF8BC34A), // Default Green
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
            )

            // Sub-labels for price/date would go here as per image
        }
    }
}