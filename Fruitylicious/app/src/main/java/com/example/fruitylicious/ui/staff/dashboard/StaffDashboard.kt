package com.example.fruitylicious.ui.staff.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
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

// ── Staff brand colors (private, no conflict) ────────────────────────────────
private val StaffGreenPrimary  = Color(0xFF2C8C44)
private val StaffGreenDark     = Color(0xFF1B5E20)
private val StaffPageBg        = Color(0xFFFFEAA0)
private val StaffCardBg        = Color.White
private val StaffChartBar      = Color(0xFFE53935)
private val StaffTextPrimary   = Color(0xFF1A1A1A)
private val StaffTextSecondary = Color(0xFF757575)

// ── Screen ────────────────────────────────────────────────────────────────────
@Composable
fun StaffDashboardScreen(
    navController: NavController,
    drawerState:   DrawerState,
    scope:         CoroutineScope,
    staffName:     String = "Staff User",
    branchName:    String = "Branch 1"
) {
    val context        = LocalContext.current
    val database       = remember { AppDatabase.getDatabase(context) }
    val transactionDao = database.transactionDao()

    var weeklySalesData    by remember { mutableStateOf(List(7) { 0f }) }
    var totalAmount        by remember { mutableStateOf(0.0) }
    var transactionCount   by remember { mutableStateOf(0) }

    // Fetch weekly sales for this branch
    LaunchedEffect(branchName) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }

        val salesPerDay = mutableListOf<Float>()
        var totalSales  = 0.0

        // Derive the branch ID from branchName (e.g. "Branch 1" → "B1")
        val branchId = when (branchName) {
            "Branch 1" -> "B1"
            "Branch 2" -> "B2"
            else       -> "B1"
        }

        for (i in 0 until 7) {
            val dayStart = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            val dayEnd = calendar.timeInMillis

            val daySum = transactionDao.getTotalSales(branchId, dayStart, dayEnd) ?: 0.0
            salesPerDay.add(daySum.toFloat())
            totalSales += daySum
        }

        weeklySalesData  = salesPerDay
        totalAmount      = totalSales
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StaffPageBg)
            .verticalScroll(rememberScrollState())
    ) {
        // Header — no branch selector pills for staff
        StaffDashboardHeader(onMenuClick = { scope.launch { drawerState.open() } })

        Spacer(modifier = Modifier.height(16.dp))

        // Greeting card — fixed branch badge, no selector
        StaffGreetingCard(
            navController = navController,
            staffName     = staffName,
            branchName    = branchName
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Quick actions — staff set only
        StaffQuickActionsSection(navController)

        Spacer(modifier = Modifier.height(20.dp))

        // Sales chart
        StaffSalesChartSection(weeklySalesData, totalAmount, transactionCount)

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ── Header (no branch pills) ──────────────────────────────────────────────────
@Composable
private fun StaffDashboardHeader(onMenuClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(StaffGreenPrimary)
            .padding(start = 16.dp, end = 16.dp, top = 48.dp, bottom = 14.dp)
    ) {
        // Hamburger
        Column(
            modifier            = Modifier
                .align(Alignment.CenterStart)
                .clickable { onMenuClick() },
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .width(22.dp)
                        .height(2.5.dp)
                        .background(Color.White, RoundedCornerShape(2.dp))
                )
            }
        }

        Text(
            text       = "DASHBOARD",
            color      = Color.White,
            fontSize   = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier   = Modifier.align(Alignment.Center)
        )
        // No branch pills — staff only sees their assigned branch
    }
}

// ── Greeting card (staff) ─────────────────────────────────────────────────────
@Composable
private fun StaffGreetingCard(
    navController: NavController,
    staffName:     String,
    branchName:    String
) {
    Surface(
        modifier        = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        color           = StaffCardBg,
        shape           = RoundedCornerShape(16.dp),
        shadowElevation = 3.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.Top
            ) {
                Column {
                    Text(
                        text       = "Hello, $staffName",
                        fontSize   = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color      = StaffTextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val sdf = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.US)
                    Text(
                        text     = sdf.format(Date()),
                        fontSize = 13.sp,
                        color    = StaffTextSecondary
                    )
                }

                // Fixed branch badge — no dropdown, staff can't switch
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFE8F5E9))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text       = branchName,
                        color      = StaffGreenPrimary,
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Dark green CTA card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(StaffGreenDark)
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

                Button(
                    onClick = {
                        navController.navigate("pos") {
                            popUpTo("home") { saveState = true }
                            launchSingleTop = true
                            restoreState    = true
                        }
                    },
                    modifier       = Modifier
                        .align(Alignment.CenterEnd)
                        .height(44.dp),
                    colors         = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape          = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(
                        imageVector        = Icons.Outlined.ShoppingCart,
                        contentDescription = null,
                        tint               = StaffGreenPrimary,
                        modifier           = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text       = "Start POS",
                        color      = StaffGreenPrimary,
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ── Quick actions (staff-only set) ────────────────────────────────────────────
@Composable
private fun StaffQuickActionsSection(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text       = "Quick Actions",
            fontSize   = 16.sp,
            fontWeight = FontWeight.Bold,
            color      = StaffTextPrimary,
            modifier   = Modifier.padding(bottom = 10.dp)
        )

        // Staff quick actions: wala pang route
        val actions = listOf(
            Triple(Icons.Outlined.BarChart,      "Sales Summary",           ""),
            Triple(Icons.Outlined.Autorenew,     "Restock",                 ""),
            Triple(Icons.Outlined.Search,        "Inventory Monitoring",    ""),
            Triple(Icons.Outlined.DeleteOutline, "Waste Management",        "")
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            actions.chunked(2).forEach { rowItems ->
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowItems.forEach { (icon, label, route) ->
                        StaffQuickActionCard(
                            icon     = icon,
                            label    = label,
                            modifier = Modifier.weight(1f),
                            onClick  = {
                                if (route.isNotEmpty()) navController.navigate(route)
                            }
                        )
                    }
                    if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun StaffQuickActionCard(
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
        color           = StaffCardBg,
        shape           = RoundedCornerShape(14.dp),
        shadowElevation = 2.dp
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier         = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(50.dp))
                        .background(Color(0xFFE8F5E9)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = icon,
                        contentDescription = label,
                        tint               = StaffGreenPrimary,
                        modifier           = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text       = label,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = StaffTextPrimary,
                    textAlign  = TextAlign.Center
                )
            }
        }
    }
}

