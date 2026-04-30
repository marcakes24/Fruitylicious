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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fruitylicious.data.local.entity.AppDatabase

// Colors are defined in ReportsDashboardScreen.kt
private val RptLowStock = Color(0xFFE53935)
private val RptOkStock  = Color(0xFF2C8C44)

private data class StockItem(
    val name: String,
    val category: String,
    val current: Int,
    val unit: String,
    val minStock: Int
)

@Composable
fun InventoryTabContent(branch: String) {
    val context      = LocalContext.current
    val db           = remember { AppDatabase.getDatabase(context) }
    val inventoryDao = db.inventoryDao()

    var totalUnits    by remember { mutableIntStateOf(0) }
    var inStockCount  by remember { mutableIntStateOf(0) }
    var lowStockCount by remember { mutableIntStateOf(0) }
    var stockItems    by remember { mutableStateOf(listOf<StockItem>()) }

    LaunchedEffect(branch) {
        val branchId = branch.takeIf { it != "All" }
        val items    = inventoryDao.getInventoryReport(branchId)
            .map { reportItem ->
                // Use a mock threshold logic since it's not in the DB schema yet
                val threshold = if (reportItem.isPackaging) 5 else 10
                StockItem(
                    name     = reportItem.ingredientName,
                    category = if (reportItem.isPackaging) "Packaging & Mix-ins" else "Fruits",
                    current  = reportItem.currentStock.toInt(),
                    unit     = reportItem.unitType,
                    minStock = threshold
                )
            }

        stockItems    = items
        totalUnits    = items.sumOf { it.current }
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
        // ── Summary Metrics Row ───────────────────────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(modifier = Modifier.weight(1f), label = "Total Stock", value = "$totalUnits", valueColor = RptTextMain)
            MetricCard(modifier = Modifier.weight(1f), label = "In Stock", value = "$inStockCount", valueColor = RptGreen, bgColor = Color(0xFFE8F5E9))
            MetricCard(modifier = Modifier.weight(1f), label = "Low Stock", value = "$lowStockCount", valueColor = RptRed, bgColor = Color(0xFFFFEBEE))
        }

        // ── Stock Details List ────────────────────────────────────────────────
        InventoryRptCard {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    "Stock Details",
                    fontWeight = FontWeight.Bold,
                    fontSize   = 15.sp,
                    color      = RptTextMain
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(RptTextMain)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        "${stockItems.size} total",
                        fontSize   = 11.sp,
                        color      = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (stockItems.isEmpty()) {
                Text("No inventory data.", fontSize = 13.sp, color = RptTextSub, modifier = Modifier.padding(vertical = 8.dp))
            } else {
                stockItems.forEach { item ->
                    val isLow = item.current <= item.minStock
                    Row(
                        modifier              = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.name,     fontSize = 15.sp, fontWeight = FontWeight.Bold, color = RptTextMain)
                            Text(item.category, fontSize = 11.sp, color = RptTextSub)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text       = "${item.current} ${item.unit}",
                                fontSize   = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color      = if (isLow) RptLowStock else RptOkStock
                            )
                            Text("Min ${item.minStock}", fontSize = 10.sp, color = RptTextSub)
                        }
                    }
                    HorizontalDivider(color = Color(0xFFF5F5F5))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    valueColor: Color,
    bgColor: Color = RptCardBg
) {
    Surface(
        modifier        = modifier,
        color           = bgColor,
        shape           = RoundedCornerShape(12.dp),
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, fontSize = 11.sp, color = RptTextSub, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = valueColor)
        }
    }
}

@Composable
private fun InventoryRptCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier        = Modifier.fillMaxWidth(),
        color           = RptCardBg,
        shape           = RoundedCornerShape(12.dp),
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}
