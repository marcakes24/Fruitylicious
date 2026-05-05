package com.example.fruitylicious.ui.admin.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.fruitylicious.data.local.entity.BranchEntity
import com.example.fruitylicious.ui.shared.AdminSideBarContent
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

private val SsGreen = Color(0xFF2E7D32)
private val SsRed = Color(0xFFE53935)
private val SsPageBg = Color(0xFFFFEAA0)
private val SsCardBg = Color.White
private val SsTextMain = Color(0xFF1A1A1A)
private val SsTextSub = Color(0xFF757575)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesSummaryScreen(
    navController: NavController,
    adminName: String = "Admin User",
    onLogout: () -> Unit = {},
    viewModel: SalesSummaryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val pctChange = if (uiState.yesterdaySales > 0.0) {
        ((uiState.todaySales - uiState.yesterdaySales) / uiState.yesterdaySales) * 100.0
    } else {
        0.0
    }

    val isUp = pctChange >= 0.0
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = uiState.selectedDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { viewModel.setSelectedDate(it) }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color.Transparent,
                drawerTonalElevation = 0.dp
            ) {
                AdminSideBarContent(
                    navController = navController,
                    drawerState = drawerState,
                    scope = scope,
                    adminName = adminName,
                    onLogout = onLogout
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SsPageBg)
        ) {
            Header(
                selectedBranchId = uiState.selectedBranchId,
                canAccessCrossBranch = uiState.canAccessCrossBranch,
                branches = uiState.branches,
                onBranchSelected = { viewModel.onBranchSelected(it) },
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
                // Period Picker Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF5F5F5))
                                .padding(4.dp)
                        ) {
                            listOf("daily", "weekly", "monthly").forEach { p ->
                                val isSelected = uiState.period == p
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color.White else Color.Transparent)
                                        .clickable { viewModel.setPeriod(p) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = p.replaceFirstChar { it.uppercase() },
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) SsGreen else SsTextSub
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(onClick = { viewModel.navigatePeriod(-1) }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Prev", tint = SsGreen)
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showDatePicker = true }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(16.dp), tint = SsGreen)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(uiState.rangeText, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SsTextMain)
                            }

                            IconButton(onClick = { viewModel.navigatePeriod(1) }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, "Next", tint = SsGreen)
                            }
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

                SummaryCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "${uiState.period.replaceFirstChar { it.uppercase() }} Sales",
                                fontSize = 14.sp,
                                color = SsTextSub,
                                fontWeight = FontWeight.Medium
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "₱${String.format(Locale.US, "%,.2f", uiState.todaySales)}",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = SsTextMain
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isUp) Color(0xFFE8F5E9) else Color(0xFFFFEBEE))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isUp) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    tint = if (isUp) SsGreen else SsRed,
                                    modifier = Modifier.size(14.dp)
                                )

                                Spacer(modifier = Modifier.width(4.dp))

                                Text(
                                    text = "${String.format(Locale.US, "%.1f", abs(pctChange))}%",
                                    color = if (isUp) SsGreen else SsRed,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Text(
                        text = "vs previous period (₱${String.format(Locale.US, "%,.2f", uiState.yesterdaySales)})",
                        fontSize = 13.sp,
                        color = SsTextSub,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    WeeklyBarChart(uiState.weekData)
                }

                SummaryCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarMonth,
                            contentDescription = null,
                            tint = SsTextMain,
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = if (uiState.period == "monthly") "This Month" else "Period Context",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = SsTextMain
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Transactions", fontSize = 12.sp, color = SsTextSub)

                            Text(
                                text = uiState.monthTransactions.toString(),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = SsTextMain
                            )
                        }

                        Column(modifier = Modifier.weight(1.5f)) {
                            Text("Total Sales", fontSize = 12.sp, color = SsTextSub)

                            Text(
                                text = "₱${String.format(Locale.US, "%,.2f", uiState.monthTotal)}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = SsRed
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    MonthlyLineChart(uiState.monthLineData)
                }

                SummaryCard {
                    Text(
                        text = "${uiState.period.replaceFirstChar { it.uppercase() }} Payment Breakdown",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = SsTextMain
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val total = uiState.todayCash + uiState.todayGcash
                    val cashFraction = if (total > 0.0) {
                        (uiState.todayCash / total).toFloat()
                    } else {
                        0f
                    }

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
                                .background(Color(0xFF2ECC71))
                        )

                        Box(
                            modifier = Modifier
                                .weight((1f - cashFraction).coerceAtLeast(0.001f))
                                .fillMaxHeight()
                                .background(Color(0xFF3498DB))
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        PaymentBox(
                            modifier = Modifier.weight(1f),
                            label = "Cash",
                            amount = uiState.todayCash,
                            total = total,
                            color = Color(0xFF2ECC71),
                            icon = Icons.Default.Payments
                        )

                        PaymentBox(
                            modifier = Modifier.weight(1f),
                            label = "Gcash",
                            amount = uiState.todayGcash,
                            total = total,
                            color = Color(0xFF3498DB),
                            icon = Icons.Default.AccountBalanceWallet
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun Header(
    selectedBranchId: Int?,
    canAccessCrossBranch: Boolean,
    branches: List<BranchEntity>,
    onBranchSelected: (Int?) -> Unit,
    onMenuClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(SsGreen)
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
                text = "SALES SUMMARY",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.width(16.dp))

            if (canAccessCrossBranch) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF5F5F5))
                        .padding(4.dp)
                        .horizontalScroll(rememberScrollState())
                ) {
                    // "All" option
                    val isAllSelected = selectedBranchId == null
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isAllSelected) SsGreen else Color.Transparent)
                            .clickable { onBranchSelected(null) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "All",
                            color = if (isAllSelected) Color.White else Color(0xFF666E7A),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    branches.forEach { branch ->
                        val isSelected = selectedBranchId == branch.branchId

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) SsGreen else Color.Transparent)
                                .clickable { onBranchSelected(branch.branchId) }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "B${branch.branchId}",
                                color = if (isSelected) Color.White else Color(0xFF666E7A),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                selectedBranchId?.let { id ->
                    Text(
                        text = "B$id",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklyBarChart(weekData: List<Double>) {
    val maxValue = weekData.maxOrNull()?.coerceAtLeast(100.0) ?: 100.0
    val yLabels = listOf(maxValue, maxValue * 0.75, maxValue * 0.5, maxValue * 0.25, 0.0)
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(bottom = 24.dp), // Space for labels
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            yLabels.forEach { label ->
                Text(
                    text = "₱${label.toInt()}",
                    fontSize = 10.sp,
                    color = Color.LightGray
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            weekData.forEachIndexed { index, value ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        val fraction = (value / maxValue).toFloat().coerceIn(0.01f, 1f)
                        Box(
                            modifier = Modifier
                                .width(20.dp)
                                .fillMaxHeight(fraction)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(SsRed)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = days[index],
                        fontSize = 10.sp,
                        color = SsTextSub,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthlyLineChart(monthLineData: List<Double>) {
    val values = monthLineData.ifEmpty { listOf(0.0) }
    val maxValue = values.maxOrNull()?.coerceAtLeast(100.0) ?: 100.0

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val step = if (values.size > 1) width / (values.size - 1) else width

            val path = Path()

            values.forEachIndexed { index, value ->
                val x = step * index
                val y = height - ((value / maxValue).toFloat() * height)

                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }

            drawPath(
                path = path,
                color = SsRed,
                style = Stroke(width = 2.dp.toPx())
            )

            val fillPath = Path().apply {
                addPath(path)
                lineTo(width, height)
                lineTo(0f, height)
                close()
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    listOf(
                        SsRed.copy(alpha = 0.2f),
                        Color.Transparent
                    )
                )
            )

            values.forEachIndexed { index, value ->
                val x = step * index
                val y = height - ((value / maxValue).toFloat() * height)

                if (value > 0.0) {
                    drawCircle(
                        color = SsRed,
                        radius = 3.dp.toPx(),
                        center = Offset(x, y)
                    )
                }
            }
        }
    }
}

@Composable
private fun PaymentBox(
    modifier: Modifier,
    label: String,
    amount: Double,
    total: Double,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    val percent = if (total > 0.0) {
        (amount / total) * 100.0
    } else {
        0.0
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = "$label (${String.format(Locale.US, "%.0f", percent)}%)",
                fontSize = 12.sp,
                color = SsTextSub
            )

            Text(
                text = "₱${String.format(Locale.US, "%,.2f", amount)}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = SsTextMain
            )
        }
    }
}

@Composable
private fun SummaryCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SsCardBg,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            content = content
        )
    }
}