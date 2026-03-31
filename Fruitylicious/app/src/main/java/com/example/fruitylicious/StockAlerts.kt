package com.example.fruitylicious

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch

@Composable
fun StockAlerts(navController: NavController) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = { SideBarContent(navController) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFFEAA0))
                .verticalScroll(rememberScrollState())
        ) {
            AlertsHeader(onMenuClick = { scope.launch { drawerState.open() } })

            Column(modifier = Modifier.padding(16.dp)) {

                WarningBanner(count = 3)

                Spacer(modifier = Modifier.height(16.dp))

                // Alert Cards
                AlertItemCard(
                    emoji = "🥭",
                    name = "Mango",
                    sku = "SKU-0041 - Mangoes",
                    current = 12f,
                    min = 50f,
                    status = "Critical",
                    statusColor = Color(0xFFFFEBEE),
                    textColor = Color(0xFFC62828)
                )

                Spacer(modifier = Modifier.height(16.dp))

                AlertItemCard(
                    emoji = "🍉",
                    name = "Watermelon",
                    sku = "SKU-0018 - Melons",
                    current = 8f,
                    min = 40f, // Estimated max for progress bar
                    status = "Critical",
                    statusColor = Color(0xFFFFEBEE),
                    textColor = Color(0xFFC62828)
                )

                Spacer(modifier = Modifier.height(16.dp))

                AlertItemCard(
                    emoji = "🥑",
                    name = "Avocado",
                    sku = "SKU-0012 - Avocados",
                    current = 15f,
                    min = 30f,
                    status = "Warning",
                    statusColor = Color(0xFFFFF3E0),
                    textColor = Color(0xFFEF6C00)
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun AlertsHeader(onMenuClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF2E7D32))
            .padding(start = 20.dp, end = 20.dp, top = 48.dp, bottom = 28.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.clickable { onMenuClick() },
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                repeat(3) {
                    Box(modifier = Modifier.width(22.dp).height(2.dp).background(Color.White))
                }
            }
            Box(
                modifier = Modifier.size(38.dp).background(Color(0xFFC62828), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("E1", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(22.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "⚠", fontSize = 32.sp, color = Color.White)
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = "Alerts", color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Bold)
        }
        Text(text = "3 items need attention", color = Color.White.copy(alpha = 0.9f), fontSize = 16.sp)
    }
}

@Composable
fun WarningBanner(count: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFFFEBEE),
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 2.dp
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = "🚨", fontSize = 28.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = "$count Low Stocks Alerts", color = Color(0xFFC62828), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(text = "Reorder immediately to avoid stockout", color = Color(0xFFC62828).copy(alpha = 0.7f), fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun AlertItemCard(
    emoji: String, name: String, sku: String,
    current: Float, min: Float,
    status: String, statusColor: Color, textColor: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = emoji, fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(text = sku, fontSize = 12.sp, color = Color.Gray)
                    }
                }
                // Status Tag
                Surface(color = statusColor, shape = RoundedCornerShape(12.dp)) {
                    Text(text = status, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), color = textColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Current: ${current.toInt()} kg", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                if (status == "Critical") Text(text = "Min: ${min.toInt()} kg", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = { current / min },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = textColor,
                trackColor = Color(0xFFF5F5F5),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {},
                    modifier = Modifier.weight(1f).height(45.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("View Details →", fontSize = 13.sp)
                }
                OutlinedButton(
                    onClick = {},
                    modifier = Modifier.weight(1f).height(45.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF5D4037))
                ) {
                    Text("Edit", fontSize = 13.sp)
                }
            }
        }
    }
}