package com.example.fruitylicious.ui.admin.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fruitylicious.data.local.dao.TopAddonRow
import com.example.fruitylicious.data.local.dao.TopComboRow
import com.example.fruitylicious.data.local.dao.TopSellingItemRow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesTimeSeriesChart(
    seriesList: List<SalesSeries>,
    selectedPointIndex: Int?,
    selectedSeriesIndex: Int?,
    onSelectionChanged: (Int?, Int?) -> Unit
) {
    if (seriesList.isEmpty() || seriesList.all { it.points.isEmpty() }) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No chart data available", color = RptTextSub)
        }
        return
    }

    val textMeasurer = rememberTextMeasurer()

    val allPoints = seriesList.flatMap { it.points }
    val maxSales = allPoints.maxOfOrNull { it.sales }?.toFloat()?.coerceAtLeast(100f) ?: 100f
    val chartHeight = 160.dp
    
    // Grid lines count
    val gridLines = 4

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight)
                .pointerInput(seriesList) {
                    val firstSeries = seriesList.firstOrNull() ?: return@pointerInput
                    val pointsCount = firstSeries.points.size
                    if (pointsCount < 2) return@pointerInput
                    val step = size.width.toFloat() / (pointsCount - 1)

                    awaitEachGesture {
                        val down = awaitFirstDown()
                        val updateSelection = { position: Offset ->
                            val index = (position.x / step).roundToInt().coerceIn(0, pointsCount - 1)
                            
                            // If multiple series, find the one closest to the touch point Y
                            // For now, we'll use the one with highest sales as a proxy if it's a tap
                            // or just iterate and find closest y
                            val chartY = position.y
                            val seriesIdx = seriesList.indices.minByOrNull { sIdx ->
                                val p = seriesList[sIdx].points.getOrNull(index) ?: return@minByOrNull Float.MAX_VALUE
                                val y = size.height - (p.sales.toFloat() / maxSales * size.height)
                                abs(y - chartY)
                            } ?: 0
                            
                            onSelectionChanged(index, seriesIdx)
                        }
                        updateSelection(down.position)
                        drag(down.id) { change ->
                            updateSelection(change.position)
                            change.consume()
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val pointsCount = seriesList.firstOrNull()?.points?.size ?: 0
                if (pointsCount < 2) return@Canvas
                
                val step = width / (pointsCount - 1)

                // Draw horizontal grid lines
                for (i in 0..gridLines) {
                    val y = height - (i * height / gridLines)
                    drawLine(
                        color = Color(0xFFEEEEEE),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Draw series
                seriesList.forEachIndexed { sIdx, series ->
                    val seriesColor = when (sIdx) {
                        0 -> RptRed
                        1 -> RptBlue
                        else -> RptGreen
                    }
                    
                    val path = Path()
                    val fillPath = Path()
                    
                    series.points.forEachIndexed { pIdx, point ->
                        val x = pIdx * step
                        val y = height - (point.sales.toFloat() / maxSales * height)
                        
                        if (pIdx == 0) {
                            path.moveTo(x, y)
                            fillPath.moveTo(x, height)
                            fillPath.lineTo(x, y)
                        } else {
                            val prevX = (pIdx - 1) * step
                            val prevPoint = series.points[pIdx - 1]
                            val prevY = height - (prevPoint.sales.toFloat() / maxSales * height)
                            
                            // Cubic Bezier for smooth curves
                            path.cubicTo(
                                prevX + step / 2f, prevY,
                                x - step / 2f, y,
                                x, y
                            )
                            fillPath.cubicTo(
                                prevX + step / 2f, prevY,
                                x - step / 2f, y,
                                x, y
                            )
                        }
                        
                        if (pIdx == series.points.lastIndex) {
                            fillPath.lineTo(x, height)
                            fillPath.close()
                        }
                    }

                    // Draw area fill
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(seriesColor.copy(alpha = 0.2f), Color.Transparent),
                            startY = 0f,
                            endY = height
                        )
                    )

                    // Draw line
                    drawPath(
                        path = path,
                        color = seriesColor,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw interactive point and tooltip
                    if (selectedSeriesIndex == sIdx && selectedPointIndex != null && 
                        selectedPointIndex in series.points.indices) {
                        val pIdx = selectedPointIndex!!
                        val point = series.points[pIdx]
                        val x = pIdx * step
                        val y = height - (point.sales.toFloat() / maxSales * height)

                        // Vertical guide line
                        drawLine(
                            color = RptTextSub.copy(alpha = 0.5f),
                            start = Offset(x, y),
                            end = Offset(x, height),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )

                        // Point circle
                        drawCircle(
                            color = seriesColor,
                            radius = 6.dp.toPx(),
                            center = Offset(x, y)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 3.dp.toPx(),
                            center = Offset(x, y)
                        )

                        // Tooltip logic
                        val tooltipText = "${point.label}\nTransactions: ${point.transactionCount}\n₱${String.format(Locale.US, "%,.2f", point.sales)}"
                        val textResult = textMeasurer.measure(
                            text = tooltipText,
                            style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RptTextMain)
                        )
                        
                        val tooltipWidth = textResult.size.width + 16.dp.toPx()
                        val tooltipHeight = textResult.size.height + 12.dp.toPx()
                        
                        var tooltipX = x - tooltipWidth / 2f
                        if (tooltipX < 0) tooltipX = 8.dp.toPx()
                        if (tooltipX + tooltipWidth > width) tooltipX = width - tooltipWidth - 8.dp.toPx()
                        
                        var tooltipY = y - tooltipHeight - 12.dp.toPx()
                        if (tooltipY < 0) tooltipY = y + 12.dp.toPx() // Show below if no room above
                        
                        drawRoundRect(
                            color = Color.White,
                            topLeft = Offset(tooltipX, tooltipY),
                            size = Size(tooltipWidth, tooltipHeight),
                            cornerRadius = CornerRadius(8.dp.toPx()),
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color.Black.copy(alpha = 0.1f),
                                offset = Offset(0f, 4f),
                                blurRadius = 8f
                            )
                        )
                        
                        // Draw Tooltip text
                        drawText(
                            textLayoutResult = textResult,
                            topLeft = Offset(tooltipX + 8.dp.toPx(), tooltipY + 6.dp.toPx())
                        )
                    }
                }
            }
        }

        // Labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val points = seriesList.first().points
            val labelCount = if (points.size > 8) 6 else points.size
            val step = if (points.size > 1) (points.size - 1) / (labelCount - 1).coerceAtLeast(1) else 1
            
            for (i in 0 until labelCount) {
                val idx = (i * step).coerceAtMost(points.size - 1)
                Text(
                    text = points[idx].label,
                    fontSize = 10.sp,
                    color = RptTextSub,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }
        
        // Legend if multiple series
        if (seriesList.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                seriesList.forEachIndexed { index, series ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { onSelectionChanged(selectedPointIndex, index) }
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    when (index) {
                                        0 -> RptRed
                                        1 -> RptBlue
                                        else -> RptGreen
                                    }
                                )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = series.name,
                            fontSize = 11.sp,
                            color = if (selectedSeriesIndex == index) RptTextMain else RptTextSub,
                            fontWeight = if (selectedSeriesIndex == index) FontWeight.Bold else FontWeight.Medium
                        )
                        if (index < seriesList.lastIndex) Spacer(modifier = Modifier.width(16.dp))
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawRoundRect(
    color: Color,
    topLeft: Offset,
    size: Size,
    cornerRadius: CornerRadius,
    shadow: androidx.compose.ui.graphics.Shadow? = null
) {
    if (shadow != null) {
        val paint = android.graphics.Paint().apply {
            this.color = android.graphics.Color.WHITE
            setShadowLayer(shadow.blurRadius, shadow.offset.x, shadow.offset.y, android.graphics.Color.argb((shadow.color.alpha * 255).toInt(), 0, 0, 0))
        }
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawRoundRect(
                topLeft.x,
                topLeft.y,
                topLeft.x + size.width,
                topLeft.y + size.height,
                cornerRadius.x,
                cornerRadius.y,
                paint
            )
        }
    } else {
        drawRoundRect(
            color = color,
            topLeft = topLeft,
            size = size,
            cornerRadius = cornerRadius
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesTabContent(
    branchId: Int?,
    navController: NavController,
    viewModel: SalesReportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var showDatePicker by remember { mutableStateOf(false) }
    
    var selectedPointIndex by remember(uiState.timeSeriesData) { mutableStateOf<Int?>(null) }
    var selectedSeriesIndex by remember(uiState.timeSeriesData) { mutableStateOf<Int?>(null) }

    LaunchedEffect(branchId) {
        viewModel.loadReport(branchId)
    }

    val pctChange = if (uiState.previousSales > 0.0) {
        ((uiState.totalSales - uiState.previousSales) / uiState.previousSales) * 100.0
    } else {
        0.0
    }

    val isUp = pctChange >= 0.0

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
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF5F5F5))
                    .padding(4.dp)
            ) {
                listOf("daily", "weekly", "monthly").forEach { period ->
                    val isSelected = uiState.period == period

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Color.White else Color.Transparent)
                            .clickable {
                                viewModel.setPeriod(period, branchId)
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = period.replaceFirstChar { it.uppercase() },
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) RptGreen else RptTextSub
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { viewModel.navigatePeriod(-1, branchId) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous",
                        tint = RptGreen
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showDatePicker = true }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = RptGreen
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = uiState.rangeText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = RptTextMain
                    )
                }

                IconButton(onClick = { viewModel.navigatePeriod(1, branchId) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next",
                        tint = RptGreen
                    )
                }
            }
        }

        uiState.error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp
            )
        }

        SalesRptCard {
            Text(
                text = "${uiState.period.replaceFirstChar { it.uppercase() }} Sales",
                fontSize = 14.sp,
                color = RptTextSub,
                fontWeight = FontWeight.Medium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "₱${String.format(Locale.US, "%,.2f", uiState.totalSales)}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = RptTextMain,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isUp) Color(0xFFE8F5E9) else Color(0xFFFFEBEE))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (isUp) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = if (isUp) RptGreen else RptRed,
                        modifier = Modifier.size(16.dp)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = "${String.format(Locale.US, "%.1f", abs(pctChange))}%",
                        color = if (isUp) RptGreen else RptRed,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = "vs previous period (₱${String.format(Locale.US, "%,.2f", uiState.previousSales)})",
                fontSize = 13.sp,
                color = Color(0xFFBDBDBD)
            )
        }

        SalesRptCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = when (uiState.period) {
                            "daily" -> "This Day"
                            "weekly" -> "This Week"
                            "monthly" -> "This Month"
                            else -> "This Day"
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = RptTextMain
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = uiState.rangeText,
                        fontSize = 13.sp,
                        color = RptTextSub
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Transactions", fontSize = 14.sp, color = RptTextSub)
                    Text(
                        text = uiState.transactionCount.toString(),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = RptTextMain
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Total Sales", fontSize = 14.sp, color = RptTextSub)
                    Text(
                        text = "₱${String.format(Locale.US, "%,.2f", uiState.totalSales)}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = RptRed,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            SalesTimeSeriesChart(
                seriesList = uiState.timeSeriesData,
                selectedPointIndex = selectedPointIndex,
                selectedSeriesIndex = selectedSeriesIndex,
                onSelectionChanged = { pIdx, sIdx ->
                    selectedPointIndex = pIdx
                    selectedSeriesIndex = sIdx
                }
            )
        }

        val activeSeriesIndex = when (branchId) {
            1 -> 0
            2 -> 1
            else -> 2
        }
        val activeColor = when (branchId) {
            1 -> RptRed
            2 -> RptBlue
            else -> RptGreen
        }

        SalesRptCard {
            Text(
                text = "Sales Breakdown (${uiState.period})",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = RptTextMain
            )

            Spacer(modifier = Modifier.height(16.dp))

            val isAllView = branchId == null && activeSeriesIndex == 2

            Row(modifier = Modifier.fillMaxWidth()) {
                Text("Product", modifier = Modifier.weight(1f), fontSize = 12.sp, color = RptTextSub, fontWeight = FontWeight.Bold)
                Text("Qty", modifier = Modifier.width(35.dp), fontSize = 12.sp, color = RptTextSub, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text("Total", modifier = Modifier.width(70.dp), fontSize = 12.sp, color = RptTextSub, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                if (isAllView) {
                    Text("B1 %", modifier = Modifier.width(42.dp), fontSize = 12.sp, color = RptTextSub, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text("B2 %", modifier = Modifier.width(42.dp), fontSize = 12.sp, color = RptTextSub, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFF5F5F5))

            if (uiState.salesBreakdown.isEmpty()) {
                Text("No sales found", color = RptTextSub, fontSize = 13.sp)
            } else {
                uiState.salesBreakdown.forEach { row ->
                    val displayQty = when (activeSeriesIndex) {
                        0 -> row.b1Qty
                        1 -> row.b2Qty
                        else -> row.qty
                    }
                    
                    val displayAmount = when (activeSeriesIndex) {
                        0 -> row.b1Amount
                        1 -> row.b2Amount
                        else -> row.totalAmount
                    }
                    
                    if (displayQty > 0 || activeSeriesIndex == 2) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = row.productName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = activeColor,
                                modifier = Modifier.weight(1f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Text(
                                text = displayQty.toString(),
                                modifier = Modifier.width(35.dp),
                                fontSize = 13.sp,
                                color = RptTextMain,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = "₱${String.format(Locale.US, "%,.0f", displayAmount)}",
                                modifier = Modifier.width(70.dp),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF9A825),
                                textAlign = TextAlign.End
                            )

                            if (isAllView) {
                                // Calculate percentages based on branch counts
                                val totalQty = (row.b1Qty + row.b2Qty).coerceAtLeast(row.qty)
                                val b1Pct = if (totalQty > 0) (row.b1Qty.toDouble() / totalQty * 100).roundToInt() else 0
                                val b2Pct = if (totalQty > 0) (row.b2Qty.toDouble() / totalQty * 100).roundToInt() else 0
                                
                                Text(
                                    text = "$b1Pct%",
                                    modifier = Modifier.width(42.dp),
                                    fontSize = 12.sp,
                                    color = RptTextMain,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "$b2Pct%",
                                    modifier = Modifier.width(42.dp),
                                    fontSize = 12.sp,
                                    color = RptTextMain,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                if (uiState.hasMore) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (uiState.isLoadingMore) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = RptGreen)
                        } else {
                            TextButton(onClick = { viewModel.loadMoreItems(branchId) }) {
                                Text("Load More", color = RptGreen, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        SalesRptCard {
            Text(
                text = "Payment Summary (${uiState.period})",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = RptTextMain
            )

            Spacer(modifier = Modifier.height(20.dp))

            val total = uiState.cashTotal + uiState.gcashTotal
            val cashFraction = if (total > 0.0) {
                (uiState.cashTotal / total).toFloat()
            } else {
                0f
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(Color(0xFF2196F3))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(cashFraction)
                        .fillMaxHeight()
                        .background(RptGreen)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PaymentSummaryItem(
                    label = "Cash",
                    amount = uiState.cashTotal,
                    color = RptGreen
                )

                PaymentSummaryItem(
                    label = "Gcash",
                    amount = uiState.gcashTotal,
                    color = Color(0xFF2196F3),
                    alignEnd = true
                )
            }
        }

        SalesRptCard {
            Text(
                text = "Top Selling Items",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = RptTextMain
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = RptGreen)
                }
            } else if (uiState.topItems.isEmpty()) {
                Text("No data.", fontSize = 13.sp, color = RptTextSub)
            } else {
                TopSellingItemsChart(items = uiState.topItems)
            }
        }

        SalesRptCard {
            Text(
                text = "Top Addons",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = RptTextMain
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = RptGreen)
                }
            } else if (uiState.topAddons.isEmpty()) {
                Text("No addons found.", fontSize = 13.sp, color = RptTextSub)
            } else {
                TopAddonsChart(items = uiState.topAddons)
            }
        }

        SalesRptCard {
            Text(
                text = "Top Fruit Combo",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = RptTextMain
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = RptGreen)
                }
            } else if (uiState.topCombos.isEmpty()) {
                Text("No combos found.", fontSize = 13.sp, color = RptTextSub)
            } else {
                TopCombosChart(items = uiState.topCombos)
            }
        }

        SalesRptCard {
            Text(
                text = "Staff Activity",
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
                            Text(
                                text = "${data.transactionCount} transactions",
                                fontSize = 12.sp,
                                color = RptTextSub
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(RptGreen.copy(alpha = 0.1f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "₱${String.format(Locale.US, "%,.0f", data.totalSales)}",
                                color = RptGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
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
private fun TopSellingItemsChart(items: List<TopSellingItemRow>) {
    val maxQty = items.maxOfOrNull { it.totalQty }?.toFloat() ?: 1f
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items.forEachIndexed { index, data ->
            val fraction = if (maxQty > 0) data.totalQty.toFloat() / maxQty else 0f
            val barColor = when (index) {
                0 -> Color(0xFFFBC02D) // Gold/Yellow for sales
                1 -> Color(0xFFFDD835)
                2 -> Color(0xFFFFEB3B)
                3 -> Color(0xFFFFF176)
                else -> Color(0xFFFFF59D)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = data.productName,
                    fontSize = 13.sp,
                    color = RptTextSub,
                    modifier = Modifier.weight(1.2f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Box(
                    modifier = Modifier
                        .weight(2f)
                        .height(20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction.coerceAtLeast(0.01f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(6.dp))
                            .background(barColor)
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = data.totalQty.toString(),
                    fontSize = 13.sp,
                    color = RptTextMain,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(35.dp),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@Composable
private fun TopAddonsChart(items: List<TopAddonRow>) {
    val maxQty = items.maxOfOrNull { it.totalQty }?.toFloat() ?: 1f
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items.forEachIndexed { index, data ->
            val fraction = if (maxQty > 0) data.totalQty.toFloat() / maxQty else 0f
            val barColor = when (index) {
                0 -> Color(0xFF2196F3) // Blue for addons
                1 -> Color(0xFF42A5F5)
                2 -> Color(0xFF64B5F6)
                3 -> Color(0xFF90CAF9)
                else -> Color(0xFFBBDEFB)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = data.addonName,
                    fontSize = 13.sp,
                    color = RptTextSub,
                    modifier = Modifier.weight(1.2f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Box(
                    modifier = Modifier
                        .weight(2f)
                        .height(20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction.coerceAtLeast(0.01f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(6.dp))
                            .background(barColor)
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = data.totalQty.toString(),
                    fontSize = 13.sp,
                    color = RptTextMain,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(35.dp),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@Composable
private fun TopCombosChart(items: List<TopComboRow>) {
    val maxCount = items.maxOfOrNull { it.count }?.toFloat() ?: 1f
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items.forEachIndexed { index, data ->
            val fraction = if (maxCount > 0) data.count.toFloat() / maxCount else 0f
            val barColor = when (index) {
                0 -> Color(0xFF9C27B0) // Purple for combos
                1 -> Color(0xFFAB47BC)
                2 -> Color(0xFFBA68C8)
                3 -> Color(0xFFCE93D8)
                else -> Color(0xFFE1BEE7)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = data.comboName,
                    fontSize = 13.sp,
                    color = RptTextSub,
                    modifier = Modifier.weight(1.2f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Box(
                    modifier = Modifier
                        .weight(2f)
                        .height(20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction.coerceAtLeast(0.01f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(6.dp))
                            .background(barColor)
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = data.count.toString(),
                    fontSize = 13.sp,
                    color = RptTextMain,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(35.dp),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@Composable
private fun PaymentSummaryItem(
    label: String,
    amount: Double,
    color: Color,
    alignEnd: Boolean = false
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(color)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column(horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
            Text(
                text = label,
                fontSize = 13.sp,
                color = RptTextSub
            )

            Text(
                text = "₱${String.format(Locale.US, "%,.2f", amount)}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = RptTextMain
            )
        }
    }
}

@Composable
private fun SalesRptCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = RptCardBg,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            content = content
        )
    }
}