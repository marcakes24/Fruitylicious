package com.example.fruitylicious

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fruitylicious.data.local.entity.AppDatabase
import java.util.*

// ── Brand colors ──────────────────────────────────────────────────────────────
private val RptCardBg   = Color.White
private val RptRed      = Color(0xFFE53935)
private val RptTextMain = Color(0xFF1A1A1A)
private val RptTextSub  = Color(0xFF757575)

// ══════════════════════════════════════════════════════════════════════════════
// TAB 2 — WASTE
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun WasteTabContent(branch: String) {
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
        cal.set(Calendar.SECOND, 0);      cal.set(Calendar.MILLISECOND, 0)
        val weekStart = cal.timeInMillis
        val weekEnd   = System.currentTimeMillis()

        totalWaste  = wasteDao.getTotalWasteCount(branchId, weekStart, weekEnd) ?: 0
        reasonData  = wasteDao.getWasteByReason(branchId, weekStart, weekEnd)
            .map { it.reason to it.count }
        wasteByItem = wasteDao.getWasteByItem(branchId, weekStart, weekEnd)
            .map { it.itemName to it.count }

        if (wasteByItem.isNotEmpty()) {
            val top       = wasteByItem.maxByOrNull { it.second }!!
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
        // ── Summary row ───────────────────────────────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Total waste card
            Surface(
                modifier        = Modifier.weight(1f),
                color           = RptCardBg,
                shape           = RoundedCornerShape(12.dp),
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Total Waste (This Week)", fontSize = 11.sp, color = RptTextSub)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("$totalWaste", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = RptRed)
                    Text("pcs", fontSize = 11.sp, color = RptTextSub)
                }
            }
            // Most wasted card
            Surface(
                modifier        = Modifier.weight(1f),
                color           = RptCardBg,
                shape           = RoundedCornerShape(12.dp),
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Most Wasted", fontSize = 11.sp, color = RptTextSub)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        mostWasted,
                        fontSize   = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color      = RptTextMain,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis
                    )
                    Text("$mostWastedQty pcs", fontSize = 11.sp, color = RptTextSub)
                }
            }
        }

        // ── Primary reasons bar chart ─────────────────────────────────────────
        WasteRptCard {
            Text("Primary Reasons", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = RptTextMain)
            Spacer(modifier = Modifier.height(12.dp))
            if (reasonData.isEmpty()) {
                Text("No waste records this week", fontSize = 13.sp, color = RptTextSub)
            } else {
                reasonData.forEach { data ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier          = Modifier.padding(vertical = 5.dp)
                    ) {
                        Text(
                            data.first,
                            modifier  = Modifier.width(120.dp),
                            fontSize  = 12.sp,
                            color     = RptTextSub,
                            maxLines  = 1,
                            overflow  = TextOverflow.Ellipsis
                        )
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

        // ── Waste by items list ───────────────────────────────────────────────
        WasteRptCard {
            Text(
                "Waste by Items (This Week)",
                fontWeight = FontWeight.Bold,
                fontSize   = 14.sp,
                color      = RptTextMain
            )
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
                        Text(
                            item.first,
                            fontSize   = 14.sp,
                            color      = RptTextMain,
                            fontWeight = FontWeight.Medium
                        )
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

// ── Shared card wrapper ───────────────────────────────────────────────────────
@Composable
private fun WasteRptCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier        = Modifier.fillMaxWidth(),
        color           = RptCardBg,
        shape           = RoundedCornerShape(12.dp),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}