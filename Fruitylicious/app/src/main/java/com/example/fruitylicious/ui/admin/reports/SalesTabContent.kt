package com.example.fruitylicious.ui.admin.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarToday
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.fruitylicious.ADMIN_SALES_SUMMARY
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesTabContent(
    branchId: Int?,
    navController: NavController,
    viewModel: SalesReportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var showDatePicker by remember { mutableStateOf(false) }

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
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = RptTextMain,
                    modifier = Modifier.weight(1f)
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
            Text(
                text = "Sales Breakdown (${uiState.period})",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = RptTextMain
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Text("Product", modifier = Modifier.weight(1f), fontSize = 12.sp, color = RptTextSub, fontWeight = FontWeight.Bold)
                Text("Qty", modifier = Modifier.width(40.dp), fontSize = 12.sp, color = RptTextSub, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text("Total", modifier = Modifier.width(100.dp), fontSize = 12.sp, color = RptTextSub, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFF5F5F5))

            if (uiState.salesBreakdown.isEmpty()) {
                Text("No sales found", color = RptTextSub, fontSize = 13.sp)
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
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = RptTextMain,
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = row.qty.toString(),
                            modifier = Modifier.width(40.dp),
                            fontSize = 15.sp,
                            color = RptTextMain,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "₱${String.format(Locale.US, "%,.2f", row.totalAmount)}",
                            modifier = Modifier.width(100.dp),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = RptGreen,
                            textAlign = TextAlign.End
                        )
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
                fontSize = 16.sp,
                color = RptTextMain
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.topItems.isEmpty()) {
                Text("No sold items found", color = RptTextSub, fontSize = 13.sp)
            } else {
                uiState.topItems.forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${index + 1}.",
                                fontSize = 15.sp,
                                color = RptTextSub,
                                modifier = Modifier.width(28.dp)
                            )

                            Text(
                                text = item.productName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = RptTextMain
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${item.totalQty} sold",
                                fontSize = 13.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Button(
            onClick = {
                navController.navigate(ADMIN_SALES_SUMMARY)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = RptGreen),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Go to Full Sales Summary",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
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