package com.example.fruitylicious.ui.admin.reports

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fruitylicious.data.local.entity.AppDatabase
import java.util.*

// ── Brand colors ──────────────────────────────────────────────────────────────
private val RptGreenDark = Color(0xFF1B5E20)
private val RptCardBg    = Color.White
private val RptTextMain  = Color(0xFF1A1A1A)
private val RptTextSub   = Color(0xFF757575)

// ── Data class for UI ─────────────────────────────────────────────────────────
private data class RestockFrequencyItem(
    val name: String,
    val frequency: String,
    val avgUnits: Int
)

// ══════════════════════════════════════════════════════════════════════════════
// TAB 3 — RESTOCK
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun RestockTabContent(branch: String) {
    val context    = LocalContext.current
    val db         = remember { AppDatabase.getDatabase(context) }
    val restockDao = db.restockLogDao()

    var totalToday   by remember { mutableIntStateOf(0) }
    var restockItems by remember { mutableStateOf(listOf<RestockFrequencyItem>()) }

    LaunchedEffect(branch) {
        val branchId   = branch.takeIf { it != "All" }
        val todayStart = run {
            val c = Calendar.getInstance()
            c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0)
            c.set(Calendar.SECOND, 0);      c.set(Calendar.MILLISECOND, 0)
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
        // ── Total Added Today hero card ────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(RptGreenDark)
                .padding(24.dp)
        ) {
            Column {
                Text(
                    "Total Added Today",
                    color    = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text       = "$totalToday",
                        color      = Color.White,
                        fontSize   = 40.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "units",
                        color    = Color.White.copy(alpha = 0.8f),
                        fontSize = 16.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
            }
        }

        // ── Restock frequency table ───────────────────────────────────────────
        RestockRptCard {
            Text(
                "Restock Frequency",
                fontWeight = FontWeight.Bold,
                fontSize   = 14.sp,
                color      = RptTextMain
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Header row
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                Text("Product",   modifier = Modifier.weight(1f),   fontSize = 11.sp, color = RptTextSub, fontWeight = FontWeight.Bold)
                Text("Times",     modifier = Modifier.width(70.dp), fontSize = 11.sp, color = RptTextSub, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text("Avg Units", modifier = Modifier.width(70.dp), fontSize = 11.sp, color = RptTextSub, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
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
                        Text(
                            item.name,
                            modifier   = Modifier.weight(1f),
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color      = RptTextMain,
                            maxLines   = 1,
                            overflow   = TextOverflow.Ellipsis
                        )
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
                            text       = "${item.avgUnits}",
                            modifier   = Modifier.width(70.dp),
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color      = RptTextMain,
                            textAlign  = TextAlign.End
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

// ── Shared card wrapper ───────────────────────────────────────────────────────
@Composable
private fun RestockRptCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier        = Modifier.fillMaxWidth(),
        color           = RptCardBg,
        shape           = RoundedCornerShape(12.dp),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}