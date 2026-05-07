package com.example.fruitylicious.ui.staff.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.fruitylicious.ADMIN_DASHBOARD
import com.example.fruitylicious.STAFF_DASHBOARD
import com.example.fruitylicious.STAFF_INVENTORY
import com.example.fruitylicious.STAFF_NOTIFICATIONS
import com.example.fruitylicious.STAFF_POS
import com.example.fruitylicious.STAFF_RESTOCK_HISTORY
import com.example.fruitylicious.STAFF_SALES_SUMMARY
import com.example.fruitylicious.STAFF_WASTE_HISTORY
import java.util.Locale
import kotlin.math.ceil
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.navigation.NavController
import com.example.fruitylicious.ui.shared.SharedDrawerContent
import com.example.fruitylicious.ui.shared.SharedScreenMode
import kotlinx.coroutines.launch

private val StaffGreenPrimary = Color(0xFF2C8C44)
private val StaffGreenDark = Color(0xFF1B5E20)
private val StaffPageBg = Color(0xFFFFEAA0)
private val StaffCardBg = Color.White
private val StaffChartBar = Color(0xFFE53935)
private val StaffTextPrimary = Color(0xFF1A1A1A)
private val StaffTextSecondary = Color(0xFF757575)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffDashboardScreen(
    navController: NavController,
    mode: SharedScreenMode = SharedScreenMode.STAFF,
    viewModel: StaffDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isAdmin) {
        if (uiState.isAdmin) {
            navController.navigate(ADMIN_DASHBOARD) {
                popUpTo(STAFF_DASHBOARD) { inclusive = true }
            }
        }
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.syncMessage, uiState.syncError) {
        uiState.syncMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSyncMessage()
        }
        uiState.syncError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSyncMessage()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color.Transparent,
                drawerTonalElevation = 0.dp
            ) {
                SharedDrawerContent(
                    mode = if (uiState.isAdmin) SharedScreenMode.OWNER else mode,
                    navController = navController,
                    drawerState = drawerState,
                    scope = scope,
                    userName = uiState.userName.ifBlank { "User" },
                    branchName = uiState.branchName.ifBlank { "Branch" },
                    isClockedIn = uiState.isClockedIn,
                    onLogout = viewModel::logout
                )
            }
        }
    ) {
        PullToRefreshBox(
            isRefreshing = uiState.isSyncing,
            onRefresh = viewModel::syncNow,
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(StaffPageBg)
                        .verticalScroll(rememberScrollState())
                ) {
                    StaffDashboardHeader(
                        hasNotifications = uiState.hasNotifications,
                        onNotificationsClick = {
                            navController.navigate(STAFF_NOTIFICATIONS)
                        },
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
                        dailySales = uiState.dailySalesData,
                        totalAmount = uiState.dailyTotalSales,
                        transactionCount = uiState.dailyTransactionCount,
                        selectedHour = uiState.selectedHourlySales,
                        onHourClick = viewModel::onHourSelected
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

                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StaffDashboardHeader(
    hasNotifications: Boolean,
    onNotificationsClick: () -> Unit,
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

            IconButton(
                onClick = onNotificationsClick,
                modifier = Modifier.size(40.dp)
            ) {
                BadgedBox(
                    badge = {
                        if (hasNotifications) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.Red)
                            )
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = "Notifications",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
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
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Hello, $staffName",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = StaffTextPrimary,
                        maxLines = 1
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
                            fontSize = 11.sp,
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
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(StaffGreenDark)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Ready to serve?",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Open POS to start taking orders.",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }

                Button(
                    onClick = onStartPos,
                    modifier = Modifier.height(40.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ShoppingCart,
                        contentDescription = null,
                        tint = StaffGreenPrimary,
                        modifier = Modifier.size(16.dp)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = "POS",
                        color = StaffGreenPrimary,
                        fontSize = 13.sp,
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
    dailySales: List<HourlySales>,
    totalAmount: Double,
    transactionCount: Int,
    selectedHour: HourlySales?,
    onHourClick: (HourlySales?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Sales Today (10 AM - 8 PM)",
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
                val hourLabels = listOf("10a", "11a", "12p", "1p", "2p", "3p", "4p", "5p", "6p", "7p")
                val maxSales = (dailySales.maxOfOrNull { it.totalSales } ?: 0f).coerceAtLeast(100f)
                val roundedMax = (ceil(maxSales / 100.0) * 100).toInt()
                val textMeasurer = rememberTextMeasurer()

                val yLabels = listOf(
                    "₱$roundedMax",
                    "₱${roundedMax * 3 / 4}",
                    "₱${roundedMax / 2}",
                    "₱${roundedMax / 4}",
                    "₱0"
                )

                val topPadding = 32.dp
                val bottomPadding = 24.dp

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .width(44.dp)
                            .fillMaxHeight()
                            .padding(top = topPadding, bottom = bottomPadding),
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

                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(dailySales) {
                                    detectTapGestures { offset ->
                                        val barCount = dailySales.size
                                        val gap = size.width.toFloat() / barCount
                                        
                                        val clickedIndex = (offset.x / gap).toInt()
                                        if (clickedIndex in dailySales.indices) {
                                            onHourClick(dailySales[clickedIndex])
                                        } else {
                                            onHourClick(null)
                                        }
                                    }
                                }
                        ) {
                            val tPaddingPx = topPadding.toPx()
                            val bPaddingPx = bottomPadding.toPx()
                            val chartHeight = size.height - tPaddingPx - bPaddingPx
                            val barCount = dailySales.size
                            val gap = size.width / barCount
                            val barWidth = gap * 0.55f

                            for (i in 0..4) {
                                val y = tPaddingPx + chartHeight * (1f - i / 4f)
                                drawLine(
                                    color = Color(0xFFE0E0E0),
                                    start = Offset(0f, y),
                                    end = Offset(size.width, y),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }

                            dailySales.forEachIndexed { index, data ->
                                val barHeight = (data.totalSales / roundedMax) * chartHeight
                                val left = index * gap + (gap - barWidth) / 2f
                                val top = tPaddingPx + chartHeight - barHeight

                                val isSelected = selectedHour?.hour == data.hour

                                drawRoundRect(
                                    color = if (isSelected) StaffGreenPrimary else StaffChartBar,
                                    topLeft = Offset(left, top),
                                    size = Size(barWidth, barHeight),
                                    cornerRadius = CornerRadius(4.dp.toPx())
                                )

                                if (isSelected) {
                                    staffDrawTooltip(
                                        textMeasurer = textMeasurer,
                                        label = "₱${data.totalSales.toInt()}\n${data.transactionCount} txns",
                                        centerX = left + barWidth / 2f,
                                        topY = top - 40.dp.toPx()
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 50.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    hourLabels.forEach { label ->
                        Text(
                            text = label,
                            fontSize = 9.sp,
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
                        text = if (selectedHour != null) {
                            val h = selectedHour.hour
                            val period = if (h < 12) "AM" else "PM"
                            val displayHour = if (h > 12) h - 12 else if (h == 0) 12 else h
                            "Details for $displayHour $period"
                        } else {
                            "Today's Total"
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = StaffTextPrimary
                    )

                    Column(horizontalAlignment = Alignment.End) {
                        val displayAmount = selectedHour?.totalSales?.toDouble() ?: totalAmount
                        val displayCount = selectedHour?.transactionCount ?: transactionCount

                        Text(
                            text = "₱${String.format(Locale.US, "%,.2f", displayAmount)}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = StaffTextPrimary
                        )

                        Text(
                            text = "$displayCount transactions",
                            fontSize = 12.sp,
                            color = StaffTextSecondary
                        )
                    }
                }
            }
        }
    }
}

private fun DrawScope.staffDrawTooltip(
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    label: String,
    centerX: Float,
    topY: Float
) {
    val bubbleWidth = 60.dp.toPx()
    val bubbleHeight = 34.dp.toPx()
    val left = centerX - bubbleWidth / 2f

    val tooltipPath = Path().apply {
        addRoundRect(
            androidx.compose.ui.geometry.RoundRect(
                left = left,
                top = topY,
                right = left + bubbleWidth,
                bottom = topY + bubbleHeight,
                cornerRadius = CornerRadius(4.dp.toPx())
            )
        )
        moveTo(centerX - 4.dp.toPx(), topY + bubbleHeight)
        lineTo(centerX + 4.dp.toPx(), topY + bubbleHeight)
        lineTo(centerX, topY + bubbleHeight + 5.dp.toPx())
        close()
    }

    drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            isAntiAlias = true
            setShadowLayer(
                12f,
                0f,
                4f,
                android.graphics.Color.argb(70, 0, 0, 0)
            )
        }
        canvas.nativeCanvas.drawPath(tooltipPath.asAndroidPath(), paint)
    }

    val textLayoutResult = textMeasurer.measure(
        text = label,
        style = TextStyle(
            color = Color(0xFF1A1A1A),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 11.sp
        )
    )

    drawText(
        textLayoutResult = textLayoutResult,
        topLeft = Offset(
            centerX - textLayoutResult.size.width / 2f,
            topY + (bubbleHeight - textLayoutResult.size.height) / 2f
        )
    )
}

