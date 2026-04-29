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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fruitylicious.data.local.entity.AppDatabase

// ── Brand colors ──────────────────────────────────────────────────────────────
private val RptGreen    = Color(0xFF2C8C44)
private val RptCardBg   = Color.White
private val RptRed      = Color(0xFFE53935)
private val RptTextMain = Color(0xFF1A1A1A)
private val RptTextSub  = Color(0xFF757575)
private val RptLowStock = Color(0xFFE53935)
private val RptOkStock  = Color(0xFF2C8C44)

// ── Data class for UI ─────────────────────────────────────────────────────────
private data class StockItem(
    val name: String,
    val category: String,
    val current: Int,
    val unit: String,
    val minStock: Int
)

// ══════════════════════════════════════════════════════════════════════════════
// TAB 4 — INVENTORY
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun InventoryTabContent(branch: String) {
    val context      = LocalContext.current
    val db           = remember { AppDatabase.getDatabase(context) }
    val inventoryDao = db.inventoryDao()

    var totalStock    by remember { mutableIntStateOf(0) }
    var inStockCount  by remember { mutableIntStateOf(0) }
    var lowStockCount by remember { mutableIntStateOf(0) }
    var stockItems    by remember { mutableStateOf(listOf<StockItem>()) }

    LaunchedEffect(branch) {
        val branchId = branch.takeIf { it != "All" }
        val items    = inventoryDao.getInventoryReport(branchId)
            .map { reportItem ->
                StockItem(
                    name     = reportItem.ingredientName,
                    category = if (reportItem.isPackaging) "Packaging" else "Raw Ingredient",
                    current  = reportItem.currentStock.toInt(),
                    unit     = reportItem.unitType,
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
        // ── Summary metrics row ───────────────────────────────────────────────
        Surface(
            modifier        = Modifier.fillMaxWidth(),
            color           = RptCardBg,
            shape           = RoundedCornerShape(12.dp),
            shadowElevation = 2.dp
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth().padding(16.dp),
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

        // ── Stock details list ────────────────────────────────────────────────
        InventoryRptCard {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    "Stock Details",
                    fontWeight = FontWeight.Bold,
                    fontSize   = 14.sp,
                    color      = RptTextMain
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(RptTextMain)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        "$totalStock total",
                        fontSize   = 11.sp,
                        color      = Color.White,
                        fontWeight = FontWeight.Bold
                    )
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
                            Text(item.name,     fontSize = 14.sp, fontWeight = FontWeight.Bold, color = RptTextMain)
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
private fun InventoryRptCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier        = Modifier.fillMaxWidth(),
        color           = RptCardBg,
        shape           = RoundedCornerShape(12.dp),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}