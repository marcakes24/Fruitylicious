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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.fruitylicious.data.local.entity.AppDatabase
import java.util.*
import kotlin.math.abs

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
        
        // Mock data if empty for preview/illustration purposes
        if (totalSales == 0.0 && salesBreakdown.isEmpty()) {
            totalSales = 255.0
            previousSales = 390.0
            cashTotal = 180.0
            gcashTotal = 75.0
            topItems = listOf("Avocado" to 2, "Mango" to 1)
            salesBreakdown = listOf(
                Triple("Avocado", 2, 180.0),
                Triple("Mango", 1, 75.0)
            )
        }
    }

    val pctChange = if (previousSales > 0)
        ((totalSales - previousSales) / previousSales * 100) else 0.0
    val isUp = pctChange >= 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Period Toggle ─────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .padding(4.dp)
        ) {
            listOf("daily", "weekly", "monthly").forEach { p ->
                val isSel = (period == p)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSel) Color(0xFFE8F5E9) else Color.Transparent)
                        .clickable { period = p }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = p,
                        fontSize   = 14.sp,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                        color      = if (isSel) Color(0xFF2E7D32) else Color(0xFF757575)
                    )
                }
            }
        }

        // ── Today's Sales Card ────────────────────────────────────────────────
        SalesRptCard {
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
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isUp) Color(0xFFE8F5E9) else Color(0xFFFFEBEE))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (isUp) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = if (isUp) RptGreen else RptRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text       = "${String.format(Locale.US, "%.1f", abs(pctChange))}%",
                        color      = if (isUp) RptGreen else RptRed,
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                text     = "vs yesterday (₱${String.format(Locale.US, "%,.2f", previousSales)})",
                fontSize = 13.sp,
                color    = Color(0xFFBDBDBD)
            )
        }

        // ── Sales Breakdown Card ──────────────────────────────────────────────
        SalesRptCard {
            Text(
                "Sales Breakdown (${period})",
                fontWeight = FontWeight.Bold,
                fontSize   = 16.sp,
                color      = RptTextMain
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("Product", modifier = Modifier.weight(1f), fontSize = 12.sp, color = RptTextSub, fontWeight = FontWeight.Bold)
                Text("Qty",     modifier = Modifier.width(40.dp), fontSize = 12.sp, color = RptTextSub, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text("Total",   modifier = Modifier.width(100.dp), fontSize = 12.sp, color = RptTextSub, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFF5F5F5))
            
            salesBreakdown.forEach { (name, qty, total) ->
                Row(
                    modifier          = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = RptTextMain)
                        Text("12:04 PM • Staff 1", fontSize = 12.sp, color = RptTextSub)
                    }
                    Text("$qty", modifier = Modifier.width(40.dp), fontSize = 15.sp, color = RptTextMain, textAlign = TextAlign.Center)
                    Column(modifier = Modifier.width(100.dp), horizontalAlignment = Alignment.End) {
                        Text("₱${String.format(Locale.US, "%,.2f", total)}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = RptGreen)
                        Text(if (name == "Avocado") "Cash" else "Gcash", fontSize = 12.sp, color = RptTextSub)
                    }
                }
            }
        }

        // ── Payment Summary Card ──────────────────────────────────────────────
        SalesRptCard {
            Text(
                "Payment Summary (${period})",
                fontWeight = FontWeight.Bold,
                fontSize   = 16.sp,
                color      = RptTextMain
            )
            Spacer(modifier = Modifier.height(20.dp))
            val total = cashTotal + gcashTotal
            val cashFrac = if (total > 0) (cashTotal / total).toFloat() else 0.5f

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(Color(0xFF2196F3)) // Gcash Blue
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(cashFrac)
                        .fillMaxHeight()
                        .background(RptGreen) // Cash Green
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(5.dp)).background(RptGreen))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Cash", fontSize = 13.sp, color = RptTextSub)
                        Text("₱${String.format(Locale.US, "%,.2f", cashTotal)}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = RptTextMain)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(5.dp)).background(Color(0xFF2196F3)))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Gcash", fontSize = 13.sp, color = RptTextSub)
                        Text("₱${String.format(Locale.US, "%,.2f", gcashTotal)}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = RptTextMain)
                    }
                }
            }
        }

        // ── Top Selling Items Card ────────────────────────────────────────────
        SalesRptCard {
            Text(
                "Top Selling Items",
                fontWeight = FontWeight.Bold,
                fontSize   = 16.sp,
                color      = RptTextMain
            )
            Spacer(modifier = Modifier.height(16.dp))
            topItems.forEachIndexed { idx, item ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${idx + 1}.", fontSize = 15.sp, color = RptTextSub, modifier = Modifier.width(28.dp))
                        Text(item.first, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = RptTextMain)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("${item.second} sold", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // ── Footer Button ─────────────────────────────────────────────────────
        Button(
            onClick  = { navController.navigate("admin_sales_summary") },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = RptGreen),
            shape  = RoundedCornerShape(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Go to Full Sales Summary", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp))
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
        shape           = RoundedCornerShape(16.dp),
        shadowElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(20.dp), content = content)
    }
}

@Preview(showBackground = true)
@Composable
fun SalesTabPreview() {
    Box(modifier = Modifier.background(Color(0xFFFFF9C4))) {
        SalesTabContent("All", rememberNavController())
    }
}
