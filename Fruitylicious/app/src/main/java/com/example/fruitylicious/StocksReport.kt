package com.example.fruitylicious

import androidx.compose.material3.DrawerState
import kotlinx.coroutines.CoroutineScope
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
fun StocksReportScreen(
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
        // Reusing header
        ReportsHeader(onMenuClick = {
            scope.launch { drawerState.open() }
        })

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
            // Time Filter Tabs
            TimeFilterTabs()

            Spacer(modifier = Modifier.height(20.dp))

            ReportChartCard(
                title = "TURNOVER",
                data = listOf(
                    "Mon" to 85, "Tue" to 15, "Wed" to 45,
                    "Thu" to 30, "Fri" to 80, "Sat" to 75, "Sun" to 85
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            ReportChartCard(
                title = "NET CHANGE",
                data = listOf(
                    "Mon" to 60, "Tue" to 45, "Wed" to 70,
                    "Thu" to 35, "Fri" to 65, "Sat" to 80, "Sun" to 65
                )
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun ReportsHeader(onMenuClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF2E7D32)) // Dark Green
            .padding(start = 20.dp, end = 20.dp, top = 48.dp, bottom = 28.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
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

            Box(
                modifier = Modifier.size(38.dp).background(Color(0xFFC62828), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("E1", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        Text(
            text = "Stocks Reports",
            color = Color.White,
            fontSize = 42.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Inventory Performance Overview",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 16.sp
        )
    }
}

@Composable
fun TimeFilterTabs() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FilterTab(text = "Daily", isSelected = true, modifier = Modifier.weight(1f))
        FilterTab(text = "Weekly", isSelected = false, modifier = Modifier.weight(1f))
        FilterTab(text = "Monthly", isSelected = false, modifier = Modifier.weight(1f))
    }
}

@Composable
fun FilterTab(text: String, isSelected: Boolean, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(45.dp),
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) Color(0xFF7CB342) else Color.White,
        shadowElevation = 4.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = if (isSelected) Color.White else Color.Black,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ReportChartCard(title: String, data: List<Pair<String, Int>>) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF3D1F00)
            )
            Text(
                text = "View Details →",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF3D1F00),
                modifier = Modifier.clickable {}
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            shadowElevation = 4.dp
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                BarChart(
                    bars = data,
                    barColor = Color(0xFF4CAF50),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp) // Adjusted height for visibility
                )
            }
        }
    }
}