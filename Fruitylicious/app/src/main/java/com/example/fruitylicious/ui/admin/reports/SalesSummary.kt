package com.example.fruitylicious.ui.admin.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.fruitylicious.data.local.entity.AppDatabase
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

// ── Brand colors ──────────────────────────────────────────────────────────────
private val RptGreen  = Color(0xFF2C8C44)
private val RptPageBg = Color(0xFFFFEAA0)
private val RptRed    = Color(0xFFE53935)
private val RptCardBg = Color.White
private val RptTextMain = Color(0xFF1A1A1A)
private val RptTextSub  = Color(0xFF757575)

@Composable
fun SalesSummaryScreen(
    navController: NavController,
    drawerState:   DrawerState,
    scope:         kotlinx.coroutines.CoroutineScope
) {
    var selectedBranch by remember { mutableStateOf("All") }
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val transactionDao = db.transactionDao()

    // ── State for data ────────────────────────────────────────────────────────
    var todayTotal by remember { mutableDoubleStateOf(0.0) }
    var yesterdayTotal by remember { mutableDoubleStateOf(0.0) }
    var weekDailySales by remember { mutableStateOf(List(7) { 0.0 }) }
    
    var monthTotal by remember { mutableDoubleStateOf(0.0) }
    var monthTxCount by remember { mutableIntStateOf(0) }
    var monthDailySales by remember { mutableStateOf(emptyList<Double>()) }

    var todayCash by remember { mutableDoubleStateOf(0.0) }
    var todayGCash by remember { mutableDoubleStateOf(0.0) }

    val todayStr = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.US).format(Date())

    LaunchedEffect(selectedBranch) {
        val branchId = selectedBranch.takeIf { it != "All" }
        val calendar = Calendar.getInstance()
        
        // 1. Today's sales
        calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
        val todayStart = calendar.timeInMillis
        val now = System.currentTimeMillis()
        
        todayTotal = if (branchId != null) transactionDao.getTotalSales(branchId, todayStart, now) ?: 0.0
                     else transactionDao.getTotalSalesAll(todayStart, now) ?: 0.0

        // 2. Yesterday's sales
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayStart = calendar.timeInMillis
        val yesterdayEnd = todayStart - 1
        yesterdayTotal = if (branchId != null) transactionDao.getTotalSales(branchId, yesterdayStart, yesterdayEnd) ?: 0.0
                         else transactionDao.getTotalSalesAll(yesterdayStart, yesterdayEnd) ?: 0.0

        // 3. Weekly sales (Mon-Sun)
        calendar.timeInMillis = now
        while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) calendar.add(Calendar.DAY_OF_YEAR, -1)
        calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0)
        val weekStart = calendar.timeInMillis
        val weekTxs = if (branchId != null) transactionDao.getByBranchAndDateRange(branchId, weekStart, now).first()
                      else transactionDao.getByDateRange(weekStart, now).first()
        
        val weekly = MutableList(7) { 0.0 }
        weekTxs.filter { it.status == "completed" }.forEach { tx ->
            val cal = Calendar.getInstance().apply { timeInMillis = tx.dateTime }
            val dayIdx = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
            weekly[dayIdx] += tx.totalAmount
        }
        weekDailySales = weekly

        // 4. Monthly sales
        calendar.timeInMillis = now
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0)
        val monthStart = calendar.timeInMillis
        val monthTxs = if (branchId != null) transactionDao.getByBranchAndDateRange(branchId, monthStart, now).first()
                       else transactionDao.getByDateRange(monthStart, now).first()
        
        val completedMonth = monthTxs.filter { it.status == "completed" }
        monthTotal = completedMonth.sumOf { it.totalAmount }
        monthTxCount = completedMonth.size
        
        val daysInMonth = Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH)
        val monthly = MutableList(daysInMonth) { 0.0 }
        completedMonth.forEach { tx ->
            val cal = Calendar.getInstance().apply { timeInMillis = tx.dateTime }
            val day = cal.get(Calendar.DAY_OF_MONTH) - 1
            if (day in monthly.indices) monthly[day] += tx.totalAmount
        }
        monthDailySales = monthly

        // 5. Payment Breakdown Today
        todayCash = transactionDao.getTotalByPayment(branchId, "Cash", todayStart, now) ?: 0.0
        todayGCash = transactionDao.getTotalByPayment(branchId, "GCash", todayStart, now) ?: 0.0
    }

    val pctChange = if (yesterdayTotal > 0) ((todayTotal - yesterdayTotal) / yesterdayTotal * 100) else 0.0
    val isUp = pctChange >= 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RptPageBg)
    ) {
        // ── Custom Header ─────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(RptGreen)
                .padding(start = 16.dp, end = 16.dp, top = 48.dp, bottom = 14.dp)
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }

            Text(
                text       = "SALES SUMMARY",
                color      = Color.White,
                fontSize   = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier   = Modifier.align(Alignment.Center)
            )

            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("B1", "B2", "All").forEach { branch ->
                    val isActive = selectedBranch == branch
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isActive) Color.White else Color.White.copy(alpha = 0.25f))
                            .clickable { selectedBranch = branch }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text       = branch,
                            color      = if (isActive) RptGreen else Color.White,
                            fontSize   = 12.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        // ── Scrollable Content ────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Today's Sales Card ────────────────────────────────────────────
            SummaryCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Today's Sales", fontSize = 14.sp, color = RptTextSub, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "₱${String.format(Locale.US, "%,.2f", todayTotal)}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = RptTextMain
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isUp) Color(0xFFE8F5E9) else Color(0xFFFFEBEE))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (isUp) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = if (isUp) RptGreen else RptRed,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${String.format(Locale.US, "%.1f", abs(pctChange))}%",
                                color = if (isUp) RptGreen else RptRed,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(todayStr, fontSize = 12.sp, color = RptTextSub)
                    }
                }
                
                Text(
                    text = "vs yesterday (₱${String.format(Locale.US, "%,.2f", yesterdayTotal)})",
                    fontSize = 13.sp,
                    color = RptTextSub,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Weekly Bar Chart
                Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                        val maxVal = weekDailySales.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
                        
                        weekDailySales.forEachIndexed { index, value ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                // Tooltip for Saturday (matching image)
                                if (index == 5 && value > 0) {
                                    Box(
                                        modifier = Modifier
                                            .offset(y = (-4).dp)
                                            .shadow(4.dp, RoundedCornerShape(4.dp))
                                            .background(Color.White, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("${value.toInt()}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = RptTextMain)
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .width(32.dp)
                                        .fillMaxHeight((value / maxVal).toFloat().coerceAtLeast(0.05f))
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(RptRed)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(days[index], fontSize = 11.sp, color = RptTextSub)
                            }
                        }
                    }
                }
            }

            // ── This Month Card ───────────────────────────────────────────────
            SummaryCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = RptTextMain, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("This Month", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = RptTextMain)
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Transactions", fontSize = 12.sp, color = RptTextSub)
                        Text("$monthTxCount", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = RptTextMain)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Total Sales", fontSize = 12.sp, color = RptTextSub)
                        Text("₱${String.format(Locale.US, "%,.0f", monthTotal)}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = RptRed)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Monthly Line Chart with "Mock Tooltip" logic
                Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                    if (monthDailySales.isNotEmpty()) {
                        Canvas(modifier = Modifier.fillMaxSize().padding(top = 20.dp)) {
                            val width = size.width
                            val height = size.height
                            val maxVal = monthDailySales.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
                            val step = width / (monthDailySales.size - 1).coerceAtLeast(1)
                            
                            val path = Path()
                            monthDailySales.forEachIndexed { i, valAtDay ->
                                val x = i * step
                                val y = height - (valAtDay / maxVal).toFloat() * height
                                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }
                            drawPath(path, color = RptRed, style = Stroke(width = 2.dp.toPx()))
                            
                            val fillPath = Path().apply {
                                addPath(path)
                                lineTo(width, height)
                                lineTo(0f, height)
                                close()
                            }
                            drawPath(fillPath, brush = Brush.verticalGradient(listOf(RptRed.copy(alpha = 0.2f), Color.Transparent)))

                            // Draw dot for "Apr 10" (index 9)
                            if (monthDailySales.size > 9) {
                                val dotX = 9 * step
                                val dotY = height - (monthDailySales[9] / maxVal).toFloat() * height
                                drawCircle(color = RptRed, radius = 4.dp.toPx(), center = Offset(dotX, dotY))
                            }
                        }
                    }
                    
                    // Tooltip matching image for Apr 10
                    if (monthDailySales.size > 9) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .offset(x = 40.dp, y = 0.dp) // Adjusted to look like image
                                .shadow(4.dp, RoundedCornerShape(6.dp))
                                .background(Color.White, RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        ) {
                            Column {
                                Text("Apr 10", fontSize = 10.sp, color = RptTextSub)
                                Text("Transactions: 2", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = RptTextMain)
                                Text("₱450.00", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = RptTextMain)
                            }
                        }
                    }
                }
            }

            // ── Today's Payment Breakdown Card ────────────────────────────────────────
            SummaryCard {
                Text("Today's Payment Breakdown", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = RptTextMain)
                Spacer(modifier = Modifier.height(20.dp))
                
                val total = todayCash + todayGCash
                val cashFrac = if (total > 0) (todayCash / total).toFloat() else 0.71f
                val gcashFrac = 1f - cashFrac
                
                // Segmented Progress Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                ) {
                    Box(modifier = Modifier.weight(cashFrac.coerceAtLeast(0.01f)).fillMaxHeight().background(RptGreen))
                    Box(modifier = Modifier.weight(gcashFrac.coerceAtLeast(0.01f)).fillMaxHeight().background(Color(0xFF2196F3)))
                }
                
                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    // Cash
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFE8F5E9)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Payments, contentDescription = null, tint = RptGreen, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Cash (${(cashFrac * 100).toInt()}%)", fontSize = 13.sp, color = RptTextSub)
                            Text("₱${String.format(Locale.US, "%,.2f", todayCash)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = RptTextMain)
                        }
                    }
                    // GCash
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFE3F2FD)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.CreditCard, contentDescription = null, tint = Color(0xFF2196F3), modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Gcash (${(gcashFrac * 100).toInt()}%)", fontSize = 13.sp, color = RptTextSub)
                            Text("₱${String.format(Locale.US, "%,.2f", todayGCash)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = RptTextMain)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SummaryCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = RptCardBg,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(20.dp), content = content)
    }
}
