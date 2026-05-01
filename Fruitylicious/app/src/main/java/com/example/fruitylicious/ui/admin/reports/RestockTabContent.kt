package com.example.fruitylicious.ui.admin.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import java.util.Locale

private data class RestockFrequencyItem(
    val name: String,
    val frequency: String,
    val avgUnits: Double
)

@Composable
fun RestockTabContent(
    branch: String,
    viewModel: RestockReportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(branch) {
        viewModel.loadReport(branch)
    }

    val restockItems = uiState.frequencyItems.map { row ->
        val label = when {
            row.restockCount >= 10 -> "Every day"
            row.restockCount >= 5 -> "Every 2 days"
            row.restockCount >= 3 -> "Every 3 days"
            else -> "Weekly"
        }

        RestockFrequencyItem(
            name = row.ingredientName,
            frequency = label,
            avgUnits = row.avgUnits
        )
    }

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

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = RptGreenDark,
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Total Added Today",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = formatQuantity(uiState.totalToday),
                        color = Color.White,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "units",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 18.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
            }
        }

        RestockRptCard {
            Text(
                text = "Restock Frequency",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = RptTextMain
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
            ) {
                Text(
                    text = "Ingredient",
                    modifier = Modifier.weight(1f),
                    fontSize = 12.sp,
                    color = RptTextSub,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Times",
                    modifier = Modifier.width(90.dp),
                    fontSize = 12.sp,
                    color = RptTextSub,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Avg Units",
                    modifier = Modifier.width(80.dp),
                    fontSize = 12.sp,
                    color = RptTextSub,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(bottom = 8.dp),
                color = Color(0xFFEEEEEE)
            )

            when {
                uiState.isLoading -> {
                    Text(
                        text = "Loading restock data...",
                        fontSize = 13.sp,
                        color = RptTextSub,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                restockItems.isEmpty() -> {
                    Text(
                        text = "No restock data found.",
                        fontSize = 13.sp,
                        color = RptTextSub,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                else -> {
                    restockItems.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.name,
                                modifier = Modifier.weight(1f),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = RptTextMain,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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
                                    text = item.frequency,
                                    fontSize = 11.sp,
                                    color = RptTextSub,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Text(
                                text = formatQuantity(item.avgUnits),
                                modifier = Modifier.width(80.dp),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = RptGreenDark,
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun RestockRptCard(
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
