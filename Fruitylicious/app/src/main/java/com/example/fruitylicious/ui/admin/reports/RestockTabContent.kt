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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fruitylicious.data.local.dao.RestockIngredientRow
import androidx.hilt.navigation.compose.hiltViewModel
import java.util.Locale

private data class RestockFrequencyItem(
    val name: String,
    val frequency: String,
    val avgUnits: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestockTabContent(
    branchId: Int?,
    viewModel: RestockReportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(branchId) {
        viewModel.loadReport(branchId)
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.selectedDate
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        viewModel.setSelectedDate(it, branchId)
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    val restockItems = uiState.frequencyItems.map { row ->
        val label = when (uiState.period) {
            "daily" -> "${row.restockCount} time(s)"
            "weekly" -> {
                when {
                    row.restockCount >= 7 -> "Daily"
                    row.restockCount >= 4 -> "Every 2 days"
                    row.restockCount >= 2 -> "Twice a week"
                    else -> "Once a week"
                }
            }
            "monthly" -> {
                when {
                    row.restockCount >= 28 -> "Daily"
                    row.restockCount >= 12 -> "3 times a week"
                    row.restockCount >= 8 -> "Twice a week"
                    row.restockCount >= 4 -> "Weekly"
                    else -> "Rarely"
                }
            }
            else -> "${row.restockCount} times"
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
        ReportPeriodSelector(
            period = uiState.period,
            rangeText = uiState.rangeText,
            onPeriodSelected = { viewModel.setPeriod(it, branchId) },
            onNavigate = { viewModel.navigatePeriod(it, branchId) },
            onDateClick = { showDatePicker = true },
            activeColor = RptGreen
        )

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
                        text = "Total Entries",
                        fontSize = 12.sp,
                        color = RptTextSub,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.totalEntries.toString(),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = RptGreenDark
                    )
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
                        text = "Most Restocked",
                        fontSize = 12.sp,
                        color = RptTextSub,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.mostRestockedIngredient,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = RptTextMain,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatQuantity(uiState.mostRestockedQty) + " " + uiState.mostRestockedUnit,
                        fontSize = 12.sp,
                        color = RptTextSub
                    )
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = RptGreenDark,
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Total Quantity Added (${uiState.period.replaceFirstChar { it.uppercase() }})",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (uiState.totalsByUnit.isEmpty()) {
                    Text(
                        text = "0",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        uiState.totalsByUnit.forEach { unitTotal ->
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = formatQuantity(unitTotal.totalQuantity),
                                    color = Color.White,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = unitTotal.unitType,
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = RptCardBg,
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Staff Restock Activity",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = RptTextMain
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (uiState.staffActivity.isEmpty()) {
                    Text("No staff activity data.", fontSize = 13.sp, color = RptTextSub)
                } else {
                    uiState.staffActivity.forEach { data ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = data.staffName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = RptTextMain
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(RptGreen.copy(alpha = 0.1f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "${data.count} restocks",
                                    color = RptGreenDark,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        RestockRptCard {
            Text(
                text = "Top Restocked Ingredients",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = RptTextMain
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (uiState.availableUnits.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = uiState.selectedUnit == null,
                            onClick = { viewModel.setUnitFilter(null) },
                            label = { Text("All", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RptGreen,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                    items(uiState.availableUnits) { unit ->
                        FilterChip(
                            selected = uiState.selectedUnit == unit,
                            onClick = { viewModel.setUnitFilter(unit) },
                            label = { Text(unit, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RptGreen,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            val filteredIngredients = if (uiState.selectedUnit == null) {
                uiState.topIngredients
            } else {
                uiState.topIngredients.filter { it.unitType == uiState.selectedUnit }
            }

            if (filteredIngredients.isEmpty()) {
                Text("No data.", fontSize = 13.sp, color = RptTextSub)
            } else {
                TopIngredientsChart(items = filteredIngredients.take(5))
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

                    if (uiState.hasMore) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (uiState.isLoadingMore) {
                                androidx.compose.material3.CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = RptGreenDark
                                )
                            } else {
                                androidx.compose.material3.TextButton(onClick = { viewModel.loadMore(branchId) }) {
                                    Text("Load More", color = RptGreenDark, fontWeight = FontWeight.Bold)
                                }
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

@Composable
private fun TopIngredientsChart(items: List<RestockIngredientRow>) {
    val maxQty = items.maxOfOrNull { it.totalQuantity }?.toFloat() ?: 1f
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items.forEachIndexed { index, data ->
            val fraction = if (maxQty > 0) data.totalQuantity.toFloat() / maxQty else 0f
            val barColor = when (index) {
                0 -> Color(0xFF2E7D32)
                1 -> Color(0xFF43A047)
                2 -> Color(0xFF66BB6A)
                3 -> Color(0xFF81C784)
                else -> Color(0xFFA5D6A7)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = data.ingredientName,
                    fontSize = 14.sp,
                    color = RptTextSub,
                    modifier = Modifier.width(130.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction.coerceAtLeast(0.01f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .background(barColor)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = formatQuantity(data.totalQuantity) + " " + data.unitType,
                    fontSize = 13.sp,
                    color = RptTextMain,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(60.dp),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

private fun formatQuantity(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        String.format(Locale.US, "%.2f", value)
    }
}
