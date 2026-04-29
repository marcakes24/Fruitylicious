package com.example.fruitylicious.ui.admin.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.fruitylicious.data.local.entity.AppDatabase
import java.util.*
import kotlin.math.abs

// ── Brand colors ──────────────────────────────────────────────────────────────
private val RptGreen     = Color(0xFF2C8C44)
private val RptGreenDark = Color(0xFF1B5E20)
private val RptCardBg    = Color.White
private val RptRed       = Color(0xFFE53935)
private val RptTextMain  = Color(0xFF1A1A1A)
private val RptTextSub   = Color(0xFF757575)

// ══════════════════════════════════════════════════════════════════════════════
// TAB 1 — SALES
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun SalesTabContent(branch: String, navController: NavController) {
    val context        = LocalContext.current
    val db             = remember { AppDatabase.getDatabase(context) }
    val transactionDao = db.transactionDao()

    // Period toggle: daily / weekly / monthly
    var period by remember { mutableStateOf("daily") }

    // Live state
    var totalSales     by remember { mutableDoubleStateOf(0.0) }
    var previousSales  by remember { mutableDoubleStateOf(0.0) }
    var cashTotal      by remember { mutableDoubleStateOf(0.0) }
    var gcashTotal     by remember { mutableDoubleStateOf(0.0) }
    var topItems       by remember { mutableStateOf(listOf<Pair<String, Int>>()) }
    var salesBreakdown by remember { mutableStateOf(listOf<Triple<String, Int, Double>>()) }

    LaunchedEffect(branch, period) {
        val branchId = branch.takeIf { it != "All" }
        val now      = Calendar.getInstance()

        val (startMs, prevStartMs, prevEndMs) = when (period) {
            "daily" -> {
                val start = now.clone() as Calendar
                start.set(Calendar.HOUR_OF_DAY, 0); start.set(Calendar.MINUTE, 0)
                start.set(Calendar.SECOND, 0);      start.set(Calendar.MILLISECOND, 0)
                val prevEnd   = start.timeInMillis
                val prevStart = prevEnd - 86_400_000L
                Triple(start.timeInMillis, prevStart, prevEnd)
            }
            "weekly" -> {
                val start = now.clone() as Calendar
                while (start.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY)
                    start.add(Calendar.DAY_OF_YEAR, -1)
                start.set(Calendar.HOUR_OF_DAY, 0); start.set(Calendar.MINUTE, 0)
                start.set(Calendar.SECOND, 0);      start.set(Calendar.MILLISECOND, 0)
                val prevEnd   = start.timeInMillis
                val prevStart = prevEnd - 7 * 86_400_000L
                Triple(start.timeInMillis, prevStart, prevEnd)
            }
            else -> { // monthly
                val start = now.clone() as Calendar
                start.set(Calendar.DAY_OF_MONTH, 1)
                start.set(Calendar.HOUR_OF_DAY, 0); start.set(Calendar.MINUTE, 0)
                start.set(Calendar.SECOND, 0);      start.set(Calendar.MILLISECOND, 0)
                val prevEnd = start.timeInMillis
                start.add(Calendar.MONTH, -1)
                Triple(start.timeInMillis, start.timeInMillis, prevEnd)
            }
        }

        val endMs = now.timeInMillis

        totalSales = if (branchId != null)
            transactionDao.getTotalSales(branchId, startMs, endMs) ?: 0.0
        else
            transactionDao.getTotalSalesAll(startMs, endMs) ?: 0.0

        previousSales = if (branchId != null)
            transactionDao.getTotalSales(branchId, prevStartMs, prevEndMs) ?: 0.0
        else
            transactionDao.getTotalSalesAll(prevStartMs, prevEndMs) ?: 0.0

        cashTotal  = transactionDao.getTotalByPayment(branchId, "Cash",  startMs, endMs) ?: 0.0
        gcashTotal = transactionDao.getTotalByPayment(branchId, "GCash", startMs, endMs) ?: 0.0

        topItems = transactionDao.getTopSellingItems(branchId, startMs, endMs)
            .map { it.productName to it.totalQty }

        salesBreakdown = transactionDao.getSalesBreakdown(branchId, startMs, endMs)
            .map { Triple(it.productName, it.qty, it.totalAmount) }
    }

    val pctChange = if (previousSales > 0)
        ((totalSales - previousSales) / previousSales * 100) else 0.0
    val isUp = pctChange >= 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Period toggle + hero card ─────────────────────────────────────────
        SalesRptCard {
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
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isUp) Color(0xFFE8F5E9) else Color(0xFFFFEBEE))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text       = "${if (isUp) "▲" else "▼"} ${String.format(Locale.US, "%.1f",
                            abs(pctChange)
                        )}%",
                        color      = if (isUp) RptGreen else RptRed,
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

        // ── Sales breakdown table ─────────────────────────────────────────────
        if (salesBreakdown.isNotEmpty()) {
            SalesRptCard {
                Text(
                    "Sales Breakdown ($period)",
                    fontWeight = FontWeight.Bold,
                    fontSize   = 14.sp,
                    color      = RptTextMain
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("Product", modifier = Modifier.weight(1f),      fontSize = 11.sp, color = RptTextSub, fontWeight = FontWeight.Bold)
                    Text("Qty",    modifier = Modifier.width(40.dp),     fontSize = 11.sp, color = RptTextSub, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text("Total",  modifier = Modifier.width(80.dp),     fontSize = 11.sp, color = RptTextSub, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                salesBreakdown.forEach { (name, qty, total) ->
                    Row(
                        modifier          = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            name,
                            modifier  = Modifier.weight(1f),
                            fontSize  = 13.sp,
                            color     = RptTextMain,
                            fontWeight = FontWeight.Medium,
                            maxLines  = 1,
                            overflow  = TextOverflow.Ellipsis
                        )
                        Text("$qty",
                            modifier  = Modifier.width(40.dp),
                            fontSize  = 13.sp,
                            color     = RptTextMain,
                            textAlign = TextAlign.Center)
                        Text(
                            "₱${String.format(Locale.US, "%,.2f", total)}",
                            modifier   = Modifier.width(80.dp),
                            fontSize   = 13.sp,
                            color      = RptGreen,
                            fontWeight = FontWeight.Bold,
                            textAlign  = TextAlign.End
                        )
                    }
                }
            }
        }

        // ── Payment summary ───────────────────────────────────────────────────
        if (cashTotal + gcashTotal > 0) {
            SalesRptCard {
                Text(
                    "Payment Summary ($period)",
                    fontWeight = FontWeight.Bold,
                    fontSize   = 14.sp,
                    color      = RptTextMain
                )
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
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(RptGreen))
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text("Cash",  fontSize = 11.sp, color = RptTextSub)
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

        // ── Top-selling items ─────────────────────────────────────────────────
        if (topItems.isNotEmpty()) {
            SalesRptCard {
                Text(
                    "Top Selling Items",
                    fontWeight = FontWeight.Bold,
                    fontSize   = 14.sp,
                    color      = RptTextMain
                )
                Spacer(modifier = Modifier.height(8.dp))
                topItems.forEachIndexed { idx, item ->
                    Row(
                        modifier              = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                        verticalAlignment     = Alignment.CenterVertically,
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

        // ── Full Sales Summary button ─────────────────────────────────────────
        Button(
            onClick  = { navController.navigate("admin_sales_summary") },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = RptGreenDark),
            shape  = RoundedCornerShape(12.dp)
        ) {
            Text(
                "Go to Full Sales Summary  →",
                color      = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize   = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

// ── Shared card wrapper ───────────────────────────────────────────────────────
@Composable
private fun SalesRptCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier        = Modifier.fillMaxWidth(),
        color           = RptCardBg,
        shape           = RoundedCornerShape(12.dp),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}