package com.example.fruitylicious

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.fruitylicious.data.local.entity.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.*

// ── Brand colors ──────────────────────────────────────────────────────────────
private val RptGreen      = Color(0xFF2C8C44)
private val RptGreenDark  = Color(0xFF1B5E20)
private val RptPageBg     = Color(0xFFFFEAA0)
private val RptCardBg     = Color.White
private val RptRed        = Color(0xFFE53935)
private val RptTextMain   = Color(0xFF1A1A1A)
private val RptTextSub    = Color(0xFF757575)
private val RptLowStock   = Color(0xFFE53935)
private val RptOkStock    = Color(0xFF2C8C44)

// ── Tab enum ──────────────────────────────────────────────────────────────────
private enum class ReportTab(val label: String) {
    SALES("Sales"), WASTE("Waste"), RESTOCK("Restock"), INVENTORY("Inventory")
}

// ── Data classes for UI ───────────────────────────────────────────────────────
private data class RestockFrequencyItem(val name: String, val frequency: String, val avgUnits: Int)

private data class StockItem(
    val name: String, val category: String,
    val current: Int, val unit: String, val minStock: Int
)

// ── Root screen ───────────────────────────────────────────────────────────────
@Composable
fun ReportsScreen(
    navController: NavController,
    drawerState:   DrawerState,
    scope:         CoroutineScope
) {
    var selectedTab    by remember { mutableStateOf(ReportTab.SALES) }
    var selectedBranch by remember { mutableStateOf("All") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RptPageBg)
    ) {
        // ── Top header ────────────────────────────────────────────────────────
        ReportsHeader(
            selectedBranch = selectedBranch,
            onBranchSelect = { selectedBranch = it },
            onMenuClick    = { scope.launch { drawerState.open() } }
        )

        // ── Tab row ───────────────────────────────────────────────────────────
        ReportsTabRow(
            selected  = selectedTab,
            onSelect  = { selectedTab = it }
        )

        // ── Tab content ───────────────────────────────────────────────────────
        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTab) {
                ReportTab.SALES     -> SalesTabContent(selectedBranch, navController)
                ReportTab.WASTE     -> WasteTabContent(selectedBranch)
                ReportTab.RESTOCK   -> RestockTabContent(selectedBranch)
                ReportTab.INVENTORY -> InventoryTabContent(selectedBranch)
            }
        }
    }
}

// ── Header ────────────────────────────────────────────────────────────────────
@Composable
private fun ReportsHeader(
    selectedBranch: String,
    onBranchSelect: (String) -> Unit,
    onMenuClick:    () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(RptGreen)
            .padding(start = 16.dp, end = 16.dp, top = 48.dp, bottom = 14.dp)
    ) {
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
            text       = "REPORTS",
            color      = Color.White,
            fontSize   = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier   = Modifier.align(Alignment.Center)
        )

        // Branch pills
        Row(
            modifier              = Modifier.align(Alignment.CenterEnd),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("B1", "B2", "All").forEach { branch ->
                val isActive = selectedBranch == branch
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isActive) Color.White else Color.White.copy(alpha = 0.25f))
                        .clickable { onBranchSelect(branch) }
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
}

