package com.example.fruitylicious.ui.admin.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.SetMeal
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
import androidx.navigation.NavController
import com.example.fruitylicious.ADMIN_INGREDIENTS
import com.example.fruitylicious.ADMIN_NOTIFICATIONS
import com.example.fruitylicious.ADMIN_PRODUCTS
import com.example.fruitylicious.ADMIN_RECIPES
import com.example.fruitylicious.ADMIN_RESTOCK_HISTORY
import com.example.fruitylicious.STAFF_POS
import com.example.fruitylicious.ui.shared.AdminSideBarContent
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.ceil

private val DashGreenPrimary = Color(0xFF2C8C44)
private val DashGreenDark = Color(0xFF1B5E20)
private val PageBg = Color(0xFFFFEAA0)
private val CardBg = Color.White
private val ChartBar = Color(0xFFE53935)
private val TextPrimary = Color(0xFF1A1A1A)
private val TextSecondary = Color(0xFF757575)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    navController: NavController,
    viewModel: AdminDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

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
                AdminSideBarContent(
                    navController = navController,
                    drawerState = drawerState,
                    scope = scope,
                    adminName = uiState.userName,
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
                        .background(PageBg)
                        .verticalScroll(rememberScrollState())
                ) {
                    DashboardHeader(
                        selectedBranch = uiState.selectedBranch,
                        hasNotifications = uiState.hasNotifications,
                        onBranchSelect = viewModel::onBranchSelected,
                        onNotificationsClick = {
                            navController.navigate(ADMIN_NOTIFICATIONS)
                        },
                        onMenuClick = {
                            scope.launch {
                                drawerState.open()
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    GreetingCard(
                        adminName = uiState.userName,
                        selectedBranch = uiState.selectedBranch,
                        dateText = uiState.dateText,
                        isOnline = uiState.isOnline,
                        onStartPos = {
                            navController.navigate(STAFF_POS)
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    QuickActionsSection(
                        onNavigate = { route -> navController.navigate(route) }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    SalesChartSection(
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

@Composable
private fun DashboardHeader(
    selectedBranch: String,
    hasNotifications: Boolean,
    onBranchSelect: (String) -> Unit,
    onNotificationsClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DashGreenPrimary)
            .padding(start = 8.dp, end = 16.dp, top = 48.dp, bottom = 14.dp)
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

            Text(
                text = "DASHBOARD",
                color = Color.White,
                fontSize = 17.sp,
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

            Spacer(modifier = Modifier.width(12.dp))

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFF5F5F5))
                    .padding(3.dp)
            ) {
                listOf("B1", "B2", "All").forEach { branch ->
                    val isSelected = selectedBranch == branch

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) DashGreenPrimary else Color.Transparent)
                            .clickable { onBranchSelect(branch) }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = branch,
                            color = if (isSelected) Color.White else Color(0xFF666E7A),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GreetingCard(
    adminName: String,
    selectedBranch: String,
    dateText: String,
    isOnline: Boolean,
    onStartPos: () -> Unit
) {
    val branchName = when (selectedBranch) {
        "B1" -> "Branch 1"
        "B2" -> "Branch 2"
        "All" -> "All Branches"
        else -> selectedBranch
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        color = CardBg,
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
                        text = "Hello, ${adminName.ifBlank { "Admin User" }}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = dateText,
                        fontSize = 13.sp,
                        color = TextSecondary
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
                            text = "Viewing: $branchName",
                            color = DashGreenPrimary,
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
                                .background(if (isOnline) DashGreenPrimary else Color.Gray)
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = if (isOnline) "Online" else "Offline",
                            color = if (isOnline) DashGreenPrimary else Color.Gray,
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
                    .background(DashGreenDark)
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
                        tint = DashGreenPrimary,
                        modifier = Modifier.size(16.dp)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = "POS",
                        color = DashGreenPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionsSection(
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
            color = TextPrimary,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        val actions = listOf(
            Triple(Icons.Outlined.Inventory2, "Products", ADMIN_PRODUCTS),
            Triple(Icons.Outlined.SetMeal, "Ingredients", ADMIN_INGREDIENTS),
            Triple(Icons.AutoMirrored.Outlined.MenuBook, "Recipes", ADMIN_RECIPES),
            Triple(Icons.Outlined.Autorenew, "Restock", ADMIN_RESTOCK_HISTORY)
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            actions.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowItems.forEach { (icon, label, route) ->
                        QuickActionCard(
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
private fun QuickActionCard(
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
        color = CardBg,
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
                        tint = DashGreenPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun SalesChartSection(
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
            color = TextPrimary,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = CardBg,
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 3.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                val maxSales = (weeklySales.maxOrNull() ?: 0f).coerceAtLeast(100f)
                val roundedMax = (ceil(maxSales / 100.0) * 100).toInt()
                val highlightIdx = weeklySales.indexOf(weeklySales.maxOrNull() ?: 0f)
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
                                color = TextSecondary,
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
                        val tPaddingPx = topPadding.toPx()
                        val bPaddingPx = bottomPadding.toPx()
                        val chartHeight = size.height - tPaddingPx - bPaddingPx
                        val barCount = weeklySales.size
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

                        weeklySales.forEachIndexed { index, value ->
                            val barHeight = (value / roundedMax) * chartHeight
                            val left = index * gap + (gap - barWidth) / 2f
                            val top = tPaddingPx + chartHeight - barHeight

                            drawRoundRect(
                                color = ChartBar,
                                topLeft = Offset(left, top),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(4.dp.toPx())
                            )

                            if (index == highlightIdx && value > 0f) {
                                adminDrawTooltip(
                                    textMeasurer = textMeasurer,
                                    label = value.toInt().toString(),
                                    centerX = left + barWidth / 2f,
                                    topY = top - 28.dp.toPx()
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
                            color = TextSecondary,
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
                        text = "Total This Week",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "₱${String.format(Locale.US, "%,.2f", totalAmount)}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        if (transactionCount > 0) {
                            Text(
                                text = "$transactionCount transactions",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.adminDrawTooltip(
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    label: String,
    centerX: Float,
    topY: Float
) {
    val bubbleWidth = 42.dp.toPx()
    val bubbleHeight = 22.dp.toPx()
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
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
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
