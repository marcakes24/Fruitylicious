package com.example.fruitylicious

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.navigation.NavController
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable

@Composable
fun MyDashboard(navController: NavController) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            SideBarContent(navController)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFFEAA0))
                .verticalScroll(rememberScrollState())
        ) {
            // Pass the toggle action to the header
            HeaderSection(onMenuClick = {
                scope.launch { drawerState.open() }
            })

            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)) {
                StatCardGrid()
                Spacer(modifier = Modifier.height(14.dp))
                WeeklyChartCard()
                Spacer(modifier = Modifier.height(14.dp))
                BestSellingCard()
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
@Composable
fun HeaderSection(onMenuClick: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF2E7D32))
            .padding(start = 20.dp, end = 20.dp, top = 48.dp, bottom = 28.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hamburger menu — tapping opens sidebar
            Column(
                modifier = Modifier.clickable { onMenuClick() },
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
                Text(
                    text = "E1",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        Text(
            text = "Good Morning,",
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 16.sp
        )
        Text(
            text = "Sean Andrei 👋",
            color = Color.White,
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
@Composable
fun StatCardGrid() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Row 1
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatBox(
                number  = "12",
                label   = "Stocks (as of today)",
                emoji   = "📦",
                bgColor = Color(0xFFFFA000),
                modifier = Modifier.weight(1f)
            )
            StatBox(
                number  = "24",
                label   = "Total Sales",
                emoji   = "🍊",
                bgColor = Color(0xFFF4511E),  // deep orange
                modifier = Modifier.weight(1f)
            )
        }
        // Row 2
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatBox(
                number  = "3",
                label   = "Pending Orders",
                emoji   = "🛒",
                bgColor = Color(0xFF488F3C),
                modifier = Modifier.weight(1f)
            )
            StatBox(
                number  = "₱2.3K",
                label   = "Inv. Value",
                emoji   = "💰",
                bgColor = Color(0xFFC62828),
                modifier = Modifier.weight(1f)
            )
        }
    }
}
@Composable
fun StatBox(
    number:   String,
    label:    String,
    emoji:    String,
    bgColor:  Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(130.dp),
        shape    = RoundedCornerShape(16.dp),
        color    = bgColor
    ) {
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
        ) {
            Column {
                Text(
                    text       = number,
                    fontSize   = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text  = label,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }

            Text(
                text     = emoji,
                fontSize = 42.sp,
                color    = Color.White.copy(alpha = 0.50f),
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }
}
@Composable
fun WeeklyChartCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        color    = Color.White,
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text          = "WEEKLY STOCK MOVEMENT",
                fontSize      = 11.sp,
                fontWeight    = FontWeight.Bold,
                color         = Color(0xFFA07840),
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            BarChart(
                bars = listOf(
                    "Mon" to 38,
                    "Tue" to 52,
                    "Wed" to 62,
                    "Thu" to 48,
                    "Fri" to 68,
                    "Sat" to 78,
                    "Sun" to 90
                ),
                barColor   = Color(0xFF4CAF50),
                modifier   = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            )
        }
    }
}
@Composable
fun BarChart(
    bars:      List<Pair<String, Int>>,
    barColor:  Color,
    modifier:  Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val labelAreaPx = 36f
        val chartHeight = size.height - labelAreaPx
        val maxValue    = bars.maxOf { it.second }.toFloat()
        val spacing     = 42f
        val barWidth    = (size.width - spacing * (bars.size + 1)) / bars.size

        // Text paint for day labels
        val labelPaint = android.graphics.Paint().apply {
            color       = "#888888".toColorInt()
            textAlign   = android.graphics.Paint.Align.CENTER
            textSize    = 28f
            isAntiAlias = true
        }

        bars.forEachIndexed { i, (dayLabel, value) ->
            val left  = spacing + i * (barWidth + spacing)
            val barH  = (value / maxValue) * (chartHeight - 10f)
            val top   = chartHeight - barH

            drawRoundRect(
                color        = barColor,
                topLeft      = Offset(left, top),
                size         = Size(barWidth, barH),
                cornerRadius = CornerRadius(10f, 10f)
            )
            drawContext.canvas.nativeCanvas.drawText(
                dayLabel,
                left + barWidth / 2,
                size.height - 4f,
                labelPaint
            )
        }
    }
}
@Composable
fun BestSellingCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        color    = Color.White,
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                text          = "BEST SELLING PRODUCTS",
                fontSize      = 11.sp,
                fontWeight    = FontWeight.Bold,
                color         = Color(0xFFA07840),
                letterSpacing = 0.8.sp
            )

            ProductRow(emoji = "🥭", name = "Mango Graham Shake with Pearl", rank = "#1")
            ProductRow(emoji = "🥑", name = "Avocado Shake with Pearl",      rank = "#2")
            ProductRow(emoji = "🍌", name = "Banana Shake with Pearl",        rank = "#3")
            ProductRow(emoji = "🍉", name = "Watermelon Shake with Pearl",    rank = "#4")
        }
    }
}
@Composable
fun ProductRow(emoji: String, name: String, rank: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = emoji, fontSize = 20.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = name, fontSize = 15.sp, color = Color(0xFF3D1F00))
        }

        Text(
            text       = rank,
            fontSize   = 15.sp,
            fontWeight = FontWeight.Bold,
            color      = Color(0xFF3D1F00)
        )
    }
}