// ── Sales chart ───────────────────────────────────────────────────────────────
@Composable
private fun StaffSalesChartSection(
    weeklySales:      List<Float>,
    totalAmount:      Double,
    transactionCount: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text       = "Sales This Week",
            fontSize   = 16.sp,
            fontWeight = FontWeight.Bold,
            color      = StaffTextPrimary,
            modifier   = Modifier.padding(bottom = 10.dp)
        )

        Surface(
            modifier        = Modifier.fillMaxWidth(),
            color           = StaffCardBg,
            shape           = RoundedCornerShape(16.dp),
            shadowElevation = 3.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {

                val days         = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                val maxSales     = (weeklySales.maxOrNull() ?: 0f).coerceAtLeast(100f)
                val roundedMax   = (ceil(maxSales / 100.0) * 100).toInt()
                val highlightIdx = weeklySales.indexOf(weeklySales.maxOrNull() ?: 0f)
                val yLabels      = listOf(
                    "P$roundedMax",
                    "P${roundedMax * 3 / 4}",
                    "P${roundedMax / 2}",
                    "P${roundedMax / 4}",
                    "P0"
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    // Y-axis
                    Column(
                        modifier            = Modifier
                            .width(40.dp)
                            .fillMaxHeight()
                            .padding(bottom = 20.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        yLabels.forEach { lbl ->
                            Text(
                                text      = lbl,
                                fontSize  = 9.sp,
                                color     = StaffTextSecondary,
                                textAlign = TextAlign.End,
                                modifier  = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

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

                        // Grid lines
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
                            val barH = (value / roundedMax) * chartHeight
                            val left = idx * gap + (gap - barWidth) / 2f
                            val top  = chartHeight - barH

                            drawRoundRect(
                                color        = StaffChartBar,
                                topLeft      = Offset(left, top),
                                size         = Size(barWidth, barH),
                                cornerRadius = CornerRadius(4.dp.toPx())
                            )

                            // Tooltip on tallest bar
                            if (idx == highlightIdx && value > 0f) {
                                staffDrawTooltip(
                                    label   = value.toInt().toString(),
                                    centerX = left + barWidth / 2f,
                                    topY    = top - 26.dp.toPx()
                                )
                            }
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
                            color     = StaffTextSecondary,
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
                        text       = "Total 7 Days",
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color      = StaffTextPrimary
                    )
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text       = "₱${String.format(Locale.US, "%,.2f", totalAmount)}",
                            fontSize   = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color      = StaffTextPrimary
                        )
                        if (transactionCount > 0) {
                            Text(
                                text     = "$transactionCount transactions",
                                fontSize = 12.sp,
                                color    = StaffTextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Canvas tooltip helper ─────────────────────────────────────────────────────
private fun DrawScope.staffDrawTooltip(label: String, centerX: Float, topY: Float) {
    val bubbleW = 42.dp.toPx()
    val bubbleH = 22.dp.toPx()
    val left    = centerX - bubbleW / 2f

    drawRoundRect(
        color        = Color(0xFF1A1A1A),
        topLeft      = Offset(left, topY),
        size         = Size(bubbleW, bubbleH),
        cornerRadius = CornerRadius(4.dp.toPx())
    )

    val triTop = topY + bubbleH
    val path   = Path().apply {
        moveTo(centerX - 4.dp.toPx(), triTop)
        lineTo(centerX + 4.dp.toPx(), triTop)
        lineTo(centerX, triTop + 5.dp.toPx())
        close()
    }
    drawPath(path, color = Color(0xFF1A1A1A))
}

// ── Preview ───────────────────────────────────────────────────────────────────
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun StaffDashboardPreview() {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope       = rememberCoroutineScope()
    StaffDashboardScreen(
        navController = rememberNavController(),
        drawerState   = drawerState,
        scope         = scope,
        staffName     = "Staff User",
        branchName    = "Branch 1"
    )
}