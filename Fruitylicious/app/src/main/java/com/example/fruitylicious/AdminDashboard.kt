package com.example.fruitylicious

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun AdminDashboardScreen(
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
        AdminHeader(onMenuClick = { scope.launch { drawerState.open() } })

        Column(modifier = Modifier.padding(16.dp)) {

            // 1. Big POS Button
            StartSellingCardAdmin(navController)

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Left Column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DashboardTile(
                        title = "Manage\nProducts",
                        backgroundColor = Color(0xFFFFB300),
                        height = 110.dp,
                        bgEmoji = "📦",
                        onClick = { }
                    )
                    DashboardTile(
                        title = "Recipe\nManagement",
                        backgroundColor = Color(0xFF68C971),
                        height = 150.dp,
                        bgEmoji = "📖",
                        onClick = { }
                    )
                }

                // Right Column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DashboardTile(
                        title = "Manage\nIngredients",
                        backgroundColor = Color(0xFFFF8C00),
                        height = 135.dp,
                        bgEmoji = "🥣",
                        onClick = { }
                    )
                    DashboardTile(
                        title = "Inventory\nMonitoring",
                        backgroundColor = Color(0xFFF27979),
                        height = 125.dp,
                        bgEmoji = "🔍",
                        onClick = { }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 3. Sales Summary Preview
            Text(
                text = "Sales Summary Preview",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(12.dp))

            AdminSalesChart()

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}


@Composable
fun AdminHeader(onMenuClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF2C8C44))
            .padding(start = 20.dp, end = 20.dp, top = 48.dp, bottom = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Column(
            modifier = Modifier.clickable { onMenuClick() },
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .width(22.dp)
                        .height(1.5.dp)
                        .background(Color.White)
                )
            }
        }

        Text(
            text = "ADMIN DASHBOARD",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun StartSellingCardAdmin(navController: NavController) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable {
                navController.navigate("pos") {
                    popUpTo("admin_home") { saveState = true }
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
fun DashboardTile(
    title: String,
    backgroundColor: Color,
    height: Dp,
    bgEmoji: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clickable { onClick() },
        color = backgroundColor,
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 2.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Text(
                text = bgEmoji,
                fontSize = 80.sp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 10.dp, y = 10.dp)
                    .alpha(0.15f)
            )
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.Black,
                lineHeight = 20.sp,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
fun AdminSalesChart() {
    Surface(
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
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    repeat(7) { HorizontalDivider(color = Color.Black.copy(alpha = 0.05f), thickness = 1.dp) }
                }
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val labelData = listOf(
                    "Feb 10 , 2026" to "12,300",
                    "Feb 11 , 2026" to "13,300",
                    "Feb 12 , 2026" to "6,000",
                    "Feb 13 , 2026" to "14,300",
                    "Feb 14 , 2026" to "8,000"
                )
                labelData.forEach { (date, amount) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(50.dp)
                    ) {
                        Text(text = date, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color.Black, textAlign = TextAlign.Center, lineHeight = 9.sp)
                        Text(text = amount, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color.Black, textAlign = TextAlign.Center, lineHeight = 9.sp)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AdminDashboardPreview() {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    AdminDashboardScreen(
        navController = rememberNavController(),
        drawerState = drawerState,
        scope = scope
    )
}