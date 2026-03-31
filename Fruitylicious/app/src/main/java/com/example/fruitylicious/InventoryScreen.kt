package com.example.fruitylicious

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch

// ── Data ──────────────────────────────────────────────────────────────────────

data class InventoryItem(
    val name: String,
    val sku: String,
    val category: String,
    val price: String,
    val stockLabel: String,
    val stockColor: Color,
    val progressFraction: Float,
    val progressColor: Color,
    val emoji: String,
    val emojiBg: Color
)

val sampleInventory = listOf(
    InventoryItem(
        name = "Mango", sku = "SKU-0041", category = "Fruits",
        price = "₱28", stockLabel = "3 boxes", stockColor = Color(0xFF2E7D32),
        progressFraction = 0.75f, progressColor = Color(0xFF4CAF50),
        emoji = "🥭", emojiBg = Color(0xFFFFF9C4)
    ),
    InventoryItem(
        name = "Avocado", sku = "SKU-0012", category = "Fruits",
        price = "₱38", stockLabel = "1 box", stockColor = Color(0xFFC62828),
        progressFraction = 0.20f, progressColor = Color(0xFFE53935),
        emoji = "🥑", emojiBg = Color(0xFFE8F5E9)
    ),
    InventoryItem(
        name = "Dragon fruit", sku = "SKU-0033", category = "Fruits",
        price = "₱12", stockLabel = "2 boxes", stockColor = Color(0xFFF57C00),
        progressFraction = 0.45f, progressColor = Color(0xFFFFA726),
        emoji = "🐉", emojiBg = Color(0xFFE8F5E9)
    ),
    InventoryItem(
        name = "Melon", sku = "SKU-0018", category = "Fruits",
        price = "₱12", stockLabel = "9kg", stockColor = Color(0xFFC62828),
        progressFraction = 0.10f, progressColor = Color(0xFFE53935),
        emoji = "🍈", emojiBg = Color(0xFFFCE4EC)
    ),
    InventoryItem(
        name = "Apple", sku = "SKU-0003", category = "Fruits",
        price = "₱35", stockLabel = "6 boxes", stockColor = Color(0xFF2E7D32),
        progressFraction = 0.60f, progressColor = Color(0xFF4CAF50),
        emoji = "🍎", emojiBg = Color(0xFFFBE9E7)
    ),
    InventoryItem(
        name = "Strawberry", sku = "SKU-0007", category = "Fruits",
        price = "₱45", stockLabel = "4 boxes", stockColor = Color(0xFF2E7D32),
        progressFraction = 0.50f, progressColor = Color(0xFF4CAF50),
        emoji = "🍓", emojiBg = Color(0xFFFCE4EC)
    ),
)

val filterTabs = listOf("All", "Fruits", "Utensils", "Add Ons", "Syrup", "Milk")

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun InventoryScreen(navController : NavController) {
    var selectedTab by remember { mutableStateOf("Fruits") }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val filtered = if (selectedTab == "All") sampleInventory
    else sampleInventory.filter { it.category == selectedTab }

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

            // ── Green Header ─────────────────────────────────────────────────────
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
                        Text(
                            "E1",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Inventory",
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "12 products • Just updated",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // ── Stat Cards Row ────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MiniStatCard(
                        label = "PRODUCTS", value = "12", emoji = "📦",
                        bg = Color(0xFFFFA000), modifier = Modifier.weight(1f)
                    )
                    MiniStatCard(
                        label = "STOCK UNITS", value = "4.5K", emoji = "📊",
                        bg = Color(0xFF388E3C), modifier = Modifier.weight(1f)
                    )
                    MiniStatCard(
                        label = "VALUE", value = "₱4K", emoji = "💰",
                        bg = Color(0xFF43A047), modifier = Modifier.weight(1f)
                    )
                    MiniStatCard(
                        label = "LOW STOCKS", value = "5", emoji = "⚠️",
                        bg = Color(0xFFC62828), modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── Filter Tabs ───────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFEAA0))
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filterTabs.forEach { tab ->
                    val selected = tab == selectedTab
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (selected) Color(0xFF2E7D32) else Color.White
                            )
                            .clickable { selectedTab = tab }
                            .padding(horizontal = 16.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = tab,
                            color = if (selected) Color.White else Color(0xFF555555),
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // ── Product List ──────────────────────────────────────────────────────
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                Text(
                    text = "ALL PRODUCTS",
                    color = Color(0xFFA07840),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                filtered.forEach { item ->
                    InventoryRow(item = item)
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // ── FAB ───────────────────────────────────────────────────────────────────
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .size(52.dp)
                    .background(Color(0xFFFFA000), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("+", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── Mini Stat Card ────────────────────────────────────────────────────────────

@Composable
fun MiniStatCard(
    label: String,
    value: String,
    emoji: String,
    bg: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(12.dp),
        color = bg
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            Column {
                Text(
                    text = label,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.3.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Text(
                text = emoji,
                fontSize = 22.sp,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }
}

// ── Inventory Row ─────────────────────────────────────────────────────────────

@Composable
fun InventoryRow(item: InventoryItem) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji icon box
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(item.emojiBg),
                contentAlignment = Alignment.Center
            ) {
                Text(text = item.emoji, fontSize = 28.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Name + SKU + progress bar
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B1B1B)
                )
                Text(
                    text = "${item.sku} • ${item.category}",
                    fontSize = 12.sp,
                    color = Color(0xFF888888)
                )
                Spacer(modifier = Modifier.height(6.dp))
                // Progress bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFE0E0E0))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(item.progressFraction)
                            .height(5.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(item.progressColor)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Price + stock badge
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = item.price,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B1B1B)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(item.stockColor)
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = item.stockLabel,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ── Preview ───────────────────────────────────────────────────────────────────


