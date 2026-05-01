package com.example.fruitylicious.ui.staff.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.fruitylicious.STAFF_INVENTORY
import com.example.fruitylicious.STAFF_POS
import com.example.fruitylicious.STAFF_RESTOCK_HISTORY
import com.example.fruitylicious.STAFF_SALES_SUMMARY
import com.example.fruitylicious.STAFF_WASTE_HISTORY
import java.util.Locale
import kotlin.math.ceil
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavController
import com.example.fruitylicious.ui.shared.StaffSideBarContent
import kotlinx.coroutines.launch

private val StaffGreenPrimary = Color(0xFF2C8C44)
private val StaffGreenDark = Color(0xFF1B5E20)
private val StaffPageBg = Color(0xFFFFEAA0)
private val StaffCardBg = Color.White
private val StaffChartBar = Color(0xFFE53935)
private val StaffTextPrimary = Color(0xFF1A1A1A)
private val StaffTextSecondary = Color(0xFF757575)

@Composable
fun StaffDashboardScreen(
    navController: NavController,
    viewModel: StaffDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color.Transparent,
                drawerTonalElevation = 0.dp
            ) {
                StaffSideBarContent(
                    navController = navController,
                    drawerState = drawerState,
                    scope = scope,
                    staffName = uiState.userName.ifBlank { "Staff User" },
                    branchName = uiState.branchName.ifBlank { "Branch 1" },
                    onLogout = viewModel::logout
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(StaffPageBg)
                .verticalScroll(rememberScrollState())
        ) {
            StaffDashboardHeader(
                onMenuClick = {
                    scope.launch {
                        drawerState.open()
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            StaffGreetingCard(
                staffName = uiState.userName.ifBlank { "Staff User" },
                branchName = uiState.branchName,
                dateText = uiState.dateText,
                isOnline = uiState.isOnline,
                onStartPos = {
                    navController.navigate(STAFF_POS)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            StaffQuickActionsSection(
                onNavigate = { route ->
                    navController.navigate(route)
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            StaffSalesChartSection(
                weeklySales = uiState.weeklySalesData,
                totalAmount = uiState.weeklyTotalSales,
                transactionCount = uiState.weeklyTransactionCount
            )

            val error = uiState.error
            if (!error.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StaffDashboardHeader(
    onMenuClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(StaffGreenPrimary)
            .padding(start = 16.dp, end = 16.dp, top = 48.dp, bottom = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "DASHBOARD",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = {}) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = "Notifications",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun StaffGreetingCard(
    staffName: String,
    branchName: String,
    dateText: String,
    isOnline: Boolean,
    onStartPos: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        color = StaffCardBg,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 3.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "Hello, $staffName",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = StaffTextPrimary
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = dateText,
                        fontSize = 13.sp,
                        color = StaffTextSecondary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFE8F5E9))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = branchName,
                            color = StaffGreenPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFE8F5E9))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(50.dp))
                                .background(if (isOnline) StaffGreenPrimary else Color.Gray)
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = if (isOnline) "Online" else "Offline",
                            color = if (isOnline) StaffGreenPrimary else Color.Gray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(StaffGreenDark)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Column(modifier = Modifier.align(Alignment.CenterStart)) {
                    Text(
                        text = "Ready to serve?",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Open POS to start taking orders.",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp
                    )
                }

                Button(
                    onClick = onStartPos,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ShoppingCart,
                        contentDescription = null,
                        tint = StaffGreenPrimary,
                        modifier = Modifier.size(18.dp)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = "Start POS",
                        color = StaffGreenPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun StaffQuickActionsSection(
    onNavigate: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Quick Actions",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = StaffTextPrimary,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        val actions = listOf(
            Triple(Icons.Outlined.BarChart, "Sales Summary", STAFF_SALES_SUMMARY),
            Triple(Icons.Outlined.Autorenew, "Restock", STAFF_RESTOCK_HISTORY),
            Triple(Icons.Outlined.Search, "Inventory Monitoring", STAFF_INVENTORY),
            Triple(Icons.Outlined.DeleteOutline, "Waste Management", STAFF_WASTE_HISTORY)
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            actions.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowItems.forEach { (icon, label, route) ->
                        StaffQuickActionCard(
                            icon = icon,
                            label = label,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate(route) }
                        )
                    }

                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun StaffQuickActionCard(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(110.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        color = StaffCardBg,
        shape = RoundedCornerShape(14.dp),
        shadowElevation = 2.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(50.dp))
                        .background(Color(0xFFE8F5E9)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = StaffGreenPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = StaffTextPrimary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun StaffSalesChartSection(
    weeklySales: List<Float>,
    totalAmount: Double,
    transactionCount: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Sales This Week",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = StaffTextPrimary,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = StaffCardBg,
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 3.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                val maxSales = (weeklySales.maxOrNull() ?: 0f).coerceAtLeast(100f)
                val roundedMax = (ceil(maxSales / 100.0) * 100).toInt()
                val highlightIdx = weeklySales.indexOf(weeklySales.maxOrNull() ?: 0f)

                val yLabels = listOf(
                    "₱$roundedMax",
                    "₱${roundedMax * 3 / 4}",
                    "₱${roundedMax / 2}",
                    "₱${roundedMax / 4}",
                    "₱0"
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .width(40.dp)
                            .fillMaxHeight()
                            .padding(bottom = 20.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        yLabels.forEach { label ->
                            Text(
                                text = label,
                                fontSize = 9.sp,
                                color = StaffTextSecondary,
                                textAlign = TextAlign.End,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Canvas(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        val chartHeight = size.height - 24.dp.toPx()
                        val barCount = weeklySales.size
                        val gap = size.width / barCount
                        val barWidth = gap * 0.55f

                        for (i in 0..4) {
                            val y = chartHeight * (1f - i / 4f)
                            drawLine(
                                color = Color(0xFFE0E0E0),
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        weeklySales.forEachIndexed { index, value ->
                            val barHeight = (value / roundedMax) * chartHeight
                            val left = index * gap + (gap - barWidth) / 2f
                            val top = chartHeight - barHeight

                            drawRoundRect(
                                color = StaffChartBar,
                                topLeft = Offset(left, top),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(4.dp.toPx())
                            )

                            if (index == highlightIdx && value > 0f) {
                                staffDrawTooltip(
                                    label = value.toInt().toString(),
                                    centerX = left + barWidth / 2f,
                                    topY = top - 26.dp.toPx()
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 46.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    days.forEach { day ->
                        Text(
                            text = day,
                            fontSize = 10.sp,
                            color = StaffTextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFFEEEEEE))
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "Total 7 Days",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = StaffTextPrimary
                    )

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "₱${String.format(Locale.US, "%,.2f", totalAmount)}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = StaffTextPrimary
                        )

                        if (transactionCount > 0) {
                            Text(
                                text = "$transactionCount transactions",
                                fontSize = 12.sp,
                                color = StaffTextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.staffDrawTooltip(
    label: String,
    centerX: Float,
    topY: Float
) {
    val bubbleWidth = 42.dp.toPx()
    val bubbleHeight = 22.dp.toPx()
    val left = centerX - bubbleWidth / 2f

    drawRoundRect(
        color = Color(0xFF1A1A1A),
        topLeft = Offset(left, topY),
        size = Size(bubbleWidth, bubbleHeight),
        cornerRadius = CornerRadius(4.dp.toPx())
    )

    val triangleTop = topY + bubbleHeight

    val path = Path().apply {
        moveTo(centerX - 4.dp.toPx(), triangleTop)
        lineTo(centerX + 4.dp.toPx(), triangleTop)
        lineTo(centerX, triangleTop + 5.dp.toPx())
        close()
    }

    drawPath(path, color = Color(0xFF1A1A1A))
}