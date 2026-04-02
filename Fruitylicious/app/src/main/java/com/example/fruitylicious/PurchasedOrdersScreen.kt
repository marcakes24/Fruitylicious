package com.example.fruitylicious

import androidx.compose.material3.DrawerState
import kotlinx.coroutines.CoroutineScope
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch

// ── Data ──────────────────────────────────────────────────────────────────────

data class PurchasedOrder(
    val orderId: String,
    val date: String,
    val customer: String,
    val size: String,
    val addOns: String,
    val quantity: String,
    val total: String
)

val sampleOrders = listOf(
    PurchasedOrder("PO-2026-038", "March 8 2026", "Maryrose", "Medium", "Pearl", "1x", "₱120"),
    PurchasedOrder("PO-2026-038", "March 8 2026", "Maryrose", "Medium", "Pearl", "1x", "₱120"),
    PurchasedOrder("PO-2026-038", "March 8 2026", "Maryrose", "Medium", "Pearl", "1x", "₱120"),
    PurchasedOrder("PO-2026-039", "March 9 2026", "Angela",   "Large",  "None",  "2x", "₱240"),
    PurchasedOrder("PO-2026-040", "March 9 2026", "Carlos",   "Small",  "Pearl", "1x", "₱95"),
)

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun PurchasedOrdersScreen(
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

        // ── Green Header ──────────────────────────────────────────────
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
                // Hamburger
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
                // Avatar
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
                text = "Purchased Orders",
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "3 ongoing • 12 completed",
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

            // ── Tab Row ───────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Complete Orders",
                    color = Color(0xFF2E7D32),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                // Filter icon (3 lines with dots)
                Column(
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    repeat(3) { i ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(if (i == 1) 14.dp else 20.dp)
                                    .height(2.dp)
                                    .background(Color(0xFF555555), RoundedCornerShape(1.dp))
                            )
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF555555))
                            )
                            Box(
                                modifier = Modifier
                                    .width(if (i == 1) 6.dp else 4.dp)
                                    .height(2.dp)
                                    .background(Color(0xFF555555), RoundedCornerShape(1.dp))
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── Order Cards ───────────────────────────────────────────
            sampleOrders.forEach { order ->
                OrderCard(order = order)
                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ── Order Card ────────────────────────────────────────────────────────────────

@Composable
fun OrderCard(order: PurchasedOrder) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Order ID
            Text(
                text = order.orderId,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B1B1B)
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Date + Customer
            Text(
                text = "${order.date} • ${order.customer}",
                fontSize = 12.sp,
                color = Color(0xFF2E7D32)
            )

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = Color(0xFFEEEEEE), thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Size row
            OrderDetailRow(label = "Size", value = order.size)
            Spacer(modifier = Modifier.height(4.dp))

            // Add Ons row
            OrderDetailRow(label = "Add Ons", value = order.addOns)
            Spacer(modifier = Modifier.height(4.dp))

            // Quantity row
            OrderDetailRow(label = "Quantity", value = order.quantity)

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = Color(0xFFEEEEEE), thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Order Total row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ORDER TOTAL",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFA07840),
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = order.total,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF2E7D32)
                )
            }
        }
    }
}

// ── Order Detail Row ──────────────────────────────────────────────────────────

@Composable
fun OrderDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = Color(0xFF555555)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            color = Color(0xFF1B1B1B),
            fontWeight = FontWeight.Medium
        )
    }
}

// ── Preview ───────────────────────────────────────────────────────────────────