// ── Tab row ───────────────────────────────────────────────────────────────────
@Composable
private fun ReportsTabRow(
    selected: ReportTab,
    onSelect: (ReportTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(RptGreen)
            .padding(start = 8.dp, end = 8.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val tabIcons = mapOf(
            ReportTab.SALES     to Icons.AutoMirrored.Outlined.TrendingUp,
            ReportTab.WASTE     to Icons.Outlined.DeleteOutline,
            ReportTab.RESTOCK   to Icons.Outlined.Autorenew,
            ReportTab.INVENTORY to Icons.Outlined.Inventory2
        )

        ReportTab.entries.forEach { tab ->
            val isActive = selected == tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isActive) Color.White else Color.White.copy(alpha = 0.15f))
                    .clickable { onSelect(tab) }
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment      = Alignment.CenterVertically,
                    horizontalArrangement  = Arrangement.Center
                ) {
                    Icon(
                        imageVector        = tabIcons[tab]!!,
                        contentDescription = null,
                        tint               = if (isActive) RptGreen else Color.White,
                        modifier           = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text       = tab.label,
                        color      = if (isActive) RptGreen else Color.White,
                        fontSize   = 12.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// TAB 1 — SALES
// ══════════════════════════════════════════════════════════════════════════════
@Composable
private fun SalesTabContent(branch: String, navController: NavController) {
    val context        = LocalContext.current
    val db             = remember { AppDatabase.getDatabase(context) }
    val transactionDao = db.transactionDao()

    // Period toggle: daily / weekly / monthly
    var period by remember { mutableStateOf("daily") }

    // Live state
    var totalSales         by remember { mutableDoubleStateOf(0.0) }
    var previousSales      by remember { mutableDoubleStateOf(0.0) }
    var cashTotal          by remember { mutableDoubleStateOf(0.0) }
    var gcashTotal         by remember { mutableDoubleStateOf(0.0) }
    var topItems           by remember { mutableStateOf(listOf<Pair<String, Int>>()) }
    var salesBreakdown     by remember { mutableStateOf(listOf<Triple<String, Int, Double>>()) }

    LaunchedEffect(branch, period) {
        val branchId = branch.takeIf { it != "All" }
        val now      = Calendar.getInstance()

        val (startMs, prevStartMs, prevEndMs) = when (period) {
            "daily" -> {
                val start = now.clone() as Calendar
                start.set(Calendar.HOUR_OF_DAY, 0); start.set(Calendar.MINUTE, 0)
                start.set(Calendar.SECOND, 0); start.set(Calendar.MILLISECOND, 0)
                val prevEnd   = start.timeInMillis
                val prevStart = prevEnd - 86_400_000L
                Triple(start.timeInMillis, prevStart, prevEnd)
            }
            "weekly" -> {
                val start = now.clone() as Calendar
                while (start.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY)
                    start.add(Calendar.DAY_OF_YEAR, -1)
                start.set(Calendar.HOUR_OF_DAY, 0); start.set(Calendar.MINUTE, 0)
                start.set(Calendar.SECOND, 0); start.set(Calendar.MILLISECOND, 0)
                val prevEnd   = start.timeInMillis
                val prevStart = prevEnd - 7 * 86_400_000L
                Triple(start.timeInMillis, prevStart, prevEnd)
            }
            else -> { // monthly
                val start = now.clone() as Calendar
                start.set(Calendar.DAY_OF_MONTH, 1)
                start.set(Calendar.HOUR_OF_DAY, 0); start.set(Calendar.MINUTE, 0)
                start.set(Calendar.SECOND, 0); start.set(Calendar.MILLISECOND, 0)
                val prevEnd   = start.timeInMillis
                start.add(Calendar.MONTH, -1)
                Triple(start.timeInMillis, start.timeInMillis, prevEnd)
            }
        }

        val endMs = now.timeInMillis

        totalSales     = if (branchId != null)
            transactionDao.getTotalSales(branchId, startMs, endMs) ?: 0.0
        else
            transactionDao.getTotalSalesAll(startMs, endMs) ?: 0.0

        previousSales  = if (branchId != null)
            transactionDao.getTotalSales(branchId, prevStartMs, prevEndMs) ?: 0.0
        else
            transactionDao.getTotalSalesAll(prevStartMs, prevEndMs) ?: 0.0

        // Payment method breakdown
        cashTotal  = transactionDao.getTotalByPayment(branchId, "Cash", startMs, endMs)  ?: 0.0
        gcashTotal = transactionDao.getTotalByPayment(branchId, "GCash", startMs, endMs) ?: 0.0

        // Top-selling items
        topItems       = transactionDao.getTopSellingItems(branchId, startMs, endMs)
            .map { it.productName to it.totalQty }

        // Sales breakdown table
        salesBreakdown = transactionDao.getSalesBreakdown(branchId, startMs, endMs)
            .map { Triple(it.productName, it.qty, it.totalAmount) }
    }

    val pctChange = if (previousSales > 0)
        ((totalSales - previousSales) / previousSales * 100) else 0.0
    val isUp      = pctChange >= 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Period toggle
        RptCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFF0F0F0))
                    .padding(3.dp)
            ) {
                listOf("daily", "weekly", "monthly").forEach { p ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (period == p) Color.White else Color.Transparent)
                            .clickable { period = p }
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text       = p,
                            fontSize   = 13.sp,
                            fontWeight = if (period == p) FontWeight.Bold else FontWeight.Normal,
                            color      = if (period == p) RptTextMain else RptTextSub
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text     = "${period.replaceFirstChar { it.uppercase() }}'s Sales",
                fontSize = 13.sp,
                color    = RptTextSub
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text       = "₱${String.format(Locale.US, "%,.2f", totalSales)}",
                    fontSize   = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color      = RptTextMain,
                    modifier   = Modifier.weight(1f)
                )
                // % change chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isUp) Color(0xFFE8F5E9) else Color(0xFFFFEBEE))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text  = "${if (isUp) "▲" else "▼"} ${String.format(Locale.US, "%.1f", kotlin.math.abs(pctChange))}%",
                        color = if (isUp) RptGreen else RptRed,
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                text     = "vs previous (₱${String.format(Locale.US, "%,.2f", previousSales)})",
                fontSize = 12.sp,
                color    = RptTextSub
            )
        }

        // Sales breakdown table
        if (salesBreakdown.isNotEmpty()) {
            RptCard {
                Text("Sales Breakdown ($period)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = RptTextMain)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("Product", modifier = Modifier.weight(1f), fontSize = 11.sp, color = RptTextSub, fontWeight = FontWeight.Bold)
                    Text("Qty",    modifier = Modifier.width(40.dp), fontSize = 11.sp, color = RptTextSub, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text("Total",  modifier = Modifier.width(80.dp), fontSize = 11.sp, color = RptTextSub, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                salesBreakdown.forEach { (name, qty, total) ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(name, modifier = Modifier.weight(1f), fontSize = 13.sp, color = RptTextMain, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("$qty", modifier = Modifier.width(40.dp), fontSize = 13.sp, color = RptTextMain, textAlign = TextAlign.Center)
                        Text("₱${String.format(Locale.US, "%,.2f", total)}", modifier = Modifier.width(80.dp), fontSize = 13.sp, color = RptGreen, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                    }
                }
            }
        }

        // Payment summary
        if (cashTotal + gcashTotal > 0) {
            RptCard {
                Text("Payment Summary ($period)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = RptTextMain)
                Spacer(modifier = Modifier.height(10.dp))
                val total    = cashTotal + gcashTotal
                val cashFrac = if (total > 0) (cashTotal / total).toFloat() else 0f

                // Stacked bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color(0xFF2196F3))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(cashFrac)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(5.dp))
                            .background(RptGreen)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(RptGreen))
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text("Cash", fontSize = 11.sp, color = RptTextSub)
                            Text("₱${String.format(Locale.US, "%,.2f", cashTotal)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = RptTextMain)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF2196F3)))
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text("GCash", fontSize = 11.sp, color = RptTextSub)
                            Text("₱${String.format(Locale.US, "%,.2f", gcashTotal)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = RptTextMain)
                        }
                    }
                }
            }
        }

        // Top-selling items
        if (topItems.isNotEmpty()) {
            RptCard {
                Text("Top Selling Items", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = RptTextMain)
                Spacer(modifier = Modifier.height(8.dp))
                topItems.forEachIndexed { idx, item ->
                    Row(
                        modifier          = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${idx + 1}.", fontSize = 13.sp, color = RptTextSub, modifier = Modifier.width(24.dp))
                            Text(item.first, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = RptTextMain)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(RptTextMain)
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Text("${item.second} sold", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Go to Full Sales Summary button
        Button(
            onClick  = { navController.navigate("admin_sales_summary") },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = RptGreenDark),
            shape    = RoundedCornerShape(12.dp)
        ) {
            Text("Go to Full Sales Summary  →", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// TAB 2 — WASTE
// ══════════════════════════════════════════════════════════════════════════════
@Composable
private fun WasteTabContent(branch: String) {
    val context  = LocalContext.current
    val db       = remember { AppDatabase.getDatabase(context) }
    val wasteDao = db.wasteLogDao()

    var totalWaste    by remember { mutableIntStateOf(0) }
    var mostWasted    by remember { mutableStateOf("—") }
    var mostWastedQty by remember { mutableIntStateOf(0) }
    var reasonData    by remember { mutableStateOf(listOf<Pair<String, Int>>()) }
    var wasteByItem   by remember { mutableStateOf(listOf<Pair<String, Int>>()) }

    LaunchedEffect(branch) {
        val branchId = branch.takeIf { it != "All" }
        val cal      = Calendar.getInstance()
        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY)
            cal.add(Calendar.DAY_OF_YEAR, -1)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val weekStart = cal.timeInMillis
        val weekEnd   = System.currentTimeMillis()

        totalWaste    = wasteDao.getTotalWasteCount(branchId, weekStart, weekEnd) ?: 0
        reasonData    = wasteDao.getWasteByReason(branchId, weekStart, weekEnd)
            .map { it.reason to it.count }
        wasteByItem   = wasteDao.getWasteByItem(branchId, weekStart, weekEnd)
            .map { it.itemName to it.count }

        if (wasteByItem.isNotEmpty()) {
            val top    = wasteByItem.maxByOrNull { it.second }!!
            mostWasted    = top.first
            mostWastedQty = top.second
        }
    }

    val maxReason = reasonData.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Summary row
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Total waste card
            Surface(modifier = Modifier.weight(1f), color = RptCardBg, shape = RoundedCornerShape(12.dp), shadowElevation = 2.dp) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Total Waste (This Week)", fontSize = 11.sp, color = RptTextSub)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("$totalWaste", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = RptRed)
                    Text("pcs", fontSize = 11.sp, color = RptTextSub)
                }
            }
            // Most wasted card
            Surface(modifier = Modifier.weight(1f), color = RptCardBg, shape = RoundedCornerShape(12.dp), shadowElevation = 2.dp) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Most Wasted", fontSize = 11.sp, color = RptTextSub)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(mostWasted, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = RptTextMain, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("$mostWastedQty pcs", fontSize = 11.sp, color = RptTextSub)
                }
            }
        }

        // Primary reasons bar chart
        RptCard {
            Text("Primary Reasons", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = RptTextMain)
            Spacer(modifier = Modifier.height(12.dp))
            if (reasonData.isEmpty()) {
                Text("No waste records this week", fontSize = 13.sp, color = RptTextSub)
            } else {
                reasonData.forEach { data ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 5.dp)) {
                        Text(data.first, modifier = Modifier.width(120.dp), fontSize = 12.sp, color = RptTextSub, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(14.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(Color(0xFFFFCDD2))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(data.second.toFloat() / maxReason)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(RptRed)
                            )
                        }
                    }
                }
            }
        }

        // Waste by items list
        RptCard {
            Text("Waste by Items (This Week)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = RptTextMain)
            Spacer(modifier = Modifier.height(8.dp))
            if (wasteByItem.isEmpty()) {
                Text("No waste recorded this week", fontSize = 13.sp, color = RptTextSub)
            } else {
                wasteByItem.forEach { item ->
                    Row(
                        modifier              = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(item.first, fontSize = 14.sp, color = RptTextMain, fontWeight = FontWeight.Medium)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(RptTextMain)
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Text("${item.second} wasted", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// TAB 3 — RESTOCK
// ══════════════════════════════════════════════════════════════════════════════
@Composable
private fun RestockTabContent(branch: String) {
    val context    = LocalContext.current
    val db         = remember { AppDatabase.getDatabase(context) }
    val restockDao = db.restockLogDao()

    var totalToday   by remember { mutableIntStateOf(0) }
    var restockItems by remember { mutableStateOf(listOf<RestockFrequencyItem>()) }

    LaunchedEffect(branch) {
        val branchId  = branch.takeIf { it != "All" }
        val todayStart = run {
            val c = Calendar.getInstance()
            c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0)
            c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
            c.timeInMillis
        }

        totalToday   = restockDao.getTotalRestockedToday(branchId, todayStart, System.currentTimeMillis()) ?: 0
        restockItems = restockDao.getRestockFrequency(branchId)
            .map { RestockFrequencyItem(it.ingredientName, it.frequencyLabel, it.avgUnits) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Total Added Today hero card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(RptGreenDark)
                .padding(24.dp)
        ) {
            Column {
                Text("Total Added Today", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text       = "$totalToday",
                        color      = Color.White,
                        fontSize   = 40.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("units", color = Color.White.copy(alpha = 0.8f), fontSize = 16.sp,
                        modifier = Modifier.padding(bottom = 6.dp))
                }
            }
        }

        // Restock frequency table
        RptCard {
            Text("Restock Frequency", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = RptTextMain)
            Spacer(modifier = Modifier.height(8.dp))

            // Header row
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                Text("Product",   modifier = Modifier.weight(1f),    fontSize = 11.sp, color = RptTextSub, fontWeight = FontWeight.Bold)
                Text("Times",     modifier = Modifier.width(70.dp),  fontSize = 11.sp, color = RptTextSub, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text("Avg Units", modifier = Modifier.width(70.dp),  fontSize = 11.sp, color = RptTextSub, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
            }
            HorizontalDivider(modifier = Modifier.padding(bottom = 6.dp))

            if (restockItems.isEmpty()) {
                Text("No restock data available", fontSize = 13.sp, color = RptTextSub)
            } else {
                restockItems.forEach { item ->
                    Row(
                        modifier          = Modifier.fillMaxWidth().padding(vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(item.name, modifier = Modifier.weight(1f), fontSize = 14.sp,
                            fontWeight = FontWeight.Medium, color = RptTextMain, maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                        Box(
                            modifier = Modifier
                                .width(70.dp)
                                .padding(horizontal = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF0F0F0)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text      = item.frequency,
                                fontSize  = 10.sp,
                                color     = RptTextSub,
                                textAlign = TextAlign.Center,
                                modifier  = Modifier.padding(horizontal = 4.dp, vertical = 3.dp)
                            )
                        }
                        Text(
                            text      = "${item.avgUnits}",
                            modifier  = Modifier.width(70.dp),
                            fontSize  = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color     = RptTextMain,
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// TAB 4 — INVENTORY
// ══════════════════════════════════════════════════════════════════════════════
@Composable
private fun InventoryTabContent(branch: String) {
    val context      = LocalContext.current
    val db           = remember { AppDatabase.getDatabase(context) }
    val inventoryDao = db.inventoryDao()

    var totalStock   by remember { mutableIntStateOf(0) }
    var inStockCount by remember { mutableIntStateOf(0) }
    var lowStockCount by remember { mutableIntStateOf(0) }
    var stockItems   by remember { mutableStateOf(listOf<StockItem>()) }

    LaunchedEffect(branch) {
        val branchId = branch.takeIf { it != "All" }
        val items    = inventoryDao.getInventoryReport(branchId)
            .map { reportItem ->
                StockItem(
                    name = reportItem.ingredientName,
                    category = if (reportItem.isPackaging) "Packaging" else "Raw Ingredient",
                    current = reportItem.currentStock.toInt(),
                    unit = reportItem.unitType,
                    minStock = 10 // Default minimum threshold
                )
            }

        stockItems    = items
        totalStock    = items.size
        lowStockCount = items.count { it.current <= it.minStock }
        inStockCount  = items.count { it.current > it.minStock }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Summary metrics row
        Surface(modifier = Modifier.fillMaxWidth(), color = RptCardBg, shape = RoundedCornerShape(12.dp), shadowElevation = 2.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text("Total Stock", fontSize = 11.sp, color = RptTextSub)
                    Text("$totalStock", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = RptTextMain)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("In Stock", fontSize = 11.sp, color = RptGreen)
                    Text("$inStockCount", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = RptGreen)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Low Stock", fontSize = 11.sp, color = RptRed)
                    Text("$lowStockCount", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = RptRed)
                }
            }
        }

        // Stock details list
        RptCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Stock Details", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = RptTextMain)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(RptTextMain)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("$totalStock total", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            if (stockItems.isEmpty()) {
                Text("No inventory data available", fontSize = 13.sp, color = RptTextSub)
            } else {
                stockItems.forEach { item ->
                    val isLow = item.current <= item.minStock
                    Row(
                        modifier              = Modifier.fillMaxWidth().padding(vertical = 7.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = RptTextMain)
                            Text(item.category, fontSize = 11.sp, color = RptTextSub)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text       = "${item.current} ${item.unit}",
                                fontSize   = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color      = if (isLow) RptLowStock else RptOkStock
                            )
                            Text("Min ${item.minStock}", fontSize = 10.sp, color = RptTextSub)
                        }
                    }
                    HorizontalDivider(color = Color(0xFFF0F0F0))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

// ── Shared card wrapper ───────────────────────────────────────────────────────
@Composable
private fun RptCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier        = Modifier.fillMaxWidth(),
        color           = RptCardBg,
        shape           = RoundedCornerShape(12.dp),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

// ── Preview ───────────────────────────────────────────────────────────────────
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ReportsScreenPreview() {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope       = rememberCoroutineScope()
    ReportsScreen(
        navController = rememberNavController(),
        drawerState   = drawerState,
        scope         = scope
    )
}
