package com.example.fruitylicious

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.navigation.NavController
import kotlinx.coroutines.launch

@Composable
fun StocksReportScreenMain(navController : NavController) {
    var selectedPeriod by remember { mutableStateOf("Daily") }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = { SideBarContent(navController) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFFEAA0))
                .verticalScroll(rememberScrollState())
        ) {

            // ── Green Header ──────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2E7D32))
                    .padding(start = 20.dp, end = 20.dp, top = 48.dp, bottom = 24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.clickable { scope.launch { drawerState.open() } },
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        repeat(3) {
                            Box(
                                modifier = Modifier
                                    .width(22.dp)
                                    .height(2.dp)
                                    .background(Color.White, RoundedCornerShape(1.dp))
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color(0xFFC62828), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("E1", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Stocks Reports",
                    color = Color.White,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Inventory Performance Overview",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 13.sp
                )
            }

            // ── Body ──────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {

                // ── Period Toggle ─────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF388E3C))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("Daily", "Weekly", "Monthly").forEach { period ->
                        val selected = period == selectedPeriod
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) Color(0xFF2E7D32) else Color.Transparent)
                                .clickable { selectedPeriod = period }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = period,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── Stock In / Stock Out Cards ────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StockDirectionCard(
                        title = "STOCK IN",
                        value = "+420",
                        subtitle = "12% vs yesterday",
                        bg = Color(0xFFFFA000),
                        barColor = Color(0xFFFFD54F),
                        modifier = Modifier.weight(1f)
                    )
                    StockDirectionCard(
                        title = "STOCK OUT",
                        value = "-285",
                        subtitle = "5% vs yesterday",
                        bg = Color(0xFFF57C00),
                        barColor = Color(0xFFFFD54F),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ── Turnover Card ─────────────────────────────────────────
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF2E7D32)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "TURNOVER",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "68%",
                                color = Color.White,
                                fontSize = 40.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "of stock moved",
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 12.sp
                            )
                        }
                        // Mini bar chart inside turnover card
                        MiniBarChart(
                            modifier = Modifier
                                .weight(1f)
                                .height(70.dp),
                            barColor = Color(0xFFAED581),
                            accentColor = Color(0xFFFFD54F)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ── Recent Stocks Movement ────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RECENT STOCKS MOVEMENT",
                        color = Color(0xFFA07840),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "View Details →",
                        color = Color(0xFF2E7D32),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable {
                            navController.navigate("stocks_report")
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ── Large Bar Chart Card ──────────────────────────────────
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 4.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        LargeBarChart(
                            bars = listOf(
                                "Mon" to 55,
                                "Tue" to 80,
                                "Wed" to 40,
                                "Thu" to 30,
                                "Fri" to 60,
                                "Sat" to 70,
                                "Sun" to 90
                            ),
                            barColor = Color(0xFF4CAF50),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// ── Stock Direction Card ──────────────────────────────────────────────────────

@Composable
fun StockDirectionCard(
    title: String,
    value: String,
    subtitle: String,
    bg: Color,
    barColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(150.dp),
        shape = RoundedCornerShape(16.dp),
        color = bg
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.weight(1f))
            // Mini inline bars
            MiniBarChart(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
                barColor = barColor,
                accentColor = Color.White
            )
        }
    }
}

// ── Mini Bar Chart (inline decorative) ───────────────────────────────────────

@Composable
fun MiniBarChart(
    modifier: Modifier = Modifier,
    barColor: Color,
    accentColor: Color
) {
    val bars = listOf(0.4f, 0.6f, 0.5f, 0.8f, 0.7f, 0.9f, 1.0f)
    Canvas(modifier = modifier) {
        val barCount = bars.size
        val spacing = 6f
        val barWidth = (size.width - spacing * (barCount + 1)) / barCount
        bars.forEachIndexed { i, fraction ->
            val barH = fraction * (size.height - 4f)
            val left = spacing + i * (barWidth + spacing)
            val top = size.height - barH
            val color = if (i == barCount - 1) accentColor else barColor
            drawRoundRect(
                color = color,
                topLeft = Offset(left, top),
                size = Size(barWidth, barH),
                cornerRadius = CornerRadius(6f, 6f)
            )
        }
    }
}

// ── Large Bar Chart ───────────────────────────────────────────────────────────

@Composable
fun LargeBarChart(
    bars: List<Pair<String, Int>>,
    barColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val labelAreaPx = 40f
        val chartHeight = size.height - labelAreaPx
        val maxValue = bars.maxOf { it.second }.toFloat()
        val spacing = 28f
        val barWidth = (size.width - spacing * (bars.size + 1)) / bars.size

        val labelPaint = android.graphics.Paint().apply {
            color = "#888888".toColorInt()
            textAlign = android.graphics.Paint.Align.CENTER
            textSize = 30f
            isAntiAlias = true
        }

        bars.forEachIndexed { i, (label, value) ->
            val left = spacing + i * (barWidth + spacing)
            val barH = (value / maxValue) * (chartHeight - 10f)
            val top = chartHeight - barH

            drawRoundRect(
                color = barColor,
                topLeft = Offset(left, top),
                size = Size(barWidth, barH),
                cornerRadius = CornerRadius(12f, 12f)
            )
            drawContext.canvas.nativeCanvas.drawText(
                label,
                left + barWidth / 2,
                size.height - 6f,
                labelPaint
            )
        }
    }
}

// ── Preview ───────────────────────────────────────────────────────────────────
