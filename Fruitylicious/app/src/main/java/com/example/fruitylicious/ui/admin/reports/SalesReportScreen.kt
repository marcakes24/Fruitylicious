package com.example.fruitylicious.ui.admin.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
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

// Colors are defined in ReportsDashboardScreen.kt

@Composable
fun SalesTabContent(branch: String, navController: NavController) {
    val context        = LocalContext.current
    val db             = remember { AppDatabase.getDatabase(context) }
    val transactionDao = db.transactionDao()

    var period by remember { mutableStateOf("daily") }

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
        // ── Period Toggle & Hero Card ─────────────────────────────────────────
        SalesRptCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFF5F5F5))
                    .padding(4.dp)
            ) {
                listOf("daily", "weekly", "monthly").forEach { p ->
                    val isSel = (period == p)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isSel) Color.White else Color.Transparent)
                            .clickable { period = p }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text       = p,
                            fontSize   = 13.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            color      = if (isSel) RptTextMain else RptTextSub
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text     = "Today's Sales",
                fontSize = 14.sp,
                color    = RptTextSub,
                fontWeight = FontWeight.Medium
            )
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text       = "₱${String.format(Locale.US, "%,.2f", totalSales)}",
                    fontSize   = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color      = RptTextMain,
                    modifier   = Modifier.weight(1f)
                )
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
                    Text(
                        text       = "${String.format(Locale.US, "%.1f", abs(pctChange))}%",
                        color      = if (isUp) RptGreen else RptRed,
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                text     = "vs yesterday (₱${String.format(Locale.US, "%,.2f", previousSales)})",
                fontSize = 12.sp,
                color    = RptTextSub
            )
        }

        // ── Sales Breakdown ───────────────────────────────────────────────────
        SalesRptCard {
            Text(
                "Sales Breakdown (${period})",
                fontWeight = FontWeight.Bold,
                fontSize   = 15.sp,
                color      = RptTextMain
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("Product", modifier = Modifier.weight(1f), fontSize = 12.sp, color = RptTextSub, fontWeight = FontWeight.Bold)
                Text("Qty",     modifier = Modifier.width(40.dp), fontSize = 12.sp, color = RptTextSub, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text("Total",   modifier = Modifier.width(90.dp), fontSize = 12.sp, color = RptTextSub, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFEEEEEE))
            
            if (salesBreakdown.isEmpty()) {
                Text("No sales data found.", fontSize = 13.sp, color = RptTextSub, modifier = Modifier.padding(vertical = 8.dp))
            } else {
                salesBreakdown.forEach { (name, qty, total) ->
                    Row(
                        modifier          = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = RptTextMain, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("12:04 PM • Staff 1", fontSize = 11.sp, color = RptTextSub) // Mock metadata
                        }
                        Text("$qty", modifier = Modifier.width(40.dp), fontSize = 14.sp, color = RptTextMain, textAlign = TextAlign.Center)
                        Column(modifier = Modifier.width(90.dp), horizontalAlignment = Alignment.End) {
                            Text("₱${String.format(Locale.US, "%,.2f", total)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = RptGreen)
                            Text("Cash", fontSize = 11.sp, color = RptTextSub)
                        }
                    }
                }
            }
        }

        // ── Payment Summary ───────────────────────────────────────────────────
        SalesRptCard {
            Text(
                "Payment Summary (${period})",
                fontWeight = FontWeight.Bold,
                fontSize   = 15.sp,
                color      = RptTextMain
            )
            Spacer(modifier = Modifier.height(16.dp))
            val total = cashTotal + gcashTotal
            val cashFrac = if (total > 0) (cashTotal / total).toFloat() else 0.5f

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF2196F3))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(cashFrac)
                        .fillMaxHeight()
                        .background(RptGreen)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(RptGreen))
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text("Cash", fontSize = 12.sp, color = RptTextSub)
                        Text("₱${String.format(Locale.US, "%,.2f", cashTotal)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = RptTextMain)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF2196F3)))
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text("GCash", fontSize = 12.sp, color = RptTextSub)
                        Text("₱${String.format(Locale.US, "%,.2f", gcashTotal)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = RptTextMain)
                    }
                }
            }
        }

        // ── Top Selling Items ─────────────────────────────────────────────────
        SalesRptCard {
            Text(
                "Top Selling Items",
                fontWeight = FontWeight.Bold,
                fontSize   = 15.sp,
                color      = RptTextMain
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (topItems.isEmpty()) {
                Text("No data.", fontSize = 13.sp, color = RptTextSub)
            } else {
                topItems.forEachIndexed { idx, item ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${idx + 1}.", fontSize = 14.sp, color = RptTextSub, modifier = Modifier.width(28.dp))
                            Text(item.first, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = RptTextMain)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(RptTextMain)
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("${item.second} sold", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ── Full Sales Summary Button ─────────────────────────────────────────
        Button(
            onClick  = { navController.navigate("admin_sales_summary") },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = RptGreenDark),
            shape  = RoundedCornerShape(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Go to Full Sales Summary", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SalesRptCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier        = Modifier.fillMaxWidth(),
        color           = RptCardBg,
        shape           = RoundedCornerShape(12.dp),
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}
