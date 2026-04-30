package com.example.fruitylicious

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.*

// ── Brand colors ──────────────────────────────────────────────────────────────
private val RptGreen  = Color(0xFF2C8C44)
private val RptPageBg = Color(0xFFFFEAA0)
private val RptRed    = Color(0xFFE53935)
private val RptCardBg = Color.White
private val RptTextMain = Color(0xFF1A1A1A)
private val RptTextSub  = Color(0xFF757575)

@Composable
fun StaffSalesSummaryScreen(
    navController: NavController,
    drawerState:   DrawerState,
    scope:         CoroutineScope
) {
    // ── Mock Data Logic ──────────────────────────────────────────────────────
    val todaySales = 255.0
    val yesterdaySales = 390.0
    val weekData = listOf(210.0, 240.0, 270.0, 120.0, 270.0, 390.0, 270.0)
    val monthTransactions = 17
    val monthTotal = 2670.0

    val pctChange = ((todaySales - yesterdaySales) / yesterdaySales * 100)
    val isUp = pctChange >= 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RptPageBg)
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(RptGreen)
                .padding(start = 16.dp, end = 16.dp, top = 40.dp, bottom = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                }

                Text(
                    text = "SALES SUMMARY",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                // "12 pending" Badge
                Surface(
                    color = Color.Red,
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
                            text = "12 pending",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // ── Scrollable Content ────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Today's Sales Card ────────────────────────────────────────────
            StaffSummaryCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Today's Sales", fontSize = 14.sp, color = RptTextSub, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "₱${String.format("%.2f", todaySales)}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = RptTextMain
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
                                tint = if (isUp) RptGreen else RptRed,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${String.format("%.1f", Math.abs(pctChange))}%",
                                color = if (isUp) RptGreen else RptRed,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Sunday, April 26, 2026", fontSize = 12.sp, color = RptTextSub)
                    }
                }
                
                Text(
                    text = "vs yesterday (₱${String.format("%.2f", yesterdaySales)})",
                    fontSize = 13.sp,
                    color = RptTextSub,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Weekly Bar Chart
                Row(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                    // Y-Axis Labels
                    Column(
                        modifier = Modifier.fillMaxHeight().padding(bottom = 28.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.End
                    ) {
                        listOf("P400", "P300", "P200", "P100", "P0").forEach { label ->
                            Text(label, fontSize = 10.sp, color = Color.LightGray)
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))

                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                            
                            weekData.forEachIndexed { index, value ->
                                Column(
                                    modifier = Modifier.fillMaxHeight(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom
                                ) {
                                    // Tooltip for Saturday
                                    if (index == 5 && value > 0) {
                                        Box(
                                            modifier = Modifier
                                                .shadow(4.dp, RoundedCornerShape(4.dp))
                                                .background(Color.White, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("${value.toInt()}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = RptTextMain)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }

                                    Box(
                                        modifier = Modifier
                                            .width(28.dp)
                                            .weight(1f, fill = false)
                                            .fillMaxHeight((value / 400.0).toFloat().coerceIn(0.05f, 1f))
                                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            .background(RptRed)
                                    )
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    Text(
                                        text = days[index], 
                                        fontSize = 10.sp, 
                                        color = RptTextSub,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── This Month Card ───────────────────────────────────────────────
            StaffSummaryCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = RptTextMain, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("This Month", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = RptTextMain)
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Transactions", fontSize = 12.sp, color = RptTextSub)
                        Text("$monthTransactions", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = RptTextMain)
                    }
                    Column(modifier = Modifier.weight(1.5f)) {
                        Text("Total Sales", fontSize = 12.sp, color = RptTextSub)
                        Text("₱${String.format("%.2f", monthTotal)}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = RptRed)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Monthly Line Chart
                Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height
                        
                        // Mock line path
                        val path = Path().apply {
                            moveTo(0f, height * 0.7f)
                            lineTo(width * 0.1f, height * 0.72f)
                            lineTo(width * 0.2f, height * 0.65f)
                            lineTo(width * 0.3f, height * 0.68f)
                            lineTo(width * 0.4f, height * 0.5f) // The peak for tooltip
                            lineTo(width * 0.5f, height * 0.6f)
                            lineTo(width * 0.6f, height * 0.45f)
                            lineTo(width * 0.7f, height * 0.55f)
                            lineTo(width * 0.8f, height * 0.65f)
                            lineTo(width * 0.9f, height * 0.58f)
                            lineTo(width, height * 0.62f)
                        }
                        
                        drawPath(path, color = RptRed, style = Stroke(width = 2.dp.toPx()))
                        
                        val fillPath = Path().apply {
                            addPath(path)
                            lineTo(width, height)
                            lineTo(0f, height)
                            close()
                        }
                        drawPath(fillPath, brush = Brush.verticalGradient(listOf(RptRed.copy(alpha = 0.2f), Color.Transparent)))

                        // Dot for Apr 10
                        drawCircle(color = RptRed, radius = 4.dp.toPx(), center = Offset(width * 0.4f, height * 0.5f))
                    }
                    
                    // Tooltip for Apr 10
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(x = 60.dp, y = 10.dp)
                            .shadow(4.dp, RoundedCornerShape(6.dp))
                            .background(Color.White, RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        Column {
                            Text("Apr 10", fontSize = 10.sp, color = RptTextSub)
                            Text("Transactions: 2", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = RptTextMain)
                            Text("₱450.00", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = RptTextMain)
                        }
                    }
                }
            }

            // ── Today's Payment Breakdown Card ────────────────────────────────────────
            StaffSummaryCard {
                Text("Today's Payment Breakdown", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = RptTextMain)
                Spacer(modifier = Modifier.height(16.dp))
                
                // Segmented Progress Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                ) {
                    Box(modifier = Modifier.weight(0.71f).fillMaxHeight().background(Color(0xFF2ECC71))) // Green
                    Box(modifier = Modifier.weight(0.29f).fillMaxHeight().background(Color(0xFF3498DB))) // Blue
                }
                
                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    // Cash
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFE8F5E9)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Payments, contentDescription = null, tint = Color(0xFF2ECC71), modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Cash (71%)", fontSize = 12.sp, color = RptTextSub)
                            Text("₱180.00", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = RptTextMain)
                        }
                    }
                    // GCash
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFE3F2FD)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color(0xFF3498DB), modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Gcash (29%)", fontSize = 12.sp, color = RptTextSub)
                            Text("₱75.00", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = RptTextMain)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StaffSummaryCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = RptCardBg,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(20.dp), content = content)
    }
}
