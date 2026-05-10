package com.example.fruitylicious.ui.staff.sales

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.fruitylicious.ui.shared.StaffSideBarContent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
import kotlinx.coroutines.launch

private val StaffSalesGreen = Color(0xFF2C8C44)
private val StaffSalesPageBg = Color(0xFFFFEAA0)
private val StaffSalesRed = Color(0xFFE53935)
private val StaffSalesCardBg = Color.White
private val StaffSalesTextMain = Color(0xFF1A1A1A)
private val StaffSalesTextSub = Color(0xFF757575)
private val StaffSalesBlue = Color(0xFF3498DB)
private val StaffSalesCashGreen = Color(0xFF2ECC71)

@Composable
fun SalesSummaryScreen(
    navController: NavController,
    staffName: String = "Staff User",
    onLogout: () -> Unit = {},
    viewModel: StaffSalesSummaryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color.Transparent,
                drawerTonalElevation = 0.dp
            ) {
                StaffSideBarContent(
                    navController = navController,
                    drawerState = drawerState,
                    scope = scope,
                    staffName = staffName,
                    branchName = uiState.branchName.ifBlank { "B${uiState.branchId}" },
                    isClockedIn = uiState.isClockedIn,
                    onLogout = onLogout
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(StaffSalesPageBg)
        ) {
            StaffSalesHeader(
                onMenuClick = {
                    scope.launch {
                        drawerState.open()
                    }
                }
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (!uiState.error.isNullOrBlank()) {
                    Text(
                        text = uiState.error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp
                    )
                }

                TodaySalesCard(uiState)

                StaffDailySalesChartSection(
                    dailySales = uiState.dailySalesData,
                    totalAmount = uiState.todaySales,
                    transactionCount = uiState.transactionCount,
                    selectedHour = uiState.selectedHourlySales,
                    onHourClick = viewModel::onHourSelected
                )

                TodayStatsCard(uiState)

                TodayPaymentBreakdownCard(uiState)

                TodaySalesBreakdownCard(uiState)

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun StaffSalesHeader(
    onMenuClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(StaffSalesGreen)
            .padding(start = 16.dp, end = 16.dp, top = 40.dp, bottom = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = Color.White
                )
            }

            Text(
                text = "TODAY'S SALES",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TodaySalesCard(
    uiState: StaffSalesSummaryUiState
) {
    StaffSummaryCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Today's Sales",
                    fontSize = 14.sp,
                    color = StaffSalesTextSub,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "₱${String.format(Locale.US, "%,.2f", uiState.todaySales)}",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = StaffSalesTextMain
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Branch ${uiState.branchId}",
                    fontSize = 12.sp,
                    color = StaffSalesTextSub,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = SimpleDateFormat(
                        "EEEE, MMMM dd, yyyy",
                        Locale.US
                    ).format(Date()),
                    fontSize = 12.sp,
                    color = StaffSalesTextSub
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = if (uiState.isLoading) {
                "Loading today's sales..."
            } else {
                "Includes non-void transactions for today."
            },
            fontSize = 13.sp,
            color = StaffSalesTextSub
        )
    }
}

@Composable
private fun StaffDailySalesChartSection(
    dailySales: List<HourlySales>,
    totalAmount: Double,
    transactionCount: Int,
    selectedHour: HourlySales?,
    onHourClick: (HourlySales?) -> Unit
) {
    StaffSummaryCard {
        Text(
            text = "Hourly Sales Performance",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = StaffSalesTextMain
        )

        Spacer(modifier = Modifier.height(16.dp))

        val hourLabels = listOf("10a", "11a", "12p", "1p", "2p", "3p", "4p", "5p", "6p", "7p")
        val maxSales = (dailySales.maxOfOrNull { it.totalSales } ?: 0f).coerceAtLeast(100f)
        val roundedMax = (ceil(maxSales / 100.0) * 100).toInt()
        val textMeasurer = rememberTextMeasurer()

        val yLabels = listOf(
            "₱$roundedMax",
            "₱${roundedMax * 3 / 4}",
            "₱${roundedMax / 2}",
            "₱${roundedMax / 4}",
            "₱0"
        )

        val topPadding = 32.dp
        val bottomPadding = 24.dp

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            Column(
                modifier = Modifier
                    .width(40.dp)
                    .fillMaxHeight()
                    .padding(top = topPadding, bottom = bottomPadding),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                yLabels.forEach { label ->
                    Text(
                        text = label,
                        fontSize = 8.sp,
                        color = StaffSalesTextSub,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(dailySales) {
                            detectTapGestures { offset ->
                                val barCount = dailySales.size
                                val gap = size.width.toFloat() / barCount
                                val clickedIndex = (offset.x / gap).toInt()
                                if (clickedIndex in dailySales.indices) {
                                    onHourClick(dailySales[clickedIndex])
                                } else {
                                    onHourClick(null)
                                }
                            }
                        }
                ) {
                    val tPaddingPx = topPadding.toPx()
                    val bPaddingPx = bottomPadding.toPx()
                    val chartHeight = size.height - tPaddingPx - bPaddingPx
                    val barCount = dailySales.size
                    val gap = size.width / barCount
                    val barWidth = gap * 0.6f

                    for (i in 0..4) {
                        val y = tPaddingPx + chartHeight * (1f - i / 4f)
                        drawLine(
                            color = Color(0xFFEEEEEE),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    dailySales.forEachIndexed { index, data ->
                        val barHeight = (data.totalSales / roundedMax) * chartHeight
                        val left = index * gap + (gap - barWidth) / 2f
                        val top = tPaddingPx + chartHeight - barHeight

                        val isSelected = selectedHour?.hour == data.hour

                        drawRoundRect(
                            color = if (isSelected) StaffSalesGreen else StaffSalesRed,
                            topLeft = Offset(left, top),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(4.dp.toPx())
                        )

                        if (isSelected) {
                            staffSummaryDrawTooltip(
                                textMeasurer = textMeasurer,
                                label = "₱${data.totalSales.toInt()}\n${data.transactionCount} txns",
                                centerX = left + barWidth / 2f,
                                topY = top - 40.dp.toPx()
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 46.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            hourLabels.forEach { label ->
                Text(
                    text = label,
                    fontSize = 8.sp,
                    color = StaffSalesTextSub,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(color = Color(0xFFEEEEEE))
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = if (selectedHour != null) {
                    val h = selectedHour.hour
                    val period = if (h < 12) "AM" else "PM"
                    val displayHour = if (h > 12) h - 12 else if (h == 0) 12 else h
                    "Details for $displayHour $period"
                } else {
                    "Today's Total"
                },
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = StaffSalesTextMain
            )

            Column(horizontalAlignment = Alignment.End) {
                val displayAmount = selectedHour?.totalSales?.toDouble() ?: totalAmount
                val displayCount = selectedHour?.transactionCount ?: transactionCount

                Text(
                    text = "₱${String.format(Locale.US, "%,.2f", displayAmount)}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = StaffSalesTextMain
                )

                Text(
                    text = "$displayCount transactions",
                    fontSize = 12.sp,
                    color = StaffSalesTextSub
                )
            }
        }
    }
}

private fun DrawScope.staffSummaryDrawTooltip(
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    label: String,
    centerX: Float,
    topY: Float
) {
    val bubbleWidth = 60.dp.toPx()
    val bubbleHeight = 34.dp.toPx()
    val left = centerX - bubbleWidth / 2f

    val tooltipPath = Path().apply {
        addRoundRect(
            androidx.compose.ui.geometry.RoundRect(
                left = left,
                top = topY,
                right = left + bubbleWidth,
                bottom = topY + bubbleHeight,
                cornerRadius = CornerRadius(4.dp.toPx())
            )
        )
        moveTo(centerX - 4.dp.toPx(), topY + bubbleHeight)
        lineTo(centerX + 4.dp.toPx(), topY + bubbleHeight)
        lineTo(centerX, topY + bubbleHeight + 5.dp.toPx())
        close()
    }

    drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            isAntiAlias = true
            setShadowLayer(
                12f,
                0f,
                4f,
                android.graphics.Color.argb(70, 0, 0, 0)
            )
        }
        canvas.nativeCanvas.drawPath(tooltipPath.asAndroidPath(), paint)
    }

    val textLayoutResult = textMeasurer.measure(
        text = label,
        style = TextStyle(
            color = Color(0xFF1A1A1A),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 11.sp
        )
    )

    drawText(
        textLayoutResult = textLayoutResult,
        topLeft = Offset(
            centerX - textLayoutResult.size.width / 2f,
            topY + (bubbleHeight - textLayoutResult.size.height) / 2f
        )
    )
}

@Composable
private fun TodayStatsCard(
    uiState: StaffSalesSummaryUiState
) {
    StaffSummaryCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.CalendarMonth,
                contentDescription = null,
                tint = StaffSalesTextMain,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Today's Activity",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = StaffSalesTextMain
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Transactions",
                    fontSize = 12.sp,
                    color = StaffSalesTextSub
                )

                Text(
                    text = uiState.transactionCount.toString(),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = StaffSalesTextMain
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Active Queue",
                    fontSize = 12.sp,
                    color = StaffSalesTextSub
                )

                Text(
                    text = uiState.pendingCount.toString(),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = StaffSalesRed
                )
            }
        }
    }
}

@Composable
private fun TodayPaymentBreakdownCard(
    uiState: StaffSalesSummaryUiState
) {
    val total = uiState.cashTotal + uiState.gcashTotal

    val cashFraction = if (total > 0.0) {
        (uiState.cashTotal / total).toFloat()
    } else {
        0f
    }

    val gcashFraction = if (total > 0.0) {
        (uiState.gcashTotal / total).toFloat()
    } else {
        0f
    }

    StaffSummaryCard {
        Text(
            text = "Today's Payment Breakdown",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = StaffSalesTextMain
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
        ) {
            Box(
                modifier = Modifier
                    .weight(cashFraction.coerceAtLeast(0.001f))
                    .fillMaxHeight()
                    .background(StaffSalesCashGreen)
            )

            Box(
                modifier = Modifier
                    .weight(gcashFraction.coerceAtLeast(0.001f))
                    .fillMaxHeight()
                    .background(StaffSalesBlue)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            PaymentBreakdownItem(
                modifier = Modifier.weight(1f),
                label = "Cash",
                amount = uiState.cashTotal,
                percent = if (total > 0.0) uiState.cashTotal / total * 100.0 else 0.0,
                iconColor = StaffSalesCashGreen,
                icon = Icons.Default.Payments
            )

            PaymentBreakdownItem(
                modifier = Modifier.weight(1f),
                label = "Gcash",
                amount = uiState.gcashTotal,
                percent = if (total > 0.0) uiState.gcashTotal / total * 100.0 else 0.0,
                iconColor = StaffSalesBlue,
                icon = Icons.Default.AccountBalanceWallet
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        HorizontalDivider(color = Color(0xFFEEEEEE))

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Total Payments",
                fontSize = 13.sp,
                color = StaffSalesTextSub
            )

            Text(
                text = "₱${String.format(Locale.US, "%,.2f", total)}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = StaffSalesTextMain
            )
        }
    }
}

@Composable
private fun PaymentBreakdownItem(
    modifier: Modifier,
    label: String,
    amount: Double,
    percent: Double,
    iconColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(iconColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = "$label (${String.format(Locale.US, "%.0f", percent)}%)",
                fontSize = 12.sp,
                color = StaffSalesTextSub
            )

            Text(
                text = "₱${String.format(Locale.US, "%,.2f", amount)}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = StaffSalesTextMain
            )
        }
    }
}

@Composable
private fun TodaySalesBreakdownCard(
    uiState: StaffSalesSummaryUiState
) {
    StaffSummaryCard {
        Text(
            text = "Today's Product Breakdown",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = StaffSalesTextMain
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Text("Product", modifier = Modifier.weight(1f), fontSize = 12.sp, color = StaffSalesTextSub, fontWeight = FontWeight.Bold)
            Text("Qty", modifier = Modifier.width(35.dp), fontSize = 12.sp, color = StaffSalesTextSub, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text("Total", modifier = Modifier.width(70.dp), fontSize = 12.sp, color = StaffSalesTextSub, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFF5F5F5))

        if (uiState.salesBreakdown.isEmpty()) {
            Text(
                text = "No items sold today yet.",
                fontSize = 13.sp,
                color = StaffSalesTextSub,
                modifier = Modifier.padding(vertical = 10.dp)
            )
        } else {
            uiState.salesBreakdown.forEach { row ->
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
                        color = StaffSalesGreen,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = row.qty.toString(),
                        modifier = Modifier.width(35.dp),
                        fontSize = 13.sp,
                        color = StaffSalesTextMain,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "₱${String.format(Locale.US, "%,.0f", row.totalAmount)}",
                        modifier = Modifier.width(70.dp),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF9A825),
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

@Composable
private fun StaffSummaryCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = StaffSalesCardBg,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            content = content
        )
    }
}
