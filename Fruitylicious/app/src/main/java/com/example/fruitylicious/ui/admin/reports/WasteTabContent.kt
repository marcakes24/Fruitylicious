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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fruitylicious.data.local.dao.WasteReasonRow
import androidx.hilt.navigation.compose.hiltViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WasteTabContent(
    branchId: Int?,
    viewModel: WasteReportViewModel = hiltViewModel()
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
            activeColor = RptRed
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
                        color = RptRed
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
                        text = "Most Wasted",
                        fontSize = 12.sp,
                        color = RptTextSub,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = uiState.mostWasted,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = RptTextMain,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "${formatQuantity(uiState.mostWastedQty)} ${uiState.mostWastedUnit}",
                        fontSize = 14.sp,
                        color = RptTextSub
                    )
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = RptRed,
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Total Waste (${uiState.period.replaceFirstChar { it.uppercase() }})",
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
                    text = "Staff Waste Activity",
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
                                    .background(RptRed.copy(alpha = 0.1f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "${data.count} entries",
                                    color = RptRed,
                                    fontSize = 12.sp,
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
                    WasteReasonChart(reasons = uiState.reasonData)
                    
                    if (uiState.hasMore) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (uiState.isLoadingMore) {
                                androidx.compose.material3.CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = RptRed
                                )
                            } else {
                                androidx.compose.material3.TextButton(onClick = { viewModel.loadMore(branchId) }) {
                                    Text("Load More", color = RptRed, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        WasteRptCard {
            Text(
                text = "Waste Ingredients (${uiState.period.replaceFirstChar { it.uppercase() }})",
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
                                selectedContainerColor = RptRed,
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
                                selectedContainerColor = RptRed,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            val filteredItems = if (uiState.selectedUnit == null) {
                uiState.wasteByItem
            } else {
                uiState.wasteByItem.filter { it.unitType == uiState.selectedUnit }
            }.sortedByDescending { it.totalQuantity }

            if (branchId == null && filteredItems.isNotEmpty()) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("Ingredient", modifier = Modifier.weight(1f), fontSize = 12.sp, color = RptTextSub, fontWeight = FontWeight.Bold)
                    Text("Total", modifier = Modifier.width(60.dp), fontSize = 12.sp, color = RptTextSub, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.End)
                    Text("B1 %", modifier = Modifier.width(45.dp), fontSize = 12.sp, color = RptTextSub, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Text("B2 %", modifier = Modifier.width(45.dp), fontSize = 12.sp, color = RptTextSub, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFF5F5F5))
            }

            when {
                uiState.isLoading -> {
                    Text(
                        text = "Loading waste items...",
                        fontSize = 13.sp,
                        color = RptTextSub
                    )
                }

                filteredItems.isEmpty() -> {
                    Text(
                        text = "No data.",
                        fontSize = 13.sp,
                        color = RptTextSub
                    )
                }

                else -> {
                    filteredItems.forEach { item ->
                        if (branchId == null) {
                            val totalQty = (item.b1Qty + item.b2Qty).coerceAtLeast(item.totalQuantity)
                            val b1Pct = if (totalQty > 0.01) ((item.b1Qty / totalQty) * 100.0).toInt() else 0
                            val b2Pct = if (totalQty > 0.01) ((item.b2Qty / totalQty) * 100.0).toInt() else 0

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.ingredientName,
                                    fontSize = 14.sp,
                                    color = RptTextMain,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Text(
                                    text = formatQuantity(item.totalQuantity),
                                    modifier = Modifier.width(60.dp),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RptRed,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                                )

                                Text(
                                    text = "$b1Pct%",
                                    modifier = Modifier.width(45.dp),
                                    fontSize = 12.sp,
                                    color = RptTextMain,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )

                                Text(
                                    text = "$b2Pct%",
                                    modifier = Modifier.width(45.dp),
                                    fontSize = 12.sp,
                                    color = RptTextMain,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        } else {
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
                                        text = "${formatQuantity(item.totalQuantity)} ${item.unitType}",
                                        fontSize = 12.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
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
                                    color = RptRed
                                )
                            } else {
                                androidx.compose.material3.TextButton(onClick = { viewModel.loadMore(branchId) }) {
                                    Text("Load More", color = RptRed, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        RecommendedActionsSection()

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

@Composable
private fun WasteReasonChart(reasons: List<WasteReasonRow>) {
    val dataToShow = reasons // Use all provided reasons
    val maxCount = dataToShow.maxOfOrNull { it.count }?.toFloat() ?: 1f
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        dataToShow.forEachIndexed { index, data ->
            val fraction = if (maxCount > 0) data.count.toFloat() / maxCount else 0f
            
            // Design matches the provided image: Shades of Red
            val barColor = when (index) {
                0 -> Color(0xFFE53935)
                1 -> Color(0xFFEF5350)
                2 -> Color(0xFFE57373)
                3 -> Color(0xFFEF9A9A)
                else -> Color(0xFFFFCDD2)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = data.reason.ifBlank { "Unspecified" },
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
                    text = data.count.toString(),
                    fontSize = 13.sp,
                    color = RptTextMain,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(30.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )
            }
        }
    }
}

@Composable
private fun RecommendedActionsSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(bottom = 8.dp)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF388E3C)) // Green header background
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Recommended Actions",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            RecommendedActionItem(
                title = "Organic Composting",
                description = "Convert into nutrient-rich fertilizer for local community gardens."
            )

            HorizontalDivider(color = Color(0xFFF5F5F5), thickness = 1.dp)

            RecommendedActionItem(
                title = "Repurpose for Production",
                description = "Transform overripe into secondary products like purees, syrups, smoothies, juices, jams, or baked goods."
            )
        }
    }
}

@Composable
private fun RecommendedActionItem(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2C3E50) // Dark Slate color for title
        )
        Text(
            text = description,
            fontSize = 14.sp,
            color = Color(0xFF636E72), // Muted color for description
            lineHeight = 20.sp
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