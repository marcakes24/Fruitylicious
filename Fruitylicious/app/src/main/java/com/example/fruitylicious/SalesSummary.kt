package com.example.fruitylicious

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.fruitylicious.ui.staff.pos.GreenPrimary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun SalesSummaryScreen(
    navController: NavController,
    drawerState: DrawerState,
    scope: CoroutineScope
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFEAA0))
            .verticalScroll(rememberScrollState())
    ) {
        // --- 1. Header ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(GreenPrimary)
                .padding(start = 20.dp, end = 20.dp, top = 48.dp, bottom = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Column(
                modifier = Modifier
                    .padding(start = 20.dp)
                    .clickable { scope.launch { drawerState.open() } },
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Thinner, custom hamburger icon
                repeat(3) {
                    Box(modifier = Modifier.width(22.dp).height(1.5.dp).background(Color.White))
                }
            }

            Text(
                text = "SALES SUMMARY",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // --- 2. Body Content ---
        Column(modifier = Modifier.padding(16.dp)) {

            // Section 1: Number of Transactions
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFFAFF82), // Bright yellow card
                shape = RoundedCornerShape(8.dp),
                shadowElevation = 0.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Number of Transactions", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                    Text(
                        text = "March 12, 2025 - February 13, 2026",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black.copy(alpha = 0.8f)
                    )

                    // Graph Area Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .padding(top = 16.dp)
                    ) {
                        // Background Grid Lines
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            repeat(4) { HorizontalDivider(color = Color.Black.copy(alpha = 0.1f), thickness = 1.dp) }
                        }

                        // Big Left-Aligned Value
                        Text(
                            text = "15, 312",
                            fontSize = 40.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.align(Alignment.TopStart).padding(top = 4.dp)
                        )

                        // Offset Bars
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(start = 135.dp), // Pushes bars to the right of the big number
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            // Hardcoded bar heights to match the visual mock
                            val card1Data = listOf(35, 75, 65, 30, 85, 55)

                            card1Data.forEach { value ->
                                val barColor = if (value < 50) Color(0xFFFF3B30) else Color(0xFF7CC444)
                                Box(
                                    modifier = Modifier
                                        .width(22.dp)
                                        .fillMaxHeight(value / 100f)
                                        .background(barColor)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section 2: Daily Sales Summary
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFFAFF82),
                shape = RoundedCornerShape(8.dp),
                shadowElevation = 0.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Daily Sales Summary", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                    Text(
                        text = "12%+ vs yesterday",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black.copy(alpha = 0.8f)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .padding(top = 24.dp)
                    ) {
                        // Grid and Bars (Takes up remaining vertical space)
                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            // Gridlines behind
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                repeat(7) { HorizontalDivider(color = Color.Black.copy(alpha = 0.1f), thickness = 1.dp) }
                            }

                            // Full-height bar row
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                val chartHeights = listOf(40, 70, 60, 25, 85, 50)
                                chartHeights.forEach { value ->
                                    val barColor = if (value < 50) Color(0xFFFF3B30) else Color(0xFF7CC444)
                                    Box(
                                        modifier = Modifier
                                            .width(36.dp)
                                            .fillMaxHeight(value / 100f)
                                            .background(barColor)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // X-Axis Multi-line Labels Bottom Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            val labelData = listOf(
                                Pair("Mar 1-7 , 2026", "23, 100"),
                                Pair("Mar 8-14 , 2026", "44, 100"),
                                Pair("Mar 15-21 , 2026", "38, 000"),
                                Pair("Mar 22-30 , 2026", "9, 100"),
                                Pair("Apr 1-7 , 2026", "56, 000"),
                                Pair("Apr 8-14 , 2026", "35, 000")
                            )
                            labelData.forEach { (date, amount) ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.width(50.dp)
                                ) {
                                    Text(
                                        text = date,
                                        fontSize = 7.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 9.sp
                                    )
                                    Text(
                                        text = amount,
                                        fontSize = 7.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 9.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}