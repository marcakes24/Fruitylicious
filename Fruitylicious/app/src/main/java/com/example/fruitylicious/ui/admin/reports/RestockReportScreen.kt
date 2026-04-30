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

// Colors are defined in ReportsDashboardScreen.kt

private data class RestockFrequencyItem(
    val name: String,
    val frequency: String,
    val avgUnits: Int
)

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
            .map { 
                val label = when {
                    it.count >= 10 -> "Every day"
                    it.count >= 5  -> "Every 2 days"
                    it.count >= 3  -> "Every 3 days"
                    else           -> "Weekly"
                }
                RestockFrequencyItem(it.ingredientName, label, it.avgUnits) 
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Total Added Today Hero Card ──────────────────────────────────────
        Surface(
            modifier        = Modifier.fillMaxWidth(),
            color           = RptGreenDark,
            shape           = RoundedCornerShape(12.dp),
            shadowElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "Total Added Today",
                    color    = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text       = "$totalToday",
                        color      = Color.White,
                        fontSize   = 42.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "units",
                        color    = Color.White.copy(alpha = 0.8f),
                        fontSize = 18.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
            }
        }

        // ── Restock Frequency Table ──────────────────────────────────────────
        RestockRptCard {
            Text(
                "Restock Frequency",
                fontWeight = FontWeight.Bold,
                fontSize   = 15.sp,
                color      = RptTextMain
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Header row
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                Text("Product",   modifier = Modifier.weight(1f),   fontSize = 12.sp, color = RptTextSub, fontWeight = FontWeight.Bold)
                Text("Times",     modifier = Modifier.width(90.dp), fontSize = 12.sp, color = RptTextSub, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text("Avg Units", modifier = Modifier.width(80.dp), fontSize = 12.sp, color = RptTextSub, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
            }
            HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp), color = Color(0xFFEEEEEE))

            if (restockItems.isEmpty()) {
                Text("No restock data found.", fontSize = 13.sp, color = RptTextSub, modifier = Modifier.padding(vertical = 8.dp))
            } else {
                restockItems.forEach { item ->
                    Row(
                        modifier          = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            item.name,
                            modifier   = Modifier.weight(1f),
                            fontSize   = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color      = RptTextMain,
                            maxLines   = 1,
                            overflow   = TextOverflow.Ellipsis
                        )
                        Box(
                            modifier = Modifier
                                .width(90.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF5F5F5))
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text      = item.frequency,
                                fontSize  = 11.sp,
                                color     = RptTextSub,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Text(
                            text       = "${item.avgUnits}",
                            modifier   = Modifier.width(80.dp),
                            fontSize   = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color      = RptGreenDark,
                            textAlign  = TextAlign.End
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun RestockRptCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier        = Modifier.fillMaxWidth(),
        color           = RptCardBg,
        shape           = RoundedCornerShape(12.dp),
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}
