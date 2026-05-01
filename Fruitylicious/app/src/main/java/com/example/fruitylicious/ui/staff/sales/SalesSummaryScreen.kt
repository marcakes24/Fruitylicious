package com.example.fruitylicious.ui.staff.sales

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.fruitylicious.ui.shared.StaffSideBarContent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
                pendingCount = uiState.pendingCount,
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

                TodayStatsCard(uiState)

                TodayPaymentBreakdownCard(uiState)

                Button(
                    onClick = {
                        viewModel.loadTodaySales()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StaffSalesGreen
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Refresh",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun StaffSalesHeader(
    pendingCount: Int,
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

            Surface(
                color = StaffSalesRed,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = "$pendingCount pending",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
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