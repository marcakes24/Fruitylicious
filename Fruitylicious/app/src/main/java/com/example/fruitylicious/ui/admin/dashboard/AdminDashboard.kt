package com.example.fruitylicious.ui.admin.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.fruitylicious.data.local.entity.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.ceil

// ── Brand colors (Renamed to avoid conflicts) ────────────────────────────────
private val DashGreenPrimary = Color(0xFF2C8C44)
private val DashGreenDark    = Color(0xFF1B5E20)
private val PageBg           = Color(0xFFFFEAA0)   
private val CardBg           = Color.White
private val ChartBar         = Color(0xFFE53935)   // red bars in Figma
private val TextPrimary      = Color(0xFF1A1A1A)
private val TextSecondary    = Color(0xFF757575)

// ── Selected branch state (lifted so header & card share it) ─────────────────
@Composable
fun AdminDashboardScreen(
    navController: NavController,
    drawerState:   DrawerState,
    scope:         CoroutineScope
) {
    var selectedBranch by remember { mutableStateOf("B1") }
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val transactionDao = database.transactionDao()

    // Map branch labels to actual database branch_ids
    val branchIdFilter = when (selectedBranch) {
        "B1"  -> "B1"
        "B2"  -> "B2"
        else  -> null // "All" branches
    }

    // Sales data state for the week (Monday to Sunday)
    var weeklySalesData by remember { mutableStateOf(List(7) { 0f }) }
    var totalAmount by remember { mutableStateOf(0.0) }
    var transactionCount by remember { mutableStateOf(0) }

    // Fetch and aggregate data whenever the branch filter changes
    LaunchedEffect(selectedBranch) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        // Find Monday of the current week
        while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        
        val salesPerDay = mutableListOf<Float>()
        var totalSales = 0.0
        var totalCount = 0
        
        // Loop through each day of the week
        for (i in 0 until 7) {
            val dayStart = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            val dayEnd = calendar.timeInMillis
            
            val daySum: Double
            if (branchIdFilter != null) {
                daySum = transactionDao.getTotalSales(branchIdFilter, dayStart, dayEnd) ?: 0.0
                // For transaction count, we might need a specific query in DAO, but here's a placeholder logic
            } else {
                // Sum for ALL branches
                // We'd ideally have a getTotalSalesAllBranches in DAO, but let's approximate or use a simple loop
                // (Optimally add @Query("SELECT SUM(total_amount) FROM transactions WHERE status = 'completed' AND date_time BETWEEN :from AND :to") to DAO)
                daySum = 0.0 // Replace with actual DAO call if available
            }
            
            salesPerDay.add(daySum.toFloat())
            totalSales += daySum
        }
        
        weeklySalesData = salesPerDay
        totalAmount = totalSales
        // transactionCount = totalCount // Update count if needed
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBg)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        DashboardHeader(
            navController   = navController,
            selectedBranch  = selectedBranch,
            onBranchSelect  = { selectedBranch = it },
            onMenuClick     = { scope.launch { drawerState.open() } }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Greeting card
        GreetingCard(
            navController  = navController,
            selectedBranch = selectedBranch
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Quick actions
        QuickActionsSection(navController)

        Spacer(modifier = Modifier.height(20.dp))

        // Sales chart
        SalesChartSection(weeklySalesData, totalAmount)

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ── Header ───────────────────────────────────────────────────────────────────
@Composable
private fun DashboardHeader(
    navController: NavController,
    selectedBranch: String,
    onBranchSelect: (String) -> Unit,
    onMenuClick:    () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DashGreenPrimary)
            .padding(start = 16.dp, end = 16.dp, top = 48.dp, bottom = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hamburger
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Title
            Text(
                text       = "DASHBOARD",
                color      = Color.White,
                fontSize   = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier   = Modifier.weight(1f)
            )

            // Notification Icon
            IconButton(onClick = { navController.navigate("admin_notification") }) {
                Icon(Icons.Outlined.Notifications, contentDescription = "Notifications", tint = Color.White)
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Branch Selector Toggle
            Surface(
                color = DashGreenDark,
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
                                .clickable { onBranchSelect(branch) }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            ) {
                                Text(
                                    text = branch,
                                    color = if (isSelected) DashGreenPrimary else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Greeting card ─────────────────────────────────────────────────────────────
@Composable
private fun GreetingCard(
    navController:  NavController,
    selectedBranch: String
) {
    val branchName = when (selectedBranch) {
        "B1"  -> "Branch 1"
        "B2"  -> "Branch 2"
        "All" -> "All Branches"
        else  -> selectedBranch
    }

    Surface(
        modifier        = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        color           = CardBg,
        shape           = RoundedCornerShape(16.dp),
        shadowElevation = 3.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Top row: greeting + branch badge
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.Top
            ) {
                Column {
                    Text(
                        text       = "Hello, Admin User",
                        fontSize   = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color      = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val sdf = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.US)
                    Text(
                        text     = sdf.format(Date()),
                        fontSize = 13.sp,
                        color    = TextSecondary
                    )
                }

                // "Viewing: Branch X" badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFE8F5E9))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text       = "Viewing: $branchName",
                        color      = DashGreenPrimary,
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Dark green "Ready to serve?" CTA row
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DashGreenDark)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Column(modifier = Modifier.align(Alignment.CenterStart)) {
                    Text(
                        text       = "Ready to serve?",
                        color      = Color.White,
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text     = "Open POS to start taking orders.",
                        color    = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp
                    )
                }

                // Start POS button
                Button(
                    onClick = {
                        navController.navigate("pos") {
                            popUpTo("admin_home") { saveState = true }
                            launchSingleTop = true
                            restoreState    = true
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape  = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(
                        imageVector        = Icons.Outlined.ShoppingCart,
                        contentDescription = null,
                        tint               = DashGreenPrimary,
                        modifier           = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text       = "Start POS",
                        color      = DashGreenPrimary,
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ── Quick actions ─────────────────────────────────────────────────────────────
@Composable
private fun QuickActionsSection(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text       = "Quick Actions",
            fontSize   = 16.sp,
            fontWeight = FontWeight.Bold,
            color      = TextPrimary,
            modifier   = Modifier.padding(bottom = 10.dp)
        )

        // 2 × 2 grid - wala pa tong mga route
        val actions = listOf(
            Triple(Icons.Outlined.Inventory2,  "Products",    "admin_manage_products"),
            Triple(Icons.Outlined.SetMeal,     "Ingredients", "admin_manage_ingredients"),
            Triple(Icons.AutoMirrored.Outlined.MenuBook, "Recipes", "admin_recipe_management"),
            Triple(Icons.Outlined.Autorenew,   "Restock",     "admin_restock")
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            actions.chunked(2).forEach { rowItems ->
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowItems.forEach { (icon, label, route) ->
                        QuickActionCard(
                            icon     = icon,
                            label    = label,
                            modifier = Modifier.weight(1f),
                            onClick  = {
                                if (route.isNotEmpty()) navController.navigate(route)
                            }
                        )
                    }
                    // Fill empty cell if odd number
                    if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    icon:     ImageVector,
    label:    String,
    modifier: Modifier = Modifier,
    onClick:  () -> Unit
) {
    Surface(
        modifier        = modifier
            .height(110.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        color           = CardBg,
        shape           = RoundedCornerShape(14.dp),
        shadowElevation = 2.dp
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Outline icon in a light circle
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(50.dp))
                        .background(Color(0xFFE8F5E9)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = icon,
                        contentDescription = label,
                        tint               = DashGreenPrimary,
                        modifier           = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text       = label,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = TextPrimary,
                    textAlign  = TextAlign.Center
                )
            }
        }
    }
}

// ── Sales chart ───────────────────────────────────────────────────────────────
@Composable
private fun SalesChartSection(weeklySales: List<Float>, totalAmount: Double) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text       = "Sales This Week",
            fontSize   = 16.sp,
            fontWeight = FontWeight.Bold,
            color      = TextPrimary,
            modifier   = Modifier.padding(bottom = 10.dp)
        )

        Surface(
            modifier        = Modifier.fillMaxWidth(),
            color           = CardBg,
            shape           = RoundedCornerShape(16.dp),
            shadowElevation = 3.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {

                val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                
                // Dynamically scale Y-axis based on data
                val maxSales = (weeklySales.maxOrNull() ?: 0f).coerceAtLeast(100f)
                val roundedMax = (ceil(maxSales / 100.0) * 100).toInt()
                val yLabels = listOf("P$roundedMax", "P${roundedMax*3/4}", "P${roundedMax/2}", "P${roundedMax/4}", "P0")

                // Y-axis labels + bars
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    // Y-axis labels
                    Column(
                        modifier              = Modifier
                            .width(40.dp)
                            .fillMaxHeight()
                            .padding(bottom = 20.dp),
                        verticalArrangement   = Arrangement.SpaceBetween
                    ) {
                        yLabels.forEach { label ->
                            Text(
                                text      = label,
                                fontSize  = 9.sp,
                                color     = TextSecondary,
                                textAlign = TextAlign.End,
                                modifier  = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Bar chart canvas
                    Canvas(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        val chartHeight  = size.height - 24.dp.toPx()
                        val barAreaWidth = size.width
                        val barCount     = weeklySales.size
                        val gap          = barAreaWidth / barCount
                        val barWidth     = gap * 0.55f

                        // Horizontal grid lines
                        for (i in 0..4) {
                            val y = chartHeight * (1f - i / 4f)
                            drawLine(
                                color       = Color(0xFFE0E0E0),
                                start       = Offset(0f, y),
                                end         = Offset(size.width, y),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        weeklySales.forEachIndexed { idx, value ->
                            val barHeight = (value / roundedMax) * chartHeight
                            val left      = idx * gap + (gap - barWidth) / 2f
                            val top       = chartHeight - barHeight

                            // Bar
                            drawRoundRect(
                                color        = ChartBar,
                                topLeft      = Offset(left, top),
                                size         = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(4.dp.toPx())
                            )
                        }
                    }
                }

                // Day labels
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(start = 46.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    days.forEach { day ->
                        Text(
                            text      = day,
                            fontSize  = 10.sp,
                            color     = TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier  = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFFEEEEEE))
                Spacer(modifier = Modifier.height(12.dp))

                // Summary row
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.Bottom
                ) {
                    Text(
                        text       = "Total This Week",
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color      = TextPrimary
                    )
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text       = "₱${String.format(Locale.US, "%,.2f", totalAmount)}",
                            fontSize   = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color      = TextPrimary
                        )
                    }
                }
            }
        }
    }
}

// ── Preview ───────────────────────────────────────────────────────────────────
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AdminDashboardPreview() {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope       = rememberCoroutineScope()
    AdminDashboardScreen(
        navController = rememberNavController(),
        drawerState   = drawerState,
        scope         = scope
    )
}
