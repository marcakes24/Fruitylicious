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
import androidx.compose.ui.text.style.TextAlign
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
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(12.dp))

            QuickActionRow(emoji = "🗑", label = "Waste Management", onClick = { navController.navigate("waste_management") })

            Spacer(modifier = Modifier.height(12.dp))

            QuickActionRow(
                emoji = "📦",
                label = "Inventory Adjustment",
                onClick = {
                    navController.navigate("inventory_adjustment") {
                        popUpTo("home") { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 3. Sales Summary Preview
            Text(
                text = "Sales Summary Preview",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(12.dp))

            StaffSalesChart(navController)
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
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(3) {
                Box(modifier = Modifier.width(22.dp).height(1.5.dp).background(Color.White))
            }
        }

        // Title Centered
        Text(
            text = "STAFF DASHBOARD",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun StartSellingCard(navController: NavController) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable {
                navController.navigate("pos") {
                    popUpTo("home") { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
        color = Color(0xFFFAFF82),
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "🖥", fontSize = 50.sp)

            Spacer(modifier = Modifier.width(20.dp))

            Column {
                Text(
                    text = "Start Selling",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black
                )
                Text(
                    text = "Open POS",
                    fontSize = 14.sp,
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
            .height(70.dp)
            .clickable { onClick() },
        color = Color(0xFF7CC444), // Bright Green matching the bars
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = emoji, fontSize = 28.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = label,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
    }
}

@Composable
fun StaffSalesChart(navController: NavController) {
    Surface(
        onClick = { navController.navigate("SalesSummary") },
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .padding(16.dp)
        ) {
            // Grid and Bars
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                // Background Grid Lines
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    repeat(7) { HorizontalDivider(color = Color.Black.copy(alpha = 0.05f), thickness = 1.dp) }
                }

                // Full-height bar row
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Heights matching visual mock for "Sales Summary Preview"
                    val chartHeights = listOf(60, 80, 40, 95, 45)

                    chartHeights.forEach { value ->
                        val barColor = if (value < 50) Color(0xFFFF3B30) else Color(0xFF7CC444)
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .fillMaxHeight(value / 100f)
                                .background(barColor)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // X-Axis Multi-line Labels Bottom Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val labelData = listOf(
                    Pair("Feb 10 , 2026", "12, 300"),
                    Pair("Feb 11 , 2026", "13, 300"),
                    Pair("Feb 12 , 2026", "6, 000"),
                    Pair("Feb 13 , 2026", "14, 300"),
                    Pair("Feb 14 , 2026", "8, 000")
                )
                labelData.forEach { (date, amount) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(50.dp)
                    ) {
                        Text(
                            text = date,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            textAlign = TextAlign.Center,
                            lineHeight = 9.sp
                        )
                        Text(
                            text = amount,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            textAlign = TextAlign.Center,
                            lineHeight = 9.sp
                        )
                    }
                }
            }
        }
    }
}