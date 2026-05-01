package com.example.fruitylicious.ui.admin.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import java.util.Locale

@Composable
fun WasteTabContent(
    branch: String,
    viewModel: WasteReportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(branch) {
        viewModel.loadReport(branch)
    }

    val maxReason = uiState.reasonData.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!uiState.error.isNullOrBlank()) {
            Text(
                text = uiState.error ?: "",
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.weight(1f),
                color = RptCardBg,
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Total Waste (This Week)",
                        fontSize = 12.sp,
                        color = RptTextSub,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = formatQuantity(uiState.totalWaste),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = RptRed
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        Text(
                            text = "units",
                            fontSize = 14.sp,
                            color = RptTextSub,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier.weight(1f),
                color = RptCardBg,
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Most Wasted",
                        fontSize = 12.sp,
                        color = RptTextSub,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = uiState.mostWasted,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = RptTextMain,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "${formatQuantity(uiState.mostWastedQty)} units",
                        fontSize = 14.sp,
                        color = RptTextSub
                    )
                }
            }
        }

        WasteRptCard {
            Text(
                text = "Primary Reasons",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = RptTextMain
            )

            Spacer(modifier = Modifier.height(16.dp))

            when {
                uiState.isLoading -> {
                    Text(
                        text = "Loading waste data...",
                        fontSize = 13.sp,
                        color = RptTextSub
                    )
                }

                uiState.reasonData.isEmpty() -> {
                    Text(
                        text = "No data.",
                        fontSize = 13.sp,
                        color = RptTextSub
                    )
                }

                else -> {
                    uiState.reasonData.forEach { data ->
                        Column(modifier = Modifier.padding(vertical = 6.dp)) {
                            Text(
                                text = data.reason.ifBlank { "Unspecified" },
                                fontSize = 13.sp,
                                color = RptTextSub,
                                fontWeight = FontWeight.Medium
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(12.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFFFEBEE))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(data.count.toFloat() / maxReason)
                                            .fillMaxHeight()
                                            .background(RptRed)
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = data.count.toString(),
                                    fontSize = 12.sp,
                                    color = RptTextMain,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        WasteRptCard {
            Text(
                text = "Waste by Items (This Week)",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = RptTextMain
            )

            Spacer(modifier = Modifier.height(12.dp))

            when {
                uiState.isLoading -> {
                    Text(
                        text = "Loading waste items...",
                        fontSize = 13.sp,
                        color = RptTextSub
                    )
                }

                uiState.wasteByItem.isEmpty() -> {
                    Text(
                        text = "No data.",
                        fontSize = 13.sp,
                        color = RptTextSub
                    )
                }

                else -> {
                    uiState.wasteByItem.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.ingredientName,
                                fontSize = 15.sp,
                                color = RptTextMain,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(RptTextMain)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${formatQuantity(item.totalQuantity)} wasted",
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun WasteRptCard(
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