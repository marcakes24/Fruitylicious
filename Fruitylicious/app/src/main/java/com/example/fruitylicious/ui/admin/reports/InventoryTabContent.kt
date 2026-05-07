package com.example.fruitylicious.ui.admin.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import java.util.Locale

private val RptLowStock = Color(0xFFE53935)
private val RptOkStock = Color(0xFF2C8C44)

@Composable
fun InventoryTabContent(
    branchId: Int?,
    viewModel: InventoryReportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(branchId) {
        viewModel.loadReport(branchId)
    }

    val filteredRows = uiState.rows // Already filtered/aggregated by ViewModel

    val totalUnits = filteredRows.sumOf { it.currentStock }
    val lowStockCount = filteredRows.count {
        it.currentStock <= it.lowStockThreshold
    }
    val inStockCount = filteredRows.count {
        it.currentStock > it.lowStockThreshold
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                modifier = Modifier.weight(1f),
                label = "Total Stock",
                value = formatQuantity(totalUnits),
                valueColor = RptTextMain
            )

            MetricCard(
                modifier = Modifier.weight(1f),
                label = "In Stock",
                value = inStockCount.toString(),
                valueColor = RptGreen,
                bgColor = Color(0xFFE8F5E9)
            )

            MetricCard(
                modifier = Modifier.weight(1f),
                label = "Low Stock",
                value = lowStockCount.toString(),
                valueColor = RptRed,
                bgColor = Color(0xFFFFEBEE)
            )
        }

        InventoryRptCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Stock Details",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = RptTextMain
                )

                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(RptTextMain)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${filteredRows.size} total",
                        fontSize = 11.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when {
                uiState.isLoading -> {
                    Text(
                        text = "Loading inventory data...",
                        fontSize = 13.sp,
                        color = RptTextSub,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                filteredRows.isEmpty() -> {
                    Text(
                        text = "No inventory data.",
                        fontSize = 13.sp,
                        color = RptTextSub,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                else -> {
                    filteredRows.forEach { item ->
                        val isLow = item.currentStock <= item.lowStockThreshold

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.ingredientName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RptTextMain
                                )

                                Text(
                                    text = "${item.category} • Branch ${item.branchId}",
                                    fontSize = 11.sp,
                                    color = RptTextSub
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${formatQuantity(item.currentStock)} ${item.unitType}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isLow) RptLowStock else RptOkStock
                                )

                                Text(
                                    text = "Min ${formatQuantity(item.lowStockThreshold)}",
                                    fontSize = 10.sp,
                                    color = RptTextSub
                                )
                            }
                        }

                        HorizontalDivider(color = Color(0xFFF5F5F5))
                    }
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
        modifier = modifier,
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                fontSize = 11.sp,
                color = RptTextSub,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
        }
    }
}

@Composable
private fun InventoryRptCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = RptCardBg,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

private fun formatQuantity(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        String.format(Locale.US, "%.2f", value)
    }
}
